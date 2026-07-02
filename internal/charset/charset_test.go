package charset

import (
	"testing"

	"golang.org/x/text/encoding/japanese"
	"golang.org/x/text/encoding/unicode"
)

func TestLookup(t *testing.T) {
	tests := []struct {
		name string
		want any
	}{
		{"utf8", unicode.UTF8},
		{"UTF-8", unicode.UTF8},
		{"ms932", japanese.ShiftJIS},
		{"MS932", japanese.ShiftJIS},
		{"Windows-31J", japanese.ShiftJIS},
		{"sjis", japanese.ShiftJIS},
		{"Shift_JIS", japanese.ShiftJIS},
		{"euc-jp", japanese.EUCJP},
		{"EUC-JP", japanese.EUCJP},
	}
	for _, tt := range tests {
		enc, err := Lookup(tt.name)
		if err != nil {
			t.Errorf("Lookup(%q): %v", tt.name, err)
			continue
		}
		if enc != tt.want {
			t.Errorf("Lookup(%q) = %v, want %v", tt.name, enc, tt.want)
		}
	}
}

func TestLookupInvalid(t *testing.T) {
	if _, err := Lookup("not-a-charset"); err == nil {
		t.Error("不正な charset 名でエラーにならない")
	}
	if _, err := Lookup(""); err == nil {
		t.Error("空文字でエラーにならない")
	}
}

func TestIsValid(t *testing.T) {
	if !IsValid("ms932") {
		t.Error("ms932 は有効なはず")
	}
	if IsValid("bogus-charset-name") {
		t.Error("不正名が有効判定された")
	}
}
