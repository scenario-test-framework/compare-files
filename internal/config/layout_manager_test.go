package config

import (
	"testing"

	"github.com/scenario-test-framework/compare-files/internal/status"
)

const sampleLayoutJSON = `{
  "layoutList": [
    {
      "logicalFileName": "ヘッダーありCSVサンプル",
      "fileRegexPattern": "csv_with-header.*\\.csv",
      "fileFormat": "CSV_withHeader",
      "charset": "utf8",
      "lineSp": "LF",
      "recordList": [
        {
          "type": "Data", "codeValue": "-", "itemList": [
            { "id": "KEY1", "name": "キー1", "byteLength": 3, "criteria": "Equal", "compareKey": "true" },
            { "id": "LAST_UPDATE_TIME", "name": "更新時刻", "byteLength": 19, "criteria": "Equal", "compareKey": "false" }
          ]
        }
      ]
    },
    {
      "logicalFileName": "PNGサンプル",
      "fileRegexPattern": "png.*\\.png",
      "fileFormat": "Image",
      "ignoreAreaList": [{"x": 256, "y": 452, "width": 120, "height": 60}]
    }
  ]
}`

func TestGetLayoutMatch(t *testing.T) {
	m := NewLayoutManager()
	if err := m.AddLayoutData([]byte(sampleLayoutJSON), "test"); err != nil {
		t.Fatal(err)
	}

	layout := m.GetLayout("csv_with-header_20170101235959.csv", nil)
	if layout == nil {
		t.Fatal("マッチするはずのレイアウトが nil")
	}
	if layout.FileFormat != status.FormatCSVWithHeader {
		t.Errorf("fileFormat: %v", layout.FileFormat)
	}
	if layout.LineSp != status.LineSpLF {
		t.Errorf("lineSp: %v", layout.LineSp)
	}
	if !layout.RecordList[0].ItemList[0].CompareKey {
		t.Error("compareKey: 文字列 \"true\" が bool に変換されていない")
	}

	if got := m.GetLayout("unmatched.txt", nil); got != nil {
		t.Errorf("マッチしないファイル名で nil 以外: %+v", got)
	}
	// Pattern.matches は全体一致: 部分一致ではマッチしない
	if got := m.GetLayout("prefix_csv_with-header_x.csv", nil); got != nil {
		t.Errorf("全体一致のはずが部分一致でマッチ: %+v", got.LogicalFileName)
	}
}

func TestGetLayoutOverwriteWins(t *testing.T) {
	m := NewLayoutManager()
	if err := m.AddLayoutData([]byte(sampleLayoutJSON), "base"); err != nil {
		t.Fatal(err)
	}
	overwrite := `{"layoutList": [{
      "logicalFileName": "上書き",
      "fileRegexPattern": "csv_with-header.*\\.csv",
      "fileFormat": "CSV_noHeader",
      "charset": "utf8"
    }]}`
	if err := m.AddLayoutData([]byte(overwrite), "overwrite"); err != nil {
		t.Fatal(err)
	}
	layout := m.GetLayout("csv_with-header_1.csv", nil)
	if layout == nil || layout.LogicalFileName != "上書き" {
		t.Errorf("後勝ちで上書きされていない: %+v", layout)
	}
}

func TestUpdateIgnoreForText(t *testing.T) {
	m := NewLayoutManager()
	if err := m.AddLayoutData([]byte(sampleLayoutJSON), "test"); err != nil {
		t.Fatal(err)
	}
	cfg := NewConfig()
	cfg.IgnoreItemList = []string{"LAST_UPDATE_TIME"}

	layout := m.GetLayout("csv_with-header_1.csv", cfg)
	if layout.RecordList[0].ItemList[1].Criteria != status.CriteriaIgnore {
		t.Errorf("ignoreItemList が criteria に反映されていない: %v", layout.RecordList[0].ItemList[1].Criteria)
	}
	// 元の登録レイアウトには影響しない(コピーが返る)
	fresh := m.GetLayout("csv_with-header_1.csv", nil)
	if fresh.RecordList[0].ItemList[1].Criteria != status.CriteriaEqual {
		t.Errorf("登録済みレイアウトが汚染されている: %v", fresh.RecordList[0].ItemList[1].Criteria)
	}
}

func TestUpdateIgnoreForImage(t *testing.T) {
	m := NewLayoutManager()
	if err := m.AddLayoutData([]byte(sampleLayoutJSON), "test"); err != nil {
		t.Fatal(err)
	}
	cfg := NewConfig()
	cfg.IgnoreAreaList = []Rectangle{{X: 0, Y: 0, Width: 1024, Height: 128}}

	layout := m.GetLayout("png_ok.png", cfg)
	if len(layout.IgnoreAreaList) != 2 {
		t.Errorf("除外エリアがマージされていない: %+v", layout.IgnoreAreaList)
	}
	// レイアウト定義側が先、システム設定が後
	if layout.IgnoreAreaList[0].X != 256 || layout.IgnoreAreaList[1].Width != 1024 {
		t.Errorf("除外エリアのマージ順序が不正: %+v", layout.IgnoreAreaList)
	}
}

func TestGetLayoutMatchOrder(t *testing.T) {
	// TreeMap 順(辞書順)の先勝ちマッチを確認
	m := NewLayoutManager()
	layouts := `{"layoutList": [
	  {"logicalFileName": "B", "fileRegexPattern": "b.*\\.txt", "fileFormat": "Text", "charset": "utf8"},
	  {"logicalFileName": "A", "fileRegexPattern": ".*\\.txt", "fileFormat": "Text", "charset": "utf8"}
	]}`
	if err := m.AddLayoutData([]byte(layouts), "test"); err != nil {
		t.Fatal(err)
	}
	// "b1.txt" は両方にマッチするが、辞書順で ".*\\.txt" < "b.*\\.txt" のため A が先勝ち
	layout := m.GetLayout("b1.txt", nil)
	if layout == nil || layout.LogicalFileName != "A" {
		t.Errorf("辞書順先勝ちになっていない: %+v", layout)
	}
}

func TestCompareJavaString(t *testing.T) {
	if compareJavaString("a", "b") >= 0 {
		t.Error("a < b")
	}
	if compareJavaString("abc", "ab") <= 0 {
		t.Error("abc > ab")
	}
	if compareJavaString("同じ", "同じ") != 0 {
		t.Error("同一文字列")
	}
}
