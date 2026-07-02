package cli

import (
	"github.com/scenario-test-framework/compare-files/internal/config"
)

// OverwriteConfig はコマンドラインオプションでシステム設定を上書きします。
// Java 版 CompareFilesOption.overwriteConfig と同一セマンティクス
// (値が設定された項目のみ上書き。int は 0 以外、bool は true のみ)。
func (o *Option) OverwriteConfig(cfg *config.CompareFilesConfig) {
	if o.DeleteWorkDir {
		cfg.DeleteWorkDir = true
	}
	if ic := EscapeQuote(o.InputCharset); ic != "" {
		cfg.DefaultInputCharset = ic
	}
	if o.Sorted {
		cfg.Sorted = true
	}
	if o.CsvHeaderRow != 0 {
		cfg.CsvHeaderRow = config.JInt(o.CsvHeaderRow)
	}
	if o.CsvDataStartRow != 0 {
		cfg.CsvDataStartRow = config.JInt(o.CsvDataStartRow)
	}
	if od := EscapeQuote(o.OutputDir); od != "" {
		cfg.OutputDir = od
	}
	if of := EscapeQuote(o.CompareResultFileName); of != "" {
		cfg.CompareResultFileName = of
	}
	if oc := EscapeQuote(o.OutputCharset); oc != "" {
		cfg.OutputCharset = oc
	}
	if o.WriteDiffOnly {
		cfg.WriteDiffOnly = true
	}
	if lp := EscapeQuote(o.LeftPrefix); lp != "" {
		cfg.LeftPrefix = lp
	}
	if rp := EscapeQuote(o.RightPrefix); rp != "" {
		cfg.RightPrefix = rp
	}
	if dfp := EscapeQuote(o.CompareDetailFilePrefix); dfp != "" {
		cfg.CompareDetailFilePrefix = dfp
	}
	if o.ChunkSize != 0 {
		cfg.ChunkSize = config.JInt(o.ChunkSize)
	}
	if len(o.IgnoreItemList) > 0 {
		cfg.IgnoreItemList = o.IgnoreItemList
	}
	if layout := EscapeQuote(o.OverwriteLayoutDir); layout != "" {
		cfg.OverwriteLayoutDir = layout
	}
}
