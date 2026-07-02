package compare

import (
	"fmt"
	"unicode/utf16"

	"github.com/scenario-test-framework/compare-files/internal/row"
)

// DeepCompare はオブジェクトツリーのソート向け大小比較です。
// Java 版 CompareUtils.deepCompare 相当。null は最小の値として扱います。
// 判定順序 (Number → Comparable → List → Map) も Java 版と同一です。
func DeepCompare(left, right any) (int, error) {
	switch {
	case left == nil && right == nil:
		return 0, nil
	case left == nil:
		return -1, nil
	case right == nil:
		return 1, nil
	}

	switch l := left.(type) {
	case row.Number:
		// Number: 両辺を BigDecimal (任意精度) として比較
		leftVal, err := parseBigDecimal(l.String())
		if err != nil {
			return 0, err
		}
		rightVal, err := parseBigDecimal(row.ToJavaString(right))
		if err != nil {
			return 0, err
		}
		return leftVal.Cmp(rightVal), nil

	case string:
		// String.compareTo: UTF-16 コード単位の辞書順
		r, ok := right.(string)
		if !ok {
			return 0, fmt.Errorf("string と %T は比較できません", right)
		}
		return compareJavaString(l, r), nil

	case bool:
		// Boolean.compareTo: false < true
		r, ok := right.(bool)
		if !ok {
			return 0, fmt.Errorf("bool と %T は比較できません", right)
		}
		switch {
		case l == r:
			return 0, nil
		case !l:
			return -1, nil
		default:
			return 1, nil
		}

	case []any:
		r, ok := right.([]any)
		if !ok {
			return 0, fmt.Errorf("list と %T は比較できません", right)
		}
		smaller := len(l)
		if len(r) < smaller {
			smaller = len(r)
		}
		for i := 0; i < smaller; i++ {
			result, err := DeepCompare(l[i], r[i])
			if err != nil {
				return 0, err
			}
			if result != 0 {
				return result, nil
			}
		}
		switch {
		case len(l) == len(r):
			return 0, nil
		case len(l) < len(r):
			return -1, nil
		default:
			return 1, nil
		}

	case *row.OrderedMap:
		r, ok := right.(*row.OrderedMap)
		if !ok {
			return 0, fmt.Errorf("map と %T は比較できません", right)
		}
		// 小さい方 (同サイズなら左) の Map のキー順で比較
		loopBase := l
		if r.Len() < l.Len() {
			loopBase = r
		}
		for _, key := range loopBase.Keys() {
			result, err := DeepCompare(l.GetOrNil(key), r.GetOrNil(key))
			if err != nil {
				return 0, err
			}
			if result != 0 {
				return result, nil
			}
		}
		switch {
		case l.Len() == r.Len():
			return 0, nil
		case l.Len() < r.Len():
			return -1, nil
		default:
			return 1, nil
		}

	default:
		return 0, fmt.Errorf("比較できない型です: %T", left)
	}
}

// compareJavaString は Java String.compareTo と同じ UTF-16 コード単位順の比較です。
func compareJavaString(a, b string) int {
	ua := utf16.Encode([]rune(a))
	ub := utf16.Encode([]rune(b))
	for i := 0; i < len(ua) && i < len(ub); i++ {
		if ua[i] != ub[i] {
			if ua[i] < ub[i] {
				return -1
			}
			return 1
		}
	}
	return len(ua) - len(ub)
}

// CompareRowKeys は行データの比較キーで大小比較します。
// Java 版 ComparableRow.compareTo 相当 (keyMap のみ比較)。
func CompareRowKeys(left, right *row.Row) (int, error) {
	if right == nil {
		return -1, nil
	}
	return DeepCompare(left.KeyMap, right.KeyMap)
}

// CompareRowsForSort はソート用の行比較です。
// Java 版 SortableRow.compareTo 相当 (keyMap → 同値なら valueMap も比較)。
func CompareRowsForSort(left, right *row.Row) (int, error) {
	if right == nil {
		return -1, nil
	}
	result, err := DeepCompare(left.KeyMap, right.KeyMap)
	if err != nil || result != 0 {
		return result, err
	}
	return DeepCompare(left.ValueMap, right.ValueMap)
}
