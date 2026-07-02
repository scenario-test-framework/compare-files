// Package row は比較対象の行データモデルを提供します。
// Java 版 sv/domain/BaseRow と、その値型 (Jackson がパースした
// LinkedHashMap/ArrayList/Integer/Double/String/Boolean) 相当を表現します。
package row

// Row は 1 行分のデータです。Java 版 BaseRow 相当。
type Row struct {
	RowNum   int64
	KeyMap   *OrderedMap // 比較キー項目 (挿入順保持)
	ValueMap *OrderedMap // キー以外の項目 (挿入順保持)
	RawLine  string      // 元ファイルの無変換行データ
}

// OrderedMap は挿入順を保持するマップです。Java の LinkedHashMap 相当。
// 値は string / Number / bool / nil / []any / *OrderedMap のいずれかです。
type OrderedMap struct {
	keys   []string
	values map[string]any
}

// NewOrderedMap は空の OrderedMap を返します。
func NewOrderedMap() *OrderedMap {
	return &OrderedMap{values: map[string]any{}}
}

// Put は値を設定します。既存キーは値のみ更新し、挿入順は変わりません
// (LinkedHashMap のデフォルト動作と同じ)。
func (m *OrderedMap) Put(key string, value any) {
	if _, exists := m.values[key]; !exists {
		m.keys = append(m.keys, key)
	}
	m.values[key] = value
}

// Get は値と存在有無を返します。
func (m *OrderedMap) Get(key string) (any, bool) {
	v, ok := m.values[key]
	return v, ok
}

// GetOrNil は値を返します(存在しない場合は nil)。
func (m *OrderedMap) GetOrNil(key string) any {
	return m.values[key]
}

// Delete はキーを削除します。
func (m *OrderedMap) Delete(key string) {
	if _, exists := m.values[key]; !exists {
		return
	}
	delete(m.values, key)
	for i, k := range m.keys {
		if k == key {
			m.keys = append(m.keys[:i], m.keys[i+1:]...)
			break
		}
	}
}

// Keys は挿入順のキーを返します。
func (m *OrderedMap) Keys() []string {
	return m.keys
}

// Len は要素数を返します。
func (m *OrderedMap) Len() int {
	return len(m.keys)
}
