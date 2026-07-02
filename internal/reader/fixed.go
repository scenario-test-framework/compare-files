package reader

import (
	"bufio"
	"fmt"
	"io"
	"os"

	"golang.org/x/text/encoding"
	"golang.org/x/text/encoding/unicode"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/row"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// fixedReader は固定長テキスト形式のリーダです。Java 版 FixedRowReader 相当。
// バイト単位でレコードを読み込み、項目バイト長で分割してから charset デコードします。
type fixedReader struct {
	file                          *os.File
	br                            *bufio.Reader
	layout                        *config.FileLayout
	charsetName                   string
	enc                           encoding.Encoding
	codeValueForOnlyOneRecordType string
	curRowNum                     int64
}

func newFixedReader(filePath, charsetName string, enc encoding.Encoding, layout *config.FileLayout, codeValueForOnlyOneRecordType string) (RowReader, error) {
	if layout == nil {
		return nil, fmt.Errorf("layout が設定されていません: %s", filePath)
	}
	if len(layout.RecordList) == 0 {
		return nil, fmt.Errorf("layout.recordList が設定されていません: %s", filePath)
	}
	f, err := os.Open(filePath)
	if err != nil {
		return nil, fmt.Errorf("入力ストリームをオープンできません。対象:%s: %w", filePath, err)
	}
	return &fixedReader{
		file:                          f,
		br:                            bufio.NewReaderSize(f, 64*1024),
		layout:                        layout,
		charsetName:                   charsetName,
		enc:                           enc,
		codeValueForOnlyOneRecordType: codeValueForOnlyOneRecordType,
	}, nil
}

func (fr *fixedReader) Next() (*row.Row, error) {
	recordByteLength, err := fr.layout.FirstRecordByteLength()
	if err != nil {
		return nil, err
	}

	// レコードバイト長分読み込み (Java: 部分読みの残りはゼロ埋めのまま)
	lineBytes := make([]byte, recordByteLength)
	readLength, err := io.ReadFull(fr.br, lineBytes)
	if err == io.EOF || readLength <= 0 {
		return nil, nil
	}
	if err != nil && err != io.ErrUnexpectedEOF {
		return nil, fmt.Errorf("ファイルを読込みできません。対象:%s#%d: %w", fr.file.Name(), fr.curRowNum, err)
	}

	recordLayout, err := fr.getRecordLayout(lineBytes)
	if err != nil {
		return nil, err
	}
	recordMap := row.NewOrderedMap()
	var rawLineBuilder []byte
	readedPos := 0
	for _, item := range recordLayout.ItemList {
		endPos := readedPos + int(item.ByteLength)
		if endPos > len(lineBytes) {
			endPos = len(lineBytes)
		}
		value := fr.decodeBytes(lineBytes[readedPos:endPos])
		recordMap.Put(item.ID, value)
		rawLineBuilder = append(rawLineBuilder, []byte(value)...)
		readedPos = endPos
	}

	if err := fr.skipLineSp(); err != nil {
		return nil, err
	}
	fr.curRowNum++

	return parseRow(fr.layout, recordMap, string(rawLineBuilder), fr.curRowNum, fr.isMatchRecordType, putContentFlat)
}

// getRecordLayout はレコードタイプ判定コードからレコードレイアウトを決定します。
func (fr *fixedReader) getRecordLayout(lineBytes []byte) (*config.RecordLayout, error) {
	for _, rl := range fr.layout.RecordList {
		code := fr.recordTypeCode(lineBytes, rl)
		if code == rl.CodeValue {
			return rl, nil
		}
	}
	// 判別できず、レコードタイプが 1 種類のみでコード値が判定コードと一致する場合はそれを利用
	if len(fr.layout.RecordList) == 1 && fr.layout.RecordList[0].CodeValue == fr.codeValueForOnlyOneRecordType {
		return fr.layout.RecordList[0], nil
	}
	return nil, fmt.Errorf("レコードレイアウトを判別できません。ファイルレイアウト:%s、#%d",
		fr.layout.LogicalFileName, fr.curRowNum+1)
}

// recordTypeCode はコード値の文字数分、先頭から 1 バイトずつデコードして連結します。
// Java 版 getRecordTypeCode と同じく 1 バイト文字 (ASCII) 前提の実装です。
func (fr *fixedReader) recordTypeCode(lineBytes []byte, rl *config.RecordLayout) string {
	codeLen := len([]rune(rl.CodeValue))
	out := make([]byte, 0, codeLen*3)
	for i := 0; i < codeLen && i < len(lineBytes); i++ {
		out = append(out, []byte(fr.decodeBytes(lineBytes[i:i+1]))...)
	}
	return string(out)
}

// decodeBytes はバイト列を charset デコードします (不正シーケンスは U+FFFD 置換)。
func (fr *fixedReader) decodeBytes(b []byte) string {
	if fr.enc == unicode.UTF8 {
		// Java の new String(bytes, UTF-8) と同じく不正バイトは置換される
		return string(decodeUTF8Replace(b))
	}
	decoded, err := fr.enc.NewDecoder().Bytes(b)
	if err != nil {
		// x/text の Decoder は既定で置換するため通常ここには来ない
		return string(b)
	}
	return string(decoded)
}

// decodeUTF8Replace は不正な UTF-8 シーケンスを U+FFFD に置換します。
func decodeUTF8Replace(b []byte) []rune {
	return []rune(string(b))
}

// skipLineSp はレイアウトの改行コード分読み捨てます。
func (fr *fixedReader) skipLineSp() error {
	var n int
	switch fr.layout.LineSp {
	case status.LineSpCR, status.LineSpLF:
		n = 1
	case status.LineSpCRLF:
		n = 2
	default:
		return nil
	}
	if _, err := io.ReadFull(fr.br, make([]byte, n)); err != nil && err != io.EOF && err != io.ErrUnexpectedEOF {
		return fmt.Errorf("改行コード分読み進められませんでした。改行コード:%s: %w", fr.layout.LineSp, err)
	}
	return nil
}

// isMatchRecordType は parseRow でのレコードレイアウト判定です。
func (fr *fixedReader) isMatchRecordType(rl *config.RecordLayout, lineMap *row.OrderedMap) (bool, error) {
	prefix, err := firstValuePrefix(lineMap, rl.CodeValue)
	if err != nil {
		return false, err
	}
	return prefix == rl.CodeValue, nil
}

func (fr *fixedReader) Close() error {
	return fr.file.Close()
}
