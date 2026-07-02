package writer

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/scenario-test-framework/compare-files/internal/compare"
	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

func strPtr(s string) *string { return &s }

func TestDetailWriterSingleRecordType(t *testing.T) {
	path := filepath.Join(t.TempDir(), "detail.csv")
	w, err := NewDetailWriter(path, "utf8", false, "期待値:", "実績値:")
	if err != nil {
		t.Fatal(err)
	}

	layout := &config.FileLayout{
		LogicalFileName: "test",
		FileFormat:      status.FormatCSVWithHeader,
		RecordList: []*config.RecordLayout{{
			Type: status.RecordData,
			ItemList: []*config.ItemLayout{
				{ID: "KEY1", CompareKey: true, Criteria: status.CriteriaEqual},
				{ID: "AMOUNT", Criteria: status.CriteriaEqual},
			},
		}},
	}
	// OK 行
	if err := w.Write(&compare.RowResult{
		Status: status.CompareOK, FileLayout: layout, RecordType: status.RecordData,
		LeftRowNum: 2, RightRowNum: 2,
		Items: []*compare.ItemResult{
			{ID: "KEY1", LeftValue: strPtr("100"), RightValue: strPtr("100"), Status: status.CompareOK},
			{ID: "AMOUNT", LeftValue: strPtr("1,000"), RightValue: strPtr("1,000"), Status: status.CompareOK},
		},
	}); err != nil {
		t.Fatal(err)
	}
	// NG 行
	if err := w.Write(&compare.RowResult{
		Status: status.CompareNG, FileLayout: layout, RecordType: status.RecordData,
		LeftRowNum: 3, RightRowNum: 3,
		Items: []*compare.ItemResult{
			{ID: "KEY1", LeftValue: strPtr("100"), RightValue: strPtr("100"), Status: status.CompareOK},
			{ID: "AMOUNT", LeftValue: strPtr("1,000"), RightValue: strPtr("2,000"), Status: status.CompareNG},
		},
	}); err != nil {
		t.Fatal(err)
	}
	if err := w.Close(); err != nil {
		t.Fatal(err)
	}

	got, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}
	want := `"Status","RowNum","DiffItems","KEY1","AMOUNT"
"OK","期待値:2
実績値:2","-","100","1,000"
"NG","期待値:3
実績値:3","[AMOUNT]","100","期待値:1,000
実績値:2,000"
`
	if string(got) != want {
		t.Errorf("出力不一致:\n--- got ---\n%s\n--- want ---\n%s", got, want)
	}
}

func TestDetailWriterMultiRecordTypeNullColumns(t *testing.T) {
	path := filepath.Join(t.TempDir(), "detail.csv")
	w, err := NewDetailWriter(path, "utf8", false, "L:", "R:")
	if err != nil {
		t.Fatal(err)
	}

	layout := &config.FileLayout{
		LogicalFileName: "fixed",
		FileFormat:      status.FormatFixed,
		RecordList: []*config.RecordLayout{
			{Type: status.RecordHeader, CodeValue: "0", ItemList: []*config.ItemLayout{
				{ID: "RECORD_TYPE", CompareKey: true}, {ID: "DATA_COUNT"},
			}},
			{Type: status.RecordData, CodeValue: "1", ItemList: []*config.ItemLayout{
				{ID: "RECORD_TYPE", CompareKey: true}, {ID: "KEY1", CompareKey: true},
			}},
		},
	}
	if err := w.Write(&compare.RowResult{
		Status: status.CompareOK, FileLayout: layout, RecordType: status.RecordHeader,
		LeftRowNum: 1, RightRowNum: 1,
		Items: []*compare.ItemResult{
			{ID: "RECORD_TYPE", LeftValue: strPtr("0"), RightValue: strPtr("0"), Status: status.CompareOK},
			{ID: "DATA_COUNT", LeftValue: strPtr("0000000009"), RightValue: strPtr("0000000009"), Status: status.CompareOK},
		},
	}); err != nil {
		t.Fatal(err)
	}
	if err := w.Close(); err != nil {
		t.Fatal(err)
	}

	got, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}
	// Header 行なので Data.* カラムは null → 空 (クォートなし)
	want := `"Status","RowNum","DiffItems","Header.RECORD_TYPE","Header.DATA_COUNT","Data.RECORD_TYPE","Data.KEY1"
"OK","L:1
R:1","-","0","0000000009",,
`
	if string(got) != want {
		t.Errorf("出力不一致:\n--- got ---\n%s\n--- want ---\n%s", got, want)
	}
}

func TestDetailWriterWriteDiffOnly(t *testing.T) {
	path := filepath.Join(t.TempDir(), "detail.csv")
	w, err := NewDetailWriter(path, "utf8", true, "L:", "R:")
	if err != nil {
		t.Fatal(err)
	}
	if err := w.Write(&compare.RowResult{
		Status: status.CompareOK, LeftRowNum: 1, RightRowNum: 1,
		Items: []*compare.ItemResult{
			{ID: "value", LeftValue: strPtr("same"), RightValue: strPtr("same"), Status: status.CompareOK},
		},
	}); err != nil {
		t.Fatal(err)
	}
	if err := w.Close(); err != nil {
		t.Fatal(err)
	}
	got, _ := os.ReadFile(path)
	// ヘッダーのみ出力され、OK 行はスキップされる
	want := "\"Status\",\"RowNum\",\"DiffItems\",\"value\"\n"
	if string(got) != want {
		t.Errorf("出力不一致: %q", got)
	}
}
