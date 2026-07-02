package reader

import (
	"os"

	"golang.org/x/text/encoding"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/row"
)

// textReader は Text 形式のリーダです。1 行 = 1 レコード、項目 ID は "value" 固定。
// Java 版 TextRowReader 相当。
type textReader struct {
	file      *os.File
	lr        *lineReader
	layout    *config.FileLayout // 呼び出し時の生 layout (nil の場合あり)
	curRowNum int64
}

func newTextReader(filePath string, enc encoding.Encoding, layout *config.FileLayout) (RowReader, error) {
	f, r, err := openDecoded(filePath, enc)
	if err != nil {
		return nil, err
	}
	return &textReader{file: f, lr: newLineReader(r), layout: layout}, nil
}

func (t *textReader) Next() (*row.Row, error) {
	t.curRowNum++
	line, ok, err := t.lr.readLine()
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, nil
	}
	lineMap := row.NewOrderedMap()
	lineMap.Put(config.DefaultItemID, line)
	// Text 形式は parseRow で常に「行全体を 1 項目」分岐に入る
	return parseRow(t.layout, lineMap, line, t.curRowNum, nil, putContentFlat)
}

func (t *textReader) Close() error {
	return t.file.Close()
}
