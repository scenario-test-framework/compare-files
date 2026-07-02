package filecompare

import (
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
	"sync"
	"time"

	"github.com/scenario-test-framework/compare-files/internal/compare"
	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/msg"
	"github.com/scenario-test-framework/compare-files/internal/reader"
	"github.com/scenario-test-framework/compare-files/internal/row"
	"github.com/scenario-test-framework/compare-files/internal/sortfile"
	"github.com/scenario-test-framework/compare-files/internal/status"
	"github.com/scenario-test-framework/compare-files/internal/writer"
)

const textOutputExt = "csv"

// TextOptions はテキスト比較の動作オプションです。
type TextOptions struct {
	// UniqueWorkDir はソート作業ディレクトリを比較ごとに一意にします。
	// 並列実行時の同名ファイル衝突を回避するために使用します。
	UniqueWorkDir bool
}

// CompareText はテキストファイルを比較します。Java 版 TextFileCompareResult.compare 相当。
// 左右をソートしてからキーでマージ結合し、行・項目単位の比較結果を詳細ファイルに出力します。
func CompareText(leftFilePath, rightFilePath string, fileLayout *config.FileLayout, outputDirPath string, systemConfig *config.CompareFilesConfig) (*Result, error) {
	return CompareTextOpts(leftFilePath, rightFilePath, fileLayout, outputDirPath, systemConfig, TextOptions{})
}

// CompareTextOpts はオプション付きのテキスト比較です。
func CompareTextOpts(leftFilePath, rightFilePath string, fileLayout *config.FileLayout, outputDirPath string, systemConfig *config.CompareFilesConfig, opts TextOptions) (*Result, error) {
	result := &Result{
		Status:        status.CompareProcessing,
		LeftFilePath:  leftFilePath,
		RightFilePath: rightFilePath,
		FileLayout:    fileLayout,
		StartTime:     time.Now(),
	}

	// 文字コード判定: ファイルレイアウト > システムデフォルト > UTF-8
	charsetName := ""
	if fileLayout != nil {
		charsetName = fileLayout.Charset
	}
	if charsetName == "" {
		charsetName = systemConfig.DefaultInputCharset
	}
	if charsetName == "" {
		charsetName = config.ConfigCharset
	}

	// ソート
	leftSortedPath := leftFilePath
	rightSortedPath := rightFilePath
	if !isSkipSort(fileLayout, systemConfig) {
		slog.Info(msg.Get("log.text.sort"))
		var leftStatus, rightStatus status.ProcessStatus
		var leftErr, rightErr error
		var wg sync.WaitGroup
		wg.Add(2)
		leftSortedDir := filepath.Join(outputDirPath, config.DirNameWork, "left")
		rightSortedDir := filepath.Join(outputDirPath, config.DirNameWork, "right")
		if opts.UniqueWorkDir {
			var err error
			if leftSortedDir, err = uniqueDir(leftSortedDir); err != nil {
				return nil, err
			}
			if rightSortedDir, err = uniqueDir(rightSortedDir); err != nil {
				return nil, err
			}
		}
		leftSortedPath = filepath.Join(leftSortedDir, filepath.Base(leftFilePath))
		rightSortedPath = filepath.Join(rightSortedDir, filepath.Base(rightFilePath))
		go func() {
			defer wg.Done()
			leftStatus, leftErr = sortfile.Sort(leftFilePath, charsetName, leftSortedDir, fileLayout, systemConfig)
		}()
		go func() {
			defer wg.Done()
			rightStatus, rightErr = sortfile.Sort(rightFilePath, charsetName, rightSortedDir, fileLayout, systemConfig)
		}()
		wg.Wait()
		if leftErr != nil || leftStatus == status.ProcessFailure {
			return nil, fmt.Errorf("Sort.Left でエラーが発生しました。file:%s: %w", filepath.Base(leftFilePath), leftErr)
		}
		if rightErr != nil || rightStatus == status.ProcessFailure {
			return nil, fmt.Errorf("Sort.Right でエラーが発生しました。file:%s: %w", filepath.Base(rightFilePath), rightErr)
		}
	}

	// ファイル比較
	slog.Info(msg.Get("log.text.compare"))
	outputFileName := OutputFileName(leftFilePath, rightFilePath, systemConfig, textOutputExt)
	outputFilePath := filepath.Join(outputDirPath, outputFileName)
	if err := compareTextFiles(result, leftSortedPath, rightSortedPath, charsetName, outputFilePath, fileLayout, systemConfig); err != nil {
		return nil, err
	}

	result.EndTime = time.Now()
	return result, nil
}

// uniqueDir は base 配下に一意なサブディレクトリを作成して返します。
func uniqueDir(base string) (string, error) {
	if err := os.MkdirAll(base, 0o755); err != nil {
		return "", fmt.Errorf("ディレクトリを作成できません。対象:%s: %w", base, err)
	}
	dir, err := os.MkdirTemp(base, "p")
	if err != nil {
		return "", fmt.Errorf("ディレクトリを作成できません。対象:%s: %w", base, err)
	}
	return dir, nil
}

// isSkipSort はソートフェーズをスキップするか判断します。
func isSkipSort(fileLayout *config.FileLayout, systemConfig *config.CompareFilesConfig) bool {
	if fileLayout == nil {
		return true
	}
	if fileLayout.FileFormat == status.FormatFixed && fileLayout.LineSp == status.LineSpNone {
		return true
	}
	// Json/Yaml は 1 ファイル 1 レコード、path・value モードはリーダがソート済みの
	// ペアを返すため、ファイルソートは不要
	if fileLayout.FileFormat == status.FormatJSON || fileLayout.FileFormat == status.FormatYaml {
		return true
	}
	if fileLayout.IsPathValue() {
		return true
	}
	return bool(systemConfig.Sorted)
}

// compareTextFiles はソート済みの左右ファイルをマージ結合で比較します。
func compareTextFiles(result *Result, leftFilePath, rightFilePath, charsetName, outputFilePath string, fileLayout *config.FileLayout, systemConfig *config.CompareFilesConfig) error {
	opts := reader.Options{
		OverrideCharset:               charsetName,
		CsvHeaderRow:                  int(systemConfig.CsvHeaderRow),
		CsvDataStartRow:               int(systemConfig.CsvDataStartRow),
		CodeValueForOnlyOneRecordType: systemConfig.CodeValueForOnlyOneRecordType,
	}

	leftReader, err := reader.New(leftFilePath, fileLayout, opts)
	if err != nil {
		return err
	}
	defer leftReader.Close()

	rightReader, err := reader.New(rightFilePath, fileLayout, opts)
	if err != nil {
		return err
	}
	defer rightReader.Close()

	rowResultRepo, err := writer.NewDetailRepository(
		outputFilePath,
		systemConfig.OutputCharset,
		bool(systemConfig.WriteDiffOnly),
		systemConfig.LeftPrefix,
		systemConfig.RightPrefix,
	)
	if err != nil {
		return err
	}
	committed := false
	defer func() {
		if !committed {
			rowResultRepo.Rollback()
		}
	}()

	leftCurRow, err := leftReader.Next()
	if err != nil {
		return err
	}
	rightCurRow, err := rightReader.Next()
	if err != nil {
		return err
	}

	// 対象行 0 件
	if leftCurRow == nil && rightCurRow == nil {
		result.Status = status.CompareIgnore
	}

	addFixed := func(st status.CompareStatus, leftRow, rightRow *row.Row) error {
		rowResult := compare.FixedRowResult(st, fileLayout, leftRow, rightRow)
		return addRow(result, rowResult, rowResultRepo)
	}

	for {
		if leftCurRow == nil {
			if rightCurRow == nil {
				break
			}
			// 左が EOF: 右のみ
			if err := addFixed(status.CompareRightOnly, nil, rightCurRow); err != nil {
				return err
			}
			if rightCurRow, err = rightReader.Next(); err != nil {
				return err
			}
			continue
		}
		if rightCurRow == nil {
			// 右が EOF: 左のみ
			if err := addFixed(status.CompareLeftOnly, leftCurRow, nil); err != nil {
				return err
			}
			if leftCurRow, err = leftReader.Next(); err != nil {
				return err
			}
			continue
		}

		keyCompareResult, err := compare.CompareRowKeys(leftCurRow, rightCurRow)
		if err != nil {
			return err
		}
		switch {
		case keyCompareResult < 0:
			if err := addFixed(status.CompareLeftOnly, leftCurRow, nil); err != nil {
				return err
			}
			if leftCurRow, err = leftReader.Next(); err != nil {
				return err
			}
		case keyCompareResult > 0:
			if err := addFixed(status.CompareRightOnly, nil, rightCurRow); err != nil {
				return err
			}
			if rightCurRow, err = rightReader.Next(); err != nil {
				return err
			}
		default:
			rowResult, err := compare.CompareRows(fileLayout, leftCurRow, rightCurRow)
			if err != nil {
				return err
			}
			if err := addRow(result, rowResult, rowResultRepo); err != nil {
				return err
			}
			if leftCurRow, err = leftReader.Next(); err != nil {
				return err
			}
			if rightCurRow, err = rightReader.Next(); err != nil {
				return err
			}
		}
	}

	committed = true
	return rowResultRepo.Commit()
}

// addRow はサマリ項目を更新し、行比較結果を出力します。
func addRow(result *Result, rowResult *compare.RowResult, repo *writer.DetailRepository) error {
	result.updateFileStatus(rowResult.Status)
	result.updateRowSummaryFields(rowResult.Status)
	return repo.Write(rowResult)
}
