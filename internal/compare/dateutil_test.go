package compare

import (
	"testing"
	"time"
)

func mustDate(t *testing.T, s string) time.Time {
	t.Helper()
	parsed, err := ToDate(s)
	if err != nil {
		t.Fatalf("ToDate(%q): %v", s, err)
	}
	return parsed
}

func TestToDateGenericFormats(t *testing.T) {
	tests := []struct {
		in   string
		want time.Time
	}{
		{"2017/01/01", time.Date(2017, 1, 1, 0, 0, 0, 0, time.Local)},
		{"2017-01-01", time.Date(2017, 1, 1, 0, 0, 0, 0, time.Local)},
		{"20170101", time.Date(2017, 1, 1, 0, 0, 0, 0, time.Local)},
		{"17/01/01", time.Date(2017, 1, 1, 0, 0, 0, 0, time.Local)},
		{"2017/01/01 12:34:56", time.Date(2017, 1, 1, 12, 34, 56, 0, time.Local)},
		{"2017-01-01 12:34", time.Date(2017, 1, 1, 12, 34, 0, 0, time.Local)},
		{"2017/01/01 00:00:00.123", time.Date(2017, 1, 1, 0, 0, 0, 123e6, time.Local)},
		// ISO-8601 (ゾーンなしはトークナイザ経由でローカル時刻)
		{"2017-01-01T12:34:56", time.Date(2017, 1, 1, 12, 34, 56, 0, time.Local)},
	}
	for _, tt := range tests {
		got := mustDate(t, tt.in)
		if !got.Equal(tt.want) {
			t.Errorf("ToDate(%q) = %v, want %v", tt.in, got, tt.want)
		}
	}
}

func TestToDateISO8601WithZone(t *testing.T) {
	got := mustDate(t, "2017-01-01T00:00:00.000+09:00")
	want := time.Date(2017, 1, 1, 0, 0, 0, 0, time.FixedZone("", 9*3600))
	if !got.Equal(want) {
		t.Errorf("ISO8601: %v, want %v", got, want)
	}
	// コロンなしゾーン・Z
	if !mustDate(t, "2017-01-01T00:00:00.000+0900").Equal(want) {
		t.Error("+0900 形式")
	}
	gotZ := mustDate(t, "2017-01-01T00:00:00.000Z")
	if !gotZ.Equal(time.Date(2017, 1, 1, 0, 0, 0, 0, time.UTC)) {
		t.Error("Z 形式")
	}
	// カンマ区切りミリ秒
	if !mustDate(t, "2017-01-01T00:00:00,000+09:00").Equal(want) {
		t.Error(", 区切りミリ秒")
	}
	// ミリ秒なし
	if !mustDate(t, "2017-01-01T00:00:00+09:00").Equal(want) {
		t.Error("ミリ秒なし")
	}
}

func TestToDateAccessLog(t *testing.T) {
	got := mustDate(t, "[01/Jan/2017:00:00:00 +0900]")
	want := time.Date(2017, 1, 1, 0, 0, 0, 0, time.FixedZone("", 9*3600))
	if !got.Equal(want) {
		t.Errorf("accesslog: %v, want %v", got, want)
	}
}

func TestToDateInvalid(t *testing.T) {
	invalids := []string{
		"",
		"abc",
		"2017",       // 8 文字未満
		"17-1-1",     // 8 文字未満 (Java 版と同じく最小 8 文字)
		"2017.01.01", // '/' '-' なしはコンパクト形式扱いで桁不足エラー (Java 版と同じ)
		"2017/13/01", // 存在しない月
		"2017/02/30", // 存在しない日
	}
	for _, in := range invalids {
		if _, err := ToDate(in); err == nil {
			t.Errorf("ToDate(%q) がエラーにならない", in)
		}
	}
}

func TestClearTime(t *testing.T) {
	base := time.Date(2017, 1, 1, 12, 34, 56, 789e6, time.Local)
	got := ClearTime(base)
	want := time.Date(2017, 1, 1, 0, 0, 0, 0, time.Local)
	if !got.Equal(want) {
		t.Errorf("ClearTime: %v", got)
	}
}

func TestNormalizeDateString(t *testing.T) {
	tests := map[string]string{
		"2017/1/1":            "2017/01/01 00:00:00.000",
		"17/01/01":            "2017/01/01 00:00:00.000",
		"20170101":            "2017/01/01",
		"2017/01/01 1:2:3.5":  "2017/01/01 01:02:03.500",
		"2017-01-01T12:34:56": "2017/01/01 12:34:56.000",
	}
	for in, want := range tests {
		got, err := normalizeDateString(in)
		if err != nil {
			t.Errorf("normalizeDateString(%q): %v", in, err)
			continue
		}
		if got != want {
			t.Errorf("normalizeDateString(%q) = %q, want %q", in, got, want)
		}
	}
}
