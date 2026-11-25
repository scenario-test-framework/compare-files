package cli

import (
	"os"
	"path/filepath"
	"testing"
)

func TestLoadConfigSpecifiedOverridesSearchedDefault(t *testing.T) {
	t.Run("LoadConfig_探索先の起動設定がtrueで-configがfalseと空の一覧を明示した場合_-config側の値が使われること", func(t *testing.T) {
		// Arrange
		shared := t.TempDir()
		sharedJSON := `{"outputDir": "shared-result", "outputCharset": "utf8", "compareResultFileName": "CompareSummary.csv",
  "compareDetailFilePrefix": "CompareDetail_", "leftPrefix": "L:", "rightPrefix": "R:",
  "sorted": "true", "writeDiffOnly": "true", "deleteWorkDir": "true",
  "ignoreFileRegexList": ["^\\..*"], "ignoreItemList": ["ID"]}`
		if err := os.WriteFile(filepath.Join(shared, "compare_files.json"), []byte(sharedJSON), 0o644); err != nil {
			t.Fatal(err)
		}
		local := filepath.Join(t.TempDir(), "compare_files.json")
		localJSON := `{"sorted": false, "writeDiffOnly": "false", "deleteWorkDir": false, "ignoreFileRegexList": [], "ignoreItemList": []}`
		if err := os.WriteFile(local, []byte(localJSON), 0o644); err != nil {
			t.Fatal(err)
		}
		t.Chdir(t.TempDir())
		t.Setenv(EnvClasspath, shared)

		// Act
		cfg, err := LoadConfig(&Option{ConfigFilePath: local})

		// Assert
		if err != nil {
			t.Fatal(err)
		}
		if cfg.Sorted || cfg.WriteDiffOnly || cfg.DeleteWorkDir {
			t.Errorf("明示した false が探索先の true に負けた: sorted=%v writeDiffOnly=%v deleteWorkDir=%v", cfg.Sorted, cfg.WriteDiffOnly, cfg.DeleteWorkDir)
		}
		if len(cfg.IgnoreFileRegexList) != 0 || len(cfg.IgnoreItemList) != 0 {
			t.Errorf("明示した [] が探索先の一覧に負けた: %v %v", cfg.IgnoreFileRegexList, cfg.IgnoreItemList)
		}
		if cfg.OutputDir != "shared-result" {
			t.Errorf("書いていない項目が探索先の起動設定から補完されていない: outputDir=%q", cfg.OutputDir)
		}
	})
}
