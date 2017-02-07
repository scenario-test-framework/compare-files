package me.suwash.tools.comparefiles.infra.config;

import java.awt.Rectangle;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.util.ImageCompareUtils.ConfirmImageStyle;
import me.suwash.util.validation.constraints.Charset;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * システム設定。
 */
@Getter
@Setter
@ToString
@lombok.extern.slf4j.Slf4j
public class CompareFilesConfig {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String leftFilePath;

    private String rightFilePath;

    @Charset
    private String defaultInputCharset;

    @NotEmpty
    private String outputDir;

    @NotEmpty
    @Charset
    private String outputCharset;

    private boolean isSorted;

    private int csvHeaderRow = Const.DEFAULT_INT_VALUE;

    private int csvDataStartRow = Const.DEFAULT_INT_VALUE;

    private boolean isWriteDiffOnly;

    @NotEmpty
    private String leftPrefix;

    @NotEmpty
    private String rightPrefix;

    @NotEmpty
    private String compareResultFileName;

    @NotEmpty
    private String compareDetailFilePrefix;

    private int chunkSize = Const.DEFAULT_INT_VALUE;

    private List<String> ignoreFileRegexList;

    private List<String> ignoreItemList;

    private List<Rectangle> ignoreAreaList;

    private String overwriteLayoutDir;

    private boolean isDeleteWorkDir;

    private String codeValueForOnlyOneRecordType;

    private ConfirmImageStyle okImageStyle;

    private ConfirmImageStyle ngImageStyle;

    /**
     * 指定パスのファイルをオブジェクトにパースします。
     *
     * @param filePath 設定ファイルパス
     * @param isClasspath クラスパス指定か？
     * @return パース後のオブジェクト
     */
    public static CompareFilesConfig parse(final String filePath, final boolean isClasspath) {
        CompareFilesConfig config = null;

        try {
            if (isClasspath) {
                final InputStream inStream = CompareFilesConfig.class.getResourceAsStream(filePath);
                final Reader reader = new InputStreamReader(inStream, Const.CHARSET_DEFAULT_CONFIG);
                config = MAPPER.readValue(reader, CompareFilesConfig.class);

            } else {
                config = MAPPER.readValue(new File(filePath), CompareFilesConfig.class);
            }
        } catch (Exception e) {
            throw new CompareFilesException(Const.MSGCD_ERROR_PARSE, new Object[] {filePath}, e);
        }

        return config;
    }

    /**
     * フィールドに初期値が設定されている項目を、指定されたデフォルト設定で上書きします。
     *
     * @param defaultConfig デフォルト設定
     */
    public void setDefault(final CompareFilesConfig defaultConfig) {
        final String leftFilePath = defaultConfig.getLeftFilePath();
        if (StringUtils.isEmpty(this.leftFilePath)) {
            this.leftFilePath = leftFilePath;
        }

        final String rightFilePath = defaultConfig.getRightFilePath();
        if (StringUtils.isEmpty(this.rightFilePath)) {
            this.rightFilePath = rightFilePath;
        }

        final String outputDir = defaultConfig.getOutputDir();
        if (StringUtils.isEmpty(this.outputDir)) {
            this.outputDir = outputDir;
        }

        final String outputCharset = defaultConfig.getOutputCharset();
        if (StringUtils.isEmpty(this.outputCharset)) {
            this.outputCharset = outputCharset;
        }

        final boolean isSorted = defaultConfig.isSorted();
        if (!this.isSorted) {
            this.isSorted = isSorted;
        }

        final int csvHeaderRow = defaultConfig.getCsvHeaderRow();
        if (this.csvHeaderRow == Const.DEFAULT_INT_VALUE) {
            this.csvHeaderRow = csvHeaderRow;
        }

        final int csvDataStartRow = defaultConfig.getCsvDataStartRow();
        if (this.csvDataStartRow == Const.DEFAULT_INT_VALUE) {
            this.csvDataStartRow = csvDataStartRow;
        }

        final boolean isWriteDiffOnly = defaultConfig.isWriteDiffOnly();
        if (!this.isWriteDiffOnly) {
            this.isWriteDiffOnly = isWriteDiffOnly;
        }

        final String leftPrefix = defaultConfig.getLeftPrefix();
        if (StringUtils.isEmpty(this.leftPrefix)) {
            this.leftPrefix = leftPrefix;
        }

        final String rightPrefix = defaultConfig.getRightPrefix();
        if (StringUtils.isEmpty(this.rightPrefix)) {
            this.rightPrefix = rightPrefix;
        }

        final String compareResultFileName = defaultConfig.getCompareResultFileName();
        if (StringUtils.isEmpty(this.compareResultFileName)) {
            this.compareResultFileName = compareResultFileName;
        }

        final String compareDetailFilePrefix = defaultConfig.getCompareDetailFilePrefix();
        if (StringUtils.isEmpty(this.compareDetailFilePrefix)) {
            this.compareDetailFilePrefix = compareDetailFilePrefix;
        }

        final int chunkSize = defaultConfig.getChunkSize();
        if (this.chunkSize == Const.DEFAULT_INT_VALUE) {
            this.chunkSize = chunkSize;
        }

        final List<String> ignoreFileRegexList = defaultConfig.getIgnoreFileRegexList();
        if (this.ignoreFileRegexList == null || this.ignoreFileRegexList.size() == 0) {
            this.ignoreFileRegexList = ignoreFileRegexList;
        }

        final List<String> ignoreItemList = defaultConfig.getIgnoreItemList();
        if (this.ignoreItemList == null || this.ignoreItemList.size() == 0) {
            this.ignoreItemList = ignoreItemList;
        }

        final List<Rectangle> ignoreAreaList = defaultConfig.getIgnoreAreaList();
        if (this.ignoreAreaList == null || this.ignoreAreaList.size() == 0) {
            this.ignoreAreaList = ignoreAreaList;
        }

        final String overwriteLayoutDir = defaultConfig.getOverwriteLayoutDir();
        if (StringUtils.isEmpty(this.overwriteLayoutDir)) {
            this.overwriteLayoutDir = overwriteLayoutDir;
        }

        final boolean isDeleteWorkDir = defaultConfig.isDeleteWorkDir();
        if (!this.isDeleteWorkDir) {
            this.isDeleteWorkDir = isDeleteWorkDir;
        }

        final String codeValueForOnlyOneRecordType = defaultConfig.getCodeValueForOnlyOneRecordType();
        if (StringUtils.isEmpty(this.codeValueForOnlyOneRecordType)) {
            this.codeValueForOnlyOneRecordType = codeValueForOnlyOneRecordType;
        }

        final ConfirmImageStyle okImageStyle = defaultConfig.getOkImageStyle();
        if (this.okImageStyle == null) {
            this.okImageStyle = okImageStyle;
        }

        final ConfirmImageStyle ngImageStyle = defaultConfig.getNgImageStyle();
        if (this.ngImageStyle == null) {
            this.ngImageStyle = ngImageStyle;
        }
    }

    /**
     * 保持している設定をロガー出力します。
     */
    public void printDetails() {
        log.info("・設定");
        log.info("  ・比較情報");
        log.info("    ・ソート済か？                  : " + isSorted);
        log.info("    ・レイアウト配置ディレクトリ    : " + overwriteLayoutDir);
        log.info("    ・除外ファイル名正規表現        : " + ignoreFileRegexList);
        log.info("    ・共通除外項目                  : " + ignoreItemList);
        log.info("    ・共通除外エリア                : " + ignoreAreaList);
        log.info("    ・CSV/TSVヘッダー行番号         : " + csvHeaderRow);
        log.info("    ・CSV/TSVデータ開始行番号       : " + csvDataStartRow);
        log.info("  ・出力情報");
        log.info("    ・出力ディレクトリ              : " + outputDir);
        log.info("    ・出力文字コード                : " + outputCharset);
        log.info("    ・作業ディレクトリを削除するか？: " + isDeleteWorkDir);
        log.info("    ・出力バッファ行数              : " + chunkSize);
        log.info("    ・結果ファイル名                : " + compareResultFileName);
        log.info("    ・詳細ファイル");
        log.info("      ・詳細ファイル名プリフィックス: " + compareDetailFilePrefix);
        log.info("      ・差分のみ出力するか？        : " + isWriteDiffOnly);
        log.info("      ・左差分プリフィックス        : " + leftPrefix);
        log.info("      ・右差分プリフィックス        : " + rightPrefix);
    }

}
