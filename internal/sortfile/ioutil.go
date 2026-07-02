package sortfile

import (
	"bufio"
	"io"
)

// newBufWriter はバッファ付きライタを返します。
func newBufWriter(w io.Writer) *bufio.Writer {
	return bufio.NewWriterSize(w, 64*1024)
}

// lineReader は Java の BufferedReader.readLine 相当 (\n, \r, \r\n 対応) です。
type lineReader struct {
	r *bufio.Reader
}

func newLineReader(r io.Reader) *lineReader {
	return &lineReader{r: bufio.NewReaderSize(r, 64*1024)}
}

// readLine は 1 行を返します。EOF は ("", false, nil)。
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
