package bulk

import (
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
	"time"

	"github.com/dlclark/regexp2"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/csvio"
	"github.com/scenario-test-framework/compare-files/internal/msg"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

const (
	msgDirNotExist   = "[DirNotExist]"
	msgFileUnmatched = "[FileUnmatched]"
)

// RegexTarget は比較対象設定 (左ディレクトリ, 右ディレクトリ, ファイル名正規表現) です。
type RegexTarget struct {
	LeftDir       string
	RightDir      string
	FileNameRegex string
	pattern       *regexp2.Regexp
}

// LoadRegexTargetList は比較対象設定 CSV をパースします。
// Java 版 CompareRegexTargetList 相当 (# 始まりコメント・空行スキップ・3 列固定)。
func LoadRegexTargetList(filePath string) ([]*RegexTarget, error) {
	f, err := os.Open(filePath)
	if err != nil {
		return nil, fmt.Errorf("ファイルを読込みできません。対象:%s: %w", filePath, err)
	}
	defer f.Close()

	r := csvio.NewReader(f, csvio.CsvConfig())
	var targets []*RegexTarget
	lineNum := 0
	for {
		values, err := r.ReadValues()
		if err != nil {
			return nil, err
		}
		if values == nil {
			return targets, nil
		}
		lineNum++
		// 空行スキップ
		if len(values) == 1 && values[0] == "" {
			continue
		}
		// # で始まる行をスキップ
		if len(values) > 0 && len(values[0]) > 0 && values[0][0] == '#' {
			continue
		}
		if len(values) != 3 {
			return nil, fmt.Errorf("%s", msg.Get("error.file.layout", filePath, config.ConfigCharset, lineNum, fmt.Sprint(values)))
		}
		target, err := newRegexTarget(values[0], values[1], values[2])
		if err != nil {
			return nil, fmt.Errorf("%s: %w", msg.Get("error.file.parse", filePath, config.ConfigCharset, lineNum, fmt.Sprint(values)), err)
		}
		targets = append(targets, target)
	}
}

func newRegexTarget(leftDir, rightDir, fileNameRegex string) (*RegexTarget, error) {
	if leftDir == "" || rightDir == "" || fileNameRegex == "" {
		return nil, fmt.Errorf("左ディレクトリ・右ディレクトリ・ファイル名正規表現は必須です")
	}
	pattern, err := regexp2.Compile(`\A(?:`+fileNameRegex+`)\z`, regexp2.None)
	if err != nil {
		return nil, fmt.Errorf("%s", msg.Get("error.regex.parse", fileNameRegex))
	}
	return &RegexTarget{LeftDir: leftDir, RightDir: rightDir, FileNameRegex: fileNameRegex, pattern: pattern}, nil
}

// CompareRegex は比較対象設定 CSV に従って一括比較します。
// Java 版 FileNameRegexCompareResult.compare 相当。
func CompareRegex(targetConfigFilePath, outputDirPath string, systemConfig *config.CompareFilesConfig, layoutManager *config.LayoutManager) (*Counts, error) {
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

	// 比較対象設定の読込み
	slog.Info(msg.Get("log.regex.loadTargets"))
	targets, err := LoadRegexTargetList(targetConfigFilePath)
	if err != nil {
		return nil, err
	}

	// 比較
	slog.Info(msg.Get("log.regex.compare"))
	for _, target := range targets {
		if err := compareTarget(counts, resultRepo, target, systemConfig, layoutManager, startTime); err != nil {
			return nil, err
		}
	}

	committed = true
	if err := resultRepo.Commit(); err != nil {
		return nil, err
	}

	if systemConfig.DeleteWorkDir {
		os.RemoveAll(filepath.Join(systemConfig.OutputDir, config.DirNameWork))
	}
	return counts, nil
}

// compareTarget は 1 件の比較対象設定を処理します。
func compareTarget(counts *Counts, resultRepo *SummaryRepository, target *RegexTarget, systemConfig *config.CompareFilesConfig, layoutManager *config.LayoutManager, startTime time.Time) error {
	isExistLeftDir := isDir(target.LeftDir)
	var leftFile string
	if isExistLeftDir {
		leftFile = matchedTargetFile(target.LeftDir, target.pattern)
	}

	isExistRightDir := isDir(target.RightDir)
	var rightFile string
	if isExistRightDir {
		rightFile = matchedTargetFile(target.RightDir, target.pattern)
	}

	if leftFile != "" && rightFile != "" {
		// 左右どちらもマッチした場合、比較を実行
		compareFileWithRecover(counts, resultRepo, leftFile, rightFile, systemConfig.OutputDir, systemConfig, layoutManager, startTime)
		return nil
	}

	// チェック NG の結果を出力
	return writeNgResult(counts, resultRepo, isExistLeftDir, isExistRightDir, target, leftFile, rightFile, startTime)
}

// matchedTargetFile はディレクトリ直下で最初に正規表現にマッチしたファイルパスを返します。
// エントリはファイル名順に走査します (Java 版は OS 依存順)。
func matchedTargetFile(dirPath string, pattern *regexp2.Regexp) string {
	entries, err := os.ReadDir(dirPath)
	if err != nil {
		return ""
	}
	for _, entry := range entries {
		ok, err := pattern.MatchString(entry.Name())
		if err == nil && ok {
			return filepath.Join(dirPath, entry.Name())
		}
	}
	return ""
}

// writeNgResult はチェック NG の結果をサマリに出力します。
// Java 版 FileNameRegexCompareResult.writeNgResult 相当。
func writeNgResult(counts *Counts, resultRepo *SummaryRepository, isExistLeftDir, isExistRightDir bool, target *RegexTarget, leftFile, rightFile string, startTime time.Time) error {
	var leftFilePath, rightFilePath string
	var compareStatus status.CompareStatus

	leftDirAbs := absPath(target.LeftDir)
	rightDirAbs := absPath(target.RightDir)

	if isExistLeftDir {
		if isExistRightDir {
			// 左Dir:○、右Dir:○
			if leftFile == "" {
				leftFilePath = msgFileUnmatched + leftDirAbs + string(filepath.Separator) + target.FileNameRegex
				if rightFile == "" {
					rightFilePath = msgFileUnmatched + rightDirAbs + string(filepath.Separator) + target.FileNameRegex
					compareStatus = status.CompareError
				} else {
					rightFilePath = absPath(rightFile)
					compareStatus = status.CompareRightOnly
				}
			} else {
				leftFilePath = absPath(leftFile)
				if rightFile == "" {
					rightFilePath = msgFileUnmatched + rightDirAbs + string(filepath.Separator) + target.FileNameRegex
					compareStatus = status.CompareLeftOnly
				} else {
					return fmt.Errorf("この機能には対応していません")
				}
			}
		} else {
			// 左Dir:○、右Dir:×
			rightFilePath = msgDirNotExist + rightDirAbs
			compareStatus = status.CompareLeftOnly
			if leftFile == "" {
				leftFilePath = leftDirAbs
			} else {
				leftFilePath = absPath(leftFile)
			}
		}
	} else {
		leftFilePath = msgDirNotExist + leftDirAbs
		if isExistRightDir {
			// 左Dir:×、右Dir:○
			compareStatus = status.CompareRightOnly
			if rightFile == "" {
				rightFilePath = rightDirAbs
			} else {
				rightFilePath = absPath(rightFile)
			}
		} else {
			// 左Dir:×、右Dir:×
			rightFilePath = msgDirNotExist + rightDirAbs
			compareStatus = status.CompareError
		}
	}

	result := fixedResult(compareStatus, leftFilePath, rightFilePath, startTime)
	if err := resultRepo.Write(result); err != nil {
		return err
	}
	switch compareStatus {
	case status.CompareLeftOnly, status.CompareRightOnly:
		counts.AddFileResult(compareStatus)
	default:
		counts.AddFileError()
	}
	return nil
}
