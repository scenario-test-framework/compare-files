package compare

import (
	"fmt"
	"log/slog"
	"strings"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/msg"
	"github.com/scenario-test-framework/compare-files/internal/row"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// ItemResult は項目単位の比較結果です。Java 版 TextFileItemCompareResult 相当。
// LeftValue/RightValue の nil は Java の null に対応します。
type ItemResult struct {
	ID         string
	LeftValue  *string
	RightValue *string
	Criteria   status.CompareCriteria
	Status     status.CompareStatus
}

// RowResult は行単位の比較結果です。Java 版 TextFileRowCompareResult 相当。
type RowResult struct {
	Status      status.CompareStatus
	FileLayout  *config.FileLayout
	RecordType  status.RecordType // "" = レコードタイプなし (Java の null)
	LeftRowNum  int64
	RightRowNum int64
	Items       []*ItemResult
}

// DiffItemIDList は差分を検出した項目 ID のリストを返します。
func (r *RowResult) DiffItemIDList() []string {
	var list []string
	for _, item := range r.Items {
		if item.Status != status.CompareOK && item.Status != status.CompareIgnore {
			list = append(list, item.ID)
		}
	}
	return list
}

// CompareRows は左右の行データを比較します。Java 版 TextFileRowCompareResult.compare 相当。
func CompareRows(fileLayout *config.FileLayout, leftRow, rightRow *row.Row) (*RowResult, error) {
	result := &RowResult{
		Status:     status.CompareProcessing,
		FileLayout: fileLayout,
	}

	leftRawLine := leftRow.RawLine
	criteriaMap, criteriaOrder, err := getCriteriaMap(fileLayout, leftRawLine)
	if err != nil {
		return nil, err
	}

	if criteriaMap == nil {
		// レイアウトなし: value 1 項目に 1 行のデータがまとめて入っている前提で比較
		itemID := config.DefaultItemID
		if err := result.compareItem(itemID, GetItemValue(leftRow, itemID), GetItemValue(rightRow, itemID), status.CriteriaEqual); err != nil {
			return nil, err
		}
	} else {
		for _, itemID := range criteriaOrder {
			criteria := criteriaMap[itemID]
			if err := result.compareItem(itemID, GetItemValue(leftRow, itemID), GetItemValue(rightRow, itemID), criteria); err != nil {
				return nil, err
			}
		}
	}

	// 比較ステータスの更新
	if result.Status == status.CompareProcessing {
		tempStatus := status.CompareOK
		for _, item := range result.Items {
			if item.Status == status.CompareNG || item.Status == status.CompareLeftOnly || item.Status == status.CompareRightOnly {
				tempStatus = status.CompareNG
				break
			}
		}
		result.Status = tempStatus
	}

	// レコードタイプの更新
	recordType, err := getRecordType(fileLayout, leftRawLine)
	if err != nil {
		return nil, err
	}
	result.RecordType = recordType

	// 行番号の更新
	result.LeftRowNum = leftRow.RowNum
	result.RightRowNum = rightRow.RowNum
	if result.Status == status.CompareLeftOnly {
		result.RightRowNum = config.UnknownLine
	} else if result.Status == status.CompareRightOnly {
		result.LeftRowNum = config.UnknownLine
	}
	return result, nil
}

// FixedRowResult は LeftOnly/RightOnly の固定行比較結果を返します。
// Java 版 TextFileRowCompareResult.getFixedResult 相当。
func FixedRowResult(st status.CompareStatus, fileLayout *config.FileLayout, leftRow, rightRow *row.Row) *RowResult {
	leftRowNum := int64(-1)
	if leftRow != nil {
		leftRowNum = leftRow.RowNum
	}
	rightRowNum := int64(-1)
	if rightRow != nil {
		rightRowNum = rightRow.RowNum
	}

	curRow := leftRow
	if st != status.CompareLeftOnly {
		curRow = rightRow
	}

	result := &RowResult{
		Status:      st,
		FileLayout:  fileLayout,
		LeftRowNum:  leftRowNum,
		RightRowNum: rightRowNum,
	}
	// キー項目 → バリュー項目の順に固定値を設定
	setFixed := func(itemID string, itemValue any) {
		value := row.ToJavaString(itemValue) // null は "null"
		item := &ItemResult{ID: itemID, Status: st}
		if st == status.CompareLeftOnly {
			item.LeftValue = &value
		} else {
			item.RightValue = &value
		}
		result.Items = append(result.Items, item)
	}
	if curRow.KeyMap != nil {
		for _, key := range curRow.KeyMap.Keys() {
			setFixed(key, curRow.KeyMap.GetOrNil(key))
		}
	}
	if curRow.ValueMap != nil {
		for _, key := range curRow.ValueMap.Keys() {
			setFixed(key, curRow.ValueMap.GetOrNil(key))
		}
	}
	return result
}

// compareItem は項目を比較して結果に追加します。
func (r *RowResult) compareItem(itemID string, leftValue, rightValue *string, criteria status.CompareCriteria) error {
	var st status.CompareStatus
	switch {
	case leftValue == nil && rightValue == nil:
		st = status.CompareOK
	case leftValue != nil && rightValue == nil:
		st = status.CompareLeftOnly
	case leftValue == nil && rightValue != nil:
		st = status.CompareRightOnly
	default:
		var err error
		st, err = CompareInCriteria(criteria, *leftValue, *rightValue)
		if err != nil {
			return err
		}
	}
	r.Items = append(r.Items, &ItemResult{
		ID:         itemID,
		LeftValue:  leftValue,
		RightValue: rightValue,
		Criteria:   criteria,
		Status:     st,
	})
	return nil
}

// getCriteriaMap はファイルレイアウトと行文字列から比較条件マップを返します。
// レイアウトなし・レコードタイプなしの場合は (nil, nil, nil) を返します。
func getCriteriaMap(fileLayout *config.FileLayout, rawLine string) (map[string]status.CompareCriteria, []string, error) {
	recordType, err := getRecordType(fileLayout, rawLine)
	if err != nil {
		return nil, nil, err
	}
	if recordType == "" {
		return nil, nil, nil
	}
	for _, rl := range fileLayout.RecordList {
		if rl.Type == recordType {
			m, order := rl.CriteriaMap()
			return m, order, nil
		}
	}
	return nil, nil, fmt.Errorf("レコードレイアウトを判別できません。ファイルレイアウト:%s、行データ:%s",
		fileLayout.LogicalFileName, rawLine)
}

// getRecordType はファイルレイアウトと行文字列からレコードタイプを判定します。
// "" は Java の null (レコードタイプなし) に対応します。
func getRecordType(fileLayout *config.FileLayout, rawLine string) (status.RecordType, error) {
	if fileLayout == nil {
		return "", nil
	}
	recordList := fileLayout.RecordList
	if len(recordList) == 0 {
		return "", nil
	}
	if len(recordList) == 1 {
		return recordList[0].Type, nil
	}

	// ヘッダー付き CSV/TSV は常に Data
	if fileLayout.FileFormat == status.FormatCSVWithHeader || fileLayout.FileFormat == status.FormatTSVWithHeader {
		return status.RecordData, nil
	}

	for _, rl := range recordList {
		if rl.CodeValue == "" {
			return "", fmt.Errorf("コード値が取得できません。ファイルレイアウト:%s、レコードタイプ:%s",
				fileLayout.LogicalFileName, rl.Type)
		}
		var target string
		switch fileLayout.FileFormat {
		case status.FormatCSVNoHeader, status.FormatCSVWithHeader, status.FormatTSVNoHeader, status.FormatTSVWithHeader:
			// CSV/TSV は括り文字を除去してからコード値を取得
			target = strings.ReplaceAll(rawLine, "\"", "")
		default:
			target = rawLine
		}
		code, err := javaSubstring(target, len([]rune(rl.CodeValue)))
		if err != nil {
			return "", err
		}
		if code == rl.CodeValue {
			return rl.Type, nil
		}
	}

	// 判別できない場合、Data レコードとして扱う
	slog.Warn(msg.Get("compare.file.text.row.layoutAsData", fileLayout.LogicalFileName, rawLine))
	return status.RecordData, nil
}
