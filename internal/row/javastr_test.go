package row

import "testing"

func TestJavaDoubleString(t *testing.T) {
	tests := map[float64]string{
		0:         "0.0",
		1:         "1.0",
		100:       "100.0",
		100.5:     "100.5",
		0.001:     "0.001",
		0.0001:    "1.0E-4",
		0.00015:   "1.5E-4",
		1e6:       "1000000.0",
		1e7:       "1.0E7",
		12345678:  "1.2345678E7",
		-3.14:     "-3.14",
		-0.5:      "-0.5",
		9999999.5: "9999999.5",
	}
	for in, want := range tests {
		if got := JavaDoubleString(in); got != want {
			t.Errorf("JavaDoubleString(%v) = %q, want %q", in, got, want)
		}
	}
}

func TestNumberString(t *testing.T) {
	tests := map[string]string{
		"100":                    "100",
		"0":                      "0",
		"-42":                    "-42",
		"100.5":                  "100.5",
		"1.0":                    "1.0",
		"3.0":                    "3.0",
		"1e2":                    "100.0",
		"201701":                 "201701",
		"1000000000000000000000": "1000000000000000000000", // BigInteger 相当
	}
	for raw, want := range tests {
		if got := (Number{Raw: raw}).String(); got != want {
			t.Errorf("Number(%q).String() = %q, want %q", raw, got, want)
		}
	}
}

func TestToJavaString(t *testing.T) {
	m := NewOrderedMap()
	m.Put("key1", Number{Raw: "100"})
	m.Put("key2", "AAA")

	tests := []struct {
		in   any
		want string
	}{
		{nil, "null"},
		{"text", "text"},
		{true, "true"},
		{Number{Raw: "201701"}, "201701"},
		{[]any{Number{Raw: "201701"}, Number{Raw: "201702"}}, "[201701, 201702]"},
		{[]any{"a", nil, "c"}, "[a, null, c]"},
		{m, "{key1=100, key2=AAA}"},
		{[]any{m}, "[{key1=100, key2=AAA}]"},
	}
	for _, tt := range tests {
		if got := ToJavaString(tt.in); got != tt.want {
			t.Errorf("ToJavaString(%v) = %q, want %q", tt.in, got, tt.want)
		}
	}
}

func TestParseJSONObjectOrder(t *testing.T) {
	m, err := ParseJSONObject(`{"b": 1, "a": {"z": true, "y": [1, 2.5, "x"]}, "c": null}`)
	if err != nil {
		t.Fatal(err)
	}
	keys := m.Keys()
	if len(keys) != 3 || keys[0] != "b" || keys[1] != "a" || keys[2] != "c" {
		t.Errorf("キー順が保持されていない: %v", keys)
	}
	inner, ok := m.GetOrNil("a").(*OrderedMap)
	if !ok {
		t.Fatal("ネストが OrderedMap でない")
	}
	if got := ToJavaString(inner); got != "{z=true, y=[1, 2.5, x]}" {
		t.Errorf("ネスト表現: %q", got)
	}
	if v := m.GetOrNil("c"); v != nil {
		t.Errorf("null: %v", v)
	}
}

func TestParseJSONObjectNull(t *testing.T) {
	m, err := ParseJSONObject("null")
	if err != nil || m != nil {
		t.Errorf("JSON null は (nil, nil) のはず: %v, %v", m, err)
	}
	if _, err := ParseJSONObject("{invalid"); err == nil {
		t.Error("不正 JSON でエラーにならない")
	}
}
