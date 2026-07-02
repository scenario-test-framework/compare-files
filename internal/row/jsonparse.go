package row

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strings"
)

// ParseJSONObject は JSON 文字列をキー順を保持した OrderedMap にパースします。
// Jackson の LinkedHashMap ベースのパース結果 (ドキュメント内のキー順保持) 相当です。
// JSON リテラル "null" は nil を返します。
func ParseJSONObject(s string) (*OrderedMap, error) {
	dec := json.NewDecoder(bytes.NewReader([]byte(s)))
	dec.UseNumber()

	v, err := decodeValue(dec)
	if err != nil {
		return nil, err
	}
	switch val := v.(type) {
	case nil:
		return nil, nil
	case *OrderedMap:
		return val, nil
	default:
		return nil, fmt.Errorf("JSON オブジェクトではありません: %s", trimForLog(s))
	}
}

func decodeValue(dec *json.Decoder) (any, error) {
	tok, err := dec.Token()
	if err != nil {
		return nil, err
	}
	return decodeFromToken(dec, tok)
}

func decodeFromToken(dec *json.Decoder, tok json.Token) (any, error) {
	switch t := tok.(type) {
	case json.Delim:
		switch t {
		case '{':
			m := NewOrderedMap()
			for dec.More() {
				keyTok, err := dec.Token()
				if err != nil {
					return nil, err
				}
				key, ok := keyTok.(string)
				if !ok {
					return nil, fmt.Errorf("オブジェクトキーが不正です: %v", keyTok)
				}
				val, err := decodeValue(dec)
				if err != nil {
					return nil, err
				}
				m.Put(key, val)
			}
			if _, err := dec.Token(); err != nil { // '}'
				return nil, err
			}
			return m, nil
		case '[':
			list := []any{}
			for dec.More() {
				val, err := decodeValue(dec)
				if err != nil {
					return nil, err
				}
				list = append(list, val)
			}
			if _, err := dec.Token(); err != nil { // ']'
				return nil, err
			}
			return list, nil
		default:
			return nil, fmt.Errorf("不正なデリミタ: %v", t)
		}
	case string:
		return t, nil
	case json.Number:
		return Number{Raw: string(t)}, nil
	case bool:
		return t, nil
	case nil:
		return nil, nil
	default:
		return nil, fmt.Errorf("不正なトークン: %v (%T)", tok, tok)
	}
}

// trimForLog はログ出力用に長い文字列を切り詰めます。
func trimForLog(s string) string {
	s = strings.TrimSpace(s)
	if len(s) > 200 {
		return s[:200] + "..."
	}
	return s
}
