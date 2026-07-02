package config

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"strings"

	"gopkg.in/yaml.v3"
)

// hierarchyMapping は階層化設定 (利用目的ごとのグループ) のキーと
// フラット設定キーの対応表です。グループ内では簡潔な別名と従来のフラットキー名の
// 両方を受け付けます。
var hierarchyMapping = map[string]string{
	// input: 入力ファイルの読み込み設定
	// (leftFilePath / rightFilePath は実行系が参照しない Java 互換フィールドのため対応表に載せない)
	"input.defaultCharset":      "defaultInputCharset",
	"input.defaultInputCharset": "defaultInputCharset",
	"input.ignoreFileRegexList": "ignoreFileRegexList",

	// compare: 比較処理の設定
	"compare.sorted":                              "sorted",
	"compare.chunkSize":                           "chunkSize",
	"compare.ignoreItemList":                      "ignoreItemList",
	"compare.layoutDir":                           "overwriteLayoutDir",
	"compare.overwriteLayoutDir":                  "overwriteLayoutDir",
	"compare.csv.headerRow":                       "csvHeaderRow",
	"compare.csv.csvHeaderRow":                    "csvHeaderRow",
	"compare.csv.dataStartRow":                    "csvDataStartRow",
	"compare.csv.csvDataStartRow":                 "csvDataStartRow",
	"compare.fixed.codeValueForOnlyOneRecordType": "codeValueForOnlyOneRecordType",
	"compare.image.ignoreAreaList":                "ignoreAreaList",
	"compare.image.okStyle":                       "okImageStyle",
	"compare.image.okImageStyle":                  "okImageStyle",
	"compare.image.ngStyle":                       "ngImageStyle",
	"compare.image.ngImageStyle":                  "ngImageStyle",

	// output: 比較結果の出力設定
	"output.dir":                     "outputDir",
	"output.outputDir":               "outputDir",
	"output.charset":                 "outputCharset",
	"output.outputCharset":           "outputCharset",
	"output.resultFileName":          "compareResultFileName",
	"output.compareResultFileName":   "compareResultFileName",
	"output.detailFilePrefix":        "compareDetailFilePrefix",
	"output.compareDetailFilePrefix": "compareDetailFilePrefix",
	"output.writeDiffOnly":           "writeDiffOnly",
	"output.leftPrefix":              "leftPrefix",
	"output.rightPrefix":             "rightPrefix",
	"output.deleteWorkDir":           "deleteWorkDir",
}

// hierarchyGroups は階層化設定のトップレベルグループ名です。
var hierarchyGroups = map[string]bool{"input": true, "compare": true, "output": true}

// ParseConfig はバイト列(UTF-8 JSON)から設定をパースします。
// フラット形式・階層化形式のどちらも受け付けます (両方指定時は階層化側が優先)。
func ParseConfig(data []byte) (*CompareFilesConfig, error) {
	var raw map[string]any
	dec := json.NewDecoder(bytes.NewReader(data))
	dec.UseNumber()
	if err := dec.Decode(&raw); err != nil {
		return nil, err
	}
	// 末尾に余分なデータがあれば壊れた設定として弾く (json.Unmarshal と同じ厳密さ)
	if _, err := dec.Token(); err != io.EOF {
		return nil, fmt.Errorf("設定の末尾に余分なデータがあります")
	}
	return buildConfig(raw)
}

// ParseConfigYAML はバイト列(UTF-8 YAML)から設定をパースします。
// フラット形式・階層化形式のどちらも受け付けます (両方指定時は階層化側が優先)。
func ParseConfigYAML(data []byte) (*CompareFilesConfig, error) {
	var raw map[string]any
	if err := yaml.Unmarshal(data, &raw); err != nil {
		return nil, err
	}
	return buildConfig(raw)
}

// ParseConfigFile は指定パスの設定ファイルをパースします。
// 拡張子 .yaml / .yml は YAML、それ以外は JSON として解釈します。
func ParseConfigFile(path string) (*CompareFilesConfig, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("設定ファイルを読み込めません: %s: %w", path, err)
	}
	var cfg *CompareFilesConfig
	switch strings.ToLower(filepath.Ext(path)) {
	case ".yaml", ".yml":
		cfg, err = ParseConfigYAML(data)
	default:
		cfg, err = ParseConfig(data)
	}
	if err != nil {
		return nil, fmt.Errorf("設定ファイルをパースできません: %s: %w", path, err)
	}
	return cfg, nil
}

// buildConfig は生の設定マップを階層化キーの解決後に CompareFilesConfig へ変換します。
func buildConfig(raw map[string]any) (*CompareFilesConfig, error) {
	flat, err := flattenHierarchy(raw)
	if err != nil {
		return nil, err
	}
	jsonBytes, err := json.Marshal(flat)
	if err != nil {
		return nil, err
	}
	cfg := NewConfig()
	if err := json.Unmarshal(jsonBytes, cfg); err != nil {
		return nil, err
	}
	return cfg, nil
}

// flattenHierarchy は階層化設定 (input / compare / output グループ) を
// フラットキーへ変換します。グループ外のキーはそのまま維持します。
// フラットキーと階層化キーが両方指定された場合は階層化側を優先します。
// 同じフラットキーに写る同義キー (output.dir と output.outputDir など) の
// 併記は非決定になるためエラーにします。
func flattenHierarchy(raw map[string]any) (map[string]any, error) {
	flat := map[string]any{}
	var groups []string
	for key, value := range raw {
		if hierarchyGroups[key] {
			groups = append(groups, key)
			continue
		}
		flat[key] = value
	}
	seen := map[string]string{} // flatKey → 階層化キーの path
	for _, group := range groups {
		groupMap, ok := raw[group].(map[string]any)
		if !ok {
			return nil, fmt.Errorf("設定の %s はオブジェクトで指定してください", group)
		}
		if err := applyHierarchyGroup(flat, group, groupMap, seen); err != nil {
			return nil, err
		}
	}
	return flat, nil
}

// applyHierarchyGroup はグループ配下のキーを対応表でフラットキーへ変換します。
// 対応表にないキーは設定ミスの検出のためエラーにします。
func applyHierarchyGroup(flat map[string]any, prefix string, groupMap map[string]any, seen map[string]string) error {
	for key, value := range groupMap {
		path := prefix + "." + key
		if flatKey, ok := hierarchyMapping[path]; ok {
			if prevPath, dup := seen[flatKey]; dup {
				return fmt.Errorf("設定の %s と %s は同じ設定 (%s) の別名です。どちらか一方だけを指定してください", prevPath, path, flatKey)
			}
			seen[flatKey] = path
			flat[flatKey] = value
			continue
		}
		// 中間グループ (compare.csv など): 配下に対応キーがあるオブジェクトのみ許容
		if subMap, ok := value.(map[string]any); ok && hasHierarchyPrefix(path) {
			if err := applyHierarchyGroup(flat, path, subMap, seen); err != nil {
				return err
			}
			continue
		}
		return fmt.Errorf("設定の %s は未定義のキーです", path)
	}
	return nil
}

// hasHierarchyPrefix は対応表に path 配下のキーが存在するかを返します。
func hasHierarchyPrefix(path string) bool {
	prefix := path + "."
	for key := range hierarchyMapping {
		if strings.HasPrefix(key, prefix) {
			return true
		}
	}
	return false
}
