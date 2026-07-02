// Package config はシステム設定・レイアウト定義の読み込みとマージを提供します。
// Java 版 me.suwash.tools.comparefiles.infra.config 相当です。
package config

import (
	"encoding/json"
	"fmt"
	"math"
	"os"

	"github.com/scenario-test-framework/compare-files/internal/charset"
)

// システム定数。Java 版 infra/Const.java と同値。
const (
	DefaultItemID      = "value"
	DefaultChunkSize   = 1000
	DefaultIntValue    = math.MinInt32 // Java の Integer.MIN_VALUE
	DummyValue         = "-"
	UnknownLine        = -1
	DirNameWork        = "work"
	RangeReport        = 100
	DefaultPrefixLeft  = "L:"
	DefaultPrefixRight = "R:"
	FormatTimestamp    = "2006-01-02 15:04:05.000" // Java: yyyy-MM-dd HH:mm:ss.SSS
	LayoutDirName      = "compare_layout"
	ConfigCharset      = "UTF-8"
)

// Rectangle は画像比較の除外エリアです。Java 版 java.awt.Rectangle 相当。
type Rectangle struct {
	X      JInt `json:"x"`
	Y      JInt `json:"y"`
	Width  JInt `json:"width"`
	Height JInt `json:"height"`
}

// RgbaColor は RGBA 色指定です。alpha 未指定時は 255 (Java 版のフィールド初期値)。
type RgbaColor struct {
	R JInt `json:"r"`
	G JInt `json:"g"`
	B JInt `json:"b"`
	A JInt `json:"a"`
}

func (c *RgbaColor) UnmarshalJSON(data []byte) error {
	type alias RgbaColor
	tmp := alias{A: 255}
	if err := json.Unmarshal(data, &tmp); err != nil {
		return err
	}
	*c = RgbaColor(tmp)
	return nil
}

// ConfirmImageStyle は画像比較の結果画像スタイルです。
type ConfirmImageStyle struct {
	Border           JInt       `json:"border"`
	LabelFontSize    JInt       `json:"labelFontSize"`
	LabelHeight      JInt       `json:"labelHeight"`
	LabelPaddingLeft JInt       `json:"labelPaddingLeft"`
	LabelPaddingTop  JInt       `json:"labelPaddingTop"`
	LabelColor       *RgbaColor `json:"labelColor"`
	LeftBgColor      *RgbaColor `json:"leftBgColor"`
	RightBgColor     *RgbaColor `json:"rightBgColor"`
}

// CompareFilesConfig はシステム設定です。Java 版 CompareFilesConfig 相当。
// int フィールドの未設定値は DefaultIntValue、bool は false、文字列は空で表現します
// (Java 版 setDefault のマージセマンティクスと同一)。
type CompareFilesConfig struct {
	LeftFilePath                  string             `json:"leftFilePath"`
	RightFilePath                 string             `json:"rightFilePath"`
	DefaultInputCharset           string             `json:"defaultInputCharset"`
	OutputDir                     string             `json:"outputDir"`
	OutputCharset                 string             `json:"outputCharset"`
	Sorted                        JBool              `json:"sorted"`
	CsvHeaderRow                  JInt               `json:"csvHeaderRow"`
	CsvDataStartRow               JInt               `json:"csvDataStartRow"`
	WriteDiffOnly                 JBool              `json:"writeDiffOnly"`
	LeftPrefix                    string             `json:"leftPrefix"`
	RightPrefix                   string             `json:"rightPrefix"`
	CompareResultFileName         string             `json:"compareResultFileName"`
	CompareDetailFilePrefix       string             `json:"compareDetailFilePrefix"`
	ChunkSize                     JInt               `json:"chunkSize"`
	IgnoreFileRegexList           []string           `json:"ignoreFileRegexList"`
	IgnoreItemList                []string           `json:"ignoreItemList"`
	IgnoreAreaList                []Rectangle        `json:"ignoreAreaList"`
	OverwriteLayoutDir            string             `json:"overwriteLayoutDir"`
	DeleteWorkDir                 JBool              `json:"deleteWorkDir"`
	CodeValueForOnlyOneRecordType string             `json:"codeValueForOnlyOneRecordType"`
	OkImageStyle                  *ConfirmImageStyle `json:"okImageStyle"`
	NgImageStyle                  *ConfirmImageStyle `json:"ngImageStyle"`
}

// NewConfig は未設定状態(int フィールド = DefaultIntValue)の設定を返します。
func NewConfig() *CompareFilesConfig {
	return &CompareFilesConfig{
		CsvHeaderRow:    DefaultIntValue,
		CsvDataStartRow: DefaultIntValue,
		ChunkSize:       DefaultIntValue,
	}
}

// ParseConfig はバイト列(UTF-8 JSON)から設定をパースします。
func ParseConfig(data []byte) (*CompareFilesConfig, error) {
	cfg := NewConfig()
	if err := json.Unmarshal(data, cfg); err != nil {
		return nil, err
	}
	return cfg, nil
}

// ParseConfigFile は指定パスの設定ファイルをパースします。
func ParseConfigFile(path string) (*CompareFilesConfig, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("設定ファイルを読み込めません: %s: %w", path, err)
	}
	cfg, err := ParseConfig(data)
	if err != nil {
		return nil, fmt.Errorf("設定ファイルをパースできません: %s: %w", path, err)
	}
	return cfg, nil
}

// SetDefault はフィールドが未設定の項目を defaultConfig の値で埋めます。
// Java 版 CompareFilesConfig.setDefault と同一セマンティクス。
func (c *CompareFilesConfig) SetDefault(d *CompareFilesConfig) {
	if c.LeftFilePath == "" {
		c.LeftFilePath = d.LeftFilePath
	}
	if c.RightFilePath == "" {
		c.RightFilePath = d.RightFilePath
	}
	if c.DefaultInputCharset == "" {
		c.DefaultInputCharset = d.DefaultInputCharset
	}
	if c.OutputDir == "" {
		c.OutputDir = d.OutputDir
	}
	if c.OutputCharset == "" {
		c.OutputCharset = d.OutputCharset
	}
	if !c.Sorted {
		c.Sorted = d.Sorted
	}
	if c.CsvHeaderRow == DefaultIntValue {
		c.CsvHeaderRow = d.CsvHeaderRow
	}
	if c.CsvDataStartRow == DefaultIntValue {
		c.CsvDataStartRow = d.CsvDataStartRow
	}
	if !c.WriteDiffOnly {
		c.WriteDiffOnly = d.WriteDiffOnly
	}
	if c.LeftPrefix == "" {
		c.LeftPrefix = d.LeftPrefix
	}
	if c.RightPrefix == "" {
		c.RightPrefix = d.RightPrefix
	}
	if c.CompareResultFileName == "" {
		c.CompareResultFileName = d.CompareResultFileName
	}
	if c.CompareDetailFilePrefix == "" {
		c.CompareDetailFilePrefix = d.CompareDetailFilePrefix
	}
	if c.ChunkSize == DefaultIntValue {
		c.ChunkSize = d.ChunkSize
	}
	if len(c.IgnoreFileRegexList) == 0 {
		c.IgnoreFileRegexList = d.IgnoreFileRegexList
	}
	if len(c.IgnoreItemList) == 0 {
		c.IgnoreItemList = d.IgnoreItemList
	}
	if len(c.IgnoreAreaList) == 0 {
		c.IgnoreAreaList = d.IgnoreAreaList
	}
	if c.OverwriteLayoutDir == "" {
		c.OverwriteLayoutDir = d.OverwriteLayoutDir
	}
	if !c.DeleteWorkDir {
		c.DeleteWorkDir = d.DeleteWorkDir
	}
	if c.CodeValueForOnlyOneRecordType == "" {
		c.CodeValueForOnlyOneRecordType = d.CodeValueForOnlyOneRecordType
	}
	if c.OkImageStyle == nil {
		c.OkImageStyle = d.OkImageStyle
	}
	if c.NgImageStyle == nil {
		c.NgImageStyle = d.NgImageStyle
	}
}

// Validate は必須項目と charset の妥当性を検証します。
// Java 版の Bean Validation (@NotEmpty, @Charset) 相当。
func (c *CompareFilesConfig) Validate() error {
	var errs []string
	requireNotEmpty := map[string]string{
		"outputDir":               c.OutputDir,
		"outputCharset":           c.OutputCharset,
		"leftPrefix":              c.LeftPrefix,
		"rightPrefix":             c.RightPrefix,
		"compareResultFileName":   c.CompareResultFileName,
		"compareDetailFilePrefix": c.CompareDetailFilePrefix,
	}
	// 出力順を安定させるため固定順で検証
	for _, name := range []string{"outputDir", "outputCharset", "leftPrefix", "rightPrefix", "compareResultFileName", "compareDetailFilePrefix"} {
		if requireNotEmpty[name] == "" {
			errs = append(errs, fmt.Sprintf("%s は必須です。", name))
		}
	}
	if c.DefaultInputCharset != "" && !charset.IsValid(c.DefaultInputCharset) {
		errs = append(errs, fmt.Sprintf("defaultInputCharset は文字コードを指定してください。: %s", c.DefaultInputCharset))
	}
	if c.OutputCharset != "" && !charset.IsValid(c.OutputCharset) {
		errs = append(errs, fmt.Sprintf("outputCharset は文字コードを指定してください。: %s", c.OutputCharset))
	}
	if len(errs) > 0 {
		return fmt.Errorf("設定が不正です。\n%s", joinLines(errs))
	}
	return nil
}

func joinLines(lines []string) string {
	out := ""
	for i, l := range lines {
		if i > 0 {
			out += "\n"
		}
		out += "  ・" + l
	}
	return out
}

// EffectiveChunkSize は書き出しバッファ行数を返します(0 以下なら既定値)。
func (c *CompareFilesConfig) EffectiveChunkSize() int {
	if int(c.ChunkSize) <= 0 || c.ChunkSize == DefaultIntValue {
		return DefaultChunkSize
	}
	return int(c.ChunkSize)
}
