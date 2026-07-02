// Package bulk はディレクトリ・正規表現指定の一括比較と、
// ファイル比較のディスパッチを提供します。
// Java 版 sv/domain/compare/bulk + FileCompareResult + ap/Facade 相当です。
package bulk

import (
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/dlclark/regexp2"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/filecompare"
	"github.com/scenario-test-framework/compare-files/internal/imagecmp"
	"github.com/scenario-test-framework/compare-files/internal/msg"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// fileOptions は 1 ファイル比較の内部オプションです。
type fileOptions struct {
	// parallel は並列実行モードです。ソート作業ディレクトリを一意化し、
	// 比較ごとの work ディレクトリ削除をスキップします (一括処理の最後に削除)。
	parallel bool
}

// CompareOneFile は 1 ファイルの比較を実行します。Java 版 FileCompareResult.compare 相当。
//  1. ファイル名除外ルール確認 → Ignore
//  2. ファイルの存在比較 → LeftOnly/RightOnly/Ignore(両方 0 バイト)
//  3. レイアウト判定 (左ファイル名でマッチ)
//  4. 画像 or テキスト比較
//  5. deleteWorkDir 設定時は work ディレクトリを削除
func CompareOneFile(leftFilePath, rightFilePath, outputDirPath string, systemConfig *config.CompareFilesConfig, layoutManager *config.LayoutManager) (*filecompare.Result, error) {
	return compareOneFile(leftFilePath, rightFilePath, outputDirPath, systemConfig, layoutManager, fileOptions{})
}

func compareOneFile(leftFilePath, rightFilePath, outputDirPath string, systemConfig *config.CompareFilesConfig, layoutManager *config.LayoutManager, opts fileOptions) (*filecompare.Result, error) {
	startTime := time.Now()

	if err := os.MkdirAll(outputDirPath, 0o755); err != nil {
		return nil, fmt.Errorf("ディレクトリを作成できません。対象:%s: %w", outputDirPath, err)
	}

	leftAbs := absPath(leftFilePath)
	rightAbs := absPath(rightFilePath)
	leftFileName := filepath.Base(leftFilePath)

	// 既存の出力ファイルを削除 (setupOverwrite 相当)。
	// 拡張子はレイアウト判定前に確定しないため、text/image 両方を削除する。
	for _, ext := range []string{"csv", "png"} {
		outputName := filecompare.OutputFileName(leftFilePath, rightFilePath, systemConfig, ext)
		os.Remove(filepath.Join(outputDirPath, outputName))
	}

	slog.Info(msg.Get("log.file.compare", leftFileName, filepath.Base(rightFilePath)))

	// ファイル名除外ルール確認
	if isIgnoreFile(leftFileName, systemConfig.IgnoreFileRegexList) {
		return &filecompare.Result{
			Status: status.CompareIgnore, LeftFilePath: leftAbs, RightFilePath: rightAbs,
			StartTime: startTime, EndTime: time.Now(),
		}, nil
	}

	// ファイルの存在比較
	existStatus, err := fileExistCheck(leftFilePath, rightFilePath)
	if err != nil {
		return nil, err
	}
	if existStatus != status.CompareOK {
		return &filecompare.Result{
			Status: existStatus, LeftFilePath: leftAbs, RightFilePath: rightAbs,
			StartTime: startTime, EndTime: time.Now(),
		}, nil
	}

	// ファイルレイアウト判定
	fileLayout := layoutManager.GetLayout(leftFileName, systemConfig)

	// ファイルフォーマット判定
	isImage := false
	if fileLayout == nil {
		isImage = imagecmp.IsAllowedExt(fileExt(leftFileName))
	} else {
		isImage = fileLayout.FileFormat == status.FormatImage
	}

	var result *filecompare.Result
	if isImage {
		result, err = filecompare.CompareImage(leftAbs, rightAbs, fileLayout, outputDirPath, systemConfig)
	} else {
		result, err = filecompare.CompareTextOpts(leftAbs, rightAbs, fileLayout, outputDirPath, systemConfig,
			filecompare.TextOptions{UniqueWorkDir: opts.parallel})
	}
	if err != nil {
		return nil, err
	}

	// work ディレクトリの削除 (並列実行時は一括処理の最後にまとめて削除)
	if bool(systemConfig.DeleteWorkDir) && !opts.parallel {
		os.RemoveAll(filepath.Join(outputDirPath, config.DirNameWork))
	}
	return result, nil
}

func absPath(path string) string {
	abs, err := filepath.Abs(path)
	if err != nil {
		return path
	}
	return abs
}

// fileExt はファイル名の拡張子 (ドットなし) を返します。
func fileExt(fileName string) string {
	idx := strings.LastIndex(fileName, ".")
	if idx < 0 {
		return ""
	}
	return fileName[idx+1:]
}

// isIgnoreFile はファイル名が除外正規表現リストにマッチするか返します。
func isIgnoreFile(fileName string, ignoreFileRegexList []string) bool {
	for _, regex := range ignoreFileRegexList {
		re, err := regexp2.Compile(`\A(?:`+regex+`)\z`, regexp2.None)
		if err != nil {
			slog.Error("ファイル名除外ルールの正規表現の評価でエラーが発生しました。", "正規表現", regex, "error", err)
			continue
		}
		ok, err := re.MatchString(fileName)
		if err != nil {
			slog.Error("ファイル名除外ルールの正規表現の評価でエラーが発生しました。", "正規表現", regex, "error", err)
			continue
		}
		if ok {
			slog.Info(msg.Get("log.file.skip", regex, fileName))
			return true
		}
	}
	return false
}

// fileExistCheck はファイルの存在と 0 バイトチェックを行います。
func fileExistCheck(leftFilePath, rightFilePath string) (status.CompareStatus, error) {
	leftInfo, leftErr := os.Stat(leftFilePath)
	rightInfo, rightErr := os.Stat(rightFilePath)
	leftExists := leftErr == nil
	rightExists := rightErr == nil

	switch {
	case leftExists && !rightExists:
		return status.CompareLeftOnly, nil
	case !leftExists && rightExists:
		return status.CompareRightOnly, nil
	case !leftExists && !rightExists:
		return "", fmt.Errorf("%s", msg.Get("error.compare.file.bothNotExist", leftFilePath, rightFilePath))
	}

	if leftInfo.Size() == 0 && rightInfo.Size() == 0 {
		return status.CompareIgnore, nil
	}
	return status.CompareOK, nil
}
