// Package msg はメッセージリソースを提供します。
// Java 版の MessageSource (properties + MessageFormat) 相当の小実装です。
// 子(compare-files 固有)→親(共通)の順でキーを解決します。
// ログ・エラーメッセージの文言は Java 版 ja リソースを踏襲しています
// (Java 版は en リソースも日本語のままだったため、日本語のみ保持します)。
package msg

import (
	"fmt"
	"strings"
)

// アプリ固有メッセージ (compare_files_message_source_ja.properties)。
var appMessages = map[string]string{
	"process.start": "比較を開始しました。",
	"exit.success":  "比較が終了しました。比較結果にNGはありません。",
	"exit.warn":     "処理が警告終了しました。比較結果にNGが含まれています。",
	"exit.fail":     "処理が異常終了しました。比較設定を見直してください。",
	"exit.error":    "想定外のエラーが発生しました。",

	// 進捗ログ (log.*)。外部設定 (compare_files_messages.properties) で上書きできます。
	"log.config.default":     "デフォルト起動設定: {0}",
	"log.config.custom":      "カスタム起動設定: {0}",
	"log.input.header":       "・入力情報",
	"log.input.leftPath":     "  ・左パス                          : {0}",
	"log.input.rightPath":    "  ・右パス                          : {0}",
	"log.input.targetConfig": "  ・比較対象設定ファイル            : {0}",
	"log.input.modeFile":     "  ・比較モード                      : ファイル比較",
	"log.input.modeDir":      "  ・比較モード                      : ディレクトリ比較",
	"log.dir.leftOnly":       "・[左のみ]{0}",
	"log.dir.rightOnly":      "・[右のみ]{0}",
	"log.dir.scan":           "・ファイル走査",
	"log.dir.compare":        "・ディレクトリ比較",
	"log.file.compare":       "  ・ファイル比較 左:{0}、右:{1}",
	"log.file.skip":          "      ・[SKIP]正規表現:{0}, ファイル名:{1}",
	"log.regex.loadTargets":  "・比較対象設定の読込み",
	"log.regex.compare":      "・比較",
	"log.text.sort":          "    ・ソート",
	"log.text.compare":       "    ・テキスト比較",

	"error.arg":         "引数の指定内容に誤りがあります。",
	"error.parse":       "{0} のパースに失敗しました。",
	"error.validate":    "妥当性チェックエラーが発生しました。",
	"error.streamClose": "ストリームを閉じられません。",

	"wrap.validateError": "{0} 設定値={1}",

	"error.regex.parse": "正規表現のパースに失敗しました。正規表現：{0}",
	"error.file.layout": "想定外のレイアウトです。対象ファイル：{0}、文字コード：{1}、行番号：{2}、レコード：{3}",
	"error.file.parse":  "ファイルをパースできません。対象ファイル：{0}、文字コード：{1}、行番号：{2}、レコード：{3}",

	"error.compare.illegalStatus":                   "想定外のステータスです。ステータス：{0}",
	"error.compare.file.bothNotExist":               "左右のファイルがどちらも存在しません。左：{0}、右：{1}",
	"error.compare.dir.bothNotExist":                "左右のディレクトリがどちらも存在しません。左：{0}、右：{1}",
	"error.compare.file.text.row.layoutNotFound":    "レコードレイアウトを判別できません。ファイルレイアウト：{0}、行データ：{1}",
	"error.compare.file.text.row.codeValueNotFound": "コード値が取得できません。ファイルレイアウト：{0}、レコードタイプ：{1}",
	"compare.file.text.row.layoutAsData":            "レコードレイアウトを判別できないため、Dataレコードとして扱います。ファイルレイアウト：{0}、当該行：{1}",

	"error.reader.unsupportedRecordPattern": "{0}形式の場合、レコードタイプは「{1}」をサポートしています。レイアウト定義を見なおして下さい。",
	"error.reader.csv.cantSkipHeader":       "データ開始行まで読み進められませんでした。ファイル：{0}, ファイルレイアウト：{1}",
	"error.reader.csv.cantParseRow":         "行のMap変換に失敗しました。現在行データリスト：{0}",
	"error.reader.fixed.cantSkipLineSp":     "改行コード分読み進まられませんでした。改行コード:{0}",
	"error.reader.fixed.cantParseBytes":     "バイト→文字列の変換に失敗しました。文字コード：{0}、バイト配列：{1}",
	"error.reader.jsonList.notExistItem":    "変換元マップに 親項目ID：{0} が存在しません。対象項目ID：{1}、変換元マップ：{2}",

	"error.repository.tx.notExist": "トランザクションが開始されていません。処理を見直してください。対象：{0}",
}

// 共通メッセージ (message_source.properties)。
var commonMessages = map[string]string{
	"check.countSame":        "{0} の数は {1} でなければなりません。(実績値：{2})",
	"check.notNull":          "{0} が設定されていません。",
	"check.undefined":        "{0} が定義されていません。",
	"check.illegalArguments": "\"{0}\" に {1}({2}) は適用できません。",
	"check.notExist":         "{0} が存在しません。",
	"check.noSet":            "{0} に {1} が設定されていません。",
	"check.notSame":          "{0} は {1} と一致しません。",

	"errorHandle":          "{0} でエラーが発生しました。詳細:{1}",
	"errorOnErrorHandle":   "{0} の例外処理中に、{1} が発生しました。",
	"unsupported.function": "この機能には対応していません。",
	"unsupported.pattern":  "{0} で {1} には対応していません。",

	"dir.check":      "ディレクトリではありません。対象:{0}",
	"dir.cantCreate": "ディレクトリを作成できません。対象:{0}",
	"dir.cantDelete": "ディレクトリを削除できません。対象:{0}",

	"file.check":      "ファイルではありません。対象:{0}",
	"file.cantRead":   "ファイルを読込みできません。対象:{0}",
	"file.cantWrite":  "ファイルを書出しできません。対象:{0}",
	"file.cantDelete": "ファイルを削除できません。対象:{0}",
}

// overrides は外部設定ファイルによるメッセージ上書きです。
// アプリ固有・共通メッセージより優先して解決されます。
var overrides = map[string]string{}

// SetOverrides はメッセージの上書き定義を設定します (キーごとにマージ)。
func SetOverrides(m map[string]string) {
	for k, v := range m {
		overrides[k] = v
	}
}

// Get はメッセージ ID を解決し、{n} プレースホルダを引数で置換して返します。
// 外部上書き → アプリ固有 → 共通の順で解決し、見つからない場合はキーをそのまま返します。
func Get(id string, args ...any) string {
	tmpl, ok := overrides[id]
	if !ok {
		tmpl, ok = appMessages[id]
	}
	if !ok {
		tmpl, ok = commonMessages[id]
	}
	if !ok {
		return id
	}
	for i, arg := range args {
		tmpl = strings.ReplaceAll(tmpl, fmt.Sprintf("{%d}", i), fmt.Sprint(arg))
	}
	return tmpl
}
