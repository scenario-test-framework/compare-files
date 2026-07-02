package csvio

import (
	"bufio"
	"io"
	"strings"
)

// Writer は orangesignal CsvWriter 互換のレコードライタです。
//   - null 以外の全フィールドをクォートで囲む
//   - null フィールドは空出力 (クォートなし)
//   - フィールド内のクォートはエスケープ文字で二重化
//   - レコード区切りは LF
type Writer struct {
	w      *bufio.Writer
	config Config
}

// NewWriter はレコードライタを返します。
func NewWriter(w io.Writer, config Config) *Writer {
	return &Writer{w: bufio.NewWriterSize(w, 64*1024), config: config}
}

// WriteRecord は 1 レコードを書き出します。nil 要素は Java の null (空出力) 相当です。
func (w *Writer) WriteRecord(values []*string) error {
	for i, value := range values {
		if i > 0 {
			if _, err := w.w.WriteRune(w.config.Separator); err != nil {
				return err
			}
		}
		if value == nil {
			continue
		}
		if err := w.writeQuoted(*value); err != nil {
			return err
		}
	}
	if err := w.w.WriteByte('\n'); err != nil {
		return err
	}
	return nil
}

// WriteStringRecord は全フィールド非 null のレコードを書き出します。
func (w *Writer) WriteStringRecord(values []string) error {
	ptrs := make([]*string, len(values))
	for i := range values {
		ptrs[i] = &values[i]
	}
	return w.WriteRecord(ptrs)
}

func (w *Writer) writeQuoted(value string) error {
	quote := string(w.config.Quote)
	escaped := strings.ReplaceAll(value, quote, string(w.config.Escape)+quote)
	if _, err := w.w.WriteString(quote); err != nil {
		return err
	}
	if _, err := w.w.WriteString(escaped); err != nil {
		return err
	}
	if _, err := w.w.WriteString(quote); err != nil {
		return err
	}
	return nil
}

// Flush はバッファを書き出します。
func (w *Writer) Flush() error {
	return w.w.Flush()
}
