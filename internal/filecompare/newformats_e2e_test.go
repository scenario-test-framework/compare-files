package filecompare

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// 新形式 (Yaml / XML / pathValueMode) のエンドツーエンド検証。

func runNewFormatCompare(t *testing.T, layoutJSON, fileName, leftContent, rightContent string) (*Result, string) {
	t.Helper()
	cfg := defaultTestConfig(t)

	lm := config.NewLayoutManager()
	if err := lm.AddLayoutData([]byte(layoutJSON), "test-layout"); err != nil {
		t.Fatal(err)
	}
	layout := lm.GetLayout(fileName, cfg)
	if layout == nil {
		t.Fatalf("レイアウトが解決できない: %s", fileName)
	}

	dir := t.TempDir()
	leftPath := filepath.Join(dir, "left", fileName)
	rightPath := filepath.Join(dir, "right", fileName)
	for path, content := range map[string]string{leftPath: leftContent, rightPath: rightContent} {
		if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
			t.Fatal(err)
		}
		if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
			t.Fatal(err)
		}
	}

	outputDir := t.TempDir()
	result, err := CompareText(leftPath, rightPath, layout, outputDir, cfg)
	if err != nil {
		t.Fatalf("CompareText: %v", err)
	}
	outputName := OutputFileName(leftPath, rightPath, cfg, "csv")
	data, err := os.ReadFile(filepath.Join(outputDir, outputName))
	if err != nil {
		t.Fatalf("詳細ファイルが出力されていない: %v", err)
	}
	return result, string(data)
}

func TestCompareYamlWithLayout(t *testing.T) {
	layoutJSON := `{"layoutList": [{
		"fileRegexPattern": ".*\\.yaml",
		"logicalFileName": "yaml-test",
		"fileFormat": "Yaml",
		"charset": "UTF-8",
		"recordList": [{
			"type": "Data",
			"itemList": [
				{"id": "id", "compareKey": true},
				{"id": "user.name"},
				{"id": "count"}
			]
		}]
	}]}`
	left := "id: 1\nuser:\n  name: alice\ncount: 3\n"
	right := "id: 1\nuser:\n  name: bob\ncount: 3\n"

	result, detail := runNewFormatCompare(t, layoutJSON, "data.yaml", left, right)
	if result.Status != status.CompareNG {
		t.Errorf("Status = %s, want NG", result.Status)
	}
	if !strings.Contains(detail, "user.name") {
		t.Errorf("詳細に user.name の差分がない:\n%s", detail)
	}
	if !strings.Contains(detail, "期待値:alice\n実績値:bob") {
		t.Errorf("値の差分表記が不正:\n%s", detail)
	}

	// 同一内容なら OK
	result, _ = runNewFormatCompare(t, layoutJSON, "data.yaml", left, left)
	if result.Status != status.CompareOK {
		t.Errorf("同一 YAML の Status = %s, want OK", result.Status)
	}
}

func TestCompareXML(t *testing.T) {
	layoutJSON := `{"layoutList": [{
		"fileRegexPattern": ".*\\.xml",
		"logicalFileName": "xml-test",
		"fileFormat": "XML",
		"charset": "UTF-8"
	}]}`
	left := `<root><item id="1"><name>alice</name></item><item id="2"><name>bob</name></item></root>`
	right := `<root><item id="1"><name>alice</name></item><item id="2"><name>bobby</name><extra>x</extra></item></root>`

	result, detail := runNewFormatCompare(t, layoutJSON, "data.xml", left, right)
	if result.Status != status.CompareNG {
		t.Errorf("Status = %s, want NG", result.Status)
	}
	// ヘッダーは path・value の 2 項目
	header := strings.SplitN(detail, "\n", 2)[0]
	if !strings.Contains(header, `"path"`) || !strings.Contains(header, `"value"`) {
		t.Errorf("ヘッダーが path・value でない: %s", header)
	}
	// name[1] の差分
	if !strings.Contains(detail, "/root[1]/item[2]/name[1]") {
		t.Errorf("差分パスがない:\n%s", detail)
	}
	if !strings.Contains(detail, "期待値:bob\n実績値:bobby") {
		t.Errorf("値の差分表記が不正:\n%s", detail)
	}
	// 右のみの要素は RightOnly
	if !strings.Contains(detail, "RightOnly") || !strings.Contains(detail, "/root[1]/item[2]/extra[1]") {
		t.Errorf("RightOnly の行がない:\n%s", detail)
	}

	result, _ = runNewFormatCompare(t, layoutJSON, "data.xml", left, left)
	if result.Status != status.CompareOK {
		t.Errorf("同一 XML の Status = %s, want OK", result.Status)
	}
}

func TestCompareJSONPathValue(t *testing.T) {
	layoutJSON := `{"layoutList": [{
		"fileRegexPattern": ".*\\.json",
		"logicalFileName": "json-pv-test",
		"fileFormat": "Json",
		"charset": "UTF-8",
		"pathValueMode": "true"
	}]}`
	left := `{"a": {"b": 1, "c": [10, 20]}, "d": "same"}`
	right := `{"a": {"b": 2, "c": [10, 20]}, "d": "same"}`

	result, detail := runNewFormatCompare(t, layoutJSON, "data.json", left, right)
	if result.Status != status.CompareNG {
		t.Errorf("Status = %s, want NG", result.Status)
	}
	if !strings.Contains(detail, "$.a.b") {
		t.Errorf("jsonPath の差分がない:\n%s", detail)
	}
	if !strings.Contains(detail, "期待値:1\n実績値:2") {
		t.Errorf("値の差分表記が不正:\n%s", detail)
	}
	// 一致した path は OK 行として出力される (writeDiffOnly=false)
	if !strings.Contains(detail, "$.d") {
		t.Errorf("一致 path の行がない:\n%s", detail)
	}

	result, _ = runNewFormatCompare(t, layoutJSON, "data.json", left, left)
	if result.Status != status.CompareOK {
		t.Errorf("同一 JSON の Status = %s, want OK", result.Status)
	}
}

// 挿入を考慮した比較: 配列要素の挿入で後続がズレても path 単位で差分が出る
func TestCompareJSONListPathValueInsertion(t *testing.T) {
	layoutJSON := `{"layoutList": [{
		"fileRegexPattern": ".*\\.json",
		"logicalFileName": "jsonlist-pv-test",
		"fileFormat": "JsonList",
		"charset": "UTF-8",
		"pathValueMode": "true"
	}]}`
	left := `{"id": 1}
{"id": 2}
`
	right := `{"id": 1, "inserted": "yes"}
{"id": 2}
`
	result, detail := runNewFormatCompare(t, layoutJSON, "list.json", left, right)
	if result.Status != status.CompareNG {
		t.Errorf("Status = %s, want NG", result.Status)
	}
	if !strings.Contains(detail, "RightOnly") || !strings.Contains(detail, "$[0].inserted") {
		t.Errorf("挿入プロパティが RightOnly にならない:\n%s", detail)
	}
	// 既存プロパティは OK のまま
	if strings.Contains(detail, "期待値:1\n実績値:2") {
		t.Errorf("行ズレによる誤差分が発生:\n%s", detail)
	}
}
