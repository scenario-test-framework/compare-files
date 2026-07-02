package config

import (
	"strings"
	"testing"
)

func TestParseConfigYAMLFlat(t *testing.T) {
	yamlData := `
outputDir: result
outputCharset: utf8
deleteWorkDir: true
sorted: "false"
csvHeaderRow: 1
csvDataStartRow: "2"
ignoreItemList:
  - last_update_time
`
	cfg, err := ParseConfigYAML([]byte(yamlData))
	if err != nil {
		t.Fatalf("ParseConfigYAML: %v", err)
	}
	if cfg.OutputDir != "result" {
		t.Errorf("OutputDir = %q", cfg.OutputDir)
	}
	if !cfg.DeleteWorkDir {
		t.Errorf("DeleteWorkDir = false, want true")
	}
	if cfg.Sorted {
		t.Errorf("Sorted = true, want false")
	}
	if cfg.CsvHeaderRow != 1 || cfg.CsvDataStartRow != 2 {
		t.Errorf("CsvHeaderRow/CsvDataStartRow = %d/%d", cfg.CsvHeaderRow, cfg.CsvDataStartRow)
	}
	if len(cfg.IgnoreItemList) != 1 || cfg.IgnoreItemList[0] != "last_update_time" {
		t.Errorf("IgnoreItemList = %v", cfg.IgnoreItemList)
	}
}

func TestParseConfigHierarchicalJSON(t *testing.T) {
	jsonData := `{
  "input": {
    "defaultCharset": "ms932",
    "ignoreFileRegexList": ["^\\..*"]
  },
  "compare": {
    "sorted": "true",
    "chunkSize": 500,
    "ignoreItemList": ["ts"],
    "layoutDir": "layout",
    "csv": {"headerRow": 3, "dataStartRow": 4},
    "fixed": {"codeValueForOnlyOneRecordType": "*"},
    "image": {
      "ignoreAreaList": [{"x": 1, "y": 2, "width": 3, "height": 4}],
      "okStyle": {"border": 2}
    }
  },
  "output": {
    "dir": "out",
    "charset": "utf8",
    "resultFileName": "Summary.csv",
    "detailFilePrefix": "Detail_",
    "writeDiffOnly": true,
    "leftPrefix": "L:",
    "rightPrefix": "R:",
    "deleteWorkDir": true
  }
}`
	cfg, err := ParseConfig([]byte(jsonData))
	if err != nil {
		t.Fatalf("ParseConfig: %v", err)
	}
	if cfg.DefaultInputCharset != "ms932" {
		t.Errorf("DefaultInputCharset = %q", cfg.DefaultInputCharset)
	}
	if !cfg.Sorted || cfg.ChunkSize != 500 {
		t.Errorf("Sorted/ChunkSize = %v/%d", cfg.Sorted, cfg.ChunkSize)
	}
	if cfg.OverwriteLayoutDir != "layout" {
		t.Errorf("OverwriteLayoutDir = %q", cfg.OverwriteLayoutDir)
	}
	if cfg.CsvHeaderRow != 3 || cfg.CsvDataStartRow != 4 {
		t.Errorf("CsvHeaderRow/CsvDataStartRow = %d/%d", cfg.CsvHeaderRow, cfg.CsvDataStartRow)
	}
	if cfg.CodeValueForOnlyOneRecordType != "*" {
		t.Errorf("CodeValueForOnlyOneRecordType = %q", cfg.CodeValueForOnlyOneRecordType)
	}
	if len(cfg.IgnoreAreaList) != 1 || cfg.IgnoreAreaList[0].Width != 3 {
		t.Errorf("IgnoreAreaList = %v", cfg.IgnoreAreaList)
	}
	if cfg.OkImageStyle == nil || cfg.OkImageStyle.Border != 2 {
		t.Errorf("OkImageStyle = %v", cfg.OkImageStyle)
	}
	if cfg.OutputDir != "out" || cfg.OutputCharset != "utf8" {
		t.Errorf("OutputDir/OutputCharset = %q/%q", cfg.OutputDir, cfg.OutputCharset)
	}
	if cfg.CompareResultFileName != "Summary.csv" || cfg.CompareDetailFilePrefix != "Detail_" {
		t.Errorf("resultFileName/detailFilePrefix = %q/%q", cfg.CompareResultFileName, cfg.CompareDetailFilePrefix)
	}
	if !cfg.WriteDiffOnly || !cfg.DeleteWorkDir {
		t.Errorf("WriteDiffOnly/DeleteWorkDir = %v/%v", cfg.WriteDiffOnly, cfg.DeleteWorkDir)
	}
}

func TestParseConfigHierarchicalYAML(t *testing.T) {
	yamlData := `
# コメントが書ける
input:
  defaultCharset: ms932
compare:
  csv:
    headerRow: 1
output:
  dir: result
  charset: utf8
`
	cfg, err := ParseConfigYAML([]byte(yamlData))
	if err != nil {
		t.Fatalf("ParseConfigYAML: %v", err)
	}
	if cfg.DefaultInputCharset != "ms932" || cfg.CsvHeaderRow != 1 || cfg.OutputDir != "result" {
		t.Errorf("defaultCharset/csvHeaderRow/outputDir = %q/%d/%q", cfg.DefaultInputCharset, cfg.CsvHeaderRow, cfg.OutputDir)
	}
}

func TestParseConfigHierarchyDuplicateAlias(t *testing.T) {
	// 同じフラットキーに写る同義キーの併記は非決定になるためエラー
	jsonData := `{"output": {"dir": "alias", "outputDir": "legacy"}}`
	if _, err := ParseConfig([]byte(jsonData)); err == nil {
		t.Fatal("同義キーの併記はエラーになるべき")
	} else if !strings.Contains(err.Error(), "outputDir") {
		t.Errorf("エラーメッセージが不親切: %v", err)
	}
}

func TestParseConfigTrailingData(t *testing.T) {
	// 末尾に余分なデータがある壊れた JSON はエラー
	if _, err := ParseConfig([]byte(`{"outputDir":"ok"} {"outputDir":"ng"}`)); err == nil {
		t.Fatal("末尾の余分なデータはエラーになるべき")
	}
}

func TestParseConfigYAMLMultiDocument(t *testing.T) {
	// 複数ドキュメント (---) の YAML はエラー (先頭だけの黙認をしない)
	yamlData := "output:\n  dir: first\n---\noutput:\n  dir: second\n"
	if _, err := ParseConfigYAML([]byte(yamlData)); err == nil {
		t.Fatal("複数ドキュメントの YAML はエラーになるべき")
	}
}

func TestParseConfigHierarchyUnusedFilePathKeys(t *testing.T) {
	// leftFilePath / rightFilePath は実行系が参照しないため階層化キーとしては未定義
	if _, err := ParseConfig([]byte(`{"input": {"leftFilePath": "left.csv"}}`)); err == nil {
		t.Fatal("input.leftFilePath はエラーになるべき")
	}
}

func TestParseConfigHierarchyOverridesFlat(t *testing.T) {
	jsonData := `{
  "outputDir": "flat",
  "output": {"dir": "nested"}
}`
	cfg, err := ParseConfig([]byte(jsonData))
	if err != nil {
		t.Fatalf("ParseConfig: %v", err)
	}
	if cfg.OutputDir != "nested" {
		t.Errorf("OutputDir = %q, want nested (階層化側が優先)", cfg.OutputDir)
	}
}

func TestParseConfigHierarchyUnknownKey(t *testing.T) {
	jsonData := `{"output": {"unknownKey": 1}}`
	if _, err := ParseConfig([]byte(jsonData)); err == nil {
		t.Fatal("未定義の階層化キーはエラーになるべき")
	} else if !strings.Contains(err.Error(), "output.unknownKey") {
		t.Errorf("エラーメッセージにキー名が含まれない: %v", err)
	}
}

func TestParseConfigFlatCompatible(t *testing.T) {
	// 既存のフラット形式 (文字列 bool 含む) がそのまま解釈できること
	jsonData := `{
  "deleteWorkDir" : "true",
  "sorted" : "false",
  "csvHeaderRow" : 1,
  "outputDir" : "result",
  "ignoreAreaList" : [{"x": 0, "y":0, "width":1024, "height":128}],
  "okImageStyle": {"border": 4, "labelColor": {"r": 255, "g": 255, "b": 255, "a": 200}}
}`
	cfg, err := ParseConfig([]byte(jsonData))
	if err != nil {
		t.Fatalf("ParseConfig: %v", err)
	}
	if !cfg.DeleteWorkDir || cfg.Sorted || cfg.CsvHeaderRow != 1 || cfg.OutputDir != "result" {
		t.Errorf("フラット形式の解釈が不正: %+v", cfg)
	}
	if cfg.OkImageStyle == nil || cfg.OkImageStyle.LabelColor == nil || cfg.OkImageStyle.LabelColor.A != 200 {
		t.Errorf("OkImageStyle = %+v", cfg.OkImageStyle)
	}
	// csvDataStartRow 未設定は DefaultIntValue のまま
	if cfg.CsvDataStartRow != DefaultIntValue {
		t.Errorf("CsvDataStartRow = %d, want DefaultIntValue", cfg.CsvDataStartRow)
	}
}
