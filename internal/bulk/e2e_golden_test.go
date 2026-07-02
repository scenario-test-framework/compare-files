package bulk

import (
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/scenario-test-framework/compare-files/internal/assets"
	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/csvio"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

const (
	filesTestBase = "../../testdata/main/CompareFilesTest"
	regexTestBase = "../../testdata/main/CompareFileRegexTest"
)

func e2eConfig(t *testing.T, outputDir string) *config.CompareFilesConfig {
	t.Helper()
	cfg, err := config.ParseConfig(assets.DefaultConfig())
	if err != nil {
		t.Fatal(err)
	}
	cfg.OutputDir = outputDir
	return cfg
}

func e2eLayoutManager(t *testing.T) *config.LayoutManager {
	t.Helper()
	m := config.NewLayoutManager()
	for name, data := range assets.DefaultLayouts() {
		if err := m.AddLayoutData(data, name); err != nil {
			t.Fatal(err)
		}
	}
	return m
}

// assertSummaryEquals は Java 版 assertCompareResultFiles のサマリ照合を再現します。
//   - ヘッダー行: 全項目一致
//   - データ行: Left/Right (col 1,2) は actual が expect で終わること、
//     col 0,3..9 は一致、col 10-12 (時刻・処理時間) は比較しない
func assertSummaryEquals(t *testing.T, expectPath, actualPath string) {
	t.Helper()
	expectLines := readCsvLines(t, expectPath)
	actualLines := readCsvLines(t, actualPath)
	if len(expectLines) != len(actualLines) {
		t.Fatalf("サマリ行数不一致: expect=%d actual=%d\nactual: %v", len(expectLines), len(actualLines), actualLines)
	}
	for lineIdx, expectLine := range expectLines {
		actualLine := actualLines[lineIdx]
		if len(expectLine) != len(actualLine) {
			t.Errorf("行 %d: カラム数不一致 expect=%d actual=%d", lineIdx, len(expectLine), len(actualLine))
			continue
		}
		for colIdx, expect := range expectLine {
			actual := actualLine[colIdx]
			if lineIdx == 0 {
				if expect != actual {
					t.Errorf("ヘッダー[%d]: expect=%q actual=%q", colIdx, expect, actual)
				}
				continue
			}
			switch {
			case colIdx == 1 || colIdx == 2:
				if !strings.HasSuffix(actual, expect) {
					t.Errorf("行 %d col %d: actual=%q が expect=%q で終わらない", lineIdx, colIdx, actual, expect)
				}
			case colIdx < 10:
				if expect != actual {
					t.Errorf("行 %d col %d: expect=%q actual=%q", lineIdx, colIdx, expect, actual)
				}
			}
		}
	}
}

func readCsvLines(t *testing.T, path string) [][]string {
	t.Helper()
	f, err := os.Open(path)
	if err != nil {
		t.Fatal(err)
	}
	defer f.Close()
	r := csvio.NewReader(f, csvio.CsvConfig())
	var lines [][]string
	for {
		values, err := r.ReadValues()
		if err != nil {
			t.Fatal(err)
		}
		if values == nil {
			return lines
		}
		if len(values) == 1 && values[0] == "" {
			continue
		}
		lines = append(lines, values)
	}
}

// assertDetailFilesEqual は詳細ファイル群を照合します。
// CSV は Java 版 golden とバイト一致、PNG は Go 版で再生成した golden とバイト一致を確認します。
func assertDetailFilesEqual(t *testing.T, expectDir, actualDir string) {
	t.Helper()
	entries, err := os.ReadDir(expectDir)
	if err != nil {
		t.Fatal(err)
	}
	for _, entry := range entries {
		name := entry.Name()
		if entry.IsDir() || name == "CompareSummary.csv" {
			continue
		}
		actualPath := filepath.Join(actualDir, name)
		expectData, err := os.ReadFile(filepath.Join(expectDir, name))
		if err != nil {
			t.Fatal(err)
		}
		actualData, err := os.ReadFile(actualPath)
		if err != nil {
			t.Errorf("%s: 出力されていない: %v", name, err)
			continue
		}
		if !bytesEqual(expectData, actualData) {
			if filepath.Ext(name) == ".csv" {
				t.Errorf("%s: golden と不一致\n--- got ---\n%s\n--- want ---\n%s", name, actualData, expectData)
			} else {
				t.Errorf("%s: golden と不一致 (バイナリ, expect=%d bytes actual=%d bytes)", name, len(expectData), len(actualData))
			}
		}
	}
}

func bytesEqual(a, b []byte) bool {
	if len(a) != len(b) {
		return false
	}
	for i := range a {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}

func TestE2EDirCompare(t *testing.T) {
	outputDir := t.TempDir()
	cfg := e2eConfig(t, outputDir)
	lm := e2eLayoutManager(t)

	counts, err := CompareDir(filepath.Join(filesTestBase, "input/left"), filepath.Join(filesTestBase, "input/right"), outputDir, cfg, lm)
	if err != nil {
		t.Fatal(err)
	}
	if got := counts.ProcessStatus(); got != status.ProcessWarning {
		t.Errorf("ProcessStatus = %v, want Warning", got)
	}
	if counts.ErrorCount != 0 {
		t.Errorf("errorCount = %d", counts.ErrorCount)
	}

	expectDir := filepath.Join(filesTestBase, "expect/dir")
	assertDetailFilesEqual(t, expectDir, outputDir)
	assertSummaryEquals(t, filepath.Join(expectDir, "CompareSummary.csv"), filepath.Join(outputDir, "CompareSummary.csv"))
}

func TestE2EDirLeftOnly(t *testing.T) {
	outputDir := t.TempDir()
	cfg := e2eConfig(t, outputDir)
	lm := e2eLayoutManager(t)

	counts, err := CompareDir(filepath.Join(filesTestBase, "input/left"), filepath.Join(filesTestBase, "input/not_exist"), outputDir, cfg, lm)
	if err != nil {
		t.Fatal(err)
	}
	if got := counts.ProcessStatus(); got != status.ProcessWarning {
		t.Errorf("ProcessStatus = %v, want Warning", got)
	}
	assertSummaryEquals(t,
		filepath.Join(filesTestBase, "expect/dir_left_only/CompareSummary.csv"),
		filepath.Join(outputDir, "CompareSummary.csv"))
}

func TestE2EDirRightOnly(t *testing.T) {
	outputDir := t.TempDir()
	cfg := e2eConfig(t, outputDir)
	lm := e2eLayoutManager(t)

	counts, err := CompareDir(filepath.Join(filesTestBase, "input/not_exist"), filepath.Join(filesTestBase, "input/right"), outputDir, cfg, lm)
	if err != nil {
		t.Fatal(err)
	}
	if got := counts.ProcessStatus(); got != status.ProcessWarning {
		t.Errorf("ProcessStatus = %v, want Warning", got)
	}
	assertSummaryEquals(t,
		filepath.Join(filesTestBase, "expect/dir_right_only/CompareSummary.csv"),
		filepath.Join(outputDir, "CompareSummary.csv"))
}

func TestE2EDirBothNotExist(t *testing.T) {
	outputDir := t.TempDir()
	cfg := e2eConfig(t, outputDir)
	lm := e2eLayoutManager(t)

	_, err := CompareDir(filepath.Join(filesTestBase, "input/not_exist_l"), filepath.Join(filesTestBase, "input/not_exist_r"), outputDir, cfg, lm)
	if err == nil {
		t.Fatal("両方存在しないディレクトリでエラーにならない")
	}
	// 空のサマリファイルが生成される (Java 版と同じ)
	info, statErr := os.Stat(filepath.Join(outputDir, "CompareSummary.csv"))
	if statErr != nil {
		t.Fatalf("空サマリが生成されていない: %v", statErr)
	}
	if info.Size() != 0 {
		t.Errorf("サマリは空のはず: %d bytes", info.Size())
	}
}

func TestE2ERegexCompare(t *testing.T) {
	// compare_target.csv 内のパスはリポジトリルートからの相対パス
	t.Chdir("../..")

	outputDir := t.TempDir()
	cfg := e2eConfig(t, outputDir)
	lm := e2eLayoutManager(t)

	counts, err := CompareRegex("testdata/main/CompareFileRegexTest/input/compare_target.csv", outputDir, cfg, lm)
	if err != nil {
		t.Fatal(err)
	}
	// 存在しないディレクトリ指定を含むため Failure
	if got := counts.ProcessStatus(); got != status.ProcessFailure {
		t.Errorf("ProcessStatus = %v, want Failure", got)
	}

	expectDir := "testdata/main/CompareFileRegexTest/expect"
	assertDetailFilesEqual(t, expectDir, outputDir)
	assertSummaryEquals(t, filepath.Join(expectDir, "CompareSummary.csv"), filepath.Join(outputDir, "CompareSummary.csv"))
}
