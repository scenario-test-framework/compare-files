// compare_files はファイル比較・ディレクトリ比較のエントリポイントです。
// Usage: compare_files [Options] LEFT_PATH RIGHT_PATH
// 左パスがファイルの場合はファイル比較、それ以外はディレクトリ比較を実行します。
package main

import (
	"fmt"
	_ "time/tzdata" // scratch イメージでもタイムゾーンを解決できるように埋め込む

	"log/slog"
	"os"

	"github.com/scenario-test-framework/compare-files/internal/bulk"
	"github.com/scenario-test-framework/compare-files/internal/cli"
	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/msg"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

func main() {
	os.Exit(run(append(cli.EnvArgs(), os.Args[1:]...)))
}

func run(args []string) int {
	slog.Info(msg.Get("process.start"))

	opt, err := cli.ParseArgs(args)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		printUsage()
		return status.ExitCodeError
	}
	if opt.ShowVersion {
		fmt.Println("compare-files " + cli.Version)
		return status.ExitCodeSuccess
	}
	if opt.Help {
		printUsage()
		return status.ExitCodeSuccess
	}
	// レイアウト定義の検証モード
	if opt.LintLayout != "" {
		return lintLayout(opt.LintLayout)
	}
	// 設定ファイルパスが指定されていない場合、左右パスの指定が必須
	if opt.GetConfigFilePath() == "" && len(opt.ParamList) != 2 {
		printUsage()
		slog.Error(msg.Get("error.arg"))
		return status.ExitCodeError
	}

	cfg, err := cli.LoadConfig(opt)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		slog.Error(msg.Get("exit.fail"))
		return status.ExitCodeError
	}

	layoutManager, err := cli.LoadLayoutManager(cfg)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		slog.Error(msg.Get("exit.fail"))
		return status.ExitCodeError
	}

	if len(opt.ParamList) < 2 {
		printUsage()
		slog.Error(msg.Get("error.arg"))
		return status.ExitCodeError
	}
	leftPath := opt.ParamList[0]
	rightPath := opt.ParamList[1]
	outputDirPath := cfg.OutputDir

	slog.Info("・入力情報")
	slog.Info("  ・左パス                          : " + leftPath)
	slog.Info("  ・右パス                          : " + rightPath)

	var processStatus status.ProcessStatus
	if isFile(leftPath) {
		slog.Info("  ・比較モード                      : ファイル比較")
		_, st, err := bulk.CompareFile(leftPath, rightPath, outputDirPath, cfg, layoutManager)
		if err != nil {
			fmt.Fprintln(os.Stderr, err)
			slog.Error(msg.Get("exit.fail"))
			return status.ExitCodeError
		}
		processStatus = st
	} else {
		slog.Info("  ・比較モード                      : ディレクトリ比較")
		counts, err := bulk.CompareDir(leftPath, rightPath, outputDirPath, cfg, layoutManager)
		if err != nil {
			fmt.Fprintln(os.Stderr, err)
			slog.Error(msg.Get("exit.fail"))
			return status.ExitCodeError
		}
		processStatus = counts.ProcessStatus()
	}

	return cli.ExitCode(processStatus)
}

func isFile(path string) bool {
	info, err := os.Stat(path)
	return err == nil && !info.IsDir()
}

// lintLayout はレイアウト定義ファイルを検証し、結果を表示します。
// エラーなし (警告のみ含む) なら 0、エラーありなら 6 を返します。
func lintLayout(path string) int {
	issues := config.LintLayoutFile(path)
	hasError := false
	for _, issue := range issues {
		fmt.Fprintln(os.Stderr, issue)
		if issue.Error {
			hasError = true
		}
	}
	if hasError {
		fmt.Fprintf(os.Stderr, "NG: %s にエラーがあります\n", path)
		return status.ExitCodeError
	}
	fmt.Printf("OK: %s\n", path)
	return status.ExitCodeSuccess
}

func printUsage() {
	fmt.Fprintln(os.Stderr, "Usage: compare_files [Options] LEFT_PATH RIGHT_PATH\n\n"+cli.Usage())
}
