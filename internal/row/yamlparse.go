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
		// merge key (<<) より明示キーが優先されるため、先に明示キーを収集する
		explicit := map[string]bool{}
		for i := 0; i+1 < len(n.Content); i += 2 {
			if !isYamlMergeKey(n.Content[i]) {
				explicit[n.Content[i].Value] = true
			}
		}
		m := NewOrderedMap()
		for i := 0; i+1 < len(n.Content); i += 2 {
			keyNode := n.Content[i]
			if isYamlMergeKey(keyNode) {
				if err := applyYamlMerge(m, n.Content[i+1], explicit); err != nil {
					return nil, err
				}
				continue
			}
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

// isYamlMergeKey は YAML merge key (<<) かを返します。
// yaml.v3 はプレーンな << を !!merge タグに解決します (引用された "<<" は通常キー)。
func isYamlMergeKey(n *yaml.Node) bool {
	return n.Kind == yaml.ScalarNode && n.Tag == "!!merge"
}

// applyYamlMerge は merge key (<<) の値を YAML merge セマンティクスで展開します。
//   - マッピング内の明示キーが merge されたキーより常に優先される
//   - 値がシーケンスの場合、先に書かれたマッピングのキーが優先される
//
// 展開されたキーは merge key の位置に挿入されます。
func applyYamlMerge(m *OrderedMap, valNode *yaml.Node, explicit map[string]bool) error {
	sources := []*yaml.Node{valNode}
	resolved := valNode
	if resolved.Kind == yaml.AliasNode {
		resolved = resolved.Alias
	}
	if resolved.Kind == yaml.SequenceNode {
		sources = resolved.Content
	}
	for _, src := range sources {
		v, err := yamlNodeValue(src)
		if err != nil {
			return err
		}
		mm, ok := v.(*OrderedMap)
		if !ok {
			return fmt.Errorf("merge key (<<) の値はマッピングで指定してください: 行=%d", valNode.Line)
		}
		for _, key := range mm.Keys() {
			if explicit[key] {
				continue
			}
			if _, exists := m.Get(key); exists {
				// 先に merge されたキーが優先される
				continue
			}
			m.Put(key, mm.GetOrNil(key))
		}
	}
	return nil
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
