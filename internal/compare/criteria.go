package compare

import (
	"fmt"
	"math/big"
	"strings"

	"github.com/scenario-test-framework/compare-files/internal/status"
)

// CompareInCriteria は比較条件に従って左右の値を比較します。
// Java 版 CompareUtils.compareInCriteria 相当。
// criteria が空 (Java の null) の場合は文字列 Equal 比較を行います。
// 数値・日付変換に失敗した場合はエラーを返します (Java の UtilException 相当)。
func CompareInCriteria(criteria status.CompareCriteria, left, right string) (status.CompareStatus, error) {
	switch criteria {
	case "":
		return compareEqual(left, right), nil
	case status.CriteriaIgnore:
		return status.CompareIgnore, nil
	case status.CriteriaEqual:
		return compareEqual(left, right), nil
	case status.CriteriaNotEqual:
		return compareNotEqual(left, right), nil
	}

	// 変換系の比較: どちらかが空なら NG
	if left == "" || right == "" {
		return status.CompareNG, nil
	}

	var result int
	var err error
	switch criteria {
	case status.CriteriaNumberGreaterThanLeft, status.CriteriaNumberGreaterEqualThanLeft,
		status.CriteriaNumberLessThanLeft, status.CriteriaNumberLessEqualThanLeft:
		result, err = compareAsNumber(left, right)
	case status.CriteriaYearGreaterThanLeft, status.CriteriaYearGreaterEqualThanLeft,
		status.CriteriaYearLessThanLeft, status.CriteriaYearLessEqualThanLeft:
		result, err = compareAsPrefixNumber(left, right, 4)
	case status.CriteriaMonthGreaterThanLeft, status.CriteriaMonthGreaterEqualThanLeft,
		status.CriteriaMonthLessThanLeft, status.CriteriaMonthLessEqualThanLeft:
		result, err = compareAsPrefixNumber(left, right, 6)
	case status.CriteriaDateGreaterThanLeft, status.CriteriaDateGreaterEqualThanLeft,
		status.CriteriaDateLessThanLeft, status.CriteriaDateLessEqualThanLeft:
		result, err = compareAsDate(left, right, true)
	case status.CriteriaDatetimeGreaterThanLeft, status.CriteriaDatetimeGreaterEqualThanLeft,
		status.CriteriaDatetimeLessThanLeft, status.CriteriaDatetimeLessEqualThanLeft:
		result, err = compareAsDate(left, right, false)
	default:
		return status.CompareError, fmt.Errorf("criteria で %s には対応していません", criteria)
	}
	if err != nil {
		return status.CompareError, err
	}
	return judgeByDirection(criteria, result), nil
}

// judgeByDirection は比較結果 (左-右) を条件の向きに従って OK/NG に変換します。
func judgeByDirection(criteria status.CompareCriteria, result int) status.CompareStatus {
	name := string(criteria)
	switch {
	case strings.Contains(name, "_GreaterEqualThan_"): // 左 <= 右: OK
		if result <= 0 {
			return status.CompareOK
		}
	case strings.Contains(name, "_GreaterThan_"): // 左 < 右: OK
		if result < 0 {
			return status.CompareOK
		}
	case strings.Contains(name, "_LessEqualThan_"): // 左 >= 右: OK
		if result >= 0 {
			return status.CompareOK
		}
	case strings.Contains(name, "_LessThan_"): // 左 > 右: OK
		if result > 0 {
			return status.CompareOK
		}
	}
	return status.CompareNG
}

// compareEqual は文字列 Equal 比較です (空文字と null を同一視)。
func compareEqual(left, right string) status.CompareStatus {
	switch {
	case left == "" && right == "":
		return status.CompareOK
	case left == "" || right == "":
		return status.CompareNG
	case left == right:
		return status.CompareOK
	default:
		return status.CompareNG
	}
}

// compareNotEqual は文字列 NotEqual 比較です。
func compareNotEqual(left, right string) status.CompareStatus {
	switch {
	case left == "" && right == "":
		return status.CompareNG
	case left == "" || right == "":
		return status.CompareOK
	case left == right:
		return status.CompareNG
	default:
		return status.CompareOK
	}
}

// compareAsNumber は BigDecimal 相当の任意精度数値比較です。
// big.Rat は "1/2" のような分数表記も受理するため、BigDecimal に合わせて除外します。
func compareAsNumber(left, right string) (int, error) {
	leftVal, err := parseBigDecimal(left)
	if err != nil {
		return 0, err
	}
	rightVal, err := parseBigDecimal(right)
	if err != nil {
		return 0, err
	}
	return leftVal.Cmp(rightVal), nil
}

func parseBigDecimal(s string) (*big.Rat, error) {
	if strings.Contains(s, "/") {
		return nil, fmt.Errorf("convNum でエラーが発生しました。target=%s", s)
	}
	v, ok := new(big.Rat).SetString(s)
	if !ok {
		return nil, fmt.Errorf("convNum でエラーが発生しました。target=%s", s)
	}
	return v, nil
}

// compareAsPrefixNumber は先頭 n 文字を数値として比較します (Year: 4 / Month: 6)。
func compareAsPrefixNumber(left, right string, n int) (int, error) {
	leftPrefix, err := javaSubstring(left, n)
	if err != nil {
		return 0, err
	}
	rightPrefix, err := javaSubstring(right, n)
	if err != nil {
		return 0, err
	}
	return compareAsNumber(leftPrefix, rightPrefix)
}

func javaSubstring(s string, n int) (string, error) {
	r := []rune(s)
	if len(r) < n {
		return "", fmt.Errorf("先頭 %d 文字を取得できません。target=%s", n, s)
	}
	return string(r[:n]), nil
}

// compareAsDate は日付 (clearTime=true で時刻切り捨て) として比較します。
func compareAsDate(left, right string, clearTime bool) (int, error) {
	leftVal, err := ToDate(left)
	if err != nil {
		return 0, fmt.Errorf("convDate でエラーが発生しました。target=%s: %w", left, err)
	}
	rightVal, err := ToDate(right)
	if err != nil {
		return 0, fmt.Errorf("convDate でエラーが発生しました。target=%s: %w", right, err)
	}
	if clearTime {
		leftVal = ClearTime(leftVal)
		rightVal = ClearTime(rightVal)
	}
	return leftVal.Compare(rightVal), nil
}
