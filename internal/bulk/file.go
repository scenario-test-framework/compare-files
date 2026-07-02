package bulk

import (
	"github.com/scenario-test-framework/compare-files/internal/config"
	"github.com/scenario-test-framework/compare-files/internal/filecompare"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// CompareFile は単一ファイル比較を実行し、終了ステータスを返します。
// Java 版 FileCompareFacade + FileCompareFacadeOutput.getProcessStatus 相当。
func CompareFile(leftFilePath, rightFilePath, outputDirPath string, systemConfig *config.CompareFilesConfig, layoutManager *config.LayoutManager) (*filecompare.Result, status.ProcessStatus, error) {
	result, err := CompareOneFile(leftFilePath, rightFilePath, outputDirPath, systemConfig, layoutManager)
	if err != nil {
		return nil, status.ProcessFailure, err
	}
	return result, fileProcessStatus(result.Status), nil
}

// fileProcessStatus はファイル比較結果から終了ステータスを判定します。
func fileProcessStatus(st status.CompareStatus) status.ProcessStatus {
	switch st {
	case status.CompareOK, status.CompareIgnore:
		return status.ProcessSuccess
	case status.CompareNG, status.CompareLeftOnly, status.CompareRightOnly:
		return status.ProcessWarning
	default:
		return status.ProcessFailure
	}
}
