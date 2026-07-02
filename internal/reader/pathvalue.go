package reader

import (
	"fmt"
	"io"
	"os"
	"regexp"
	"sort"
	"strconv"
	"strings"

	"golang.org/x/text/encoding"

	"github.com/scenario-test-framework/compare-files/internal/compare"
	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/row"
)

// pathValuePair は path・value モードの 1 比較単位です。
type pathValuePair struct {
	path  string
	value any
}

// pathValueReader は path・value ペアの列を行データとして返す共通リーダです。
// マージ結合で比較できるよう、ペアは path (比較キー) 順にソートして返します。
// RowNum はドキュメント内の出現順 (1 始まり) を保持します。
type pathValueReader struct {
	file   *os.File
	layout *config.FileLayout
	rows   []*row.Row
	index  int
	loaded bool
	load   func() ([]pathValuePair, error)
}

func (p *pathValueReader) Next() (*row.Row, error) {
	if !p.loaded {
		p.loaded = true
		pairs, err := p.load()
		if err != nil {
			return nil, err
		}
		rows, err := pathValueRows(p.layout, pairs)
		if err != nil {
			return nil, err
		}
		p.rows = rows
	}
	if p.index >= len(p.rows) {
		return nil, nil
	}
	r := p.rows[p.index]
	p.index++
	return r, nil
}

func (p *pathValueReader) Close() error {
	return p.file.Close()
}

// pathValueRows はペアを行データに変換し、比較キー (path) 順にソートします。
func pathValueRows(layout *config.FileLayout, pairs []pathValuePair) ([]*row.Row, error) {
	rows := make([]*row.Row, 0, len(pairs))
	for i, pair := range pairs {
		lineMap := row.NewOrderedMap()
		lineMap.Put(config.PathValueItemPath, pair.path)
		lineMap.Put(config.PathValueItemValue, pair.value)
		parsed, err := parseRow(layout, lineMap, pair.path, int64(i+1), nil, deepCopy)
		if err != nil {
			return nil, err
		}
		rows = append(rows, parsed)
	}
	var sortErr error
	sort.SliceStable(rows, func(i, j int) bool {
		result, err := compare.CompareRowKeys(rows[i], rows[j])
		if err != nil && sortErr == nil {
			sortErr = err
		}
		return result < 0
	})
	if sortErr != nil {
		return nil, sortErr
	}
	return rows, nil
}

// flattenJSONPath は値ツリーを jsonPath → リーフ値 のペア列 (ドキュメント順) に
// 平坦化します。空のオブジェクト・リストはそれ自体を 1 ペアとして出力します。
func flattenJSONPath(v any, path string, out []pathValuePair) []pathValuePair {
	switch val := v.(type) {
	case *row.OrderedMap:
		if val.Len() == 0 {
			return append(out, pathValuePair{path: path, value: val})
		}
		for _, key := range val.Keys() {
			out = flattenJSONPath(val.GetOrNil(key), path+jsonPathKey(key), out)
		}
		return out
	case []any:
		if len(val) == 0 {
			return append(out, pathValuePair{path: path, value: val})
		}
		for i, elem := range val {
			out = flattenJSONPath(elem, path+"["+strconv.Itoa(i)+"]", out)
		}
		return out
	default:
		return append(out, pathValuePair{path: path, value: val})
	}
}

// safeJSONPathKey はドット記法で表記できるキーのパターンです。
var safeJSONPathKey = regexp.MustCompile(`^[A-Za-z_][A-Za-z0-9_\-]*$`)

// jsonPathKey はキーの jsonPath 表記を返します。
// 記号を含むキーはブラケット表記 (['key']) にエスケープします。
func jsonPathKey(key string) string {
	if safeJSONPathKey.MatchString(key) {
		return "." + key
	}
	escaped := strings.ReplaceAll(key, `\`, `\\`)
	escaped = strings.ReplaceAll(escaped, `'`, `\'`)
	return "['" + escaped + "']"
}

// newJSONPathValueReader は Json / JsonList 形式の path・value モードリーダを返します。
// Json はルート値を $ から、JsonList は各行を $[n] (n は 0 始まりの行番号) から
// 平坦化します。
func newJSONPathValueReader(filePath string, enc encoding.Encoding, layout *config.FileLayout, isList bool) (RowReader, error) {
	f, r, err := openDecoded(filePath, enc)
	if err != nil {
		return nil, err
	}
	reader := &pathValueReader{file: f, layout: layout}
	reader.load = func() ([]pathValuePair, error) {
		if isList {
			return loadJSONListPairs(filePath, r)
		}
		return loadJSONPairs(filePath, r)
	}
	return reader, nil
}

func loadJSONPairs(filePath string, r io.Reader) ([]pathValuePair, error) {
	content, err := io.ReadAll(r)
	if err != nil {
		return nil, fmt.Errorf("ファイルを読込みできません。対象:%s: %w", filePath, err)
	}
	lineMap, err := row.ParseJSONObject(string(content))
	if err != nil {
		return nil, fmt.Errorf("%s のパースに失敗しました。: %w", filePath, err)
	}
	if lineMap == nil {
		return nil, nil
	}
	return flattenJSONPath(lineMap, "$", nil), nil
}

func loadJSONListPairs(filePath string, r io.Reader) ([]pathValuePair, error) {
	lr := newLineReader(r)
	var pairs []pathValuePair
	index := 0
	for {
		line, ok, err := lr.readLine()
		if err != nil {
			return nil, fmt.Errorf("ファイルを読込みできません。対象:%s#%d: %w", filePath, index+1, err)
		}
		if !ok {
			return pairs, nil
		}
		if strings.TrimSpace(line) == "" {
			continue
		}
		lineMap, err := row.ParseJSONObject(line)
		if err != nil {
			return nil, fmt.Errorf("%s のパースに失敗しました。#%d: %w", filePath, index+1, err)
		}
		if lineMap == nil {
			// Java 版 JsonList と同様、JSON リテラル null は EOF 扱い
			return pairs, nil
		}
		pairs = flattenJSONPath(lineMap, "$["+strconv.Itoa(index)+"]", pairs)
		index++
	}
}

// newYamlPathValueReader は Yaml 形式の path・value モードリーダを返します。
func newYamlPathValueReader(filePath string, enc encoding.Encoding, layout *config.FileLayout) (RowReader, error) {
	f, r, err := openDecoded(filePath, enc)
	if err != nil {
		return nil, err
	}
	reader := &pathValueReader{file: f, layout: layout}
	reader.load = func() ([]pathValuePair, error) {
		content, err := io.ReadAll(r)
		if err != nil {
			return nil, fmt.Errorf("ファイルを読込みできません。対象:%s: %w", filePath, err)
		}
		lineMap, err := row.ParseYAMLObject(string(content))
		if err != nil {
			return nil, fmt.Errorf("%s のパースに失敗しました。: %w", filePath, err)
		}
		if lineMap == nil {
			return nil, nil
		}
		return flattenJSONPath(lineMap, "$", nil), nil
	}
	return reader, nil
}
