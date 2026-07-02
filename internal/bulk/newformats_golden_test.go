package bulk

import (
	"path/filepath"
	"testing"

	"github.com/scenario-test-framework/compare-files/internal/status"
)

// 新形式 (Yaml / XML / pathValueMode) の golden 回帰テスト。
// testdata/main/CompareFilesTest は Java 版互換の golden 専用のため、
// Java 版に存在しない新形式は独立した golden セットで回帰を検出する。
// expect は Go 実装の出力を目視レビューして正としたもの
// (入力は dist/sample の TEXT_YAML / TEXT_XML / TEXT_JSONPATH と同一)。
const newFormatsTestBase = "../../testdata/main/CompareFilesNewFormatsTest"

func TestE2ENewFormatsDirCompare(t *testing.T) {
	outputDir := t.TempDir()
	cfg := e2eConfig(t, outputDir)
	lm := e2eLayoutManager(t)

	counts, err := CompareDir(
		filepath.Join(newFormatsTestBase, "input/left"),
		filepath.Join(newFormatsTestBase, "input/right"),
		outputDir, cfg, lm)
	if err != nil {
		t.Fatal(err)
	}
	if got := counts.ProcessStatus(); got != status.ProcessWarning {
		t.Errorf("ProcessStatus = %v, want Warning", got)
	}
	if counts.ErrorCount != 0 {
		t.Errorf("errorCount = %d", counts.ErrorCount)
	}

	expectDir := filepath.Join(newFormatsTestBase, "expect/dir")
	assertDetailFilesEqual(t, expectDir, outputDir)
	assertSummaryEquals(t, filepath.Join(expectDir, "CompareSummary.csv"), filepath.Join(outputDir, "CompareSummary.csv"))
}
