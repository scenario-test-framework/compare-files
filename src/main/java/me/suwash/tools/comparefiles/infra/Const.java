package me.suwash.tools.comparefiles.infra;

/**
 * システム定数。
 */
public interface Const extends MessageConst {
    /** 設定ファイル拡張子。 */
    String EXT_CONFIG = "json";

    /** デフォルト設定ファイルの読み込み文字コード。 */
    String CHARSET_DEFAULT_CONFIG = "UTF-8";

    /** ファイルレイアウト定義配置ディレクトリ（classpath内）。 */
    String CLASSPATH_LAYOUT_DIR_NAME = "compareFilesLayout";

    /** デフォルトの項目ID。 */
    String DEFAULT_ITEM_ID = "value";

    /** デフォルトの書き出しバッファサイズ。 */
    int DEFAULT_CHUNK_SIZE = 1000;

    /** intデフォルト値。 */
    int DEFAULT_INT_VALUE = Integer.MIN_VALUE;

    /** ダミー値。 */
    String DUMMY_VALUE = "-";
    long UNKNOWN_LINE = -1;

    /** CUIのリターンコード：成功。 */
    int EXITCODE_SUCCESS = 0;

    /** CUIのリターンコード：警告終了。 */
    int EXITCODE_WARN = 3;

    /** CUIのリターンコード：エラー終了。 */
    int EXITCODE_ERROR = 6;

    /** ユーザ入力値：Yes。 */
    String INPUT_YES = "y";

    /** workディレクトリ名。 */
    String DIRNAME_WORK = "work";

    /** INFOログ出力する間隔。 */
    int RANGE_REPORT = 100;

    /** ファイル比較結果.差分プリフィックス.左データ。 */
    String DEFAULT_PREFIX_LEFT = "L:";

    /** ファイル比較結果.差分プリフィックス.右データ。 */
    String DEFAULT_PREFIX_RIGHT = "R:";

    /** フォーマット：タイムスタンプ。 */
    String FORMAT_TIMESTAMP = "yyyy-MM-dd HH:mm:ss.SSS";

    /** フォーマット：件数。 */
    String FORMAT_COUNT = "#,##0";
}
