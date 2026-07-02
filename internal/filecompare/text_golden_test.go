package filecompare

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/scenario-test-framework/compare-files/internal/assets"
	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// 既存の Java 版テストデータ (golden files) を利用した互換性検証。
const goldenBase = "../../testdata/main/CompareFilesTest"

func defaultTestConfig(t *testing.T) *config.CompareFilesConfig {
	t.Helper()
	cfg, err := config.ParseConfig(assets.DefaultConfig())
	if err != nil {
		t.Fatal(err)
	}
	return cfg
}

func testLayoutManager(t *testing.T) *config.LayoutManager {
	t.Helper()
	m := config.NewLayoutManager()
	for name, data := range assets.DefaultLayouts() {
		if err := m.AddLayoutData(data, name); err != nil {
			t.Fatal(err)
		}
	}
	return m
}

// runTextCompare は左右ファイルを比較し、詳細ファイルの内容と結果を返します。
func runTextCompare(t *testing.T, fileName string) (*Result, string) {
	t.Helper()
	cfg := defaultTestConfig(t)
	lm := testLayoutManager(t)

	leftPath := filepath.Join(goldenBase, "input/left", dirOf(fileName), fileName)
	rightPath := filepath.Join(goldenBase, "input/right", dirOf(fileName), fileName)
	layout := lm.GetLayout(fileName, cfg)

	outputDir := t.TempDir()
	result, err := CompareText(leftPath, rightPath, layout, outputDir, cfg)
	if err != nil {
		t.Fatalf("CompareText(%s): %v", fileName, err)
	}

	outputName := OutputFileName(leftPath, rightPath, cfg, "csv")
	data, err := os.ReadFile(filepath.Join(outputDir, outputName))
	if err != nil {
		t.Fatalf("詳細ファイルが出力されていない: %v", err)
	}
	return result, string(data)
}

// dirOf はテストデータのサブディレクトリ名を返します。
func dirOf(fileName string) string {
	switch filepath.Ext(fileName) {
	case ".csv":
		return "TEXT_CSV"
	case ".tsv":
		return "TEXT_TSV"
	case ".txt":
		if len(fileName) >= 5 && fileName[:5] == "fixed" {
			return "TEXT_FIXED"
		}
		return "TEXT_PLAINTEXT"
	case ".json":
		if len(fileName) >= 8 && fileName[:8] == "jsonlist" {
			return "TEXT_JSONLIST"
		}
		return "TEXT_JSON"
	}
	return ""
}

func expectDetail(t *testing.T, expectFileName string) string {
	t.Helper()
	data, err := os.ReadFile(filepath.Join(goldenBase, "expect/dir", expectFileName))
	if err != nil {
		t.Fatal(err)
	}
	return string(data)
}

func assertGolden(t *testing.T, fileName, expectFileName string, wantStatus status.CompareStatus) {
	t.Helper()
	result, got := runTextCompare(t, fileName)
	if result.Status != wantStatus {
		t.Errorf("%s: status = %v, want %v", fileName, result.Status, wantStatus)
	}
	want := expectDetail(t, expectFileName)
	if got != want {
		t.Errorf("%s: 詳細ファイルが golden と不一致\n--- got ---\n%s\n--- want ---\n%s", fileName, got, want)
	}
}

func TestGoldenCsvWithHeader(t *testing.T) {
	assertGolden(t, "csv_with-header_ok.csv", "CompareDetail_csv_with-header_ok.csv_right_TEXT_CSV.csv", status.CompareOK)
	assertGolden(t, "csv_with-header_ng.csv", "CompareDetail_csv_with-header_ng.csv_right_TEXT_CSV.csv", status.CompareNG)
}

func TestGoldenCsvMultiRecordType(t *testing.T) {
	assertGolden(t, "csv_multi-record-type_ok.csv", "CompareDetail_csv_multi-record-type_ok.csv_right_TEXT_CSV.csv", status.CompareOK)
	assertGolden(t, "csv_multi-record-type_ng.csv", "CompareDetail_csv_multi-record-type_ng.csv_right_TEXT_CSV.csv", status.CompareNG)
}

func TestGoldenTsv(t *testing.T) {
	assertGolden(t, "tsv_ok.tsv", "CompareDetail_tsv_ok.tsv_right_TEXT_TSV.csv", status.CompareOK)
	assertGolden(t, "tsv_ng.tsv", "CompareDetail_tsv_ng.tsv_right_TEXT_TSV.csv", status.CompareNG)
}

func TestGoldenFixed(t *testing.T) {
	assertGolden(t, "fixed_multi-record-type_ok.txt", "CompareDetail_fixed_multi-record-type_ok.txt_right_TEXT_FIXED.csv", status.CompareOK)
	assertGolden(t, "fixed_multi-record-type_ng.txt", "CompareDetail_fixed_multi-record-type_ng.txt_right_TEXT_FIXED.csv", status.CompareNG)
}

func TestGoldenJson(t *testing.T) {
	assertGolden(t, "json_ok.json", "CompareDetail_json_ok.json_right_TEXT_JSON.csv", status.CompareOK)
	assertGolden(t, "json_ng.json", "CompareDetail_json_ng.json_right_TEXT_JSON.csv", status.CompareNG)
}

func TestGoldenJsonList(t *testing.T) {
	assertGolden(t, "jsonlist_ok.json", "CompareDetail_jsonlist_ok.json_right_TEXT_JSONLIST.csv", status.CompareOK)
	assertGolden(t, "jsonlist_ng.json", "CompareDetail_jsonlist_ng.json_right_TEXT_JSONLIST.csv", status.CompareNG)
}

func TestGoldenPlaintext(t *testing.T) {
	assertGolden(t, "plaintext_ok.txt", "CompareDetail_plaintext_ok.txt_right_TEXT_PLAINTEXT.csv", status.CompareOK)
	assertGolden(t, "plaintext_ng.txt", "CompareDetail_plaintext_ng.txt_right_TEXT_PLAINTEXT.csv", status.CompareNG)
}
