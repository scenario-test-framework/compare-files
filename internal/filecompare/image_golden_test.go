package filecompare

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/scenario-test-framework/compare-files/internal/status"
)

// runImageCompare は左右画像を比較して結果を返します。
func runImageCompare(t *testing.T, subDir, fileName string) (*Result, string) {
	t.Helper()
	cfg := defaultTestConfig(t)
	lm := testLayoutManager(t)

	leftPath := filepath.Join(goldenBase, "input/left", subDir, fileName)
	rightPath := filepath.Join(goldenBase, "input/right", subDir, fileName)
	layout := lm.GetLayout(fileName, cfg)

	outputDir := t.TempDir()
	result, err := CompareImage(leftPath, rightPath, layout, outputDir, cfg)
	if err != nil {
		t.Fatalf("CompareImage(%s): %v", fileName, err)
	}
	outputName := OutputFileName(leftPath, rightPath, cfg, "png")
	return result, filepath.Join(outputDir, outputName)
}

// Java 版 golden サマリ (expect/dir/CompareSummary.csv) の期待値との一致を確認。
func assertImageGolden(t *testing.T, subDir, fileName string, wantStatus status.CompareStatus, wantNg, wantIgnore int64) {
	t.Helper()
	result, outputPath := runImageCompare(t, subDir, fileName)
	if result.Status != wantStatus {
		t.Errorf("%s: status = %v, want %v", fileName, result.Status, wantStatus)
	}
	if result.NgRowCount != wantNg {
		t.Errorf("%s: NG Row = %d, want %d", fileName, result.NgRowCount, wantNg)
	}
	if result.IgnoreRowCount != wantIgnore {
		t.Errorf("%s: Ignore Row = %d, want %d", fileName, result.IgnoreRowCount, wantIgnore)
	}
	// 結果画像ファイルが生成されている (中身は見た目同等のため非空のみ確認)
	info, err := os.Stat(outputPath)
	if err != nil {
		t.Errorf("%s: 結果画像がない: %v", fileName, err)
	} else if info.Size() == 0 {
		t.Errorf("%s: 結果画像が空", fileName)
	}
}

func TestImageGoldenPng(t *testing.T) {
	// レイアウト「PNGサンプル」の除外エリア + システム設定の除外エリア = 2
	assertImageGolden(t, "IMAGE_PNG", "png_ok.png", status.CompareOK, 0, 2)
	assertImageGolden(t, "IMAGE_PNG", "png_ng.png", status.CompareNG, 1, 2)
}

func TestImageGoldenBmp(t *testing.T) {
	// レイアウトなし → デフォルト画像レイアウト + システム設定の除外エリア = 1
	assertImageGolden(t, "IMAGE_BMP", "bmp_ok.bmp", status.CompareOK, 0, 1)
	assertImageGolden(t, "IMAGE_BMP", "bmp_ng.bmp", status.CompareNG, 2, 1)
}

func TestImageGoldenGif(t *testing.T) {
	assertImageGolden(t, "IMAGE_GIF", "gif_ok.gif", status.CompareOK, 0, 1)
	assertImageGolden(t, "IMAGE_GIF", "gif_ng.gif", status.CompareNG, 2, 1)
}

func TestImageGoldenJpg(t *testing.T) {
	// JPEG はデコーダ差により NG エリア数が Java 版とずれる可能性がある
	assertImageGolden(t, "IMAGE_JPG", "jpg_ok.jpg", status.CompareOK, 0, 1)
	assertImageGolden(t, "IMAGE_JPG", "jpg_ng.jpg", status.CompareNG, 2, 1)
}
