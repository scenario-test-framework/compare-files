package reader

import (
	"fmt"
	"os"
	"strconv"
	"strings"

	"golang.org/x/text/encoding"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/csvio"
	"github.com/scenario-test-framework/compare-files/internal/row"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// csvReader は CSV/TSV 形式のリーダです。Java 版 CsvRowReader / TsvRowReader 相当。
type csvReader struct {
	file            *os.File
	cr              *csvio.Reader
	csvConfig       csvio.Config
	layout          *config.FileLayout // nil の場合あり
	recordPattern   status.RecordPattern
	csvHeaderRow    int
	csvDataStartRow int
	// mapperMap: レコードタイプ → カラム名リスト。
	// エントリなし or nil 値はカラム番号マッピング (Java の mapper == null 相当)。
	mapperMap map[status.RecordType][]string
	curRowNum int64
}

func newCsvReader(filePath string, enc encoding.Encoding, layout *config.FileLayout, opts Options, isTsv bool) (RowReader, error) {
	f, r, err := openDecoded(filePath, enc)
	if err != nil {
		return nil, err
	}
	csvConfig := csvio.CsvConfig()
	if isTsv {
		csvConfig = csvio.TsvConfig()
	}
	c := &csvReader{
		file:            f,
		cr:              csvio.NewReader(r, csvConfig),
		csvConfig:       csvConfig,
		layout:          layout,
		csvHeaderRow:    opts.CsvHeaderRow,
		csvDataStartRow: opts.CsvDataStartRow,
		mapperMap:       map[status.RecordType][]string{},
	}
	if layout == nil {
		c.recordPattern = status.PatternNone
	} else {
		c.recordPattern = layout.RecordPattern()
	}
	if c.csvDataStartRow <= 0 {
		c.csvDataStartRow = 1
	}

	if layout == nil {
		if err := c.setCsvHeaderByHeaderRow(); err != nil {
			f.Close()
			return nil, err
		}
	} else {
		if err := c.setCsvHeaderByLayout(); err != nil {
			f.Close()
			return nil, err
		}
	}
	return c, nil
}

// setCsvHeaderByLayout はレイアウト定義からレコードタイプごとのカラム名を登録し、
// withHeader 形式の場合はデータ開始行の前行まで読み進めます。
func (c *csvReader) setCsvHeaderByLayout() error {
	for _, rl := range c.layout.RecordList {
		c.mapperMap[rl.Type] = rl.CompareItemNameList()
	}
	if c.layout.FileFormat == status.FormatCSVWithHeader || c.layout.FileFormat == status.FormatTSVWithHeader {
		for c.cr.LineNumber() < c.csvDataStartRow-1 {
			curLine, err := c.cr.ReadValues()
			if err != nil {
				return fmt.Errorf("データ開始行まで読み進められませんでした。ファイル:%s: %w", c.file.Name(), err)
			}
			if curLine == nil {
				break
			}
		}
	}
	return nil
}

// setCsvHeaderByHeaderRow はヘッダー行番号からカラム名を取得します(レイアウトなしの場合)。
func (c *csvReader) setCsvHeaderByHeaderRow() error {
	var headerNameList []string
	for c.cr.LineNumber() < c.csvDataStartRow-1 {
		curLine, err := c.cr.ReadValues()
		if err != nil {
			return fmt.Errorf("データ開始行まで読み進められませんでした。ファイル:%s: %w", c.file.Name(), err)
		}
		if curLine == nil {
			break
		}
		if c.cr.LineNumber() == c.csvHeaderRow {
			headerNameList = curLine
		}
	}
	c.mapperMap[status.RecordData] = headerNameList // nil の場合はカラム番号マッピング
	return nil
}

func (c *csvReader) Next() (*row.Row, error) {
	contentList, err := c.cr.ReadValues()
	c.curRowNum = int64(c.cr.LineNumber())
	if err != nil {
		return nil, fmt.Errorf("ファイルを読込みできません。対象:%s: %w", c.file.Name(), err)
	}
	if contentList == nil || (len(contentList) == 1 && contentList[0] == "") {
		return nil, nil
	}

	// rawLine: 各値をクォートで囲み、区切り文字で連結して再構成
	var rawLineBuilder strings.Builder
	for i, content := range contentList {
		if i > 0 {
			rawLineBuilder.WriteRune(c.csvConfig.Separator)
		}
		rawLineBuilder.WriteByte('"')
		rawLineBuilder.WriteString(content)
		rawLineBuilder.WriteByte('"')
	}

	// レコードタイプの判定
	var recordType status.RecordType // 空 = 判別不能 (Java の null)
	if c.recordPattern == status.PatternDataOnly {
		recordType = status.RecordData
	} else if c.layout == nil {
		recordType = status.RecordData
	} else {
		firstValue := []rune(contentList[0])
		for _, rl := range c.layout.RecordList {
			codeLen := len([]rune(rl.CodeValue))
			if len(firstValue) < codeLen {
				return nil, fmt.Errorf("レコードタイプ判定コードを抽出できません。ファイル:%s、値:%q、コード値:%q",
					c.file.Name(), contentList[0], rl.CodeValue)
			}
			if string(firstValue[:codeLen]) == rl.CodeValue {
				recordType = rl.Type
				break
			}
		}
	}

	lineMap := c.toMap(recordType, contentList)
	return parseRow(c.layout, lineMap, rawLineBuilder.String(), c.curRowNum, c.isMatchRecordType, putContentFlat)
}

// toMap は行データリストをカラム名 (または 1 始まりのカラム番号) をキーにした Map に変換します。
func (c *csvReader) toMap(recordType status.RecordType, contentList []string) *row.OrderedMap {
	m := row.NewOrderedMap()
	names, ok := c.mapperMap[recordType]
	if !ok || names == nil {
		for i, content := range contentList {
			m.Put(strconv.Itoa(i+1), content)
		}
		return m
	}
	for i, name := range names {
		if i < len(contentList) {
			m.Put(name, contentList[i])
		} else {
			m.Put(name, nil)
		}
	}
	return m
}

// isMatchRecordType は parseRow でのレコードレイアウト判定です。
func (c *csvReader) isMatchRecordType(rl *config.RecordLayout, lineMap *row.OrderedMap) (bool, error) {
	if c.recordPattern == status.PatternDataOnly || c.recordPattern == status.PatternHeaderData {
		return rl.Type == status.RecordData, nil
	}
	prefix, err := firstValuePrefix(lineMap, rl.CodeValue)
	if err != nil {
		return false, err
	}
	return prefix == rl.CodeValue, nil
}

func (c *csvReader) Close() error {
	return c.file.Close()
}
