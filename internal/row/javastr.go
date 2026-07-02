package row

import (
	"fmt"
	"math"
	"strconv"
	"strings"
)

// Number は JSON 数値です。Jackson がパースした Integer/Long/BigInteger/Double の
// toString 表現を再現するため、元のリテラルを保持します。
type Number struct {
	Raw string
}

// IsIntegral は整数リテラル(小数点・指数なし)かを返します。
func (n Number) IsIntegral() bool {
	return !strings.ContainsAny(n.Raw, ".eE")
}

// String は Java の Number.toString() 相当の文字列を返します。
// 整数は桁そのまま (Integer/Long/BigInteger.toString)、
// 小数は Double.toString のルールで整形します。
func (n Number) String() string {
	if n.IsIntegral() {
		return n.Raw
	}
	f, err := strconv.ParseFloat(n.Raw, 64)
	if err != nil {
		return n.Raw
	}
	return JavaDoubleString(f)
}

// Float64 は数値を float64 で返します。
func (n Number) Float64() (float64, error) {
	return strconv.ParseFloat(n.Raw, 64)
}

// JavaDoubleString は Java の Double.toString(d) と同じ表記を返します。
//   - 10^-3 <= |d| < 10^7 : 小数表記(小数部は最低 1 桁)
//   - それ以外            : 科学表記 "d.dddEexp"
//
// 仮数は値を一意に識別できる最短の桁数を使用します。
func JavaDoubleString(f float64) string {
	if math.IsNaN(f) {
		return "NaN"
	}
	neg := math.Signbit(f)
	if math.IsInf(f, 0) {
		if neg {
			return "-Infinity"
		}
		return "Infinity"
	}
	if f == 0 {
		if neg {
			return "-0.0"
		}
		return "0.0"
	}

	abs := math.Abs(f)
	// 最短で一意な桁を 'e' 形式から取得: "d.ddde±dd"
	shortest := strconv.FormatFloat(abs, 'e', -1, 64)
	mantissa, expStr, _ := strings.Cut(shortest, "e")
	digits := strings.Replace(mantissa, ".", "", 1)
	exp, _ := strconv.Atoi(expStr)

	var out string
	if abs >= 1e-3 && abs < 1e7 {
		pointPos := exp + 1
		switch {
		case pointPos <= 0:
			out = "0." + strings.Repeat("0", -pointPos) + digits
		case pointPos >= len(digits):
			out = digits + strings.Repeat("0", pointPos-len(digits)) + ".0"
		default:
			out = digits[:pointPos] + "." + digits[pointPos:]
		}
	} else {
		rest := digits[1:]
		if rest == "" {
			rest = "0"
		}
		out = digits[:1] + "." + rest + "E" + strconv.Itoa(exp)
	}
	if neg {
		return "-" + out
	}
	return out
}

// ToJavaString は値を Java の String.valueOf(obj) 相当で文字列化します。
//   - nil        → "null"
//   - []any      → "[a, b, c]"  (AbstractCollection.toString)
//   - OrderedMap → "{k1=v1, k2=v2}"  (AbstractMap.toString)
func ToJavaString(v any) string {
	switch val := v.(type) {
	case nil:
		return "null"
	case string:
		return val
	case Number:
		return val.String()
	case bool:
		return strconv.FormatBool(val)
	case []any:
		var b strings.Builder
		b.WriteByte('[')
		for i, elem := range val {
			if i > 0 {
				b.WriteString(", ")
			}
			b.WriteString(ToJavaString(elem))
		}
		b.WriteByte(']')
		return b.String()
	case *OrderedMap:
		var b strings.Builder
		b.WriteByte('{')
		for i, key := range val.Keys() {
			if i > 0 {
				b.WriteString(", ")
			}
			b.WriteString(key)
			b.WriteByte('=')
			b.WriteString(ToJavaString(val.GetOrNil(key)))
		}
		b.WriteByte('}')
		return b.String()
	default:
		// 想定外の型 (リーダは上記の型しか生成しない)
		return fmt.Sprint(v)
	}
}
