package me.suwash.tools.comparefiles.infra;

import me.suwash.util.constant.UtilMessageConst;

/**
 * メッセージコード。
 */
public interface MessageConst extends UtilMessageConst {

    /** 処理を開始しました。 */
    String MSGCD_PROCESS_START = "process.start";
    /** 処理が終了しました。 */
    String MSGCD_EXIT_SUCCESS = "exit.success";
    /** 処理が警告終了しました。 */
    String MSGCD_EXIT_WARN = "exit.warn";
    /** 処理が異常終了しました。 */
    String MSGCD_EXIT_FAIL = "exit.fail";
    /** 想定外のエラーが発生しました。 */
    String MSGCD_EXIT_ERROR = "exit.error";

    /** 引数の指定内容に誤りがあります。 */
    String MSGCD_ERROR_ARG = "error.arg";
    /** {0} のパースに失敗しました。 */
    String MSGCD_ERROR_PARSE = "error.parse";
    /** 妥当性チェックエラーが発生しました。 */
    String MSGCD_ERROR_VALIDATE = "error.validate";
    /** ストリームを閉じられません。 */
    String MSGCD_ERROR_STREAM_CLOSE = "error.streamClose";

    /** {0} 設定値={1}。 */
    String MSGCD_WRAP_VALIDATE_ERROR = "wrap.validateError";

    /** 正規表現のパースに失敗しました。正規表現：{0}。 */
    String MSGCD_ERROR_REGEX_PARSE = "error.regex.parse";

    /** 想定外のレイアウトです。対象ファイル：{0}、文字コード：{1}、行番号：{2}、レコード：{3}。 */
    String MSGCD_ERROR_FILE_LAYOUT = "error.file.layout";
    /** ファイルをパースできません。対象ファイル：{0}、文字コード：{1}、行番号：{2}、レコード：{3}。 */
    String MSGCD_ERROR_FILE_PARSE = "error.file.parse";

    /** 想定外のステータスです。ステータス：{0}。 */
    String MSGCD_ERROR_COMPARE_ILLEGAL_STATUS = "error.compare.illegalStatus";
    /** 左右のファイルがどちらも存在しません。左：{0}、右：{1}。 */
    String MSGCD_ERROR_COMPARE_FILE_BOTH_NOTEXIST = "error.compare.file.bothNotExist";
    /** 左右のディレクトリがどちらも存在しません。左：{0}、右：{1}。 */
    String MSGCD_ERROR_COMPARE_DIR_BOTH_NOTEXIST = "error.compare.dir.bothNotExist";

    /** レコードレイアウトを判別できません。ファイルレイアウト：{0}、行データ：{1}。 */
    String MSGCD_ERROR_COMPARE_FILE_TEXT_ROW_LAYOUT_NOTFOUND = "error.compare.file.text.row.layoutNotFound";
    /** コード値が取得できません。ファイルレイアウト：{0}、レコードタイプ：{1}。 */
    String MSGCD_ERROR_COMPARE_FILE_TEXT_ROW_CODEVALUE_NOTFOUND = "error.compare.file.text.row.codeValueNotFound";

    /** インスタンス化できません。対象クラス：{0}、原因：{1}。 */
    String MSGCD_ERROR_CANT_NEW_INSTANCE = "error.cantNewInstance";

    /** {0}形式の場合、レコードタイプは「{1}」をサポートしています。レイアウト定義を見なおして下さい。 */
    String MSGCD_ERROR_READER_UNSUPPORTED_RECORDPATTERN = "error.reader.unsupportedRecordPattern";

    /** データ開始行まで読み進められませんでした。ファイル:{0}, ファイルレイアウト:{1}。 */
    String MSGCD_ERROR_READER_CSV_CANT_SKIP_HEADER = "error.reader.csv.cantSkipHeader";
    /** 行のMap変換に失敗しました。現在行データリスト:{0}。 */
    String MSGCD_ERROR_READER_CSV_CANT_PARSE_ROW = "error.reader.csv.cantParseRow";

    /** 改行コード分読み進まられませんでした。改行コード:{0}。 */
    String MSGCD_ERROR_READER_FIXED_CANT_SKIP_LINESP = "error.reader.fixed.cantSkipLineSp";
    /** バイト→文字列の変換に失敗しました。文字コード：{0}、バイト配列：{1}。 */
    String MSGCD_ERROR_READER_FIXED_CANT_PARSE_BYTES = "error.reader.fixed.cantParseBytes";

    /** 変換元マップに 親項目ID：{0} が存在しません。対象項目ID：{1}、変換元マップ：{2}。 */
    String MSGCD_ERROR_READER_JSONLIST_NOTEXIST_ITEM = "error.reader.jsonList.notExistItem";

    /** トランザクションが開始されていません。処理を見直してください。対象：{0}。 */
    String MSGCD_ERROR_REPOSITORY_TX_NOTEXIST = "error.repository.tx.notExist";

    /** レコードレイアウトを判別できないため、Dataレコードとして扱います。ファイルレイアウト：{0}、当該行：{1}。 */
    String MSGCD_COMPARE_TEXT_ROW_LAYOUT_AS_DATA = "compare.file.text.row.layoutAsData";
}
