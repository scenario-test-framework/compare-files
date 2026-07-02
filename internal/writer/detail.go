// Package writer は比較結果の出力を提供します。
// Java 版 sv/da/file/writer/impl 相当です。
package writer

import (
	"fmt"
	"io"
	"os"
	"path/filepath"

	"golang.org/x/text/encoding/unicode"

	"github.com/scenario-test-framework/compare-files/internal/charset"
	"github.com/scenario-test-framework/compare-files/internal/compare"
	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/csvio"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// DetailWriter は行比較結果 (CompareDetail_*.csv) のライタです。
// Java 版 TextFileRowCompareResultWriter 相当。
// ヘッダーは最初の行の書き込み時に 1 度だけ出力されます。
type DetailWriter struct {
	file            *os.File
	w               *csvio.Writer
	filePath        string
	isWriteDiffOnly bool
	prefixLeft      string
	prefixRight     string
	headerWritten   bool
	columns         []string
}

// NewDetailWriter は詳細結果ライタを返します。既存ファイルは上書きされます。
func NewDetailWriter(filePath, charsetName string, isWriteDiffOnly bool, prefixLeft, prefixRight string) (*DetailWriter, error) {
	enc, err := charset.Lookup(charsetName)
	if err != nil {
		return nil, err
	}
	if err := os.MkdirAll(filepath.Dir(filePath), 0o755); err != nil {
		return nil, fmt.Errorf("ディレクトリを作成できません。対象:%s: %w", filepath.Dir(filePath), err)
	}
	f, err := os.Create(filePath)
	if err != nil {
		return nil, fmt.Errorf("出力ストリームをオープンできません。対象:%s: %w", filePath, err)
	}
	var w io.Writer = f
	if enc != unicode.UTF8 {
		w = enc.NewEncoder().Writer(f)
	}
	return &DetailWriter{
		file:            f,
		w:               csvio.NewWriter(w, csvio.CsvConfig()),
		filePath:        filePath,
		isWriteDiffOnly: isWriteDiffOnly,
		prefixLeft:      prefixLeft,
		prefixRight:     prefixRight,
	}, nil
}

// Write は行比較結果を書き出します。
func (d *DetailWriter) Write(result *compare.RowResult) error {
	if !d.headerWritten {
		d.columns = headerColumns(result)
		if err := d.w.WriteStringRecord(d.columns); err != nil {
			return fmt.Errorf("ファイルを書出しできません。対象:%s#header: %w", d.filePath, err)
		}
		d.headerWritten = true
	}

	if d.isWriteDiffOnly && result.Status == status.CompareOK {
		return nil
	}

	dataMap, err := d.dataRecordMap(result)
	if err != nil {
		return err
	}
	record := make([]*string, len(d.columns))
	for i, col := range d.columns {
		record[i] = dataMap[col]
	}
	if err := d.w.WriteRecord(record); err != nil {
		return fmt.Errorf("ファイルを書出しできません。対象:%s: %w", d.filePath, err)
	}
	return nil
}

// Close はバッファを書き出してファイルを閉じます。
func (d *DetailWriter) Close() error {
	if err := d.w.Flush(); err != nil {
		d.file.Close()
		return err
	}
	return d.file.Close()
}

// headerColumns はヘッダーカラム名リストを決定します。
// Java 版 TextFileCompareResultOutput.getHeaderMap 相当 (LinkedHashMap の重複排除を再現)。
func headerColumns(result *compare.RowResult) []string {
	var columns []string
	seen := map[string]bool{}
	add := func(name string) {
		if !seen[name] {
			seen[name] = true
			columns = append(columns, name)
		}
	}
	add("Status")
	add("RowNum")
	add("DiffItems")

	layout := result.FileLayout
	if layout == nil || len(layout.RecordList) <= 1 {
		// 単一レコードタイプ: 出力行の項目から
		for _, item := range result.Items {
			add(dynamicColumnID(layout, result.RecordType, item.ID))
		}
	} else {
		// 複数レコードタイプ: 全レコードタイプの項目から
		for _, rl := range layout.RecordList {
			for _, item := range rl.ItemList {
				add(dynamicColumnID(layout, rl.Type, item.ID))
			}
		}
	}
	return columns
}

// dataRecordMap はデータ行の カラム名 → 値 マップを返します。
// Java 版 TextFileCompareResultOutput.getDataRecordMap 相当。
func (d *DetailWriter) dataRecordMap(result *compare.RowResult) (map[string]*string, error) {
	m := map[string]*string{}
	put := func(key, value string) {
		v := value
		m[key] = &v
	}

	put("Status", string(result.Status))
	put("RowNum", diffContent(d.prefixLeft, d.prefixRight, rowNumString(result.LeftRowNum), rowNumString(result.RightRowNum)))
	if result.Status == status.CompareNG {
		put("DiffItems", javaListString(result.DiffItemIDList()))
	} else {
		put("DiffItems", config.DummyValue)
	}

	for _, item := range result.Items {
		columnName := dynamicColumnID(result.FileLayout, result.RecordType, item.ID)
		switch item.Status {
		case status.CompareOK:
			m[columnName] = item.LeftValue
		case status.CompareNG, status.CompareIgnore, status.CompareLeftOnly, status.CompareRightOnly:
			put(columnName, diffContent(d.prefixLeft, d.prefixRight, derefOrNull(item.LeftValue), derefOrNull(item.RightValue)))
		default:
			return nil, fmt.Errorf("想定外のステータスです。ステータス:%s", item.Status)
		}
	}
	return m, nil
}

// dynamicColumnID はカラム名を決定します (複数レコードタイプは "RecordType.項目ID")。
func dynamicColumnID(layout *config.FileLayout, recordType status.RecordType, itemID string) string {
	if layout == nil || len(layout.RecordList) <= 1 {
		return itemID
	}
	return string(recordType) + "." + itemID
}

// diffContent は差分セルの出力文言 (左プリフィックス+左値、改行、右プリフィックス+右値) を返します。
func diffContent(prefixLeft, prefixRight, left, right string) string {
	return prefixLeft + left + "\n" + prefixRight + right
}

func derefOrNull(v *string) string {
	if v == nil {
		return "null"
	}
	return *v
}

func rowNumString(n int64) string {
	return fmt.Sprintf("%d", n)
}

// javaListString は Java の List.toString 形式 ("[a, b]") を返します。
func javaListString(list []string) string {
	out := "["
	for i, s := range list {
		if i > 0 {
			out += ", "
		}
		out += s
	}
	return out + "]"
}
