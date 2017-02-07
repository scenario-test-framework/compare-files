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
     * 起動パラメータでシステム設定を上書きします。。
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

//        if (string.charAt(0) == '\'' && string.charAt(string.length() - 1) == '\'' ||
//            string.charAt(0) == '\"' && string.charAt(string.length() - 1) == '\"') {
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
}
