// Package status は比較処理で利用する区分値を定義します。
// 各区分値の名称・許容値は Java 版の enum (me.suwash.tools.comparefiles.infra.classification,
// me.suwash.util.CompareUtils) と一致させています。
package status

import (
	"encoding/json"
	"fmt"
)

// CompareStatus は比較結果ステータスです。
type CompareStatus string

const (
	CompareProcessing CompareStatus = "Processing"
	CompareOK         CompareStatus = "OK"
	CompareNG         CompareStatus = "NG"
	CompareIgnore     CompareStatus = "Ignore"
	CompareLeftOnly   CompareStatus = "LeftOnly"
	CompareRightOnly  CompareStatus = "RightOnly"
	CompareError      CompareStatus = "Error"
)

// ProcessStatus はプロセス全体のステータスです。
type ProcessStatus string

const (
	ProcessProcessing ProcessStatus = "Processing"
	ProcessSuccess    ProcessStatus = "Success"
	ProcessWarning    ProcessStatus = "Warning"
	ProcessFailure    ProcessStatus = "Failure"
)

// 終了コード。Java 版 Const.EXITCODE_* と同値。
const (
	ExitCodeSuccess = 0
	ExitCodeWarn    = 3
	ExitCodeError   = 6
)

// ExitCode は ProcessStatus に対応する終了コードを返します。
func (s ProcessStatus) ExitCode() int {
	switch s {
	case ProcessSuccess:
		return ExitCodeSuccess
	case ProcessWarning:
		return ExitCodeWarn
	default:
		return ExitCodeError
	}
}

// FileFormat はファイル形式です。
type FileFormat string

const (
	FormatText          FileFormat = "Text"
	FormatCSVWithHeader FileFormat = "CSV_withHeader"
	FormatCSVNoHeader   FileFormat = "CSV_noHeader"
	FormatTSVWithHeader FileFormat = "TSV_withHeader"
	FormatTSVNoHeader   FileFormat = "TSV_noHeader"
	FormatJSON          FileFormat = "Json"
	FormatJSONList      FileFormat = "JsonList"
	FormatYaml          FileFormat = "Yaml"
	FormatXML           FileFormat = "XML"
	FormatFixed         FileFormat = "Fixed"
	FormatImage         FileFormat = "Image"
)

var fileFormats = map[string]FileFormat{
	string(FormatText):          FormatText,
	string(FormatCSVWithHeader): FormatCSVWithHeader,
	string(FormatCSVNoHeader):   FormatCSVNoHeader,
	string(FormatTSVWithHeader): FormatTSVWithHeader,
	string(FormatTSVNoHeader):   FormatTSVNoHeader,
	string(FormatJSON):          FormatJSON,
	string(FormatJSONList):      FormatJSONList,
	string(FormatYaml):          FormatYaml,
	string(FormatXML):           FormatXML,
	string(FormatFixed):         FormatFixed,
	string(FormatImage):         FormatImage,
}

// UnmarshalJSON は Java enum と同様に名称完全一致でパースします。
func (f *FileFormat) UnmarshalJSON(data []byte) error {
	var s string
	if err := json.Unmarshal(data, &s); err != nil {
		return err
	}
	v, ok := fileFormats[s]
	if !ok {
		return fmt.Errorf("不明な fileFormat: %q", s)
	}
	*f = v
	return nil
}

// RecordType はレコードタイプです。
type RecordType string

const (
	RecordHeader  RecordType = "Header"
	RecordData    RecordType = "Data"
	RecordTrailer RecordType = "Trailer"
	RecordEnd     RecordType = "End"
)

var recordTypes = map[string]RecordType{
	string(RecordHeader):  RecordHeader,
	string(RecordData):    RecordData,
	string(RecordTrailer): RecordTrailer,
	string(RecordEnd):     RecordEnd,
}

func (r *RecordType) UnmarshalJSON(data []byte) error {
	var s string
	if err := json.Unmarshal(data, &s); err != nil {
		return err
	}
	v, ok := recordTypes[s]
	if !ok {
		return fmt.Errorf("不明な record type: %q", s)
	}
	*r = v
	return nil
}

// RecordPattern はレコードリストの構成パターンです。
type RecordPattern string

const (
	PatternNone       RecordPattern = "None"
	PatternHeaderData RecordPattern = "HeaderData"
	PatternDataOnly   RecordPattern = "DataOnly"
	PatternMulti      RecordPattern = "Multi"
)

// LineSp は改行コードです。
type LineSp string

const (
	LineSpCR   LineSp = "CR"
	LineSpLF   LineSp = "LF"
	LineSpCRLF LineSp = "CRLF"
	LineSpNone LineSp = "None"
)

var lineSps = map[string]LineSp{
	string(LineSpCR):   LineSpCR,
	string(LineSpLF):   LineSpLF,
	string(LineSpCRLF): LineSpCRLF,
	string(LineSpNone): LineSpNone,
}

func (l *LineSp) UnmarshalJSON(data []byte) error {
	var s string
	if err := json.Unmarshal(data, &s); err != nil {
		return err
	}
	v, ok := lineSps[s]
	if !ok {
		return fmt.Errorf("不明な lineSp: %q", s)
	}
	*l = v
	return nil
}

// Value は書き込み時に出力する改行文字列を返します。
// 空(未設定)の場合は OS 依存改行を利用する呼び出し側の責務とします。
func (l LineSp) Value() string {
	switch l {
	case LineSpCR:
		return "\r"
	case LineSpLF:
		return "\n"
	case LineSpCRLF:
		return "\r\n"
	default: // None
		return ""
	}
}

// CompareCriteria は項目の比較条件です。
type CompareCriteria string

const (
	CriteriaIgnore   CompareCriteria = "Ignore"
	CriteriaEqual    CompareCriteria = "Equal"
	CriteriaNotEqual CompareCriteria = "NotEqual"

	CriteriaNumberGreaterThanLeft      CompareCriteria = "Number_GreaterThan_Left"
	CriteriaNumberGreaterEqualThanLeft CompareCriteria = "Number_GreaterEqualThan_Left"
	CriteriaNumberLessThanLeft         CompareCriteria = "Number_LessThan_Left"
	CriteriaNumberLessEqualThanLeft    CompareCriteria = "Number_LessEqualThan_Left"

	CriteriaYearGreaterThanLeft      CompareCriteria = "Year_GreaterThan_Left"
	CriteriaYearGreaterEqualThanLeft CompareCriteria = "Year_GreaterEqualThan_Left"
	CriteriaYearLessThanLeft         CompareCriteria = "Year_LessThan_Left"
	CriteriaYearLessEqualThanLeft    CompareCriteria = "Year_LessEqualThan_Left"

	CriteriaMonthGreaterThanLeft      CompareCriteria = "Month_GreaterThan_Left"
	CriteriaMonthGreaterEqualThanLeft CompareCriteria = "Month_GreaterEqualThan_Left"
	CriteriaMonthLessThanLeft         CompareCriteria = "Month_LessThan_Left"
	CriteriaMonthLessEqualThanLeft    CompareCriteria = "Month_LessEqualThan_Left"

	CriteriaDateGreaterThanLeft      CompareCriteria = "Date_GreaterThan_Left"
	CriteriaDateGreaterEqualThanLeft CompareCriteria = "Date_GreaterEqualThan_Left"
	CriteriaDateLessThanLeft         CompareCriteria = "Date_LessThan_Left"
	CriteriaDateLessEqualThanLeft    CompareCriteria = "Date_LessEqualThan_Left"

	CriteriaDatetimeGreaterThanLeft      CompareCriteria = "Datetime_GreaterThan_Left"
	CriteriaDatetimeGreaterEqualThanLeft CompareCriteria = "Datetime_GreaterEqualThan_Left"
	CriteriaDatetimeLessThanLeft         CompareCriteria = "Datetime_LessThan_Left"
	CriteriaDatetimeLessEqualThanLeft    CompareCriteria = "Datetime_LessEqualThan_Left"
)

var compareCriterias = func() map[string]CompareCriteria {
	m := map[string]CompareCriteria{}
	for _, c := range []CompareCriteria{
		CriteriaIgnore, CriteriaEqual, CriteriaNotEqual,
		CriteriaNumberGreaterThanLeft, CriteriaNumberGreaterEqualThanLeft, CriteriaNumberLessThanLeft, CriteriaNumberLessEqualThanLeft,
		CriteriaYearGreaterThanLeft, CriteriaYearGreaterEqualThanLeft, CriteriaYearLessThanLeft, CriteriaYearLessEqualThanLeft,
		CriteriaMonthGreaterThanLeft, CriteriaMonthGreaterEqualThanLeft, CriteriaMonthLessThanLeft, CriteriaMonthLessEqualThanLeft,
		CriteriaDateGreaterThanLeft, CriteriaDateGreaterEqualThanLeft, CriteriaDateLessThanLeft, CriteriaDateLessEqualThanLeft,
		CriteriaDatetimeGreaterThanLeft, CriteriaDatetimeGreaterEqualThanLeft, CriteriaDatetimeLessThanLeft, CriteriaDatetimeLessEqualThanLeft,
	} {
		m[string(c)] = c
	}
	return m
}()

func (c *CompareCriteria) UnmarshalJSON(data []byte) error {
	var s string
	if err := json.Unmarshal(data, &s); err != nil {
		return err
	}
	v, ok := compareCriterias[s]
	if !ok {
		return fmt.Errorf("不明な criteria: %q", s)
	}
	*c = v
	return nil
}
