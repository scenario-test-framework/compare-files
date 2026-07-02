package reader

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/row"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// layoutFromJSON はテスト用のレイアウトを ParseLayoutList 経由で生成します
// (path・value の合成 recordList 適用を含む)。
func layoutFromJSON(t *testing.T, layoutJSON string) *config.FileLayout {
	t.Helper()
	list, err := config.ParseLayoutList([]byte(layoutJSON))
	if err != nil {
		t.Fatalf("ParseLayoutList: %v", err)
	}
	return list.LayoutList[0]
}

func writeTemp(t *testing.T, name, content string) string {
	t.Helper()
	path := filepath.Join(t.TempDir(), name)
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}
	return path
}

func readAllRows(t *testing.T, r RowReader) []*row.Row {
	t.Helper()
	defer r.Close()
	var rows []*row.Row
	for {
		cur, err := r.Next()
		if err != nil {
			t.Fatalf("Next: %v", err)
		}
		if cur == nil {
			return rows
		}
		rows = append(rows, cur)
	}
}

func TestYamlReader(t *testing.T) {
	layout := layoutFromJSON(t, `{"layoutList": [{
		"fileRegexPattern": ".*\\.yaml",
		"logicalFileName": "yaml",
		"fileFormat": "Yaml",
		"charset": "UTF-8",
		"recordList": [{
			"type": "Data",
			"itemList": [
				{"id": "id", "compareKey": true},
				{"id": "user.name"},
				{"id": "tags"}
			]
		}]
	}]}`)
	path := writeTemp(t, "test.yaml", `
id: 100
user:
  name: alice
  age: 20
tags:
  - a
  - b
`)
	r, err := New(path, layout, Options{})
	if err != nil {
		t.Fatalf("New: %v", err)
	}
	rows := readAllRows(t, r)
	if len(rows) != 1 {
		t.Fatalf("rows = %d, want 1", len(rows))
	}
	if got := row.ToJavaString(rows[0].KeyMap.GetOrNil("id")); got != "100" {
		t.Errorf("id = %q", got)
	}
	user, _ := rows[0].ValueMap.GetOrNil("user").(*row.OrderedMap)
	if user == nil || row.ToJavaString(user.GetOrNil("name")) != "alice" {
		t.Errorf("user.name が取得できない: %v", rows[0].ValueMap.GetOrNil("user"))
	}
	if got := row.ToJavaString(rows[0].ValueMap.GetOrNil("tags")); got != "[a, b]" {
		t.Errorf("tags = %q", got)
	}
}

func TestYamlPathValueReader(t *testing.T) {
	layout := layoutFromJSON(t, `{"layoutList": [{
		"fileRegexPattern": ".*\\.yaml",
		"logicalFileName": "yaml-pv",
		"fileFormat": "Yaml",
		"charset": "UTF-8",
		"pathValueMode": "true"
	}]}`)
	path := writeTemp(t, "test.yaml", `
b: 2
a:
  - x
  - {nested: true}
empty: {}
`)
	r, err := New(path, layout, Options{})
	if err != nil {
		t.Fatalf("New: %v", err)
	}
	rows := readAllRows(t, r)
	got := map[string]string{}
	var paths []string
	for _, cur := range rows {
		p := row.ToJavaString(cur.KeyMap.GetOrNil("path"))
		got[p] = row.ToJavaString(cur.ValueMap.GetOrNil("value"))
		paths = append(paths, p)
	}
	want := map[string]string{
		"$.b":           "2",
		"$.a[0]":        "x",
		"$.a[1].nested": "true",
		"$.empty":       "{}",
	}
	for k, v := range want {
		if got[k] != v {
			t.Errorf("%s = %q, want %q (全体: %v)", k, got[k], v, got)
		}
	}
	// path 順にソートされていること
	for i := 1; i < len(paths); i++ {
		if paths[i-1] > paths[i] {
			t.Errorf("path がソートされていない: %v", paths)
		}
	}
}

func TestJSONPathValueReader(t *testing.T) {
	layout := layoutFromJSON(t, `{"layoutList": [{
		"fileRegexPattern": ".*\\.json",
		"logicalFileName": "json-pv",
		"fileFormat": "Json",
		"charset": "UTF-8",
		"pathValueMode": "true"
	}]}`)
	path := writeTemp(t, "test.json", `{"b": 1.5, "a": {"c": null, "日本語キー": "v"}}`)
	r, err := New(path, layout, Options{})
	if err != nil {
		t.Fatalf("New: %v", err)
	}
	rows := readAllRows(t, r)
	got := map[string]string{}
	for _, cur := range rows {
		got[row.ToJavaString(cur.KeyMap.GetOrNil("path"))] = row.ToJavaString(cur.ValueMap.GetOrNil("value"))
	}
	want := map[string]string{
		"$.b":          "1.5",
		"$.a.c":        "null",
		"$.a['日本語キー']": "v",
	}
	for k, v := range want {
		if got[k] != v {
			t.Errorf("%s = %q, want %q (全体: %v)", k, got[k], v, got)
		}
	}
}

func TestJSONListPathValueReader(t *testing.T) {
	layout := layoutFromJSON(t, `{"layoutList": [{
		"fileRegexPattern": ".*\\.jsonlist",
		"logicalFileName": "jsonlist-pv",
		"fileFormat": "JsonList",
		"charset": "UTF-8",
		"pathValueMode": "true"
	}]}`)
	path := writeTemp(t, "test.jsonlist", `{"id": 1}
{"id": 2, "name": "bob"}
`)
	r, err := New(path, layout, Options{})
	if err != nil {
		t.Fatalf("New: %v", err)
	}
	rows := readAllRows(t, r)
	got := map[string]string{}
	for _, cur := range rows {
		got[row.ToJavaString(cur.KeyMap.GetOrNil("path"))] = row.ToJavaString(cur.ValueMap.GetOrNil("value"))
	}
	want := map[string]string{
		"$[0].id":   "1",
		"$[1].id":   "2",
		"$[1].name": "bob",
	}
	for k, v := range want {
		if got[k] != v {
			t.Errorf("%s = %q, want %q (全体: %v)", k, got[k], v, got)
		}
	}
}

func TestXMLReader(t *testing.T) {
	layout := layoutFromJSON(t, `{"layoutList": [{
		"fileRegexPattern": ".*\\.xml",
		"logicalFileName": "xml",
		"fileFormat": "XML",
		"charset": "UTF-8"
	}]}`)
	path := writeTemp(t, "test.xml", `<?xml version="1.0" encoding="UTF-8"?>
<root>
  <item id="1">
    <name>alice</name>
    <memo></memo>
  </item>
  <item id="2">
    <name>bob</name>
  </item>
  <mixed>text<sub>s</sub></mixed>
</root>`)
	r, err := New(path, layout, Options{})
	if err != nil {
		t.Fatalf("New: %v", err)
	}
	rows := readAllRows(t, r)
	got := map[string]string{}
	for _, cur := range rows {
		got[row.ToJavaString(cur.KeyMap.GetOrNil("path"))] = row.ToJavaString(cur.ValueMap.GetOrNil("value"))
	}
	want := map[string]string{
		"/root[1]/item[1]/@id":        "1",
		"/root[1]/item[1]/name[1]":    "alice",
		"/root[1]/item[1]/memo[1]":    "",
		"/root[1]/item[2]/@id":        "2",
		"/root[1]/item[2]/name[1]":    "bob",
		"/root[1]/mixed[1]/sub[1]":    "s",
		"/root[1]/mixed[1]/text()[1]": "text",
	}
	if len(got) != len(want) {
		t.Errorf("ペア数 = %d, want %d (全体: %v)", len(got), len(want), got)
	}
	for k, v := range want {
		if gotV, ok := got[k]; !ok || gotV != v {
			t.Errorf("%s = %q (ok=%v), want %q", k, gotV, ok, v)
		}
	}
}

// mixed content のテキスト境界が保持されること (連結すると構造差分が潰れる)
func TestXMLReaderMixedTextRuns(t *testing.T) {
	layout := layoutFromJSON(t, `{"layoutList": [{
		"fileRegexPattern": ".*\\.xml",
		"fileFormat": "XML",
		"charset": "UTF-8"
	}]}`)
	path := writeTemp(t, "runs.xml", `<root><mixed>a<sub/>b</mixed></root>`)
	r, err := New(path, layout, Options{})
	if err != nil {
		t.Fatalf("New: %v", err)
	}
	rows := readAllRows(t, r)
	got := map[string]string{}
	for _, cur := range rows {
		got[row.ToJavaString(cur.KeyMap.GetOrNil("path"))] = row.ToJavaString(cur.ValueMap.GetOrNil("value"))
	}
	want := map[string]string{
		"/root[1]/mixed[1]/text()[1]": "a",
		"/root[1]/mixed[1]/sub[1]":    "",
		"/root[1]/mixed[1]/text()[2]": "b",
	}
	if len(got) != len(want) {
		t.Errorf("ペア数 = %d, want %d (全体: %v)", len(got), len(want), got)
	}
	for k, v := range want {
		if gotV, ok := got[k]; !ok || gotV != v {
			t.Errorf("%s = %q (ok=%v), want %q", k, gotV, ok, v)
		}
	}
}

func TestPathValueOnlyForSupportedFormats(t *testing.T) {
	// CSV に pathValueMode を指定しても path・value にはならない
	layout := layoutFromJSON(t, `{"layoutList": [{
		"fileRegexPattern": ".*\\.csv",
		"fileFormat": "CSV_withHeader",
		"charset": "UTF-8",
		"pathValueMode": "true",
		"recordList": [{"type": "Data", "itemList": [{"id": "id", "compareKey": true}]}]
	}]}`)
	if layout.IsPathValue() {
		t.Error("CSV で IsPathValue() = true になってはいけない")
	}
	if layout.RecordList[0].ItemList[0].ID != "id" {
		t.Error("CSV の recordList が置き換えられている")
	}
}

func TestXMLLayoutRecordListReplaced(t *testing.T) {
	layout := layoutFromJSON(t, `{"layoutList": [{
		"fileRegexPattern": ".*\\.xml",
		"fileFormat": "XML",
		"charset": "UTF-8"
	}]}`)
	if !layout.IsPathValue() {
		t.Fatal("XML は常に IsPathValue() = true")
	}
	if len(layout.RecordList) != 1 {
		t.Fatalf("recordList = %d 件", len(layout.RecordList))
	}
	items := layout.RecordList[0].ItemList
	if len(items) != 2 || items[0].ID != "path" || !items[0].CompareKey || items[1].ID != "value" {
		t.Errorf("合成 recordList が不正: %+v", items)
	}
	if status.RecordData != layout.RecordList[0].Type {
		t.Errorf("type = %s", layout.RecordList[0].Type)
	}
}
