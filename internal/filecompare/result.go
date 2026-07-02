// Package filecompare はファイル単位の比較を提供します。
// Java 版 sv/domain/compare/file 相当です。
package filecompare

import (
	"path/filepath"
	"strings"
	"time"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// Result はファイル単位の比較結果です。Java 版 BaseFileCompareResult のサマリ項目相当。
type Result struct {
	Status            status.CompareStatus
	LeftFilePath      string
	RightFilePath     string
	FileLayout        *config.FileLayout
	RowCount          int64
	OkRowCount        int64
	NgRowCount        int64
	IgnoreRowCount    int64
	LeftOnlyRowCount  int64
	RightOnlyRowCount int64
	StartTime         time.Time
	EndTime           time.Time
}

// LayoutName はサマリ出力用のレイアウト論理名を返します (未適用は "-")。
func (r *Result) LayoutName() string {
	if r.FileLayout == nil {
		return config.DummyValue
	}
	return r.FileLayout.LogicalFileName
}

// updateFileStatus は行ステータスからファイルステータスを更新します。
// Java 版 TextFileCompareResult.updateFileStatus 相当。
func (r *Result) updateFileStatus(rowStatus status.CompareStatus) {
	var tempSummaryStatus status.CompareStatus
	switch rowStatus {
	case status.CompareOK, status.CompareIgnore:
		tempSummaryStatus = status.CompareOK
	case status.CompareNG, status.CompareLeftOnly, status.CompareRightOnly:
		tempSummaryStatus = status.CompareNG
	default:
		return
	}
	switch r.Status {
	case status.CompareProcessing, status.CompareOK:
		r.Status = tempSummaryStatus
	}
}

// updateRowSummaryFields は行単位の集計項目を更新します。
func (r *Result) updateRowSummaryFields(rowStatus status.CompareStatus) {
	r.RowCount++
	switch rowStatus {
	case status.CompareOK:
		r.OkRowCount++
	case status.CompareNG:
		r.NgRowCount++
	case status.CompareIgnore:
		r.IgnoreRowCount++
	case status.CompareLeftOnly:
		r.LeftOnlyRowCount++
	case status.CompareRightOnly:
		r.RightOnlyRowCount++
	}
}

// OutputFileName は詳細結果の出力ファイル名を返します。
// Java 版 FileCompareResult.getOutputFileName 相当。
// 左右のディレクトリ階層で不一致になった階層以降 (右側) をファイル名に付与します。
func OutputFileName(leftFilePath, rightFilePath string, systemConfig *config.CompareFilesConfig, outputExt string) string {
	rightName := filepath.Base(rightFilePath)

	var b strings.Builder
	b.WriteString(systemConfig.CompareDetailFilePrefix)
	b.WriteString(rightName)

	leftDirPath := absDir(leftFilePath)
	rightDirPath := absDir(rightFilePath)
	if leftDirPath != rightDirPath {
		leftDirs := strings.Split(leftDirPath, string(filepath.Separator))
		rightDirs := strings.Split(rightDirPath, string(filepath.Separator))

		maxLength := len(leftDirs)
		if len(rightDirs) > maxLength {
			maxLength = len(rightDirs)
		}
		isHitDiff := false
		for idx := 0; idx < maxLength; idx++ {
			if !isHitDiff {
				var leftDir, rightDir string
				if idx < len(leftDirs) {
					leftDir = leftDirs[idx]
				}
				if idx < len(rightDirs) {
					rightDir = rightDirs[idx]
				}
				if leftDir != rightDir {
					isHitDiff = true
				}
			}
			if isHitDiff {
				if idx < len(rightDirs) {
					b.WriteByte('_')
					b.WriteString(rightDirs[idx])
				} else {
					break
				}
			}
		}
	}

	b.WriteByte('.')
	b.WriteString(outputExt)
	return b.String()
}

func absDir(filePath string) string {
	abs, err := filepath.Abs(filePath)
	if err != nil {
		abs = filePath
	}
	return filepath.Dir(abs)
}
