package config

import (
	"strings"
	"testing"
)

func lintJSON(t *testing.T, jsonStr string) []LintIssue {
	t.Helper()
	list, err := ParseLayoutList([]byte(jsonStr))
	if err != nil {
		t.Fatalf("パースエラー: %v", err)
	}
	return LintLayoutList(list)
}

func hasErrorContaining(issues []LintIssue, substr string) bool {
	for _, issue := range issues {
		if issue.Error && strings.Contains(issue.Message, substr) {
			return true
		}
	}
	return false
}

func hasWarnContaining(issues []LintIssue, substr string) bool {
	for _, issue := range issues {
		if !issue.Error && strings.Contains(issue.Message, substr) {
			return true
		}
	}
	return false
}

func TestLintValidLayout(t *testing.T) {
	issues := lintJSON(t, `{"layoutList": [{
		"logicalFileName": "ok", "fileRegexPattern": "a.*\\.csv", "fileFormat": "CSV_withHeader",
		"charset": "utf8", "lineSp": "LF",
		"recordList": [{"type": "Data", "codeValue": "-", "itemList": [
			{"id": "KEY", "byteLength": 3, "criteria": "Equal", "compareKey": "true"},
			{"id": "VAL", "byteLength": 5, "criteria": "Equal", "compareKey": "false"}
		]}]
	}]}`)
	for _, issue := range issues {
		if issue.Error {
			t.Errorf("正常なレイアウトでエラー: %v", issue)
		}
	}
}

func TestLintErrors(t *testing.T) {
	tests := []struct {
		name    string
		json    string
		errPart string
	}{
		{"空リスト", `{"layoutList": []}`, "layoutList が空"},
		{"正規表現なし", `{"layoutList": [{"logicalFileName": "x", "fileFormat": "Text", "charset": "utf8"}]}`, "fileRegexPattern は必須"},
		{"不正な正規表現", `{"layoutList": [{"logicalFileName": "x", "fileRegexPattern": "([", "fileFormat": "Text", "charset": "utf8"}]}`, "コンパイルできません"},
		{"fileFormat なし", `{"layoutList": [{"logicalFileName": "x", "fileRegexPattern": "a"}]}`, "fileFormat は必須"},
		{"不正な charset", `{"layoutList": [{"logicalFileName": "x", "fileRegexPattern": "a", "fileFormat": "Text", "charset": "bogus"}]}`, "解決できません"},
		{"Fixed で recordList なし", `{"layoutList": [{"logicalFileName": "x", "fileRegexPattern": "a", "fileFormat": "Fixed", "charset": "utf8"}]}`, "recordList が必須"},
		{"CSV で recordList なし", `{"layoutList": [{"logicalFileName": "x", "fileRegexPattern": "a", "fileFormat": "CSV_withHeader", "charset": "utf8"}]}`, "recordList が空"},
		{"JsonList で複数レコード", `{"layoutList": [{"logicalFileName": "x", "fileRegexPattern": "a", "fileFormat": "JsonList", "charset": "utf8",
			"recordList": [
				{"type": "Header", "codeValue": "0", "itemList": [{"id": "a"}]},
				{"type": "Data", "codeValue": "1", "itemList": [{"id": "b"}]}
			]}]}`, "複数レコードタイプは使用できません"},
		{"マルチレコードで codeValue なし", `{"layoutList": [{"logicalFileName": "x", "fileRegexPattern": "a", "fileFormat": "CSV_noHeader", "charset": "utf8",
			"recordList": [
				{"type": "Header", "itemList": [{"id": "a", "compareKey": "true"}]},
				{"type": "Data", "codeValue": "1", "itemList": [{"id": "b", "compareKey": "true"}]}
			]}]}`, "codeValue は必須"},
		{"Fixed でバイト長不一致", `{"layoutList": [{"logicalFileName": "x", "fileRegexPattern": "a", "fileFormat": "Fixed", "charset": "utf8",
			"recordList": [
				{"type": "Header", "codeValue": "0", "itemList": [{"id": "a", "byteLength": 10, "compareKey": "true"}]},
				{"type": "Data", "codeValue": "1", "itemList": [{"id": "b", "byteLength": 20, "compareKey": "true"}]}
			]}]}`, "一致しません"},
		{"Fixed で byteLength なし", `{"layoutList": [{"logicalFileName": "x", "fileRegexPattern": "a", "fileFormat": "Fixed", "charset": "utf8",
			"recordList": [{"type": "Data", "codeValue": "-", "itemList": [{"id": "a", "compareKey": "true"}]}]}]}`, "byteLength (1 以上) が必須"},
		{"item id なし", `{"layoutList": [{"logicalFileName": "x", "fileRegexPattern": "a", "fileFormat": "CSV_noHeader", "charset": "utf8",
			"recordList": [{"type": "Data", "codeValue": "-", "itemList": [{"name": "no-id", "compareKey": "true"}]}]}]}`, "id は必須"},
	}
	for _, tt := range tests {
		issues := lintJSON(t, tt.json)
		if !hasErrorContaining(issues, tt.errPart) {
			t.Errorf("%s: %q を含むエラーが出ない: %v", tt.name, tt.errPart, issues)
		}
	}
}

func TestLintWarnings(t *testing.T) {
	tests := []struct {
		name     string
		json     string
		warnPart string
	}{
		{"charset 未設定", `{"layoutList": [{"logicalFileName": "x", "fileRegexPattern": "a", "fileFormat": "CSV_withHeader",
			"recordList": [{"type": "Data", "codeValue": "-", "itemList": [{"id": "a", "compareKey": "true"}]}]}]}`, "charset が未設定"},
		{"compareKey なし", `{"layoutList": [{"logicalFileName": "x", "fileRegexPattern": "a", "fileFormat": "CSV_withHeader", "charset": "utf8",
			"recordList": [{"type": "Data", "codeValue": "-", "itemList": [{"id": "a"}]}]}]}`, "compareKey=true の項目がありません"},
		{"正規表現の重複", `{"layoutList": [
			{"logicalFileName": "x", "fileRegexPattern": "a", "fileFormat": "Text", "charset": "utf8"},
			{"logicalFileName": "y", "fileRegexPattern": "a", "fileFormat": "Text", "charset": "utf8"}
		]}`, "重複しています"},
		{"item id 重複", `{"layoutList": [{"logicalFileName": "x", "fileRegexPattern": "a", "fileFormat": "CSV_withHeader", "charset": "utf8",
			"recordList": [{"type": "Data", "codeValue": "-", "itemList": [
				{"id": "a", "compareKey": "true"}, {"id": "a"}
			]}]}]}`, "id \"a\" が重複"},
		{"Image で recordList", `{"layoutList": [{"logicalFileName": "x", "fileRegexPattern": "a", "fileFormat": "Image",
			"recordList": [{"type": "Data", "itemList": [{"id": "a"}]}]}]}`, "recordList は使用されません"},
	}
	for _, tt := range tests {
		issues := lintJSON(t, tt.json)
		if !hasWarnContaining(issues, tt.warnPart) {
			t.Errorf("%s: %q を含む警告が出ない: %v", tt.name, tt.warnPart, issues)
		}
	}
}

func TestLintSampleLayouts(t *testing.T) {
	// 同梱サンプルレイアウトはエラーなしで通ること
	for _, path := range []string{
		"../assets/compare_layout/sample_text.json",
		"../assets/compare_layout/sample_image.json",
	} {
		issues := LintLayoutFile(path)
		for _, issue := range issues {
			if issue.Error {
				t.Errorf("%s: %v", path, issue)
			}
		}
	}
}
