// Package charset は Java の Charset.forName 相当の文字コード名解決を提供します。
// Java 版では JVM が解決できる任意の charset 名を受け付けていたため、
// 主要なエイリアスを明示テーブルで解決し、未知の名称は IANA インデックスにフォールバックします。
package charset

import (
	"fmt"
	"strings"

	"golang.org/x/text/encoding"
	"golang.org/x/text/encoding/ianaindex"
	"golang.org/x/text/encoding/japanese"
	"golang.org/x/text/encoding/unicode"
)

// aliases は小文字化した charset 名 → encoding のテーブルです。
// Java の Charset.forName が解決する代表的な名称をカバーします。
var aliases = map[string]encoding.Encoding{
	"utf8":          unicode.UTF8,
	"utf-8":         unicode.UTF8,
	"ms932":         japanese.ShiftJIS,
	"windows-31j":   japanese.ShiftJIS,
	"cp932":         japanese.ShiftJIS,
	"sjis":          japanese.ShiftJIS,
	"shift_jis":     japanese.ShiftJIS,
	"shift-jis":     japanese.ShiftJIS,
	"x-sjis":        japanese.ShiftJIS,
	"euc-jp":        japanese.EUCJP,
	"eucjp":         japanese.EUCJP,
	"x-euc-jp":      japanese.EUCJP,
	"iso-2022-jp":   japanese.ISO2022JP,
	"iso2022jp":     japanese.ISO2022JP,
	"us-ascii":      unicode.UTF8, // ASCII は UTF-8 の部分集合として扱う
	"ascii":         unicode.UTF8,
	"utf-16":        unicode.UTF16(unicode.BigEndian, unicode.UseBOM),
	"utf-16be":      unicode.UTF16(unicode.BigEndian, unicode.IgnoreBOM),
	"utf-16le":      unicode.UTF16(unicode.LittleEndian, unicode.IgnoreBOM),
	"unicodebig":    unicode.UTF16(unicode.BigEndian, unicode.UseBOM),
	"unicodelittle": unicode.UTF16(unicode.LittleEndian, unicode.UseBOM),
}

// Lookup は charset 名から encoding を解決します。解決できない場合は error を返します。
func Lookup(name string) (encoding.Encoding, error) {
	if name == "" {
		return nil, fmt.Errorf("charset が指定されていません")
	}
	key := strings.ToLower(strings.TrimSpace(name))
	if enc, ok := aliases[key]; ok {
		return enc, nil
	}
	enc, err := ianaindex.IANA.Encoding(name)
	if err != nil || enc == nil {
		return nil, fmt.Errorf("サポートされていない charset です: %q", name)
	}
	return enc, nil
}

// IsValid は charset 名が解決可能かを返します(バリデーション用)。
func IsValid(name string) bool {
	_, err := Lookup(name)
	return err == nil
}

// IsUTF8 は指定名が UTF-8 相当かを返します(変換スキップの最適化用)。
func IsUTF8(name string) bool {
	enc, err := Lookup(name)
	return err == nil && enc == unicode.UTF8
}
