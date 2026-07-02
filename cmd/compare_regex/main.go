// compare_regex は対象指定 (正規表現) 比較のエントリポイントです。
// Usage: compare_regex [Options] TARGET_FILE_PATH
package main

import (
	"fmt"
	_ "time/tzdata" // scratch イメージでもタイムゾーンを解決できるように埋め込む

	"log/slog"
	"os"

	"github.com/scenario-test-framework/compare-files/internal/bulk"
	"github.com/scenario-test-framework/compare-files/internal/cli"
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
	if opt.Help {
		printUsage()
		return status.ExitCodeSuccess
	}
	if opt.GetConfigFilePath() == "" && len(opt.ParamList) != 1 {
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

	if len(opt.ParamList) < 1 {
		printUsage()
		slog.Error(msg.Get("error.arg"))
		return status.ExitCodeError
	}
	targetConfigFilePath := opt.ParamList[0]

	slog.Info("・入力情報")
	slog.Info("  ・比較対象設定ファイル            : " + targetConfigFilePath)

	counts, err := bulk.CompareRegex(targetConfigFilePath, cfg.OutputDir, cfg, layoutManager)
	if err != nil {
		fmt.Fprintln(os.Stderr, err)
		slog.Error(msg.Get("exit.fail"))
		return status.ExitCodeError
	}
	return cli.ExitCode(counts.ProcessStatus())
}

func printUsage() {
	fmt.Fprintln(os.Stderr, "Usage: compare_regex [Options] TARGET_FILE_PATH\n\n"+cli.Usage())
}
