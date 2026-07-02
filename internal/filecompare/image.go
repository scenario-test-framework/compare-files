package filecompare

import (
	"os"
	"path/filepath"
	"time"

	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/imagecmp"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

const imageOutputExt = "png"

// CompareImage は画像ファイルを比較します。Java 版 ImageFileCompareResult.compare 相当。
//   - 差分あり: NG。マーク付き結果画像を出力
//   - 差分なし: OK。writeDiffOnly でない場合のみ確認用画像を出力
//     (Java 版と同じく、出力しない場合も空の結果ファイルを作成します)
func CompareImage(leftFilePath, rightFilePath string, fileLayout *config.FileLayout, outputDirPath string, systemConfig *config.CompareFilesConfig) (*Result, error) {
	if fileLayout == nil {
		fileLayout = config.DefaultImageLayout(systemConfig)
	}
	result := &Result{
		Status:        status.CompareProcessing,
		LeftFilePath:  leftFilePath,
		RightFilePath: rightFilePath,
		FileLayout:    fileLayout,
		StartTime:     time.Now(),
	}

	outputFileName := OutputFileName(leftFilePath, rightFilePath, systemConfig, imageOutputExt)
	outputFilePath := filepath.Join(outputDirPath, outputFileName)

	leftImage, err := imagecmp.LoadImage(leftFilePath)
	if err != nil {
		return nil, err
	}
	rightImage, err := imagecmp.LoadImage(rightFilePath)
	if err != nil {
		return nil, err
	}

	ignoreAreaList := fileLayout.IgnoreAreaList
	diffAreas := imagecmp.Compare(leftImage, rightImage, ignoreAreaList)

	result.IgnoreRowCount = int64(len(ignoreAreaList))
	result.NgRowCount = int64(diffAreas.Size())

	// Java 版の ImageFileRepository.begin は対象ファイルを空作成する
	if err := os.MkdirAll(outputDirPath, 0o755); err != nil {
		return nil, err
	}
	if _, statErr := os.Stat(outputFilePath); os.IsNotExist(statErr) {
		if err := os.WriteFile(outputFilePath, nil, 0o644); err != nil {
			return nil, err
		}
	}

	if diffAreas.HasDiff() {
		result.Status = status.CompareNG
		ngImage := imagecmp.NgImage(leftImage, rightImage, diffAreas,
			systemConfig.LeftPrefix, systemConfig.RightPrefix, systemConfig.NgImageStyle)
		if err := imagecmp.SavePNG(ngImage, outputFilePath); err != nil {
			return nil, err
		}
	} else {
		result.Status = status.CompareOK
		if !systemConfig.WriteDiffOnly {
			okImage := imagecmp.OkImage(leftImage, rightImage, ignoreAreaList,
				systemConfig.LeftPrefix, systemConfig.RightPrefix, systemConfig.OkImageStyle)
			if err := imagecmp.SavePNG(okImage, outputFilePath); err != nil {
				return nil, err
			}
		}
	}

	result.EndTime = time.Now()
	return result, nil
}
