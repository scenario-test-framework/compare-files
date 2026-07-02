// Package compare は値・行の比較ロジックを提供します。
// Java 版 me.suwash.util.CompareUtils / DateUtils と
// sv/domain/compare/file/text 相当です。
package compare

import (
	"fmt"
	"strconv"
	"strings"
	"time"
)

// ToDate は日付・時刻文字列を time.Time に変換します。Java 版 DateUtils.toDate 相当。
// 対応フォーマット: ISO-8601 / apache accesslog / 汎用日時フォーマット。
func ToDate(strDate string) (time.Time, error) {
	if parsed, ok := parseCommonFormat(strDate); ok {
		return parsed, nil
	}
	return toCalendarMain(strDate)
}

// ClearTime は時刻部分をクリアします (ローカルタイムゾーンの日単位切り捨て)。
func ClearTime(t time.Time) time.Time {
	y, m, d := t.Date()
	return time.Date(y, m, d, 0, 0, 0, 0, t.Location())
}

// parseCommonFormat は ISO-8601 / apache accesslog 形式のパースを試みます。
func parseCommonFormat(strDate string) (time.Time, bool) {
	// ISO-8601: スペースなし、- あり、T あり、: あり
	if !strings.Contains(strDate, " ") && strings.Contains(strDate, "-") &&
		strings.Contains(strDate, "T") && strings.Contains(strDate, ":") {
		target := strDate
		if strings.HasSuffix(target, ":") {
			target = target[:len(target)-1]
		}
		// SimpleDateFormat の X はコロンあり/なし/時のみのゾーンを受理する
		layouts := []string{
			"2006-01-02T15:04:05.000Z07:00",
			"2006-01-02T15:04:05.000Z0700",
			"2006-01-02T15:04:05.000Z07",
			"2006-01-02T15:04:05,000Z07:00",
			"2006-01-02T15:04:05,000Z0700",
			"2006-01-02T15:04:05,000Z07",
			"2006-01-02T15:04:05Z07:00",
			"2006-01-02T15:04:05Z0700",
			"2006-01-02T15:04:05Z07",
		}
		for _, layout := range layouts {
			if parsed, err := time.Parse(layout, target); err == nil {
				return parsed, true
			}
		}
	}

	// apache accesslog: [ ] で括られ、スペース・/・: を含む
	if len(strDate) >= 2 && strDate[0] == '[' && strDate[len(strDate)-1] == ']' &&
		strings.Contains(strDate, " ") && strings.Contains(strDate, "/") && strings.Contains(strDate, ":") {
		target := strDate[1 : len(strDate)-1]
		layouts := []string{
			"02/Jan/2006:15:04:05 Z0700",
			"02/Jan/2006:15:04:05 Z07:00",
			"02/Jan/2006:15:04:05 Z07",
		}
		for _, layout := range layouts {
			if parsed, err := time.Parse(layout, target); err == nil {
				return parsed, true
			}
		}
	}

	return time.Time{}, false
}

// toCalendarMain は汎用フォーマットの正規化とカレンダー変換を行います。
// Java 版 toCalendarMain + format 相当。ローカルタイムゾーンで解釈します。
func toCalendarMain(strDate string) (time.Time, error) {
	formatted, err := normalizeDateString(strDate)
	if err != nil {
		return time.Time{}, err
	}

	year, err1 := strconv.Atoi(substr(formatted, 0, 4))
	month, err2 := strconv.Atoi(substr(formatted, 5, 7))
	day, err3 := strconv.Atoi(substr(formatted, 8, 10))
	if err1 != nil || err2 != nil || err3 != nil {
		return time.Time{}, fmt.Errorf("日時に変換できません: %q", strDate)
	}
	hour, minute, sec, milli := 0, 0, 0, 0

	switch len(formatted) {
	case 10:
		// 日付のみ
	case 23: // yyyy/MM/dd HH:mm:ss.SSS
		var errH, errM, errS, errMs error
		hour, errH = strconv.Atoi(substr(formatted, 11, 13))
		minute, errM = strconv.Atoi(substr(formatted, 14, 16))
		sec, errS = strconv.Atoi(substr(formatted, 17, 19))
		milli, errMs = strconv.Atoi(substr(formatted, 20, 23))
		if errH != nil || errM != nil || errS != nil || errMs != nil {
			return time.Time{}, fmt.Errorf("日時に変換できません: %q", strDate)
		}
	default:
		return time.Time{}, fmt.Errorf("サポートされていないフォーマットです: %q", formatted)
	}

	// Calendar.setLenient(false) 相当の妥当性チェック
	if month < 1 || month > 12 || day < 1 || hour > 23 || minute > 59 || sec > 59 {
		return time.Time{}, fmt.Errorf("日時として不正です: %q", strDate)
	}
	t := time.Date(year, time.Month(month), day, hour, minute, sec, milli*int(time.Millisecond), time.Local)
	if t.Day() != day || t.Month() != time.Month(month) || t.Year() != year {
		// 存在しない日付 (例: 2/30) は正規化されるため不一致になる
		return time.Time{}, fmt.Errorf("日時として不正です: %q", strDate)
	}
	return t, nil
}

// normalizeDateString は日時文字列をデフォルトフォーマットに正規化します。
// Java 版 DateUtils.format 相当。
//   - 日付のみ: yyyy/MM/dd (10 文字)
//   - 日付+時刻: yyyy/MM/dd HH:mm:ss.SSS (23 文字)
func normalizeDateString(paraTargetDate string) (string, error) {
	trimmed := strings.TrimSpace(paraTargetDate)
	if len([]rune(trimmed)) < 8 {
		return "", fmt.Errorf("サポートされていないフォーマットです: %q", paraTargetDate)
	}
	target := trimmed

	// "-" も "/" も無い場合
	if !strings.ContainsAny(target, "/-") {
		r := []rune(target)
		if len(r) == 8 {
			// yyyyMMdd 形式
			return string(r[0:4]) + "/" + string(r[4:6]) + "/" + string(r[6:8]), nil
		}
		// Java 版の substring 位置を踏襲: "yyyyMMdd HH:mm:ss" 形式を想定
		// (長さ不足時は Java の StringIndexOutOfBoundsException 相当のエラー)
		if len(r) < 17 {
			return "", fmt.Errorf("サポートされていないフォーマットです: %q", paraTargetDate)
		}
		return string(r[0:4]) + "/" + string(r[4:6]) + "/" + string(r[6:8]) +
			" " + string(r[9:11]) + ":" + string(r[12:14]) + ":" + string(r[15:17]) + ".000", nil
	}

	// 区切り文字あり: トークン分割して 年/月/日/時/分/秒/ミリ秒 に割当
	strYear, strMonth, strDate := "", "", ""
	strHours, strMinutes, strSeconds, strMillis := "00", "00", "00", "000"
	tokens := tokenize(target, "_/-:.,T ")
	for i, tok := range tokens {
		switch i {
		case 0:
			strYear = fillString(tok, fillLeft, 4, "20")
		case 1:
			strMonth = fillString(tok, fillLeft, 2, "0")
		case 2:
			strDate = fillString(tok, fillLeft, 2, "0")
		case 3:
			strHours = fillString(tok, fillLeft, 2, "0")
		case 4:
			strMinutes = fillString(tok, fillLeft, 2, "0")
		case 5:
			strSeconds = fillString(tok, fillLeft, 2, "0")
		case 6:
			strMillis = fillString(tok, fillRight, 3, "0")
		default:
			return "", fmt.Errorf("サポートされていないフォーマットです: %q", paraTargetDate)
		}
	}

	return strYear + "/" + strMonth + "/" + strDate + " " + strHours + ":" + strMinutes + ":" + strSeconds + "." + strMillis, nil
}

// tokenize は Java の StringTokenizer 相当 (連続する区切り文字は 1 つとして扱う)。
func tokenize(s, delims string) []string {
	return strings.FieldsFunc(s, func(r rune) bool {
		return strings.ContainsRune(delims, r)
	})
}

type fillPosition int

const (
	fillLeft fillPosition = iota
	fillRight
)

// fillString は Java 版 DateUtils.fillString 相当のパディングです。
func fillString(paraStr string, position fillPosition, length int, paraAddStr string) string {
	addStr := paraAddStr
	buffer := paraStr

	for length > len(buffer) {
		if position == fillLeft {
			sum := len(buffer) + len(addStr)
			if sum > length {
				addStr = addStr[:len(addStr)-(sum-length)]
			}
			buffer = addStr + buffer
		} else {
			buffer = buffer + addStr
		}
	}
	if len(buffer) == length {
		return buffer
	}
	return buffer[:length]
}

// substr は Java の String.substring 相当 (範囲外はそのまま切り詰めずエラー回避)。
func substr(s string, start, end int) string {
	r := []rune(s)
	if start > len(r) {
		return ""
	}
	if end > len(r) {
		end = len(r)
	}
	return string(r[start:end])
}
