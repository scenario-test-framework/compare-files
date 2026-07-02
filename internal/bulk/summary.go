package bulk

import (
	"crypto/rand"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"
	"time"

	"golang.org/x/text/encoding/unicode"

	"github.com/scenario-test-framework/compare-files/internal/charset"
	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/csvio"
	"github.com/scenario-test-framework/compare-files/internal/filecompare"
)

// summaryColumns は CompareSummary.csv の固定カラムです。
var summaryColumns = []string{
	"Status", "Left", "Right", "Layout",
	"Row", "OK Row", "NG Row", "Ignore Row", "LeftOnly Row", "RightOnly Row",
	"StartTime", "EndTime", "Length",
}

// SummaryRepository は一括比較結果 (CompareSummary.csv) のリポジトリです。
// Java 版 FileCompareResultRepositoryImpl + FileCompareResultWriter 相当。
// tx ファイル方式で、書き込みがない場合は空のサマリファイルが残ります。
type SummaryRepository struct {
	filePath      string
	txFilePath    string
	file          *os.File
	w             *csvio.Writer
	headerWritten bool
}

// NewSummaryRepository はリポジトリを作成してトランザクションを開始します。
func NewSummaryRepository(filePath, charsetName string) (*SummaryRepository, error) {
	enc, err := charset.Lookup(charsetName)
	if err != nil {
		return nil, err
	}
	if err := os.MkdirAll(filepath.Dir(filePath), 0o755); err != nil {
		return nil, fmt.Errorf("ディレクトリを作成できません。対象:%s: %w", filepath.Dir(filePath), err)
	}
	if _, statErr := os.Stat(filePath); os.IsNotExist(statErr) {
		if err := os.WriteFile(filePath, nil, 0o644); err != nil {
			return nil, fmt.Errorf("ファイルを書出しできません。対象:%s: %w", filePath, err)
		}
	}

	txFilePath := filePath + "." + randomSuffix(10)
	f, err := os.Create(txFilePath)
	if err != nil {
		return nil, fmt.Errorf("出力ストリームをオープンできません。対象:%s: %w", txFilePath, err)
	}
	var w io.Writer = f
	if enc != unicode.UTF8 {
		w = enc.NewEncoder().Writer(f)
	}
	return &SummaryRepository{
		filePath:   filePath,
		txFilePath: txFilePath,
		file:       f,
		w:          csvio.NewWriter(w, csvio.CsvConfig()),
	}, nil
}

// Write はファイル比較結果を 1 行書き出します。
func (r *SummaryRepository) Write(result *filecompare.Result) error {
	if !r.headerWritten {
		if err := r.w.WriteStringRecord(summaryColumns); err != nil {
			return err
		}
		r.headerWritten = true
	}
	record := []string{
		string(result.Status),
		result.LeftFilePath,
		result.RightFilePath,
		result.LayoutName(),
		FormatCount(result.RowCount),
		FormatCount(result.OkRowCount),
		FormatCount(result.NgRowCount),
		FormatCount(result.IgnoreRowCount),
		FormatCount(result.LeftOnlyRowCount),
		FormatCount(result.RightOnlyRowCount),
		FormatTimestamp(result.StartTime),
		FormatTimestamp(result.EndTime),
		FormatLength(result.EndTime.Sub(result.StartTime)),
	}
	return r.w.WriteStringRecord(record)
}

// Commit は書き込みを確定します (書き込みなしなら空のサマリファイルが残ります)。
func (r *SummaryRepository) Commit() error {
	if err := r.w.Flush(); err != nil {
		r.file.Close()
		return err
	}
	if err := r.file.Close(); err != nil {
		return err
	}
	info, err := os.Stat(r.txFilePath)
	if err != nil {
		return err
	}
	if info.Size() > 0 {
		if err := os.Remove(r.filePath); err != nil {
			return fmt.Errorf("ファイルを削除できません。対象:%s: %w", r.filePath, err)
		}
		return os.Rename(r.txFilePath, r.filePath)
	}
	return os.Remove(r.txFilePath)
}

// Rollback は一時ファイルを破棄します。
func (r *SummaryRepository) Rollback() {
	r.file.Close()
	os.Remove(r.txFilePath)
}

// FormatCount は件数を Java の DecimalFormat("#,##0") 形式 (3 桁カンマ区切り) にします。
func FormatCount(n int64) string {
	s := fmt.Sprintf("%d", n)
	neg := strings.HasPrefix(s, "-")
	if neg {
		s = s[1:]
	}
	var b strings.Builder
	for i, digit := range s {
		if i > 0 && (len(s)-i)%3 == 0 {
			b.WriteByte(',')
		}
		b.WriteRune(digit)
	}
	if neg {
		return "-" + b.String()
	}
	return b.String()
}

// FormatTimestamp は Java の yyyy-MM-dd HH:mm:ss.SSS 形式にします。
func FormatTimestamp(t time.Time) string {
	return t.Format(config.FormatTimestamp)
}

// FormatLength は処理時間を %02d:%02d:%02d.%03d 形式にします。
// Java 版 FileCompareResultWriter.convLength 相当。
func FormatLength(d time.Duration) string {
	millis := d.Milliseconds()
	hours := millis / (60 * 60 * 1000)
	minutes := millis % (60 * 60 * 1000) / (60 * 1000)
	seconds := millis % (60 * 1000) / 1000
	ms := millis % 1000
	return fmt.Sprintf("%02d:%02d:%02d.%03d", hours, minutes, seconds, ms)
}

// randomSuffix は英数字のランダム文字列を返します。
func randomSuffix(n int) string {
	const chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	b := make([]byte, n)
	if _, err := rand.Read(b); err != nil {
		panic(err)
	}
	for i := range b {
		b[i] = chars[int(b[i])%len(chars)]
	}
	return string(b)
}
