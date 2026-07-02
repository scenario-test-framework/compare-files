// Package reader はファイル形式ごとの行データリーダを提供します。
// Java 版 sv/da/file/reader/impl 相当です。
package reader

import (
	"bufio"
	"fmt"
	"io"
	"os"

	"golang.org/x/text/encoding"
	"golang.org/x/text/encoding/unicode"

	"github.com/scenario-test-framework/compare-files/internal/charset"
	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/row"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// RowReader は行データリーダです。Next は EOF で (nil, nil) を返します。
type RowReader interface {
	Next() (*row.Row, error)
	Close() error
}

// Options はリーダ生成オプションです。Java 版 GenericRowReadRepository のコンストラクタ引数相当。
type Options struct {
	OverrideCharset               string
	CsvHeaderRow                  int
	CsvDataStartRow               int
	CodeValueForOnlyOneRecordType string
}

// New はレイアウトのファイル形式に応じたリーダを返します。
// layout が nil の場合はデフォルトテキストレイアウト(UTF-8)を利用します。
func New(filePath string, layout *config.FileLayout, opts Options) (RowReader, error) {
	decided := layout
	if decided == nil {
		decided = config.DefaultTextLayout()
	}
	cs := decided.Charset
	if opts.OverrideCharset != "" {
		cs = opts.OverrideCharset
	}
	if cs == "" {
		return nil, fmt.Errorf("charset が設定されていません: %s", filePath)
	}
	enc, err := charset.Lookup(cs)
	if err != nil {
		return nil, err
	}

	// parse 用には「元の layout が nil か」を保持する (nil ならレイアウトなし扱い)
	switch decided.FileFormat {
	case status.FormatCSVNoHeader, status.FormatCSVWithHeader:
		return newCsvReader(filePath, enc, layout, opts, false)
	case status.FormatTSVNoHeader, status.FormatTSVWithHeader:
		return newCsvReader(filePath, enc, layout, opts, true)
	case status.FormatJSON:
		return newJSONReader(filePath, enc, layout)
	case status.FormatJSONList:
		return newJSONListReader(filePath, enc, layout)
	case status.FormatFixed:
		return newFixedReader(filePath, cs, enc, layout, opts.CodeValueForOnlyOneRecordType)
	default:
		return newTextReader(filePath, enc, layout)
	}
}

// openDecoded はファイルを開き、charset デコード済みの Reader を返します。
func openDecoded(filePath string, enc encoding.Encoding) (*os.File, io.Reader, error) {
	f, err := os.Open(filePath)
	if err != nil {
		return nil, nil, fmt.Errorf("入力ストリームをオープンできません。対象:%s: %w", filePath, err)
	}
	var r io.Reader = bufio.NewReaderSize(f, 64*1024)
	if enc != unicode.UTF8 {
		r = enc.NewDecoder().Reader(r)
	}
	return f, r, nil
}

// lineReader は Java の BufferedReader.readLine 相当 (\n, \r, \r\n 対応) です。
type lineReader struct {
	r *bufio.Reader
}

func newLineReader(r io.Reader) *lineReader {
	if br, ok := r.(*bufio.Reader); ok {
		return &lineReader{r: br}
	}
	return &lineReader{r: bufio.NewReaderSize(r, 64*1024)}
}

// readLine は 1 行を返します。EOF は (「"", false, nil)。
func (lr *lineReader) readLine() (string, bool, error) {
	var buf []byte
	readAnything := false
	for {
		b, err := lr.r.ReadByte()
		if err != nil {
			if err == io.EOF {
				if !readAnything {
					return "", false, nil
				}
				return string(buf), true, nil
			}
			return "", false, err
		}
		readAnything = true
		switch b {
		case '\n':
			return string(buf), true, nil
		case '\r':
			next, err := lr.r.ReadByte()
			if err == nil && next != '\n' {
				if uerr := lr.r.UnreadByte(); uerr != nil {
					return "", false, uerr
				}
			}
			return string(buf), true, nil
		default:
			buf = append(buf, b)
		}
	}
}

// parseRow は行データマップを Row に変換します。Java 版 BaseRowReader.parse 相当。
//   - layout が nil または Text 形式: 行全体を 1 比較項目として保持
//   - レイアウトあり: 比較キーとその他項目に分割
//
// putContent は項目のコピー方法 (フラット / JSON 階層) を差し替えます。
func parseRow(
	layout *config.FileLayout,
	lineMap *row.OrderedMap,
	rawLine string,
	rowNum int64,
	matchRecordType func(*config.RecordLayout, *row.OrderedMap) (bool, error),
	putContent func(itemID string, from *row.OrderedMap, to *row.OrderedMap) error,
) (*row.Row, error) {
	parsed := &row.Row{RowNum: rowNum, RawLine: rawLine}

	if layout == nil || layout.FileFormat == status.FormatText {
		dummyKeyMap := row.NewOrderedMap()
		dummyKeyMap.Put("dummy", nil)
		parsed.KeyMap = dummyKeyMap
		parsed.ValueMap = lineMap
		return parsed, nil
	}

	// レコードレイアウトの判定
	var recordLayout *config.RecordLayout
	switch {
	case len(layout.RecordList) == 0:
		return nil, fmt.Errorf("%s に recordList が設定されていません", layout.LogicalFileName)
	case len(layout.RecordList) == 1:
		recordLayout = layout.RecordList[0]
	default:
		for _, rl := range layout.RecordList {
			ok, err := matchRecordType(rl, lineMap)
			if err != nil {
				return nil, err
			}
			if ok {
				recordLayout = rl
				break
			}
		}
	}
	if recordLayout == nil {
		return nil, fmt.Errorf("レコードレイアウトを判別できません。ファイルレイアウト:%s、行データ:%d:%v",
			layout.LogicalFileName, rowNum, lineMap.Keys())
	}

	keyMap := row.NewOrderedMap()
	valueMap := row.NewOrderedMap()
	parsed.KeyMap = keyMap
	parsed.ValueMap = valueMap
	for _, item := range recordLayout.ItemList {
		target := valueMap
		if item.CompareKey {
			target = keyMap
		}
		if err := putContent(item.ID, lineMap, target); err != nil {
			return nil, err
		}
	}
	return parsed, nil
}

// putContentFlat は階層なしの直接コピーです (BaseFlatRowReader.putContent 相当)。
// 転記元に存在しない項目は null (nil) として転記します。
func putContentFlat(itemID string, from *row.OrderedMap, to *row.OrderedMap) error {
	to.Put(itemID, from.GetOrNil(itemID))
	return nil
}

// firstValuePrefix は行マップの最初の項目値から、コード値と同じ文字数分を抽出します。
// Java 版 isMatchRecordType (CSV/Fixed 共通ロジック) 相当。
// 値がコード値より短い場合はエラー (Java の StringIndexOutOfBoundsException 相当)。
func firstValuePrefix(lineMap *row.OrderedMap, codeValue string) (string, error) {
	keys := lineMap.Keys()
	if len(keys) == 0 {
		return "", fmt.Errorf("行データが空です")
	}
	firstValue := row.ToJavaString(lineMap.GetOrNil(keys[0]))
	runes := []rune(firstValue)
	codeLen := len([]rune(codeValue))
	if len(runes) < codeLen {
		return "", fmt.Errorf("レコードタイプ判定コードを抽出できません。値:%q、コード値:%q", firstValue, codeValue)
	}
	return string(runes[:codeLen]), nil
}
