// Package sortfile はテキストファイルの外部ソートを提供します。
// Java 版 sv/domain/sort/FileSortResult 相当です。
// Java 版の自然マージソート(複数パス)を、チャンクソート + k-way マージ(実質 2 パス)に
// 置き換えています。ソート順 (キー → 値の deepCompare 全順序、同値は入力順) は同一のため
// 出力ファイルの内容は変わりません。
package sortfile

import (
	"container/heap"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"sort"

	"golang.org/x/text/encoding"
	"golang.org/x/text/encoding/unicode"

	"github.com/scenario-test-framework/compare-files/internal/charset"
	"github.com/scenario-test-framework/compare-files/internal/compare"
	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/reader"
	"github.com/scenario-test-framework/compare-files/internal/row"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// メモリ上でソートする行数。Java 版はチャンク内ソート+自然マージだったが、
// Go 版は大きめのチャンクで一括ソートしてマージ回数を減らす。
const memChunkRows = 50000

// 1 回のマージで開くファイル数の上限。
const mergeFanIn = 64

// Sort は入力ファイルをソートし、outputDir/<入力ファイル名> に出力します。
// 空ファイルの場合は空の出力ファイルを作成し Warning を返します。
func Sort(inputFilePath, charsetName, outputDir string, layout *config.FileLayout, cfg *config.CompareFilesConfig) (status.ProcessStatus, error) {
	if err := os.MkdirAll(outputDir, 0o755); err != nil {
		return status.ProcessFailure, fmt.Errorf("ディレクトリを作成できません。対象:%s: %w", outputDir, err)
	}

	inputFileName := filepath.Base(inputFilePath)
	outputFilePath := filepath.Join(outputDir, inputFileName)

	// 0 バイトチェック
	info, err := os.Stat(inputFilePath)
	if err != nil {
		return status.ProcessFailure, fmt.Errorf("ファイルを読込みできません。対象:%s: %w", inputFilePath, err)
	}
	if info.Size() == 0 {
		if err := os.WriteFile(outputFilePath, nil, 0o644); err != nil {
			return status.ProcessFailure, err
		}
		return status.ProcessWarning, nil
	}

	// 作業ディレクトリ (マルチプロセス/ゴルーチン衝突回避のため一意な名前)
	workDirPath, err := os.MkdirTemp(outputDir, "work_")
	if err != nil {
		return status.ProcessFailure, fmt.Errorf("ディレクトリを作成できません。対象:%s: %w", outputDir, err)
	}
	defer os.RemoveAll(workDirPath)

	enc, err := charset.Lookup(charsetName)
	if err != nil {
		return status.ProcessFailure, err
	}

	// ヘッダー行の退避 (CSV_withHeader / TSV_withHeader のみ)
	hasHeader := layout.FileFormat == status.FormatCSVWithHeader || layout.FileFormat == status.FormatTSVWithHeader
	var headerLines []string
	if hasHeader {
		headerLines, err = readHeaderLines(inputFilePath, enc, int(cfg.CsvDataStartRow))
		if err != nil {
			return status.ProcessFailure, err
		}
	}

	// 行の読み込みオプション: ヘッダーありは設定値、なしは全行対象
	opts := reader.Options{
		CodeValueForOnlyOneRecordType: cfg.CodeValueForOnlyOneRecordType,
		OverrideCharset:               charsetName,
	}
	if hasHeader {
		opts.CsvHeaderRow = int(cfg.CsvHeaderRow)
		opts.CsvDataStartRow = int(cfg.CsvDataStartRow)
	} else {
		opts.CsvHeaderRow = 0
		opts.CsvDataStartRow = 1
	}

	lineSp := sortLineSp(layout)

	// チャンクソート
	chunkPaths, err := writeSortedChunks(inputFilePath, layout, opts, workDirPath, enc, lineSp)
	if err != nil {
		return status.ProcessFailure, err
	}

	// マージ (fan-in 上限を超える場合は多段マージ)
	mergedPath, err := mergeAll(chunkPaths, layout, opts, workDirPath, enc, lineSp)
	if err != nil {
		return status.ProcessFailure, err
	}

	// 結果ファイル出力: ヘッダー + ソート済みデータ
	if err := writeResultFile(outputFilePath, headerLines, mergedPath, enc, lineSp); err != nil {
		return status.ProcessFailure, err
	}
	return status.ProcessSuccess, nil
}

// sortLineSp はソート一時ファイルの改行コードを返します。
// Fixed はレイアウトの改行コード、それ以外は LF です。
func sortLineSp(layout *config.FileLayout) string {
	if layout.FileFormat == status.FormatFixed {
		return layout.LineSp.Value()
	}
	return "\n"
}

// readHeaderLines はデータ開始行より前の行を読み込みます。
func readHeaderLines(inputFilePath string, enc encoding.Encoding, dataStartRow int) ([]string, error) {
	f, err := os.Open(inputFilePath)
	if err != nil {
		return nil, fmt.Errorf("入力ストリームをオープンできません。対象:%s: %w", inputFilePath, err)
	}
	defer f.Close()

	var r io.Reader = f
	if enc != unicode.UTF8 {
		r = enc.NewDecoder().Reader(f)
	}
	lr := newLineReader(r)

	var lines []string
	for lineNum := 1; lineNum < dataStartRow; lineNum++ {
		line, ok, err := lr.readLine()
		if err != nil {
			return nil, err
		}
		if !ok {
			break
		}
		lines = append(lines, line)
	}
	return lines, nil
}

// writeSortedChunks は入力をチャンク単位でソートして一時ファイルに書き出します。
func writeSortedChunks(inputFilePath string, layout *config.FileLayout, opts reader.Options, workDirPath string, enc encoding.Encoding, lineSp string) ([]string, error) {
	r, err := reader.New(inputFilePath, layout, opts)
	if err != nil {
		return nil, err
	}
	defer r.Close()

	var chunkPaths []string
	var rows []*row.Row

	flush := func() error {
		if len(rows) == 0 {
			return nil
		}
		if err := sortRows(rows); err != nil {
			return err
		}
		chunkPath := filepath.Join(workDirPath, fmt.Sprintf("chunk_%d", len(chunkPaths)))
		if err := writeRawLines(chunkPath, rows, enc, lineSp); err != nil {
			return err
		}
		chunkPaths = append(chunkPaths, chunkPath)
		rows = rows[:0]
		return nil
	}

	for {
		rw, err := r.Next()
		if err != nil {
			return nil, err
		}
		if rw == nil {
			break
		}
		rows = append(rows, rw)
		if len(rows) >= memChunkRows {
			if err := flush(); err != nil {
				return nil, err
			}
		}
	}
	if err := flush(); err != nil {
		return nil, err
	}
	return chunkPaths, nil
}

// sortRows は行スライスを安定ソートします (キー → 値、同値は入力順)。
func sortRows(rows []*row.Row) error {
	var sortErr error
	sort.SliceStable(rows, func(i, j int) bool {
		if sortErr != nil {
			return false
		}
		result, err := compare.CompareRowsForSort(rows[i], rows[j])
		if err != nil {
			sortErr = err
			return false
		}
		return result < 0
	})
	return sortErr
}

// writeRawLines は行の rawLine を改行コード付きで書き出します。
func writeRawLines(path string, rows []*row.Row, enc encoding.Encoding, lineSp string) error {
	f, err := os.Create(path)
	if err != nil {
		return fmt.Errorf("出力ストリームをオープンできません。対象:%s: %w", path, err)
	}
	var w io.Writer = f
	if enc != unicode.UTF8 {
		w = enc.NewEncoder().Writer(f)
	}
	bw := newBufWriter(w)
	for _, rw := range rows {
		if _, err := bw.WriteString(rw.RawLine); err != nil {
			f.Close()
			return err
		}
		if _, err := bw.WriteString(lineSp); err != nil {
			f.Close()
			return err
		}
	}
	if err := bw.Flush(); err != nil {
		f.Close()
		return err
	}
	return f.Close()
}

// mergeAll はチャンクファイル群を k-way マージして 1 ファイルにします。
func mergeAll(chunkPaths []string, layout *config.FileLayout, opts reader.Options, workDirPath string, enc encoding.Encoding, lineSp string) (string, error) {
	if len(chunkPaths) == 0 {
		// 入力行 0 件 (ヘッダーのみ等): 空のマージ結果
		emptyPath := filepath.Join(workDirPath, "merged_empty")
		if err := os.WriteFile(emptyPath, nil, 0o644); err != nil {
			return "", err
		}
		return emptyPath, nil
	}
	if len(chunkPaths) == 1 {
		return chunkPaths[0], nil
	}

	// マージ用の読み込みは「全行対象」のオプションで行う
	mergeOpts := opts
	mergeOpts.CsvHeaderRow = 0
	mergeOpts.CsvDataStartRow = 1

	level := 0
	for len(chunkPaths) > 1 {
		var nextPaths []string
		for i := 0; i < len(chunkPaths); i += mergeFanIn {
			end := i + mergeFanIn
			if end > len(chunkPaths) {
				end = len(chunkPaths)
			}
			outPath := filepath.Join(workDirPath, fmt.Sprintf("merged_%d_%d", level, len(nextPaths)))
			if err := mergeChunks(chunkPaths[i:end], outPath, layout, mergeOpts, enc, lineSp); err != nil {
				return "", err
			}
			nextPaths = append(nextPaths, outPath)
		}
		chunkPaths = nextPaths
		level++
	}
	return chunkPaths[0], nil
}

// mergeEntry はマージヒープの要素です。
type mergeEntry struct {
	row      *row.Row
	chunkIdx int
}

type mergeHeap struct {
	entries []mergeEntry
	err     error
}

func (h *mergeHeap) Len() int { return len(h.entries) }
func (h *mergeHeap) Less(i, j int) bool {
	if h.err != nil {
		return false
	}
	result, err := compare.CompareRowsForSort(h.entries[i].row, h.entries[j].row)
	if err != nil {
		h.err = err
		return false
	}
	if result != 0 {
		return result < 0
	}
	// 同値はチャンク順 (= 入力順) で安定化
	return h.entries[i].chunkIdx < h.entries[j].chunkIdx
}
func (h *mergeHeap) Swap(i, j int) { h.entries[i], h.entries[j] = h.entries[j], h.entries[i] }
func (h *mergeHeap) Push(x any)    { h.entries = append(h.entries, x.(mergeEntry)) }
func (h *mergeHeap) Pop() any {
	old := h.entries
	n := len(old)
	x := old[n-1]
	h.entries = old[:n-1]
	return x
}

// mergeChunks は複数のソート済みチャンクを 1 ファイルにマージします。
func mergeChunks(chunkPaths []string, outPath string, layout *config.FileLayout, opts reader.Options, enc encoding.Encoding, lineSp string) error {
	readers := make([]reader.RowReader, len(chunkPaths))
	defer func() {
		for _, r := range readers {
			if r != nil {
				r.Close()
			}
		}
	}()
	for i, path := range chunkPaths {
		r, err := reader.New(path, layout, opts)
		if err != nil {
			return err
		}
		readers[i] = r
	}

	f, err := os.Create(outPath)
	if err != nil {
		return fmt.Errorf("出力ストリームをオープンできません。対象:%s: %w", outPath, err)
	}
	defer f.Close()
	var w io.Writer = f
	if enc != unicode.UTF8 {
		w = enc.NewEncoder().Writer(f)
	}
	bw := newBufWriter(w)

	h := &mergeHeap{}
	heap.Init(h)
	for i, r := range readers {
		rw, err := r.Next()
		if err != nil {
			return err
		}
		if rw != nil {
			heap.Push(h, mergeEntry{row: rw, chunkIdx: i})
			if h.err != nil {
				return h.err
			}
		}
	}

	for h.Len() > 0 {
		entry := heap.Pop(h).(mergeEntry)
		if h.err != nil {
			return h.err
		}
		if _, err := bw.WriteString(entry.row.RawLine); err != nil {
			return err
		}
		if _, err := bw.WriteString(lineSp); err != nil {
			return err
		}
		next, err := readers[entry.chunkIdx].Next()
		if err != nil {
			return err
		}
		if next != nil {
			heap.Push(h, mergeEntry{row: next, chunkIdx: entry.chunkIdx})
			if h.err != nil {
				return h.err
			}
		}
	}
	return bw.Flush()
}

// writeResultFile はヘッダー行とマージ済みデータを連結して出力します。
func writeResultFile(outputFilePath string, headerLines []string, mergedPath string, enc encoding.Encoding, lineSp string) error {
	out, err := os.Create(outputFilePath)
	if err != nil {
		return fmt.Errorf("出力ストリームをオープンできません。対象:%s: %w", outputFilePath, err)
	}
	var w io.Writer = out
	if enc != unicode.UTF8 {
		w = enc.NewEncoder().Writer(out)
	}
	bw := newBufWriter(w)

	for _, line := range headerLines {
		if _, err := bw.WriteString(line); err != nil {
			out.Close()
			return err
		}
		if _, err := bw.WriteString(lineSp); err != nil {
			out.Close()
			return err
		}
	}

	// マージ済みデータはバイト列のままコピー (書き出し時に改行付与済み)
	merged, err := os.Open(mergedPath)
	if err != nil {
		out.Close()
		return err
	}
	// merged は既に出力 charset で書かれているため、エンコードを介さず直接コピー
	if err := bw.Flush(); err != nil {
		merged.Close()
		out.Close()
		return err
	}
	if _, err := io.Copy(out, merged); err != nil {
		merged.Close()
		out.Close()
		return err
	}
	merged.Close()
	return out.Close()
}
