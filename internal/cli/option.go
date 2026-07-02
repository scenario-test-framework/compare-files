// Package cli はコマンドラインオプションのパースと起動骨格を提供します。
// Java 版 main パッケージ (CompareFilesOption / BaseCompareFiles) 相当です。
package cli

import (
	"fmt"
	"strconv"
	"strings"
)

// Option はコマンドラインオプションです。Java 版 CompareFilesOption 相当。
type Option struct {
	Help                    bool
	ConfigFilePath          string
	DeleteWorkDir           bool
	InputCharset            string
	Sorted                  bool
	CsvHeaderRow            int
	CsvDataStartRow         int
	OutputDir               string
	CompareResultFileName   string
	OutputCharset           string
	WriteDiffOnly           bool
	LeftPrefix              string
	RightPrefix             string
	CompareDetailFilePrefix string
	ChunkSize               int
	IgnoreItemList          []string
	OverwriteLayoutDir      string
	ParamList               []string
}

// フラグ定義。JCommander の @Parameter(names={short, long}) を再現します。
type flagDef struct {
	names  []string
	isBool bool
	isInt  bool
	isList bool
	assign func(o *Option, value string) error
}

var flagDefs = []flagDef{
	{names: []string{"-h", "--help"}, isBool: true, assign: func(o *Option, _ string) error { o.Help = true; return nil }},
	{names: []string{"-config", "--configFile"}, assign: func(o *Option, v string) error { o.ConfigFilePath = v; return nil }},
	{names: []string{"-d", "--deleteWorkDir"}, isBool: true, assign: func(o *Option, _ string) error { o.DeleteWorkDir = true; return nil }},
	{names: []string{"-ic", "--inputCharset"}, assign: func(o *Option, v string) error { o.InputCharset = v; return nil }},
	{names: []string{"-s", "--sorted"}, isBool: true, assign: func(o *Option, _ string) error { o.Sorted = true; return nil }},
	{names: []string{"-ch", "--csvHeaderRow"}, isInt: true, assign: func(o *Option, v string) error { return assignInt(v, &o.CsvHeaderRow) }},
	{names: []string{"-cd", "--csvDataStartRow"}, isInt: true, assign: func(o *Option, v string) error { return assignInt(v, &o.CsvDataStartRow) }},
	{names: []string{"-od", "--outputDir"}, assign: func(o *Option, v string) error { o.OutputDir = v; return nil }},
	{names: []string{"-of", "--compareResultFileName"}, assign: func(o *Option, v string) error { o.CompareResultFileName = v; return nil }},
	{names: []string{"-oc", "--outputCharset"}, assign: func(o *Option, v string) error { o.OutputCharset = v; return nil }},
	{names: []string{"-wdo", "--writeDiffOnly"}, isBool: true, assign: func(o *Option, _ string) error { o.WriteDiffOnly = true; return nil }},
	{names: []string{"-dpl", "--leftPrefix"}, assign: func(o *Option, v string) error { o.LeftPrefix = v; return nil }},
	{names: []string{"-dpr", "--rightPrefix"}, assign: func(o *Option, v string) error { o.RightPrefix = v; return nil }},
	{names: []string{"-dfp", "--compareDetailFilePrefix"}, assign: func(o *Option, v string) error { o.CompareDetailFilePrefix = v; return nil }},
	{names: []string{"-chunk", "--chunkSize"}, isInt: true, assign: func(o *Option, v string) error { return assignInt(v, &o.ChunkSize) }},
	{names: []string{"-ignore", "--ignoreItemList"}, isList: true, assign: func(o *Option, v string) error {
		for _, item := range strings.Split(v, ",") {
			o.IgnoreItemList = append(o.IgnoreItemList, item)
		}
		return nil
	}},
	{names: []string{"-layout", "--overwriteLayoutDir"}, assign: func(o *Option, v string) error { o.OverwriteLayoutDir = v; return nil }},
}

func assignInt(v string, target *int) error {
	n, err := strconv.Atoi(v)
	if err != nil {
		return fmt.Errorf("数値に変換できません: %q", v)
	}
	*target = n
	return nil
}

// ParseArgs はコマンドライン引数をパースします。
// JCommander と同様に、オプションと位置引数の混在を許容します。
func ParseArgs(args []string) (*Option, error) {
	opt := &Option{}
	lookup := map[string]*flagDef{}
	for i := range flagDefs {
		for _, name := range flagDefs[i].names {
			lookup[name] = &flagDefs[i]
		}
	}
	for i := 0; i < len(args); i++ {
		arg := args[i]
		def, ok := lookup[arg]
		if !ok {
			if strings.HasPrefix(arg, "-") && arg != "-" {
				return nil, fmt.Errorf("不明なオプションです: %s", arg)
			}
			opt.ParamList = append(opt.ParamList, arg)
			continue
		}
		if def.isBool {
			if err := def.assign(opt, ""); err != nil {
				return nil, err
			}
			continue
		}
		if i+1 >= len(args) {
			return nil, fmt.Errorf("オプション %s の値が指定されていません", arg)
		}
		i++
		if err := def.assign(opt, args[i]); err != nil {
			return nil, fmt.Errorf("オプション %s: %w", arg, err)
		}
	}
	return opt, nil
}

// EscapeQuote はシングルクォートで括られた文字列から括り文字を除去します。
// Java 版 CompareFilesOption.escapeQuote 相当。
func EscapeQuote(s string) string {
	if len(s) >= 2 && s[0] == '\'' && s[len(s)-1] == '\'' {
		return s[1 : len(s)-1]
	}
	return s
}

// GetConfigFilePath はクォート除去済みの設定ファイルパスを返します。
func (o *Option) GetConfigFilePath() string {
	return EscapeQuote(o.ConfigFilePath)
}
