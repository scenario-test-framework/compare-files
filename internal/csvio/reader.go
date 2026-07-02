// Package csvio は orangesignal-csv 互換の CSV/TSV 読み書きを提供します。
//   - CSV: 区切り ',' / クォート '"' / エスケープ '"' (Excel 方式)
//   - TSV: 区切り '\t' / クォート '"' / エスケープ '\'
//
// 書き込みは「null 以外の全フィールドをクォートし、null は空出力」という
// orangesignal CsvWriter の挙動を再現します。
package csvio

import (
	"bufio"
	"io"
	"strings"
)

// Config は CSV/TSV の書式設定です。
type Config struct {
	Separator rune
	Quote     rune
	Escape    rune
}

// CsvConfig は CSV 用設定を返します (orangesignal CsvUtils.getCsvConfig 相当)。
func CsvConfig() Config {
	return Config{Separator: ',', Quote: '"', Escape: '"'}
}

// TsvConfig は TSV 用設定を返します (orangesignal CsvUtils.getTsvConfig 相当)。
func TsvConfig() Config {
	return Config{Separator: '\t', Quote: '"', Escape: '\\'}
}

// Reader は orangesignal CsvReader 相当のレコードリーダです。
// 行番号(物理行)を追跡し、クォート内の改行を含むレコードに対応します。
type Reader struct {
	r          *bufio.Reader
	config     Config
	lineNumber int
	eof        bool
}

// NewReader はレコードリーダを返します。
func NewReader(r io.Reader, config Config) *Reader {
	return &Reader{r: bufio.NewReaderSize(r, 64*1024), config: config}
}

// LineNumber は最後に読み込んだレコードの終端の物理行番号を返します。
// orangesignal CsvReader.getLineNumber 相当。
func (r *Reader) LineNumber() int {
	return r.lineNumber
}

// ReadValues は 1 レコードを読み込みます。EOF の場合は nil を返します。
// 空行は [""] として返します (orangesignal と同じ)。
func (r *Reader) ReadValues() ([]string, error) {
	if r.eof {
		return nil, nil
	}

	var values []string
	var field strings.Builder
	inQuotes := false
	fieldStart := true // フィールド先頭か
	readAnything := false

	appendField := func() {
		values = append(values, field.String())
		field.Reset()
		fieldStart = true
	}

	for {
		ch, _, err := r.r.ReadRune()
		if err != nil {
			if err == io.EOF {
				r.eof = true
				if !readAnything {
					return nil, nil
				}
				r.lineNumber++
				appendField()
				return values, nil
			}
			return nil, err
		}
		readAnything = true

		if inQuotes {
			switch {
			case ch == r.config.Escape && r.config.Escape != r.config.Quote:
				// エスケープ文字 (TSV の '\') : 次のクォートを取り込む
				next, _, err := r.r.ReadRune()
				if err != nil {
					if err == io.EOF {
						field.WriteRune(ch)
						continue
					}
					return nil, err
				}
				if next == r.config.Quote {
					field.WriteRune(r.config.Quote)
				} else {
					field.WriteRune(ch)
					if err := r.r.UnreadRune(); err != nil {
						return nil, err
					}
				}
			case ch == r.config.Quote:
				// エスケープ==クォート (CSV): "" は literal quote
				next, _, err := r.r.ReadRune()
				if err == nil && next == r.config.Quote && r.config.Escape == r.config.Quote {
					field.WriteRune(r.config.Quote)
					continue
				}
				if err == nil {
					if uerr := r.r.UnreadRune(); uerr != nil {
						return nil, uerr
					}
				}
				inQuotes = false
			case ch == '\n':
				r.lineNumber++
				field.WriteRune(ch)
			case ch == '\r':
				// CRLF はまとめて 1 行と数える
				next, _, err := r.r.ReadRune()
				if err == nil {
					if next == '\n' {
						r.lineNumber++
						field.WriteRune('\r')
						field.WriteRune('\n')
						continue
					}
					if uerr := r.r.UnreadRune(); uerr != nil {
						return nil, uerr
					}
				}
				r.lineNumber++
				field.WriteRune('\r')
			default:
				field.WriteRune(ch)
			}
			continue
		}

		switch {
		case ch == r.config.Quote && fieldStart:
			inQuotes = true
			fieldStart = false
		case ch == r.config.Separator:
			appendField()
		case ch == '\n':
			r.lineNumber++
			appendField()
			return values, nil
		case ch == '\r':
			next, _, err := r.r.ReadRune()
			if err == nil && next != '\n' {
				if uerr := r.r.UnreadRune(); uerr != nil {
					return nil, uerr
				}
			}
			r.lineNumber++
			appendField()
			return values, nil
		default:
			field.WriteRune(ch)
			fieldStart = false
		}
	}
}
