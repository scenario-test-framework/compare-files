package reader

import (
	"fmt"
	"io"
	"os"

	"golang.org/x/text/encoding"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/row"
)

// yamlReader は Yaml 形式(1 ファイル = 1 マッピング)のリーダです。
// Json 形式と同様に全体を 1 レコードとして扱い、項目 ID (ドット記法) で比較します。
type yamlReader struct {
	file     *os.File
	r        io.Reader
	layout   *config.FileLayout
	filePath string
	done     bool
}

func newYamlReader(filePath string, enc encoding.Encoding, layout *config.FileLayout) (RowReader, error) {
	f, r, err := openDecoded(filePath, enc)
	if err != nil {
		return nil, err
	}
	return &yamlReader{file: f, r: r, layout: layout, filePath: filePath}, nil
}

func (y *yamlReader) Next() (*row.Row, error) {
	if y.done {
		return nil, nil
	}
	y.done = true

	content, err := io.ReadAll(y.r)
	if err != nil {
		return nil, fmt.Errorf("ファイルを読込みできません。対象:%s: %w", y.filePath, err)
	}

	lineMap, err := row.ParseYAMLObject(string(content))
	if err != nil {
		return nil, fmt.Errorf("%s のパースに失敗しました。: %w", y.filePath, err)
	}
	if lineMap == nil {
		return nil, nil
	}
	return parseRow(y.layout, lineMap, string(content), 1, nil, deepCopy)
}

func (y *yamlReader) Close() error {
	return y.file.Close()
}
