package compare

import (
	"testing"

	"github.com/scenario-test-framework/compare-files/internal/row"
)

func om(pairs ...any) *row.OrderedMap {
	m := row.NewOrderedMap()
	for i := 0; i < len(pairs); i += 2 {
		m.Put(pairs[i].(string), pairs[i+1])
	}
	return m
}

func mustDeepCompare(t *testing.T, left, right any) int {
	t.Helper()
	got, err := DeepCompare(left, right)
	if err != nil {
		t.Fatalf("DeepCompare(%v, %v): %v", left, right, err)
	}
	return got
}

func TestDeepCompareNull(t *testing.T) {
	if mustDeepCompare(t, nil, nil) != 0 {
		t.Error("null == null")
	}
	if mustDeepCompare(t, nil, "a") >= 0 {
		t.Error("null は最小")
	}
	if mustDeepCompare(t, "a", nil) <= 0 {
		t.Error("null は最小")
	}
}

func TestDeepCompareScalar(t *testing.T) {
	if mustDeepCompare(t, "abc", "abd") >= 0 {
		t.Error("string 比較")
	}
	if mustDeepCompare(t, "abc", "abc") != 0 {
		t.Error("string 一致")
	}
	// Number は BigDecimal 比較: 10 > 9 (文字列比較なら "10" < "9")
	if mustDeepCompare(t, row.Number{Raw: "10"}, row.Number{Raw: "9"}) <= 0 {
		t.Error("数値比較になっていない")
	}
	if mustDeepCompare(t, row.Number{Raw: "1.50"}, row.Number{Raw: "1.5"}) != 0 {
		t.Error("1.50 == 1.5")
	}
	if mustDeepCompare(t, false, true) >= 0 {
		t.Error("false < true")
	}
}

func TestDeepCompareList(t *testing.T) {
	if mustDeepCompare(t, []any{"a", "b"}, []any{"a", "c"}) >= 0 {
		t.Error("要素差")
	}
	if mustDeepCompare(t, []any{"a"}, []any{"a", "b"}) >= 0 {
		t.Error("短い方が小")
	}
	if mustDeepCompare(t, []any{"a", "b"}, []any{"a", "b"}) != 0 {
		t.Error("一致")
	}
}

func TestDeepCompareMap(t *testing.T) {
	left := om("k1", "100", "k2", "AAA")
	right := om("k1", "100", "k2", "BBB")
	if mustDeepCompare(t, left, right) >= 0 {
		t.Error("値差")
	}
	if mustDeepCompare(t, left, om("k1", "100", "k2", "AAA")) != 0 {
		t.Error("一致")
	}
	// ネスト
	nl := om("keyObj", om("key1", row.Number{Raw: "100"}, "key2", "AAA"))
	nr := om("keyObj", om("key1", row.Number{Raw: "100"}, "key2", "AAB"))
	if mustDeepCompare(t, nl, nr) >= 0 {
		t.Error("ネスト値差")
	}
	// サイズ差: 小さい方が小
	if mustDeepCompare(t, om("k1", "a"), om("k1", "a", "k2", "b")) >= 0 {
		t.Error("サイズ差")
	}
}

func TestGetItemValue(t *testing.T) {
	r := &row.Row{
		KeyMap:   om("keyObj", om("key1", row.Number{Raw: "100"}, "key2", "AAA")),
		ValueMap: om("valueList", []any{om("content", om("month", row.Number{Raw: "201701"})), om("content", om("month", row.Number{Raw: "201702"}))}),
	}
	if v := GetItemValue(r, "keyObj.key1"); v == nil || *v != "100" {
		t.Errorf("keyObj.key1: %v", v)
	}
	if v := GetItemValue(r, "valueList.content.month"); v == nil || *v != "[201701, 201702]" {
		t.Errorf("valueList.content.month: %v", v)
	}
	if v := GetItemValue(r, "missing"); v != nil {
		t.Errorf("missing: %v", *v)
	}
	if v := GetItemValue(r, "keyObj"); v == nil || *v != "{key1=100, key2=AAA}" {
		t.Errorf("keyObj: %v", v)
	}
}
