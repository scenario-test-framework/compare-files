package cli

import (
	"reflect"
	"testing"

	"github.com/scenario-test-framework/compare-files/internal/config"
)

func TestParseArgs(t *testing.T) {
	opt, err := ParseArgs([]string{
		"-s", "--csvHeaderRow", "1", "-cd", "2",
		"-od", "/tmp/out", "-oc", "ms932",
		"-ignore", "A,B", "-ignore", "C",
		"left.txt", "right.txt",
	})
	if err != nil {
		t.Fatal(err)
	}
	if !opt.Sorted {
		t.Error("sorted")
	}
	if opt.CsvHeaderRow != 1 || opt.CsvDataStartRow != 2 {
		t.Errorf("csv rows: %d/%d", opt.CsvHeaderRow, opt.CsvDataStartRow)
	}
	if opt.OutputDir != "/tmp/out" || opt.OutputCharset != "ms932" {
		t.Errorf("out: %q/%q", opt.OutputDir, opt.OutputCharset)
	}
	if !reflect.DeepEqual(opt.IgnoreItemList, []string{"A", "B", "C"}) {
		t.Errorf("ignoreItemList: %v", opt.IgnoreItemList)
	}
	if !reflect.DeepEqual(opt.ParamList, []string{"left.txt", "right.txt"}) {
		t.Errorf("paramList: %v", opt.ParamList)
	}
}

func TestParseArgsMixedPosition(t *testing.T) {
	// JCommander 互換: 位置引数の後のオプションも解釈する
	opt, err := ParseArgs([]string{"left.txt", "right.txt", "-od", "out"})
	if err != nil {
		t.Fatal(err)
	}
	if opt.OutputDir != "out" {
		t.Errorf("位置引数後のオプションが解釈されない: %q", opt.OutputDir)
	}
	if len(opt.ParamList) != 2 {
		t.Errorf("paramList: %v", opt.ParamList)
	}
}

func TestParseArgsUnknownOption(t *testing.T) {
	if _, err := ParseArgs([]string{"-unknown", "value"}); err == nil {
		t.Error("不明なオプションでエラーにならない")
	}
}

func TestEscapeQuote(t *testing.T) {
	tests := map[string]string{
		"'quoted'": "quoted",
		"plain":    "plain",
		"":         "",
		"'":        "'",
	}
	for in, want := range tests {
		if got := EscapeQuote(in); got != want {
			t.Errorf("EscapeQuote(%q) = %q, want %q", in, got, want)
		}
	}
}

func TestOverwriteConfig(t *testing.T) {
	opt, err := ParseArgs([]string{"-wdo", "-dpl", "'L>'", "-chunk", "500", "-ic", "sjis"})
	if err != nil {
		t.Fatal(err)
	}
	cfg := config.NewConfig()
	cfg.LeftPrefix = "期待値:"
	cfg.RightPrefix = "実績値:"
	opt.OverwriteConfig(cfg)

	if !cfg.WriteDiffOnly {
		t.Error("writeDiffOnly")
	}
	if cfg.LeftPrefix != "L>" {
		t.Errorf("leftPrefix のクォート除去: %q", cfg.LeftPrefix)
	}
	if cfg.RightPrefix != "実績値:" {
		t.Errorf("未指定項目が上書きされた: %q", cfg.RightPrefix)
	}
	if cfg.ChunkSize != 500 {
		t.Errorf("chunkSize: %d", cfg.ChunkSize)
	}
	if cfg.DefaultInputCharset != "sjis" {
		t.Errorf("inputCharset: %q", cfg.DefaultInputCharset)
	}
}

func TestLoadDefaultConfigEmbedded(t *testing.T) {
	t.Chdir(t.TempDir()) // 探索ディレクトリに設定ファイルが無い状態
	cfg, path, err := LoadDefaultConfig()
	if err != nil {
		t.Fatal(err)
	}
	if path != "embedded:compare_files.json" {
		t.Errorf("path: %q", path)
	}
	if cfg.OutputDir != "result" || !cfg.DeleteWorkDir {
		t.Errorf("同梱デフォルトが読めていない: %+v", cfg)
	}
}
