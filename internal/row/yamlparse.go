package row

import (
	"fmt"
	"strconv"
	"strings"

	"gopkg.in/yaml.v3"
)

// ParseYAMLObject は YAML 文字列をキー順を保持した OrderedMap にパースします。
// JSON パース (ParseJSONObject) と同じ値型 (string / Number / bool / nil /
// []any / *OrderedMap) に変換します。空ドキュメント・null は nil を返します。
func ParseYAMLObject(s string) (*OrderedMap, error) {
	var node yaml.Node
	if err := yaml.Unmarshal([]byte(s), &node); err != nil {
		return nil, err
	}
	if node.Kind == 0 {
		// 空ドキュメント
		return nil, nil
	}
	v, err := yamlNodeValue(&node)
	if err != nil {
		return nil, err
	}
	switch val := v.(type) {
	case nil:
		return nil, nil
	case *OrderedMap:
		return val, nil
	default:
		return nil, fmt.Errorf("YAML マッピングではありません: %s", trimForLog(s))
	}
}

// yamlNodeValue は yaml.Node を比較用の値型に変換します。
func yamlNodeValue(n *yaml.Node) (any, error) {
	switch n.Kind {
	case yaml.DocumentNode:
		if len(n.Content) == 0 {
			return nil, nil
		}
		return yamlNodeValue(n.Content[0])

	case yaml.MappingNode:
		m := NewOrderedMap()
		for i := 0; i+1 < len(n.Content); i += 2 {
			keyNode := n.Content[i]
			key, err := yamlScalarKey(keyNode)
			if err != nil {
				return nil, err
			}
			val, err := yamlNodeValue(n.Content[i+1])
			if err != nil {
				return nil, err
			}
			m.Put(key, val)
		}
		return m, nil

	case yaml.SequenceNode:
		list := []any{}
		for _, item := range n.Content {
			val, err := yamlNodeValue(item)
			if err != nil {
				return nil, err
			}
			list = append(list, val)
		}
		return list, nil

	case yaml.AliasNode:
		return yamlNodeValue(n.Alias)

	case yaml.ScalarNode:
		return yamlScalarValue(n)

	default:
		return nil, fmt.Errorf("不正な YAML ノードです: kind=%d 行=%d", n.Kind, n.Line)
	}
}

// yamlScalarKey はマッピングキーを文字列として返します。
func yamlScalarKey(n *yaml.Node) (string, error) {
	if n.Kind != yaml.ScalarNode {
		return "", fmt.Errorf("マッピングキーはスカラーで指定してください: 行=%d", n.Line)
	}
	return n.Value, nil
}

// yamlScalarValue はスカラーノードを値型に変換します。
func yamlScalarValue(n *yaml.Node) (any, error) {
	switch n.Tag {
	case "!!null":
		return nil, nil
	case "!!bool":
		var b bool
		if err := n.Decode(&b); err != nil {
			return nil, fmt.Errorf("bool に変換できません: %q 行=%d: %w", n.Value, n.Line, err)
		}
		return b, nil
	case "!!int":
		// 10 進リテラルは元の表記を保持し、16 進などは 10 進に正規化する
		if _, err := strconv.ParseInt(strings.TrimPrefix(n.Value, "+"), 10, 64); err == nil {
			return Number{Raw: strings.TrimPrefix(n.Value, "+")}, nil
		}
		var i int64
		if err := n.Decode(&i); err != nil {
			return nil, fmt.Errorf("int に変換できません: %q 行=%d: %w", n.Value, n.Line, err)
		}
		return Number{Raw: strconv.FormatInt(i, 10)}, nil
	case "!!float":
		if _, err := strconv.ParseFloat(n.Value, 64); err == nil {
			return Number{Raw: n.Value}, nil
		}
		var f float64
		if err := n.Decode(&f); err != nil {
			return nil, fmt.Errorf("float に変換できません: %q 行=%d: %w", n.Value, n.Line, err)
		}
		return Number{Raw: strconv.FormatFloat(f, 'g', -1, 64)}, nil
	default:
		// !!str / !!timestamp / カスタムタグは文字列として扱う
		return n.Value, nil
	}
}
