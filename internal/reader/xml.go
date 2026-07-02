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
//     子要素を持つ要素に空白以外のテキストがある場合は、子要素で区切られた
//     テキストランごとに 要素パス + /text()[n] (1 始まりの出現順) に出力
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
// テキストは子要素で区切られたラン単位で保持します
// (連結すると <mixed>a<sub/>b</mixed> と <mixed>ab<sub/></mixed> の構造差分が潰れるため)。
type xmlElement struct {
	path       string
	curText    strings.Builder
	textRuns   []string
	childCount map[string]int // 同名子要素の出現数
	hasChild   bool
}

// flushTextRun は蓄積中のテキストをランとして確定します。
func (e *xmlElement) flushTextRun() {
	e.textRuns = append(e.textRuns, e.curText.String())
	e.curText.Reset()
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
			parent.flushTextRun()
			name := xmlName(t.Name)
			parent.childCount[name]++
			path := parent.path + "/" + name + "[" + strconv.Itoa(parent.childCount[name]) + "]"
			elem := &xmlElement{path: path, childCount: map[string]int{}}
			for _, attr := range t.Attr {
				// 名前空間宣言 (xmlns / xmlns:prefix) は比較対象外。
				// 要素・属性名は URI で正規化済みのため、プリフィックス名や
				// 宣言位置の違いを差分にしない
				if attr.Name.Space == "xmlns" || (attr.Name.Space == "" && attr.Name.Local == "xmlns") {
					continue
				}
				pairs = append(pairs, pathValuePair{path: path + "/@" + xmlName(attr.Name), value: attr.Value})
			}
			stack = append(stack, elem)

		case xml.EndElement:
			if len(stack) <= 1 {
				return nil, fmt.Errorf("要素の対応が不正です: %s", t.Name.Local)
			}
			elem := stack[len(stack)-1]
			stack = stack[:len(stack)-1]
			elem.flushTextRun()
			if !elem.hasChild {
				// 子要素なし: 全テキストを要素値として出力
				pairs = append(pairs, pathValuePair{path: elem.path, value: strings.TrimSpace(strings.Join(elem.textRuns, ""))})
				continue
			}
			// 子要素あり (mixed content): 空白以外のテキストランを出現順に出力
			runIndex := 0
			for _, run := range elem.textRuns {
				text := strings.TrimSpace(run)
				if text == "" {
					continue
				}
				runIndex++
				pairs = append(pairs, pathValuePair{path: elem.path + "/text()[" + strconv.Itoa(runIndex) + "]", value: text})
			}

		case xml.CharData:
			stack[len(stack)-1].curText.Write(t)
		}
	}
	if len(stack) != 1 {
		return nil, fmt.Errorf("ドキュメントが閉じられていません")
	}
	return pairs, nil
}

// xmlName は名前空間付きの要素・属性名を返します。
// プリフィックスは左右のファイルで異なりうるため、名前空間 URI で正規化します。
func xmlName(name xml.Name) string {
	if name.Space == "" {
		return name.Local
	}
	return name.Space + ":" + name.Local
}
