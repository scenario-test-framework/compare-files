package me.suwash.util.constant;

/**
 * メッセージコード。
 */
public interface UtilMessageConst {

    /** メッセージコード：{0} の数は {1} でなければなりません。(実績値：{2})。 */
    String CHECK_COUNTSAME = "check.countSame";
    /** メッセージコード：{0} が設定されていません。 */
    String CHECK_NOTNULL = "check.notNull";
    /** メッセージコード：{0} が定義されていません。 */
    String CHECK_UNDEFINED = "check.undefined";
    /** メッセージコード："{0}" に {1}({2}) は適用できません。 */
    String CHECK_ILLEGALARGS = "check.illegalArguments";
    /** メッセージコード：{0} が存在しません。 */
    String CHECK_NOTEXIST = "check.notExist";
    /** メッセージコード：{0} に {1} が設定されていません。 */
    String CHECK_NOSET = "check.noSet";
    /** メッセージコード：{0} は {1} と一致しません。 */
    String CHECK_NOTSAME = "check.notSame";

    /** メッセージコード：メモリ情報:Max={0}MB, Total={1}MB, Used={2}MB [{3}%]。 */
    String MEMORY_INFO = "memory.info";

    /** メッセージコード：{0}でエラーが発生しました。詳細:{1}。 */
    String ERRORHANDLE = "errorHandle";
    /** メッセージコード：{0} の例外処理中に、{1}が発生しました。 */
    String ERROR_ON_ERRORHANDLE = "errorOnErrorHandle";
    /** メッセージコード：この機能には対応していません。 */
    String UNSUPPORTED = "unsupported.function";
    /** メッセージコード：{0} で {1} には対応していません。 */
    String UNSUPPORTED_PATTERN = "unsupported.pattern";

    /** メッセージコード：ディレクトリではありません。対象:{0}。 */
    String DIR_CHECK = "dir.check";
    /** メッセージコード：ディレクトリを作成できません。対象:{0}。 */
    String DIR_CANTCREATE = "dir.cantCreate";
    /** メッセージコード：ディレクトリを削除できません。対象:{0}。 */
    String DIR_CANTDELETE = "dir.cantDelete";

    /** メッセージコード：ファイルではありません。対象:{0}。 */
    String FILE_CHECK = "file.check";
    /** メッセージコード：ファイルを読込みできません。対象:{0}。 */
    String FILE_CANTREAD = "file.cantRead";
    /** メッセージコード：ファイルを書出しできません。対象:{0}。 */
    String FILE_CANTWRITE = "file.cantWrite";
    /** メッセージコード：ファイルを削除できません。対象:{0}。 */
    String FILE_CANTDELETE = "file.cantDelete";

    /** メッセージコード：入力ストリームをオープンできません。対象:{0}。 */
    String STREAM_CANTOPEN_INPUT = "stream.cantOpenInputStream";
    /** メッセージコード：入力ストリームをクローズできません。対象:{0}。 */
    String STREAM_CANTCLOSE_INPUT = "stream.cantCloseInputStream";
    /** メッセージコード：出力ストリームをオープンできません。対象:{0}。 */
    String STREAM_CANTOPEN_OUTPUT = "stream.cantOpenOutputStream";
    /** メッセージコード：出力ストリームをクローズできません。対象:{0}。 */
    String STREAM_CANTCLOSE_OUTPUT = "stream.cantCloseOutputStream";

    /** メッセージコード：{0} に {1} が {2} のデータは、すでに登録されています。 */
    String DATA_DUPLICATE = "data.duplicate";
    /** メッセージコード：{0} に {1} が {2} のデータは、存在しません。 */
    String DATA_NOTFOUND = "data.notFound";
    /** メッセージコード：{0} の {1} が {2} のデータは、すでに更新されています。 */
    String DATA_UPDATED = "data.updated";
    /** メッセージコード：{0} の {1} が {2} のデータは、更新されていません。 */
    String DATA_NOTUPDATED = "data.notUpdated";

}
