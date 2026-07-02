package config

import (
	"fmt"
	"os"

	"github.com/dlclark/regexp2"

	"github.com/scenario-test-framework/compare-files/internal/charset"
	"github.com/scenario-test-framework/compare-files/internal/status"
)

// LintIssue はレイアウト定義の検証で見つかった問題です。
type LintIssue struct {
	// Error は実行時にエラーや意図しない結果になる問題、そうでなければ警告です。
	Error   bool
	Message string
}

func (i LintIssue) String() string {
	level := "WARN "
	if i.Error {
		level = "ERROR"
	}
	return level + ": " + i.Message
}

// LintLayoutFile はレイアウト定義ファイルを検証します。
// パース不能の場合は 1 件のエラーとして返します。
func LintLayoutFile(path string) []LintIssue {
	data, err := os.ReadFile(path)
	if err != nil {
		return []LintIssue{{Error: true, Message: fmt.Sprintf("ファイルを読込みできません: %v", err)}}
	}
	list, err := ParseLayoutList(data)
	if err != nil {
		return []LintIssue{{Error: true, Message: fmt.Sprintf("JSON をパースできません: %v", err)}}
	}
	return LintLayoutList(list)
}

// LintLayoutList はレイアウト定義を検証します。
// 実行時の挙動 (レイアウト解決・リーダ・比較) に基づくチェックを行います。
func LintLayoutList(list *FileLayoutList) []LintIssue {
	var issues []LintIssue
	errorf := func(format string, args ...any) {
		issues = append(issues, LintIssue{Error: true, Message: fmt.Sprintf(format, args...)})
	}
	warnf := func(format string, args ...any) {
		issues = append(issues, LintIssue{Error: false, Message: fmt.Sprintf(format, args...)})
	}

	if len(list.LayoutList) == 0 {
		errorf("layoutList が空です。レイアウトを 1 件以上定義してください")
		return issues
	}

	seenPattern := map[string]int{}
	for i, layout := range list.LayoutList {
		name := layout.LogicalFileName
		if name == "" {
			name = fmt.Sprintf("layoutList[%d]", i)
		}
		prefix := fmt.Sprintf("%s: ", name)

		// fileRegexPattern
		switch {
		case layout.FileRegexPattern == "":
			errorf("%sfileRegexPattern は必須です (どのファイルにもマッチしなくなります)", prefix)
		default:
			if _, err := regexp2.Compile(`\A(?:`+layout.FileRegexPattern+`)\z`, regexp2.None); err != nil {
				errorf("%sfileRegexPattern がコンパイルできません: %v", prefix, err)
			}
			if prev, dup := seenPattern[layout.FileRegexPattern]; dup {
				warnf("%sfileRegexPattern %q が layoutList[%d] と重複しています (後勝ちで上書きされます)", prefix, layout.FileRegexPattern, prev)
			}
			seenPattern[layout.FileRegexPattern] = i
		}

		// logicalFileName
		if layout.LogicalFileName == "" {
			warnf("%slogicalFileName が未設定です (結果サマリの Layout 列に出力されます)", prefix)
		}

		// fileFormat
		if layout.FileFormat == "" {
			errorf("%sfileFormat は必須です (CSV_withHeader/CSV_noHeader/TSV_withHeader/TSV_noHeader/Json/JsonList/Fixed/Text/Image)", prefix)
			continue
		}

		if layout.FileFormat == status.FormatImage {
			lintImageLayout(layout, prefix, errorf, warnf)
			continue
		}
		lintTextLayout(layout, prefix, errorf, warnf)
	}
	return issues
}

func lintImageLayout(layout *FileLayout, prefix string, errorf, warnf func(string, ...any)) {
	if len(layout.RecordList) > 0 {
		warnf("%sImage 形式では recordList は使用されません", prefix)
	}
	for j, area := range layout.IgnoreAreaList {
		if area.Width <= 0 || area.Height <= 0 {
			warnf("%signoreAreaList[%d] の width/height が 0 以下です (マスクされません)", prefix, j)
		}
	}
}

func lintTextLayout(layout *FileLayout, prefix string, errorf, warnf func(string, ...any)) {
	// charset: 未設定は defaultInputCharset → UTF-8 にフォールバックする
	if layout.Charset == "" {
		warnf("%scharset が未設定です (設定の defaultInputCharset、なければ UTF-8 が使われます)", prefix)
	} else if !charset.IsValid(layout.Charset) {
		errorf("%scharset %q は解決できません (utf8/ms932/sjis/euc-jp 等)", prefix, layout.Charset)
	}

	if len(layout.IgnoreAreaList) > 0 {
		warnf("%signoreAreaList はテキスト形式では使用されません (画像形式専用)", prefix)
	}

	format := layout.FileFormat

	// recordList の必須チェック
	if len(layout.RecordList) == 0 {
		switch format {
		case status.FormatFixed:
			errorf("%sFixed 形式は recordList が必須です", prefix)
		case status.FormatText:
			// Text はレイアウト項目を使わない
		default:
			errorf("%srecordList が空です (この形式は比較項目の定義が必要です)", prefix)
		}
		return
	}

	if format == status.FormatText {
		warnf("%sText 形式では recordList は使用されません (行全体を 1 項目として比較します)", prefix)
		return
	}

	// Json/JsonList はレコードタイプ判定をサポートしない
	if (format == status.FormatJSON || format == status.FormatJSONList) && len(layout.RecordList) > 1 {
		errorf("%sJson/JsonList 形式で複数レコードタイプは使用できません (実行時エラーになります)", prefix)
	}

	// マルチレコードタイプの codeValue 必須
	if len(layout.RecordList) > 1 {
		for j, rl := range layout.RecordList {
			if rl.CodeValue == "" {
				errorf("%srecordList[%d] (%s): 複数レコードタイプの場合 codeValue は必須です", prefix, j, rl.Type)
			}
		}
	}

	// Fixed: 全レコードタイプのバイト長は先頭レコードと同一である必要がある
	if format == status.FormatFixed && len(layout.RecordList) > 0 {
		firstLen := layout.RecordList[0].RecordByteLength()
		for j, rl := range layout.RecordList {
			if l := rl.RecordByteLength(); l != firstLen {
				errorf("%srecordList[%d] (%s) のバイト長 %d が先頭レコードの %d と一致しません (先頭レコード長で全行を読み込みます)", prefix, j, rl.Type, l, firstLen)
			}
			if rl.CodeValue != "" && len(rl.CodeValue) > firstLen {
				errorf("%srecordList[%d] (%s) の codeValue がレコード長を超えています", prefix, j, rl.Type)
			}
		}
	}

	for j, rl := range layout.RecordList {
		rPrefix := fmt.Sprintf("%srecordList[%d] (%s): ", prefix, j, rl.Type)
		if rl.Type == "" {
			errorf("%stype は必須です (Header/Data/Trailer/End)", rPrefix)
		}
		if len(rl.ItemList) == 0 {
			errorf("%sitemList が空です", rPrefix)
			continue
		}
		seenID := map[string]bool{}
		hasKey := false
		for k, item := range rl.ItemList {
			iPrefix := fmt.Sprintf("%sitemList[%d]: ", rPrefix, k)
			if item.ID == "" {
				errorf("%sid は必須です (比較結果のカラム名になります)", iPrefix)
			} else if seenID[item.ID] {
				warnf("%sid %q が重複しています (後の定義で上書きされます)", iPrefix, item.ID)
			}
			seenID[item.ID] = true
			if format == status.FormatFixed && item.ByteLength <= 0 {
				errorf("%sFixed 形式では byteLength (1 以上) が必須です", iPrefix)
			}
			if item.CompareKey {
				hasKey = true
			}
		}
		if !hasKey && rl.Type == status.RecordData {
			warnf("%scompareKey=true の項目がありません (行の対応付けができず、行ズレが LeftOnly/RightOnly として出力されます)", rPrefix)
		}
	}
}
