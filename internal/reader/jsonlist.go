package reader

import (
	"fmt"
	"os"
	"strings"

	"golang.org/x/text/encoding"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/row"
)

// jsonListReader は JsonList 形式(改行区切り JSON)のリーダです。
// Java 版 JsonListRowReader 相当。
type jsonListReader struct {
	file      *os.File
	lr        *lineReader
	layout    *config.FileLayout
	filePath  string
	curRowNum int64
}

func newJSONListReader(filePath string, enc encoding.Encoding, layout *config.FileLayout) (RowReader, error) {
	f, r, err := openDecoded(filePath, enc)
	if err != nil {
		return nil, err
	}
	return &jsonListReader{file: f, lr: newLineReader(r), layout: layout, filePath: filePath}, nil
}

func (j *jsonListReader) Next() (*row.Row, error) {
	j.curRowNum++
	line, ok, err := j.lr.readLine()
	if err != nil {
		return nil, fmt.Errorf("ファイルを読込みできません。対象:%s#%d: %w", j.filePath, j.curRowNum, err)
	}
	if !ok {
		return nil, nil
	}
	lineMap, err := row.ParseJSONObject(line)
	if err != nil {
		return nil, fmt.Errorf("%s のパースに失敗しました。#%d: %w", j.filePath, j.curRowNum, err)
	}
	if lineMap == nil {
		// Java 版: JSON リテラル null は readLine が null を返し EOF 扱い
		return nil, nil
	}
	return parseRow(j.layout, lineMap, line, j.curRowNum, nil, deepCopy)
}

func (j *jsonListReader) Close() error {
	return j.file.Close()
}

// jsonReader は Json 形式(1 ファイル = 1 JSON オブジェクト)のリーダです。
// Java 版 JsonRowReader 相当。全行を連結して 1 レコードとしてパースします。
type jsonReader struct {
	*jsonListReader
	done bool
}

func newJSONReader(filePath string, enc encoding.Encoding, layout *config.FileLayout) (RowReader, error) {
	f, r, err := openDecoded(filePath, enc)
	if err != nil {
		return nil, err
	}
	return &jsonReader{
		jsonListReader: &jsonListReader{file: f, lr: newLineReader(r), layout: layout, filePath: filePath},
	}, nil
}

func (j *jsonReader) Next() (*row.Row, error) {
	if j.done {
		return nil, nil
	}
	j.done = true
	j.curRowNum++

	var contentBuilder strings.Builder
	for {
		line, ok, err := j.lr.readLine()
		if err != nil {
			return nil, fmt.Errorf("ファイルを読込みできません。対象:%s#%d: %w", j.filePath, j.curRowNum, err)
		}
		if !ok {
			break
		}
		contentBuilder.WriteString(line)
	}
	content := contentBuilder.String()

	lineMap, err := row.ParseJSONObject(content)
	if err != nil {
		return nil, fmt.Errorf("%s のパースに失敗しました。: %w", j.filePath, err)
	}
	if lineMap == nil {
		return nil, nil
	}
	return parseRow(j.layout, lineMap, content, j.curRowNum, nil, deepCopy)
}

// deepCopy は項目 ID (ドット記法対応) を指定して、Map 内の値を再帰的にコピーします。
// Java 版 JsonListRowReader.deepCopy 相当。
func deepCopy(itemID string, fromMap *row.OrderedMap, toMap *row.OrderedMap) error {
	dotIdx := strings.Index(itemID, ".")
	if dotIdx < 0 {
		// ドットなし: 直接コピー
		toMap.Put(itemID, fromMap.GetOrNil(itemID))
		return nil
	}

	parentItemID := itemID[:dotIdx]
	childItemID := itemID[dotIdx+1:]
	parentFromObj := fromMap.GetOrNil(parentItemID)
	if parentFromObj == nil {
		return fmt.Errorf("変換元マップに 親項目ID:%s が存在しません。対象項目ID:%s", parentItemID, itemID)
	}

	switch parentFrom := parentFromObj.(type) {
	case []any:
		// 親が List の場合
		var parentToList []any
		if existing, ok := toMap.Get(parentItemID); ok && existing != nil {
			parentToList, _ = existing.([]any)
		}
		for index, sub := range parentFrom {
			switch subVal := sub.(type) {
			case *row.OrderedMap:
				// 変換先リストの同一インデックス位置に再帰コピー
				var subTo *row.OrderedMap
				if index < len(parentToList) {
					subTo, _ = parentToList[index].(*row.OrderedMap)
				} else {
					subTo = row.NewOrderedMap()
					parentToList = append(parentToList, subTo)
				}
				if err := deepCopy(childItemID, subVal, subTo); err != nil {
					return err
				}
			case []any:
				// ネストしたリストは全要素を再帰コピーして追加
				copied, err := deepCopyList(subVal)
				if err != nil {
					return err
				}
				parentToList = append(parentToList, copied...)
			default:
				parentToList = append(parentToList, sub)
			}
		}
		toMap.Put(parentItemID, parentToList)
		return nil

	case *row.OrderedMap:
		// 親が Map の場合
		var parentToMap *row.OrderedMap
		if existing, ok := toMap.Get(parentItemID); ok && existing != nil {
			parentToMap, _ = existing.(*row.OrderedMap)
		}
		if parentToMap == nil {
			parentToMap = row.NewOrderedMap()
			toMap.Put(parentItemID, parentToMap)
		}
		return deepCopy(childItemID, parentFrom, parentToMap)

	default:
		// スカラーに対する子指定: Java 版は Map として扱い ClassCastException になるが、
		// 実行時エラーとして返す
		return fmt.Errorf("項目ID:%s の親 %s は階層を持ちません", itemID, parentItemID)
	}
}

// deepCopyList はリストの全要素を再帰コピーします (Java 版 addDeepCopiedList 相当)。
func deepCopyList(fromList []any) ([]any, error) {
	var out []any
	for _, from := range fromList {
		fromDummy := row.NewOrderedMap()
		fromDummy.Put("dummy", from)
		toDummy := row.NewOrderedMap()
		if err := deepCopy("dummy", fromDummy, toDummy); err != nil {
			return nil, err
		}
		out = append(out, toDummy.GetOrNil("dummy"))
	}
	return out, nil
}
