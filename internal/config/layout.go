package config

import (
	"encoding/json"
	"fmt"

	"github.com/scenario-test-framework/compare-files/internal/status"
)

// ItemLayout は項目レイアウト設定です。
type ItemLayout struct {
	ID         string                 `json:"id"`
	Name       string                 `json:"name"`
	ByteLength JInt                   `json:"byteLength"`
	CompareKey JBool                  `json:"compareKey"`
	Criteria   status.CompareCriteria `json:"criteria"`
}

// RecordLayout はレコードレイアウト設定です。
type RecordLayout struct {
	Type       status.RecordType `json:"type"`
	CodeValue  string            `json:"codeValue"`
	ItemList   []*ItemLayout     `json:"itemList"`
	ByteLength JInt              `json:"byteLength"`
}

// RecordByteLength は 1 レコードのバイト長を返します(未設定なら項目バイト長の合計)。
// 左右ソートの並走間でレイアウトを共有するため、フィールドは変更しません。
func (r *RecordLayout) RecordByteLength() int {
	if r.ByteLength > 0 {
		return int(r.ByteLength)
	}
	total := 0
	for _, item := range r.ItemList {
		total += int(item.ByteLength)
	}
	return total
}

// CompareKeyItemList は比較キー項目 ID のリストを返します。
func (r *RecordLayout) CompareKeyItemList() []string {
	var keys []string
	for _, item := range r.ItemList {
		if item.CompareKey {
			keys = append(keys, item.ID)
		}
	}
	return keys
}

// CompareItemNameList は全項目 ID のリストを返します。
func (r *RecordLayout) CompareItemNameList() []string {
	var names []string
	for _, item := range r.ItemList {
		names = append(names, item.ID)
	}
	return names
}

// CriteriaMap は項目 ID → 比較条件のマップと、定義順の項目 ID リストを返します。
func (r *RecordLayout) CriteriaMap() (map[string]status.CompareCriteria, []string) {
	m := make(map[string]status.CompareCriteria, len(r.ItemList))
	order := make([]string, 0, len(r.ItemList))
	for _, item := range r.ItemList {
		m[item.ID] = item.Criteria
		order = append(order, item.ID)
	}
	return m, order
}

// FileLayout はファイルレイアウト設定です。
type FileLayout struct {
	FileRegexPattern string            `json:"fileRegexPattern"`
	LogicalFileName  string            `json:"logicalFileName"`
	FileFormat       status.FileFormat `json:"fileFormat"`
	Charset          string            `json:"charset"`
	LineSp           status.LineSp     `json:"lineSp"`
	RecordList       []*RecordLayout   `json:"recordList"`
	IgnoreAreaList   []Rectangle       `json:"ignoreAreaList"`
}

// FileLayoutList はレイアウト定義ファイルのルート構造です。
type FileLayoutList struct {
	LayoutList []*FileLayout `json:"layoutList"`
}

// ParseLayoutFile はレイアウト定義 JSON(UTF-8)をパースします。
func ParseLayoutList(data []byte) (*FileLayoutList, error) {
	var list FileLayoutList
	if err := json.Unmarshal(data, &list); err != nil {
		return nil, err
	}
	return &list, nil
}

// Copy はレイアウトのディープコピーを返します。
// Java 版は項目オブジェクトを共有していましたが、除外項目適用(updateIgnore)の結果は
// 実行中の設定が不変であるため同一になります。
func (l *FileLayout) Copy() *FileLayout {
	cp := *l
	if l.RecordList != nil {
		cp.RecordList = make([]*RecordLayout, len(l.RecordList))
		for i, r := range l.RecordList {
			rc := *r
			rc.ItemList = make([]*ItemLayout, len(r.ItemList))
			for j, item := range r.ItemList {
				ic := *item
				rc.ItemList[j] = &ic
			}
			cp.RecordList[i] = &rc
		}
	}
	if l.IgnoreAreaList != nil {
		cp.IgnoreAreaList = append([]Rectangle(nil), l.IgnoreAreaList...)
	}
	return &cp
}

// FirstRecordByteLength はレコードレイアウト 1 件目のバイト長を返します。
func (l *FileLayout) FirstRecordByteLength() (int, error) {
	if len(l.RecordList) == 0 {
		return 0, fmt.Errorf("recordList が設定されていません")
	}
	return l.RecordList[0].RecordByteLength(), nil
}

// RecordPattern はレコードリストの構成からレコードパターンを判定します。
func (l *FileLayout) RecordPattern() status.RecordPattern {
	switch {
	case len(l.RecordList) == 0:
		return status.PatternNone
	case len(l.RecordList) == 1 && l.RecordList[0].Type == status.RecordData:
		return status.PatternDataOnly
	case len(l.RecordList) == 2:
		hasHeader, hasData := false, false
		for _, r := range l.RecordList {
			switch r.Type {
			case status.RecordHeader:
				hasHeader = true
			case status.RecordData:
				hasData = true
			}
		}
		if hasHeader && hasData {
			return status.PatternHeaderData
		}
	}
	return status.PatternMulti
}

// DefaultTextLayout はテキストファイルのデフォルトレイアウトを返します。
func DefaultTextLayout() *FileLayout {
	return &FileLayout{
		LogicalFileName: DummyValue,
		FileFormat:      status.FormatText,
		Charset:         ConfigCharset,
	}
}

// DefaultImageLayout は画像ファイルのデフォルトレイアウトを返します。
// システム設定の除外エリアを引き継ぎます。
func DefaultImageLayout(systemConfig *CompareFilesConfig) *FileLayout {
	layout := &FileLayout{
		LogicalFileName: DummyValue,
		FileFormat:      status.FormatImage,
	}
	if systemConfig != nil {
		updateIgnore(systemConfig, layout)
	}
	return layout
}
