package bulk

import (
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
	"sort"
	"time"
	"unicode/utf16"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/filecompare"
	"github.com/scenario-test-framework/compare-files/internal/msg"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// Counts は一括比較の集計です。Java 版 BaseBulkCompareResult 相当。
type Counts struct {
	TotalCount         int
	SuccessCount       int
	ErrorCount         int
	FileOkCount        int
	FileNgCount        int
	FileIgnoreCount    int
	FileLeftOnlyCount  int
	FileRightOnlyCount int
}

// AddFileResult はファイル比較結果を集計します。
func (c *Counts) AddFileResult(st status.CompareStatus) {
	c.TotalCount++
	if st == status.CompareError {
		c.ErrorCount++
		return
	}
	c.SuccessCount++
	switch st {
	case status.CompareOK:
		c.FileOkCount++
	case status.CompareNG:
		c.FileNgCount++
	case status.CompareIgnore:
		c.FileIgnoreCount++
	case status.CompareLeftOnly:
		c.FileLeftOnlyCount++
	case status.CompareRightOnly:
		c.FileRightOnlyCount++
	}
}

// AddFileError はエラー件数を加算します。
func (c *Counts) AddFileError() {
	c.TotalCount++
	c.ErrorCount++
}

// Result は一括比較のディレクトリ単位結果を返します。
// Java 版 BaseDirCompareFacadeOutput.getResult 相当。
func (c *Counts) Result() status.CompareStatus {
	switch {
	case c.TotalCount == 0 && c.ErrorCount == 0:
		return status.CompareIgnore
	case c.ErrorCount > 0:
		return status.CompareError
	case c.TotalCount > 0 && c.TotalCount == c.FileOkCount+c.FileIgnoreCount:
		return status.CompareOK
	default:
		return status.CompareNG
	}
}

// ProcessStatus は一括比較の終了ステータスを返します。
// Java 版 BaseDirCompareFacadeOutput.fixProcessStatus 相当。
func (c *Counts) ProcessStatus() status.ProcessStatus {
	switch c.Result() {
	case status.CompareError:
		return status.ProcessFailure
	case status.CompareOK, status.CompareIgnore:
		return status.ProcessSuccess
	default:
		return status.ProcessWarning
	}
}

// CompareDir はディレクトリ配下 (サブディレクトリ含む) を一括比較します。
// Java 版 DirCompareResult.compare 相当。出力ディレクトリは初期化 (削除して再作成) されます。
func CompareDir(leftDirPath, rightDirPath, outputDirPath string, systemConfig *config.CompareFilesConfig, layoutManager *config.LayoutManager) (*Counts, error) {
	counts := &Counts{}

	// 出力ディレクトリの初期化
	if err := os.RemoveAll(outputDirPath); err != nil {
		return nil, fmt.Errorf("ディレクトリを削除できません。対象:%s: %w", outputDirPath, err)
	}
	if err := os.MkdirAll(outputDirPath, 0o755); err != nil {
		return nil, fmt.Errorf("ディレクトリを作成できません。対象:%s: %w", outputDirPath, err)
	}

	startTime := time.Now()

	resultRepo, err := NewSummaryRepository(
		filepath.Join(outputDirPath, systemConfig.CompareResultFileName),
		systemConfig.OutputCharset,
	)
	if err != nil {
		return nil, err
	}
	committed := false
	defer func() {
		if !committed {
			resultRepo.Rollback()
		}
	}()

	// ディレクトリの存在比較
	leftExists := isDir(leftDirPath)
	rightExists := isDir(rightDirPath)
	switch {
	case leftExists && !rightExists:
		slog.Info("・[左のみ]" + leftDirPath)
		result := fixedResult(status.CompareLeftOnly, leftDirPath, rightDirPath, startTime)
		counts.AddFileResult(result.Status)
		if err := resultRepo.Write(result); err != nil {
			return nil, err
		}
		committed = true
		return counts, resultRepo.Commit()
	case !leftExists && rightExists:
		slog.Info("・[右のみ]" + rightDirPath)
		result := fixedResult(status.CompareRightOnly, leftDirPath, rightDirPath, startTime)
		counts.AddFileResult(result.Status)
		if err := resultRepo.Write(result); err != nil {
			return nil, err
		}
		committed = true
		return counts, resultRepo.Commit()
	case !leftExists && !rightExists:
		// rollback して空サマリを残す (Java 版と同じ)
		resultRepo.Rollback()
		committed = true
		return nil, fmt.Errorf("%s", msg.Get("error.compare.dir.bothNotExist", leftDirPath, rightDirPath))
	}

	// ファイル走査: 左右の相対パスをマージして辞書順にソート
	slog.Info("・ファイル走査")
	leftRelPaths, err := relPathList(leftDirPath)
	if err != nil {
		return nil, err
	}
	rightRelPaths, err := relPathList(rightDirPath)
	if err != nil {
		return nil, err
	}
	merged := append([]string{}, leftRelPaths...)
	seen := map[string]bool{}
	for _, p := range merged {
		seen[p] = true
	}
	for _, p := range rightRelPaths {
		if !seen[p] {
			merged = append(merged, p)
			seen[p] = true
		}
	}
	sort.Slice(merged, func(i, j int) bool {
		return compareJavaString(merged[i], merged[j]) < 0
	})

	// ディレクトリ比較 (ワーカープールで並列実行、サマリは入力順で出力)
	slog.Info("・ディレクトリ比較")
	pairs := make([]filePair, len(merged))
	for i, relPath := range merged {
		pairs[i] = filePair{leftFilePath: leftDirPath + relPath, rightFilePath: rightDirPath + relPath}
	}
	for _, result := range compareFilesConcurrently(pairs, outputDirPath, systemConfig, layoutManager, startTime) {
		if werr := resultRepo.Write(result); werr != nil {
			slog.Error("サマリ出力に失敗しました。", "error", werr)
		}
		counts.AddFileResult(result.Status)
	}

	committed = true
	if err := resultRepo.Commit(); err != nil {
		return nil, err
	}

	// work ディレクトリの削除
	if systemConfig.DeleteWorkDir {
		os.RemoveAll(filepath.Join(systemConfig.OutputDir, config.DirNameWork))
	}
	return counts, nil
}

// compareFileWithRecover はファイル比較を実行し、エラー時は Error 行として集計を継続します。
func compareFileWithRecover(counts *Counts, resultRepo *SummaryRepository, leftFilePath, rightFilePath, outputDirPath string, systemConfig *config.CompareFilesConfig, layoutManager *config.LayoutManager, startTime time.Time) {
	result, err := CompareOneFile(leftFilePath, rightFilePath, outputDirPath, systemConfig, layoutManager)
	if err != nil {
		slog.Error(msg.Get("errorHandle", "FileCompare", "left="+leftFilePath+", right="+rightFilePath), "error", err)
		errResult := fixedResult(status.CompareError, leftFilePath, rightFilePath, startTime)
		counts.AddFileResult(errResult.Status)
		if werr := resultRepo.Write(errResult); werr != nil {
			slog.Error("サマリ出力に失敗しました。", "error", werr)
		}
		return
	}
	if werr := resultRepo.Write(result); werr != nil {
		slog.Error("サマリ出力に失敗しました。", "error", werr)
	}
	counts.AddFileResult(result.Status)
}

// fixedResult は固定ステータスのファイル比較結果を返します。
func fixedResult(st status.CompareStatus, leftPath, rightPath string, t time.Time) *filecompare.Result {
	return &filecompare.Result{
		Status:        st,
		LeftFilePath:  leftPath,
		RightFilePath: rightPath,
		StartTime:     t,
		EndTime:       t,
	}
}

func isDir(path string) bool {
	info, err := os.Stat(path)
	return err == nil && info.IsDir()
}

// relPathList はサブディレクトリを含む全ファイルの相対パス ("/" 始まり) を返します。
func relPathList(dirPath string) ([]string, error) {
	var paths []string
	root := filepath.Clean(dirPath)
	err := filepath.WalkDir(root, func(path string, d os.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if d.IsDir() {
			return nil
		}
		rel, err := filepath.Rel(root, path)
		if err != nil {
			return err
		}
		paths = append(paths, string(filepath.Separator)+rel)
		return nil
	})
	if err != nil {
		return nil, err
	}
	return paths, nil
}

// compareJavaString は Java String.compareTo と同じ UTF-16 コード単位順の比較です。
func compareJavaString(a, b string) int {
	ua := utf16.Encode([]rune(a))
	ub := utf16.Encode([]rune(b))
	for i := 0; i < len(ua) && i < len(ub); i++ {
		if ua[i] != ub[i] {
			if ua[i] < ub[i] {
				return -1
			}
			return 1
		}
	}
	return len(ua) - len(ub)
}
