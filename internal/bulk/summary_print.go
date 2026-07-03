package bulk

import (
	"log/slog"

	"github.com/scenario-test-framework/compare-files/internal/filecompare"
	"github.com/scenario-test-framework/compare-files/internal/msg"
)

// PrintFileSummary はファイル比較結果の集計を slog へ出力します。
// Java 版 FileCompareFacadeOutput.printDetails 相当。件数は 3 桁カンマ区切り、
// ステータス名は CompareStatus (OK/NG/Ignore/LeftOnly/RightOnly/Error) です。
func PrintFileSummary(result *filecompare.Result) {
	slog.Info(msg.Get("log.summary.result"))
	slog.Info(msg.Get("log.summary.fileUnit"))
	slog.Info(msg.Get("log.summary.subResult", string(result.Status)))
	slog.Info(msg.Get("log.summary.rowUnit"))
	slog.Info(msg.Get("log.summary.totalCount", FormatCount(result.RowCount)))
	slog.Info(msg.Get("log.summary.okCount", FormatCount(result.OkRowCount)))
	slog.Info(msg.Get("log.summary.ngCount", FormatCount(result.NgRowCount)))
	slog.Info(msg.Get("log.summary.ignoreCount", FormatCount(result.IgnoreRowCount)))
	slog.Info(msg.Get("log.summary.leftOnlyCount", FormatCount(result.LeftOnlyRowCount)))
	slog.Info(msg.Get("log.summary.rightOnlyCount", FormatCount(result.RightOnlyRowCount)))
}

// PrintSummary はディレクトリ/正規表現一括比較の集計を slog へ出力します。
// Java 版 BaseDirCompareFacadeOutput.printDetails 相当。
func (c *Counts) PrintSummary() {
	slog.Info(msg.Get("log.summary.processFileCount"))
	slog.Info(msg.Get("log.summary.success", FormatCount(int64(c.SuccessCount))))
	slog.Info(msg.Get("log.summary.failure", FormatCount(int64(c.ErrorCount))))
	slog.Info(msg.Get("log.summary.result"))
	slog.Info(msg.Get("log.summary.dirUnit"))
	slog.Info(msg.Get("log.summary.subResult", string(c.Result())))
	slog.Info(msg.Get("log.summary.fileUnit"))
	slog.Info(msg.Get("log.summary.totalCount", FormatCount(int64(c.TotalCount))))
	slog.Info(msg.Get("log.summary.okCount", FormatCount(int64(c.FileOkCount))))
	slog.Info(msg.Get("log.summary.ngCount", FormatCount(int64(c.FileNgCount))))
	slog.Info(msg.Get("log.summary.ignoreCount", FormatCount(int64(c.FileIgnoreCount))))
	slog.Info(msg.Get("log.summary.leftOnlyCount", FormatCount(int64(c.FileLeftOnlyCount))))
	slog.Info(msg.Get("log.summary.rightOnlyCount", FormatCount(int64(c.FileRightOnlyCount))))
}
