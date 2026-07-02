package compare

import (
	"testing"

	"github.com/scenario-test-framework/compare-files/internal/status"
)

func TestCompareInCriteriaEqualNotEqual(t *testing.T) {
	tests := []struct {
		criteria    status.CompareCriteria
		left, right string
		want        status.CompareStatus
	}{
		{"", "a", "a", status.CompareOK}, // criteria 未設定は Equal 扱い
		{"", "a", "b", status.CompareNG},
		{status.CriteriaEqual, "", "", status.CompareOK},
		{status.CriteriaEqual, "a", "", status.CompareNG},
		{status.CriteriaEqual, "", "b", status.CompareNG},
		{status.CriteriaEqual, "abc", "abc", status.CompareOK},
		{status.CriteriaEqual, "abc", "abd", status.CompareNG},
		{status.CriteriaNotEqual, "", "", status.CompareNG},
		{status.CriteriaNotEqual, "a", "", status.CompareOK},
		{status.CriteriaNotEqual, "abc", "abc", status.CompareNG},
		{status.CriteriaNotEqual, "abc", "abd", status.CompareOK},
		{status.CriteriaIgnore, "a", "b", status.CompareIgnore},
	}
	for _, tt := range tests {
		got, err := CompareInCriteria(tt.criteria, tt.left, tt.right)
		if err != nil {
			t.Errorf("%s(%q,%q): %v", tt.criteria, tt.left, tt.right, err)
			continue
		}
		if got != tt.want {
			t.Errorf("%s(%q,%q) = %v, want %v", tt.criteria, tt.left, tt.right, got, tt.want)
		}
	}
}

func TestCompareInCriteriaNumber(t *testing.T) {
	tests := []struct {
		criteria    status.CompareCriteria
		left, right string
		want        status.CompareStatus
	}{
		// GreaterThan_Left: 左 < 右 で OK
		{status.CriteriaNumberGreaterThanLeft, "1", "2", status.CompareOK},
		{status.CriteriaNumberGreaterThanLeft, "2", "2", status.CompareNG},
		{status.CriteriaNumberGreaterThanLeft, "3", "2", status.CompareNG},
		// GreaterEqualThan_Left: 左 <= 右 で OK
		{status.CriteriaNumberGreaterEqualThanLeft, "1", "2", status.CompareOK},
		{status.CriteriaNumberGreaterEqualThanLeft, "2", "2", status.CompareOK},
		{status.CriteriaNumberGreaterEqualThanLeft, "3", "2", status.CompareNG},
		// LessThan_Left: 左 > 右 で OK
		{status.CriteriaNumberLessThanLeft, "3", "2", status.CompareOK},
		{status.CriteriaNumberLessThanLeft, "2", "2", status.CompareNG},
		// LessEqualThan_Left: 左 >= 右 で OK
		{status.CriteriaNumberLessEqualThanLeft, "2", "2", status.CompareOK},
		{status.CriteriaNumberLessEqualThanLeft, "1", "2", status.CompareNG},
		// 小数・任意精度
		{status.CriteriaNumberGreaterEqualThanLeft, "1.5", "1.50", status.CompareOK},
		{status.CriteriaNumberGreaterThanLeft, "0.1", "0.2", status.CompareOK},
		// 空値は NG
		{status.CriteriaNumberGreaterThanLeft, "", "2", status.CompareNG},
		{status.CriteriaNumberGreaterThanLeft, "1", "", status.CompareNG},
		{status.CriteriaNumberGreaterThanLeft, "", "", status.CompareNG},
	}
	for _, tt := range tests {
		got, err := CompareInCriteria(tt.criteria, tt.left, tt.right)
		if err != nil {
			t.Errorf("%s(%q,%q): %v", tt.criteria, tt.left, tt.right, err)
			continue
		}
		if got != tt.want {
			t.Errorf("%s(%q,%q) = %v, want %v", tt.criteria, tt.left, tt.right, got, tt.want)
		}
	}
	// 数値変換できない場合はエラー (Java の UtilException 相当)
	if _, err := CompareInCriteria(status.CriteriaNumberGreaterThanLeft, "abc", "2"); err == nil {
		t.Error("非数値でエラーにならない")
	}
}

func TestCompareInCriteriaYearMonth(t *testing.T) {
	tests := []struct {
		criteria    status.CompareCriteria
		left, right string
		want        status.CompareStatus
	}{
		{status.CriteriaYearGreaterEqualThanLeft, "2017", "2017", status.CompareOK},
		{status.CriteriaYearGreaterThanLeft, "20161231", "20170101", status.CompareOK},
		{status.CriteriaYearLessThanLeft, "2018", "2017", status.CompareOK},
		{status.CriteriaMonthGreaterEqualThanLeft, "201701", "201701", status.CompareOK},
		{status.CriteriaMonthGreaterThanLeft, "201701", "201702", status.CompareOK},
		{status.CriteriaMonthLessEqualThanLeft, "201702", "201701", status.CompareOK},
	}
	for _, tt := range tests {
		got, err := CompareInCriteria(tt.criteria, tt.left, tt.right)
		if err != nil {
			t.Errorf("%s(%q,%q): %v", tt.criteria, tt.left, tt.right, err)
			continue
		}
		if got != tt.want {
			t.Errorf("%s(%q,%q) = %v, want %v", tt.criteria, tt.left, tt.right, got, tt.want)
		}
	}
}

func TestCompareInCriteriaDateDatetime(t *testing.T) {
	tests := []struct {
		criteria    status.CompareCriteria
		left, right string
		want        status.CompareStatus
	}{
		// Date: 時刻切り捨てで比較
		{status.CriteriaDateGreaterEqualThanLeft, "2017/01/01", "2017-01-01", status.CompareOK},
		{status.CriteriaDateGreaterEqualThanLeft, "2017/01/01 10:00:00", "2017/01/01 09:00:00", status.CompareOK},
		{status.CriteriaDateGreaterThanLeft, "20170101", "20170102", status.CompareOK},
		{status.CriteriaDateLessThanLeft, "2017/01/02", "2017/01/01", status.CompareOK},
		// Datetime: 時刻込みで比較
		{status.CriteriaDatetimeGreaterEqualThanLeft, "2017/01/01 00:00:00.000", "2017/01/01 00:00:00.000", status.CompareOK},
		{status.CriteriaDatetimeGreaterThanLeft, "2017/01/01 00:00:00.000", "2017/01/01 00:00:00.001", status.CompareOK},
		{status.CriteriaDatetimeGreaterThanLeft, "2017/01/01 00:00:00.001", "2017/01/01 00:00:00.000", status.CompareNG},
	}
	for _, tt := range tests {
		got, err := CompareInCriteria(tt.criteria, tt.left, tt.right)
		if err != nil {
			t.Errorf("%s(%q,%q): %v", tt.criteria, tt.left, tt.right, err)
			continue
		}
		if got != tt.want {
			t.Errorf("%s(%q,%q) = %v, want %v", tt.criteria, tt.left, tt.right, got, tt.want)
		}
	}
}
