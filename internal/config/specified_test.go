package config

import "testing"

// 補完元 (共通の起動設定) に true・一覧を置いたときに、上書き側で明示した false / [] が
// 補完で消えないことを確かめる。
const trueDefaultConfigJSON = `{
  "deleteWorkDir" : "true",
  "sorted" : "true",
  "writeDiffOnly" : "true",
  "outputDir" : "result",
  "outputCharset" : "utf8",
  "compareResultFileName" : "CompareSummary.csv",
  "compareDetailFilePrefix" : "CompareDetail_",
  "leftPrefix" : "L:",
  "rightPrefix" : "R:",
  "csvHeaderRow" : 1,
  "csvDataStartRow" : 2,
  "chunkSize" : 1000,
  "ignoreFileRegexList" : ["^\\..*"],
  "ignoreItemList" : ["last_update_time"],
  "ignoreAreaList" : [{"x": 0, "y":0, "width":1024, "height":128}]
}`

func mustParse(t *testing.T, parse func([]byte) (*CompareFilesConfig, error), src string) *CompareFilesConfig {
	t.Helper()
	cfg, err := parse([]byte(src))
	if err != nil {
		t.Fatal(err)
	}
	return cfg
}

func TestSetDefaultSpecified(t *testing.T) {
	t.Run("SetDefault_JSONでfalseを明示した場合_補完元のtrueで上書きされないこと", func(t *testing.T) {
		// Arrange
		d := mustParse(t, ParseConfig, trueDefaultConfigJSON)
		cfg := mustParse(t, ParseConfig, `{"sorted": "false", "writeDiffOnly": false, "deleteWorkDir": "false"}`)

		// Act
		cfg.SetDefault(d)

		// Assert
		if cfg.Sorted || cfg.WriteDiffOnly || cfg.DeleteWorkDir {
			t.Errorf("明示した false が補完で true になった: sorted=%v writeDiffOnly=%v deleteWorkDir=%v", cfg.Sorted, cfg.WriteDiffOnly, cfg.DeleteWorkDir)
		}
	})

	t.Run("SetDefault_JSONで空の一覧を明示した場合_補完元の一覧で上書きされないこと", func(t *testing.T) {
		// Arrange
		d := mustParse(t, ParseConfig, trueDefaultConfigJSON)
		cfg := mustParse(t, ParseConfig, `{"ignoreFileRegexList": [], "ignoreItemList": [], "ignoreAreaList": []}`)

		// Act
		cfg.SetDefault(d)

		// Assert
		if len(cfg.IgnoreFileRegexList) != 0 || len(cfg.IgnoreItemList) != 0 || len(cfg.IgnoreAreaList) != 0 {
			t.Errorf("明示した [] が補完された: %v %v %v", cfg.IgnoreFileRegexList, cfg.IgnoreItemList, cfg.IgnoreAreaList)
		}
	})

	t.Run("SetDefault_YAMLの階層化形式でfalseと空の一覧を明示した場合_補完されないこと", func(t *testing.T) {
		// Arrange
		d := mustParse(t, ParseConfig, trueDefaultConfigJSON)
		cfg := mustParse(t, ParseConfigYAML, "compare:\n  sorted: false\n  ignoreItemList: []\n  image:\n    ignoreAreaList: []\noutput:\n  writeDiffOnly: false\n  deleteWorkDir: false\ninput:\n  ignoreFileRegexList: []\n")

		// Act
		cfg.SetDefault(d)

		// Assert
		if cfg.Sorted || cfg.WriteDiffOnly || cfg.DeleteWorkDir {
			t.Errorf("明示した false が補完された: sorted=%v writeDiffOnly=%v deleteWorkDir=%v", cfg.Sorted, cfg.WriteDiffOnly, cfg.DeleteWorkDir)
		}
		if len(cfg.IgnoreFileRegexList) != 0 || len(cfg.IgnoreItemList) != 0 || len(cfg.IgnoreAreaList) != 0 {
			t.Errorf("明示した [] が補完された: %v %v %v", cfg.IgnoreFileRegexList, cfg.IgnoreItemList, cfg.IgnoreAreaList)
		}
	})

	t.Run("SetDefault_JSONのキーを大文字始まりで書いた場合_明示したfalseと空の一覧が補完されないこと", func(t *testing.T) {
		// Arrange
		d := mustParse(t, ParseConfig, trueDefaultConfigJSON)
		cfg := mustParse(t, ParseConfig, `{"Sorted": false, "WRITEDIFFONLY": false, "IgnoreItemList": []}`)

		// Act
		cfg.SetDefault(d)

		// Assert
		if cfg.Sorted || cfg.WriteDiffOnly {
			t.Errorf("大文字始まりのキーで明示した false が補完された: sorted=%v writeDiffOnly=%v", cfg.Sorted, cfg.WriteDiffOnly)
		}
		if len(cfg.IgnoreItemList) != 0 {
			t.Errorf("大文字始まりのキーで明示した [] が補完された: %v", cfg.IgnoreItemList)
		}
	})

	t.Run("SetDefault_大文字始まりのフラットキーと階層化側のnullを併記した場合_優先される階層化側に従い補完されること", func(t *testing.T) {
		// Arrange
		d := mustParse(t, ParseConfig, trueDefaultConfigJSON)
		cfg := mustParse(t, ParseConfig, `{"Sorted": false, "IgnoreItemList": [], "compare": {"sorted": null, "ignoreItemList": null}}`)

		// Act
		cfg.SetDefault(d)

		// Assert
		if !cfg.Sorted || len(cfg.IgnoreItemList) != 1 {
			t.Errorf("階層化側の null が優先されていない: sorted=%v ignoreItemList=%v", cfg.Sorted, cfg.IgnoreItemList)
		}
	})

	t.Run("ParseConfig_同じフラットキーを大文字小文字違いで併記した場合_エラーであること", func(t *testing.T) {
		// Arrange
		src := `{"Sorted": false, "sorted": true}`

		// Act
		_, err := ParseConfig([]byte(src))

		// Assert
		if err == nil {
			t.Error("同じ設定の別表記の併記がエラーにならない")
		}
	})

	t.Run("SetDefault_キーを書かない場合_補完元の値で補われること", func(t *testing.T) {
		// Arrange
		d := mustParse(t, ParseConfig, trueDefaultConfigJSON)
		cfg := mustParse(t, ParseConfig, `{"outputDir": "out"}`)

		// Act
		cfg.SetDefault(d)

		// Assert
		if !cfg.Sorted || !cfg.WriteDiffOnly || !cfg.DeleteWorkDir {
			t.Errorf("書いていない bool が補完されていない: sorted=%v writeDiffOnly=%v deleteWorkDir=%v", cfg.Sorted, cfg.WriteDiffOnly, cfg.DeleteWorkDir)
		}
		if len(cfg.IgnoreFileRegexList) != 1 || len(cfg.IgnoreItemList) != 1 || len(cfg.IgnoreAreaList) != 1 {
			t.Errorf("書いていない一覧が補完されていない: %v %v %v", cfg.IgnoreFileRegexList, cfg.IgnoreItemList, cfg.IgnoreAreaList)
		}
		if cfg.OutputDir != "out" || cfg.CsvHeaderRow != 1 {
			t.Errorf("outputDir=%q csvHeaderRow=%d", cfg.OutputDir, cfg.CsvHeaderRow)
		}
	})

	t.Run("SetDefault_nullを指定した場合_書かないのと同じく補完されること", func(t *testing.T) {
		// Arrange
		d := mustParse(t, ParseConfig, trueDefaultConfigJSON)
		cfg := mustParse(t, ParseConfig, `{"sorted": null, "ignoreItemList": null}`)

		// Act
		cfg.SetDefault(d)

		// Assert
		if !cfg.Sorted || len(cfg.IgnoreItemList) != 1 {
			t.Errorf("null が補完されていない: sorted=%v ignoreItemList=%v", cfg.Sorted, cfg.IgnoreItemList)
		}
	})

	t.Run("SetDefault_文字列を空で指定した場合_従来どおり補完されること", func(t *testing.T) {
		// Arrange
		d := mustParse(t, ParseConfig, trueDefaultConfigJSON)
		cfg := mustParse(t, ParseConfig, `{"outputDir": ""}`)

		// Act
		cfg.SetDefault(d)

		// Assert
		if cfg.OutputDir != "result" {
			t.Errorf("空文字の outputDir が補完されていない: %q", cfg.OutputDir)
		}
	})
}
