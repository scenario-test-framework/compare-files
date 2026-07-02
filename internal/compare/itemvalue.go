package compare

import (
	"strings"

	"github.com/scenario-test-framework/compare-files/internal/row"
)

// GetItemValue は行データから指定項目 ID の値を文字列で返します。
// Java 版 ComparableRow.getItemValue 相当。
// キー項目 → その他項目の順に走査し、見つからない場合は nil を返します。
func GetItemValue(r *row.Row, itemID string) *string {
	if value := getItemValueMain(r.KeyMap, itemID); value != nil {
		return value
	}
	return getItemValueMain(r.ValueMap, itemID)
}

const keyDummy = "dummy"

// getItemValueMain はドット記法の項目 ID を再帰的に解決します。
func getItemValueMain(contentMap *row.OrderedMap, itemID string) *string {
	if contentMap == nil {
		return nil
	}

	dotIdx := strings.Index(itemID, ".")
	if dotIdx < 0 {
		// ドットなし: 直接取得
		valueObj, ok := contentMap.Get(itemID)
		if !ok || valueObj == nil {
			return nil
		}
		s := row.ToJavaString(valueObj)
		return &s
	}

	parentItemID := itemID[:dotIdx]
	childItemID := itemID[dotIdx+1:]
	parentObj := contentMap.GetOrNil(parentItemID)
	if parentObj == nil {
		return nil
	}

	switch parent := parentObj.(type) {
	case *row.OrderedMap:
		return getItemValueMain(parent, childItemID)
	case []any:
		// 各要素をダミー Map に詰めて再帰し、結果リストの toString を返す
		// (Java 版: List<Object> に文字列を集めて List.toString)
		results := make([]any, 0, len(parent))
		for _, sub := range parent {
			dummyMap := row.NewOrderedMap()
			dummyMap.Put(keyDummy, sub)
			child := getItemValueMain(dummyMap, keyDummy+"."+childItemID)
			if child == nil {
				results = append(results, nil)
			} else {
				results = append(results, *child)
			}
		}
		s := row.ToJavaString(results)
		return &s
	default:
		// スカラー: toString を返す
		s := row.ToJavaString(parentObj)
		return &s
	}
}
