package reader

import (
	"encoding/xml"
	"fmt"
	"io"
	"strconv"
	"strings"

	"golang.org/x/text/encoding"

	"github.com/scenario-test-framework/compare-files/internal/config"
)

// newXMLReader は XML 形式のリーダを返します。
// schema (レイアウト項目) の定義なしで、xpath を比較キー、テキスト・属性値を
// 比較値とした path・value ペアに平坦化して比較します。
//   - 要素:   /root/item[n] (n は同名兄弟要素内の 1 始まりの出現順)
//   - 属性:   要素パス + /@属性名
//   - テキスト: 子要素を持たない要素はそのパスに値を出力
//     子要素を持つ要素に空白以外のテキストがある場合は 要素パス + /text() に出力
func newXMLReader(filePath string, enc encoding.Encoding, layout *config.FileLayout) (RowReader, error) {
	f, r, err := openDecoded(filePath, enc)
	if err != nil {
		return nil, err
	}
	reader := &pathValueReader{file: f, layout: layout}
	reader.load = func() ([]pathValuePair, error) {
		pairs, err := loadXMLPairs(r)
		if err != nil {
			return nil, fmt.Errorf("%s のパースに失敗しました。: %w", filePath, err)
		}
		return pairs, nil
	}
	return reader, nil
}

// xmlElement は解析中の要素の状態です。
type xmlElement struct {
	path       string
	text       strings.Builder
	childCount map[string]int // 同名子要素の出現数
	hasChild   bool
}

// loadXMLPairs は XML ドキュメントを path・value ペアに平坦化します。
func loadXMLPairs(r io.Reader) ([]pathValuePair, error) {
	dec := xml.NewDecoder(r)
	// 入力は layout の charset で既にデコード済みのため、XML 宣言の encoding は無視する
	dec.CharsetReader = func(_ string, input io.Reader) (io.Reader, error) {
		return input, nil
	}

	var pairs []pathValuePair
	root := &xmlElement{childCount: map[string]int{}}
	stack := []*xmlElement{root}

	for {
		tok, err := dec.Token()
		if err == io.EOF {
			break
		}
		if err != nil {
			return nil, err
		}
		switch t := tok.(type) {
		case xml.StartElement:
			parent := stack[len(stack)-1]
			parent.hasChild = true
			name := xmlName(t.Name)
			parent.childCount[name]++
			path := parent.path + "/" + name + "[" + strconv.Itoa(parent.childCount[name]) + "]"
			elem := &xmlElement{path: path, childCount: map[string]int{}}
			for _, attr := range t.Attr {
				pairs = append(pairs, pathValuePair{path: path + "/@" + xmlName(attr.Name), value: attr.Value})
			}
			stack = append(stack, elem)

		case xml.EndElement:
			if len(stack) <= 1 {
				return nil, fmt.Errorf("要素の対応が不正です: %s", t.Name.Local)
			}
			elem := stack[len(stack)-1]
			stack = stack[:len(stack)-1]
			text := strings.TrimSpace(elem.text.String())
			if !elem.hasChild {
				pairs = append(pairs, pathValuePair{path: elem.path, value: text})
			} else if text != "" {
				pairs = append(pairs, pathValuePair{path: elem.path + "/text()", value: text})
			}

		case xml.CharData:
			stack[len(stack)-1].text.Write(t)
		}
	}
	if len(stack) != 1 {
		return nil, fmt.Errorf("ドキュメントが閉じられていません")
	}
	return pairs, nil
}

// xmlName は名前空間プリフィックス付きの要素・属性名を返します。
func xmlName(name xml.Name) string {
	if name.Space == "" {
		return name.Local
	}
	return name.Space + ":" + name.Local
}
