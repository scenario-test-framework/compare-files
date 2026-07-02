package bulk

import (
	"log/slog"
	"os"
	"runtime"
	"strconv"
	"sync"
	"time"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/filecompare"
	"github.com/scenario-test-framework/compare-files/internal/msg"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// EnvParallel はファイル単位比較の並列数を指定する環境変数名です。
// 未設定時は CPU コア数、"1" で逐次実行になります。
const EnvParallel = "COMPAREFILES_PARALLEL"

// parallelism は並列数を返します。
func parallelism() int {
	if v := os.Getenv(EnvParallel); v != "" {
		if n, err := strconv.Atoi(v); err == nil && n >= 1 {
			return n
		}
		slog.Warn("COMPAREFILES_PARALLEL の値が不正です。CPU コア数を使用します。", "value", v)
	}
	return runtime.NumCPU()
}

// filePair は並列比較の 1 件分の入力です。
type filePair struct {
	leftFilePath  string
	rightFilePath string
}

// compareFilesConcurrently はファイルペア群をワーカープールで比較します。
// 結果は入力順に返すため、サマリの出力順は逐次実行と同一です。
// エラーになった比較は Error ステータスの結果に変換します (Java 版の逐次処理と同じ)。
func compareFilesConcurrently(pairs []filePair, outputDirPath string, systemConfig *config.CompareFilesConfig, layoutManager *config.LayoutManager, startTime time.Time) []*filecompare.Result {
	workers := parallelism()
	if workers > len(pairs) {
		workers = len(pairs)
	}
	if workers < 1 {
		workers = 1
	}

	results := make([]*filecompare.Result, len(pairs))
	opts := fileOptions{parallel: workers > 1}

	jobs := make(chan int)
	var wg sync.WaitGroup
	for w := 0; w < workers; w++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			for idx := range jobs {
				pair := pairs[idx]
				result, err := compareOneFile(pair.leftFilePath, pair.rightFilePath, outputDirPath, systemConfig, layoutManager, opts)
				if err != nil {
					slog.Error(msg.Get("errorHandle", "FileCompare", "left="+pair.leftFilePath+", right="+pair.rightFilePath), "error", err)
					result = fixedResult(status.CompareError, pair.leftFilePath, pair.rightFilePath, startTime)
				}
				results[idx] = result
			}
		}()
	}
	for idx := range pairs {
		jobs <- idx
	}
	close(jobs)
	wg.Wait()

	// 並列実行時は比較ごとの work ディレクトリ削除をスキップしているため、ここで削除
	if opts.parallel && bool(systemConfig.DeleteWorkDir) {
		os.RemoveAll(outputDirPath + "/" + config.DirNameWork)
	}
	return results
}
