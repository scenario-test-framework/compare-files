package writer

import (
	"crypto/rand"
	"fmt"
	"os"
	"path/filepath"

	"github.com/scenario-test-framework/compare-files/internal/compare"
)

// DetailRepository は詳細結果ファイルのトランザクション制御付きリポジトリです。
// Java 版 TextFileRowCompareResultRepositoryImpl + GenericFileRepository 相当。
//   - begin: 対象ファイルが無ければ空ファイルを作成し、一時ファイルへの書き込みを開始
//   - commit: 一時ファイルに書き込みがあれば対象ファイルを置き換え、無ければ一時ファイルを削除
type DetailRepository struct {
	filePath   string
	txFilePath string
	w          *DetailWriter
}

// NewDetailRepository はリポジトリを作成してトランザクションを開始します。
func NewDetailRepository(filePath, charsetName string, isWriteDiffOnly bool, prefixLeft, prefixRight string) (*DetailRepository, error) {
	txFilePath := filePath + "." + randomAlphanumeric(10)

	// 対象ファイルが存在しない場合、親ディレクトリと空ファイルを作成
	if err := os.MkdirAll(filepath.Dir(filePath), 0o755); err != nil {
		return nil, fmt.Errorf("ディレクトリを作成できません。対象:%s: %w", filepath.Dir(filePath), err)
	}
	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		if err := os.WriteFile(filePath, nil, 0o644); err != nil {
			return nil, fmt.Errorf("ファイルを書出しできません。対象:%s: %w", filePath, err)
		}
	}

	w, err := NewDetailWriter(txFilePath, charsetName, isWriteDiffOnly, prefixLeft, prefixRight)
	if err != nil {
		return nil, err
	}
	return &DetailRepository{filePath: filePath, txFilePath: txFilePath, w: w}, nil
}

// Write は行比較結果を書き出します。
func (r *DetailRepository) Write(result *compare.RowResult) error {
	return r.w.Write(result)
}

// Commit は書き込みを確定します。
func (r *DetailRepository) Commit() error {
	if err := r.w.Close(); err != nil {
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
		if err := os.Rename(r.txFilePath, r.filePath); err != nil {
			return fmt.Errorf("ファイルを書出しできません。対象:%s: %w", r.filePath, err)
		}
		return nil
	}
	return os.Remove(r.txFilePath)
}

// Rollback は一時ファイルを破棄します。
func (r *DetailRepository) Rollback() {
	r.w.Close()
	os.Remove(r.txFilePath)
}

// randomAlphanumeric は英数字のランダム文字列を返します。
func randomAlphanumeric(n int) string {
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
