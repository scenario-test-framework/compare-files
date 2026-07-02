package config

import (
	"testing"

	"github.com/scenario-test-framework/compare-files/internal/status"
)

const defaultConfigJSON = `{
  "overwriteLayoutDir" : "",
  "deleteWorkDir" : "true",
  "sorted" : "false",
  "csvHeaderRow" : 1,
  "csvDataStartRow" : 2,
  "codeValueForOnlyOneRecordType":"-",
  "outputDir" : "result",
  "outputCharset" : "utf8",
  "compareResultFileName" : "CompareSummary.csv",
  "compareDetailFilePrefix" : "CompareDetail_",
  "writeDiffOnly" : "false",
  "leftPrefix" : "期待値:",
  "rightPrefix" : "実績値:",
  "chunkSize" : 1000,
  "ignoreFileRegexList" : ["^\\..*"],
  "ignoreItemList" : ["last_update_time", "LAST_UPDATE_TIME"],
  "ignoreAreaList" : [{"x": 0, "y":0, "width":1024, "height":128}],
  "okImageStyle": {
    "border": 4, "labelFontSize": 24, "labelHeight": 36,
    "labelPaddingLeft": 12, "labelPaddingTop": 28,
    "labelColor":   {"r": 255, "g": 255, "b": 255, "a": 200},
    "leftBgColor":  {"r": 52,  "g": 152, "b": 219, "a": 255},
    "rightBgColor": {"r": 26,  "g": 188, "b": 156, "a": 255}
  }
}`

func TestParseConfig(t *testing.T) {
	cfg, err := ParseConfig([]byte(defaultConfigJSON))
	if err != nil {
		t.Fatal(err)
	}
	if !cfg.DeleteWorkDir {
		t.Error("deleteWorkDir: 文字列 \"true\" が bool に変換されていない")
	}
	if cfg.Sorted {
		t.Error("sorted: 文字列 \"false\" が bool に変換されていない")
	}
	if cfg.CsvHeaderRow != 1 || cfg.CsvDataStartRow != 2 {
		t.Errorf("csvHeaderRow/csvDataStartRow: %d/%d", cfg.CsvHeaderRow, cfg.CsvDataStartRow)
	}
	if cfg.LeftPrefix != "期待値:" || cfg.RightPrefix != "実績値:" {
		t.Errorf("prefix: %q/%q", cfg.LeftPrefix, cfg.RightPrefix)
	}
	if len(cfg.IgnoreAreaList) != 1 || cfg.IgnoreAreaList[0].Width != 1024 {
		t.Errorf("ignoreAreaList: %+v", cfg.IgnoreAreaList)
	}
	if cfg.OkImageStyle == nil || cfg.OkImageStyle.LabelColor.A != 200 {
		t.Errorf("okImageStyle: %+v", cfg.OkImageStyle)
	}
	if cfg.NgImageStyle != nil {
		t.Error("ngImageStyle は未設定のはず")
	}
}

func TestRgbaColorDefaultAlpha(t *testing.T) {
	cfg, err := ParseConfig([]byte(`{"okImageStyle": {"labelColor": {"r": 1, "g": 2, "b": 3}}}`))
	if err != nil {
		t.Fatal(err)
	}
	if cfg.OkImageStyle.LabelColor.A != 255 {
		t.Errorf("alpha 未指定時は 255 のはず: %d", cfg.OkImageStyle.LabelColor.A)
	}
}

func TestSetDefault(t *testing.T) {
	defaultCfg, err := ParseConfig([]byte(defaultConfigJSON))
	if err != nil {
		t.Fatal(err)
	}
	cfg, err := ParseConfig([]byte(`{"overwriteLayoutDir": "DIFF_ONLY", "ignoreItemList": ["BOTH"]}`))
	if err != nil {
		t.Fatal(err)
	}
	cfg.SetDefault(defaultCfg)

	if cfg.OverwriteLayoutDir != "DIFF_ONLY" {
		t.Errorf("設定済みの値が上書きされた: %q", cfg.OverwriteLayoutDir)
	}
	if len(cfg.IgnoreItemList) != 1 || cfg.IgnoreItemList[0] != "BOTH" {
		t.Errorf("設定済みのリストが上書きされた: %v", cfg.IgnoreItemList)
	}
	if cfg.OutputDir != "result" {
		t.Errorf("未設定項目がデフォルトで補完されていない: %q", cfg.OutputDir)
	}
	if cfg.CsvHeaderRow != 1 {
		t.Errorf("未設定 int がデフォルトで補完されていない: %d", cfg.CsvHeaderRow)
	}
	if !cfg.DeleteWorkDir {
		t.Error("未設定 bool がデフォルトで補完されていない")
	}
}

func TestValidate(t *testing.T) {
	cfg := NewConfig()
	if err := cfg.Validate(); err == nil {
		t.Error("必須項目が空でもエラーにならない")
	}

	valid, err := ParseConfig([]byte(defaultConfigJSON))
	if err != nil {
		t.Fatal(err)
	}
	if err := valid.Validate(); err != nil {
		t.Errorf("正常な設定でエラー: %v", err)
	}

	invalid, err := ParseConfig([]byte(defaultConfigJSON))
	if err != nil {
		t.Fatal(err)
	}
	invalid.OutputCharset = "not-a-charset"
	if err := invalid.Validate(); err == nil {
		t.Error("不正な charset でもエラーにならない")
	}
}

func TestRecordPattern(t *testing.T) {
	tests := []struct {
		name   string
		layout *FileLayout
		want   status.RecordPattern
	}{
		{"empty", &FileLayout{}, status.PatternNone},
		{"dataOnly", &FileLayout{RecordList: []*RecordLayout{{Type: status.RecordData}}}, status.PatternDataOnly},
		{"headerData", &FileLayout{RecordList: []*RecordLayout{{Type: status.RecordHeader}, {Type: status.RecordData}}}, status.PatternHeaderData},
		{"headerOnly1", &FileLayout{RecordList: []*RecordLayout{{Type: status.RecordHeader}}}, status.PatternMulti},
		{"multi4", &FileLayout{RecordList: []*RecordLayout{
			{Type: status.RecordHeader}, {Type: status.RecordData}, {Type: status.RecordTrailer}, {Type: status.RecordEnd},
		}}, status.PatternMulti},
	}
	for _, tt := range tests {
		if got := tt.layout.RecordPattern(); got != tt.want {
			t.Errorf("%s: got %v want %v", tt.name, got, tt.want)
		}
	}
}

func TestRecordByteLength(t *testing.T) {
	r := &RecordLayout{ItemList: []*ItemLayout{{ByteLength: 1}, {ByteLength: 10}, {ByteLength: 39}}}
	if got := r.RecordByteLength(); got != 50 {
		t.Errorf("byteLength 合計: got %d want 50", got)
	}
}
