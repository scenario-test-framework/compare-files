package me.suwash.tools.comparefiles.main;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;

import org.apache.commons.lang3.StringUtils;

import com.beust.jcommander.Parameter;

/**
 * コマンドラインオプション。
 */
@Getter
public class CompareFilesOption {

    @Parameter(names = {"-h", "--help"}, help = true)
    private boolean help;

    /** 設定ファイルパス。 */
    @Parameter(names = {"-config", "--configFile"})
    private String configFilePath;

    /** workディレクトリを削除するか。 */
    @Parameter(names = {"-d", "--deleteWorkDir"})
    private boolean isDeleteWorkDir;

    /** 入力ファイル文字コード。 */
    @Parameter(names = {"-ic", "--inputCharset"})
    private String inputCharset;

    /** 入力ファイルがソート済か。 */
    @Parameter(names = {"-s", "--sorted"})
    private boolean isSorted;

    /** CSV/TSVヘッダー行。 */
    @Parameter(names = {"-ch", "--csvHeaderRow"})
    private int csvHeaderRow;

    /** CSV/TSVデータ開始行。 */
    @Parameter(names = {"-cd", "--csvDataStartRow"})
    private int csvDataStartRow;

    /** 出力ディレクトリ。 */
    @Parameter(names = {"-od", "--outputDir"})
    private String outputDir;

    /** 出力ファイル名。 */
    @Parameter(names = {"-of", "--compareResultFileName"})
    private String compareResultFileName;

    /** 出力文字コード。 */
    @Parameter(names = {"-oc", "--outputCharset"})
    private String outputCharset;

    /** 詳細出力ファイル：差分のみ出力するか。 */
    @Parameter(names = {"-wdo", "--writeDiffOnly"})
    private boolean isWriteDiffOnly;

    /** 詳細出力ファイル：左差分プリフィックス。 */
    @Parameter(names = {"-dpl", "--leftPrefix"})
    private String leftPrefix;

    /** 詳細出力ファイル：右差分プリフィックス。 */
    @Parameter(names = {"-dpr", "--rightPrefix"})
    private String rightPrefix;

    /** 詳細出力ファイル：ファイル名プリフィックス。 */
    @Parameter(names = {"-dfp", "--compareDetailFilePrefix"})
    private String compareDetailFilePrefix;

    /** 出力バッファ行数。 */
    @Parameter(names = {"-chunk", "--chunkSize"})
    private int chunkSize;

    /** 共通除外項目名リスト。 */
    @Parameter(names = {"-ignore", "--ignoreItemList"})
    private List<String> ignoreItemList;

    /** 上書きレイアウトファイル配置ディレクトリリスト。 */
    @Parameter(names = {"-layout", "--overwriteLayoutDir"})
    private String overwriteLayoutDir;

    /** その他のパラメータ。 */
    @Parameter
    private final List<String> paramList = new ArrayList<String>();

    /**
     * 起動パラメータでシステム設定を上書きします。
     *
     * @param systemConfig システム設定
     */
    protected void overwriteConfig(final CompareFilesConfig systemConfig) {
        // コマンドライン引数で値が設定されている場合、設定を上書き
        final boolean isDeleteWorkDir = this.isDeleteWorkDir();
        if (isDeleteWorkDir) {
            systemConfig.setDeleteWorkDir(isDeleteWorkDir);
        }

        final String inputCharset = this.getInputCharset();
        if (!StringUtils.isEmpty(inputCharset)) {
            systemConfig.setDefaultInputCharset(inputCharset);
        }

        final boolean isSorted = this.isSorted();
        if (isSorted) {
            systemConfig.setSorted(isSorted);
        }

        final int csvHeaderRow = this.getCsvHeaderRow();
        if (csvHeaderRow != 0) {
            systemConfig.setCsvHeaderRow(csvHeaderRow);
        }

        final int csvDataStartRow = this.getCsvDataStartRow();
        if (csvDataStartRow != 0) {
            systemConfig.setCsvDataStartRow(csvDataStartRow);
        }

        final String outputDir = escapeQuote(this.getOutputDir());
        if (!StringUtils.isEmpty(outputDir)) {
            systemConfig.setOutputDir(outputDir);
        }

        final String compareResultFileName = escapeQuote(this.getCompareResultFileName());
        if (!StringUtils.isEmpty(compareResultFileName)) {
            systemConfig.setCompareResultFileName(compareResultFileName);
        }

        final String outputCharset = escapeQuote(this.getOutputCharset());
        if (!StringUtils.isEmpty(outputCharset)) {
            systemConfig.setOutputCharset(outputCharset);
        }

        final boolean isWriteDiffOnly = this.isWriteDiffOnly();
        if (isWriteDiffOnly) {
            systemConfig.setWriteDiffOnly(isWriteDiffOnly);
        }

        final String leftPrefix = escapeQuote(this.getLeftPrefix());
        if (!StringUtils.isEmpty(leftPrefix)) {
            systemConfig.setLeftPrefix(leftPrefix);
        }

        final String rightPrefix = escapeQuote(this.getRightPrefix());
        if (!StringUtils.isEmpty(rightPrefix)) {
            systemConfig.setRightPrefix(rightPrefix);
        }

        final String compareDetailFilePrefix = escapeQuote(this.getCompareDetailFilePrefix());
        if (!StringUtils.isEmpty(compareDetailFilePrefix)) {
            systemConfig.setCompareDetailFilePrefix(compareDetailFilePrefix);
        }

        final int chunkSize = this.getChunkSize();
        if (chunkSize != 0) {
            systemConfig.setChunkSize(chunkSize);
        }

        final List<String> ignoreItemList = this.getIgnoreItemList();
        if (ignoreItemList != null && !ignoreItemList.isEmpty()) {
            systemConfig.setIgnoreItemList(ignoreItemList);
        }

        final String overwriteLayoutDir = escapeQuote(this.getOverwriteLayoutDir());
        if (!StringUtils.isEmpty(overwriteLayoutDir)) {
            systemConfig.setOverwriteLayoutDir(overwriteLayoutDir);
        }
    }

    /**
     * クォート、ダブルクォートで括られた文字列から、括り文字を除去します。
     *
     * @param string クォート、ダブルクォートで括られた文字列
     * @return 括り文字を除去した文字列
     */
    protected static String escapeQuote(final String string) {
        if (string == null) {
            return null;
        }

        if (string.charAt(0) == '\'' && string.charAt(string.length() - 1) == '\'') {
            return string.substring(1, string.length() - 1);

        } else {
            return string;
        }
    }

    /**
     * 引数で指定された設定ファイルパスを返します。
     * ※mainクラスから直接アクセスされるため、public公開しています。
     *
     * @return 設定ファイルパス
     */
    protected String getConfigFilePath() {
        return CompareFilesOption.escapeQuote(this.configFilePath);
    }

    /**
     * 引数で指定された入力ファイル文字コードを返します。
     *
     * @return 入力ファイル文字コード
     */
    protected String getInputCharset() {
        return CompareFilesOption.escapeQuote(this.inputCharset);
    }

    /**
     * UsageのOPTION分メッセージを返します。
     *
     * @return UsageのOptions分
     */
    protected String usage() {
        final StringBuilder usageBuilder = new StringBuilder(2048);
        usageBuilder
            .append("  Options:").append(StringUtils.LF)
            .append("    -h, --help").append(StringUtils.LF)
            .append("      このメッセージを表示します。").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -config, --configFile").append(StringUtils.LF)
            .append("      設定ファイルのパスを指定します。").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    ----- 以降の設定を指定すると、設定ファイルより優先されます。 -----").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -ignore, --ignoreItemList").append(StringUtils.LF)
            .append("      比較除外項目名をカンマ区切りで指定します。").append(StringUtils.LF)
            .append("      レイアウト定義の設定を上書きして、指定された項目の比較を除外します。").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -layout, --overwriteLayoutDir").append(StringUtils.LF)
            .append("      ${comparefiles_root}/config/compareFilesLayout 配下のレイアウト定義に加えて、指定したディレクトリ配下のレイアウト定義を読込みます。").append(StringUtils.LF)
            .append("      同一の物理ファイル名正規表現を持つレイアウトが存在する場合、後勝ち で上書きします。").append(StringUtils.LF)
            .append("      ※例）config配下に「定義書から自動生成したレイアウト」を配置。").append(StringUtils.LF)
            .append("         テストで「共通適用するレイアウト」、「今回のケースだけに適用するレイアウト」").append(StringUtils.LF)
            .append("         を別ディレクトリで管理。実行時に上記の記載順に指定する。 など").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -d, --deleteWorkDir").append(StringUtils.LF)
            .append("      作業ディレクトリを削除する場合に指定します。").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -s, --sorted").append(StringUtils.LF)
            .append("      比較対象ファイルが、比較キー項目でソート済の場合に指定します。").append(StringUtils.LF)
            .append("      内部的にソート後に、比較処理を行っているため、事前にソートされていると高速化が期待できます。").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -ch, --csvHeaderRow").append(StringUtils.LF)
            .append("      CSV / TSV ファイルの比較時に必須です。").append(StringUtils.LF)
            .append("      ヘッダー行番号を指定します。").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -cd, --csvDataStartRow").append(StringUtils.LF)
            .append("      CSV / TSV ファイルの比較時に必須です。").append(StringUtils.LF)
            .append("      データ開始行番号を指定します。").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -od, --outputDir").append(StringUtils.LF)
            .append("      出力ディレクトリを指定します。").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -oc, --outputCharset").append(StringUtils.LF)
            .append("      出力ファイルの文字コードを指定します。").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -of, --compareResultFileName").append(StringUtils.LF)
            .append("      比較結果ファイル名を指定します。").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -dfp, --compareDetailFilePrefix").append(StringUtils.LF)
            .append("      比較詳細ファイル名のプリフィックスを指定します。").append(StringUtils.LF)
            .append("      ファイル名の形式は ${プリフィックス}${比較対象ファイル名}.csv です。").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -ddo, --writeDiffOnly").append(StringUtils.LF)
            .append("      比較詳細ファイルの出力内容を 差分のみ にする場合に指定します。").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -dpl, --leftPrefix").append(StringUtils.LF)
            .append("      比較詳細ファイルの差分出力で 左ファイルの項目を表示するときに利用するプリフィックスを指定します。").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -dpr, --rightPrefix").append(StringUtils.LF)
            .append("      比較詳細ファイルの差分出力で 右ファイルの項目を表示するときに利用するプリフィックスを指定します。").append(StringUtils.LF)
            .append(StringUtils.LF)
            .append("    -chunk, --chunkSize").append(StringUtils.LF)
            .append("      出力バッファサイズを行数で指定します。").append(StringUtils.LF)
            .append("      比較結果の出力時に、逐次で出力するとFileI/Oで処理時間がかさむためバッファリングしています。").append(StringUtils.LF)
            .append("      リソースや比較対象のデータサイズに合わせてサイズを調整することで高速化が期待できます。").append(StringUtils.LF)
            .append(StringUtils.LF);
        return usageBuilder.toString();
    }
}
