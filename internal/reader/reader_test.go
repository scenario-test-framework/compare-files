package reader

import (
	"path/filepath"
	"testing"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/row"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// リポジトリ内の既存テストデータ (Java 版と共用)
const testInputDir = "../../testdata/main/CompareFilesTest/input/left"

func loadSampleLayouts(t *testing.T) *config.LayoutManager {
	t.Helper()
	m := config.NewLayoutManager()
	if err := m.AddLayoutFile("../../internal/assets/compare_layout/sample_text.json"); err != nil {
		t.Fatal(err)
	}
	return m
}

func readAll(t *testing.T, r RowReader) []*row.Row {
	t.Helper()
	defer r.Close()
	var rows []*row.Row
	for {
		rw, err := r.Next()
		if err != nil {
			t.Fatal(err)
		}
		if rw == nil {
			return rows
		}
		rows = append(rows, rw)
	}
}

func TestTextReader(t *testing.T) {
	path := filepath.Join(testInputDir, "TEXT_PLAINTEXT", "plaintext_ok.txt")
	r, err := New(path, nil, Options{})
	if err != nil {
		t.Fatal(err)
	}
	rows := readAll(t, r)
	if len(rows) == 0 {
		t.Fatal("行が読めていない")
	}
	first := rows[0]
	if first.RowNum != 1 {
		t.Errorf("rowNum: %d", first.RowNum)
	}
	if _, ok := first.KeyMap.Get("dummy"); !ok {
		t.Error("テキスト形式の keyMap は dummy キーを持つはず")
	}
	if v := first.ValueMap.GetOrNil(config.DefaultItemID); v != first.RawLine {
		t.Errorf("value=%v rawLine=%q", v, first.RawLine)
	}
}

func TestCsvReaderWithLayout(t *testing.T) {
	m := loadSampleLayouts(t)
	path := filepath.Join(testInputDir, "TEXT_CSV", "csv_with-header_ok.csv")
	layout := m.GetLayout(filepath.Base(path), nil)
	if layout == nil || layout.FileFormat != status.FormatCSVWithHeader {
		t.Fatalf("レイアウトが解決できない: %+v", layout)
	}
	r, err := New(path, layout, Options{CsvHeaderRow: 1, CsvDataStartRow: 2})
	if err != nil {
		t.Fatal(err)
	}
	rows := readAll(t, r)
	if len(rows) == 0 {
		t.Fatal("行が読めていない")
	}
	first := rows[0]
	if first.RowNum != 2 {
		t.Errorf("ヘッダー付き CSV の先頭データ行は物理行番号 2 のはず: %d", first.RowNum)
	}
	if v := first.KeyMap.GetOrNil("KEY1"); v != "100" {
		t.Errorf("KEY1: %v", v)
	}
	if keys := first.KeyMap.Keys(); len(keys) != 2 || keys[0] != "KEY1" || keys[1] != "KEY2" {
		t.Errorf("キー項目: %v", keys)
	}
	if v, ok := first.ValueMap.Get("AMOUNT"); !ok || v == "" {
		t.Errorf("AMOUNT: %v", v)
	}
	if first.RawLine[0] != '"' {
		t.Errorf("rawLine は全項目クォート再構成のはず: %q", first.RawLine)
	}
}

func TestFixedReaderMultiRecordType(t *testing.T) {
	m := loadSampleLayouts(t)
	path := filepath.Join(testInputDir, "TEXT_FIXED", "fixed_multi-record-type_ok.txt")
	layout := m.GetLayout(filepath.Base(path), nil)
	if layout == nil || layout.FileFormat != status.FormatFixed {
		t.Fatalf("レイアウトが解決できない: %+v", layout)
	}
	r, err := New(path, layout, Options{CodeValueForOnlyOneRecordType: "-"})
	if err != nil {
		t.Fatal(err)
	}
	rows := readAll(t, r)
	if len(rows) < 3 {
		t.Fatalf("行数: %d", len(rows))
	}
	// 1 行目は Header レコード (RECORD_TYPE=0)
	header := rows[0]
	if v := header.KeyMap.GetOrNil("RECORD_TYPE"); v != "0" {
		t.Errorf("Header RECORD_TYPE: %v", v)
	}
	if v := header.ValueMap.GetOrNil("DATA_COUNT"); v != "0000000009" {
		t.Errorf("DATA_COUNT: %v", v)
	}
	// 2 行目は Data レコード (RECORD_TYPE=1, KEY1=100, KEY2=AAA)
	data := rows[1]
	if v := data.KeyMap.GetOrNil("KEY1"); v != "100" {
		t.Errorf("Data KEY1: %v", v)
	}
	if v := data.KeyMap.GetOrNil("KEY2"); v != "AAA" {
		t.Errorf("Data KEY2: %v", v)
	}
}

func TestJsonListReaderWithLayout(t *testing.T) {
	m := loadSampleLayouts(t)
	path := filepath.Join(testInputDir, "TEXT_JSONLIST", "jsonlist_ok.json")
	layout := m.GetLayout(filepath.Base(path), nil)
	if layout == nil || layout.FileFormat != status.FormatJSONList {
		t.Fatalf("レイアウトが解決できない: %+v", layout)
	}
	r, err := New(path, layout, Options{})
	if err != nil {
		t.Fatal(err)
	}
	rows := readAll(t, r)
	if len(rows) != 3 {
		t.Fatalf("行数: %d", len(rows))
	}
	first := rows[0]
	// keyObj.key1 / keyObj.key2 が比較キー
	keyObj, ok := first.KeyMap.GetOrNil("keyObj").(*row.OrderedMap)
	if !ok {
		t.Fatalf("keyObj が Map でない: %v", first.KeyMap.GetOrNil("keyObj"))
	}
	if v := row.ToJavaString(keyObj.GetOrNil("key1")); v != "100" {
		t.Errorf("key1: %v", v)
	}
	if v := row.ToJavaString(keyObj.GetOrNil("key2")); v != "AAA" {
		t.Errorf("key2: %v", v)
	}
	valueObj, ok := first.ValueMap.GetOrNil("valueObj").(*row.OrderedMap)
	if !ok {
		t.Fatalf("valueObj が Map でない")
	}
	if v := row.ToJavaString(valueObj.GetOrNil("amount")); v != "1000" {
		t.Errorf("amount: %v", v)
	}
}

func TestJsonReaderNestedList(t *testing.T) {
	m := loadSampleLayouts(t)
	path := filepath.Join(testInputDir, "TEXT_JSON", "json_ok.json")
	layout := m.GetLayout(filepath.Base(path), nil)
	if layout == nil || layout.FileFormat != status.FormatJSON {
		t.Fatalf("レイアウトが解決できない: %+v", layout)
	}
	r, err := New(path, layout, Options{})
	if err != nil {
		t.Fatal(err)
	}
	rows := readAll(t, r)
	if len(rows) != 1 {
		t.Fatalf("Json 形式は 1 レコードのはず: %d", len(rows))
	}
	// valueList.content.month のような List 越しの階層コピーを確認
	first := rows[0]
	valueList, ok := first.ValueMap.GetOrNil("valueList").([]any)
	if !ok {
		t.Fatalf("valueList が List でない: %T", first.ValueMap.GetOrNil("valueList"))
	}
	if len(valueList) == 0 {
		t.Fatal("valueList が空")
	}
}

func TestTsvReader(t *testing.T) {
	m := loadSampleLayouts(t)
	path := filepath.Join(testInputDir, "TEXT_TSV", "tsv_ok.tsv")
	layout := m.GetLayout(filepath.Base(path), nil)
	if layout == nil {
		t.Fatalf("レイアウトが解決できない")
	}
	r, err := New(path, layout, Options{CsvHeaderRow: 1, CsvDataStartRow: 2})
	if err != nil {
		t.Fatal(err)
	}
	rows := readAll(t, r)
	if len(rows) == 0 {
		t.Fatal("行が読めていない")
	}
}
