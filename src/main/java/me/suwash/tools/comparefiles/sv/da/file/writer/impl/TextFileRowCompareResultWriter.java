package me.suwash.tools.comparefiles.sv.da.file.writer.impl;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.RecordType;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.config.ItemLayout;
import me.suwash.tools.comparefiles.infra.config.RecordLayout;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.domain.compare.file.text.TextFileItemCompareResult;
import me.suwash.tools.comparefiles.sv.domain.compare.file.text.TextFileRowCompareResult;
import me.suwash.util.CompareUtils.CompareStatus;
import me.suwash.util.CsvUtils;

import com.orangesignal.csv.CsvWriter;
import com.orangesignal.csv.io.CsvColumnNameMapWriter;

/**
 * 行比較結果Writer。
 */
public class TextFileRowCompareResultWriter extends BaseResultWriter<TextFileRowCompareResult> {

    private static final boolean IS_WRITE_HEADER = true;

    /** 差分のみ出力フラグ。 */
    private final boolean isWriteDiffOnly;
    /** ヘッダー書き出し済フラグ。 */
    private boolean isHeaderWrited;

    private final String prefixLeft;
    private final String prefixRight;

    /**
     * コンストラクタ。
     *
     * @param filePath 出力ファイルパス
     * @param charset 出力文字コード
     * @param isWriteDiffOnly 差分のみ出力フラグ
     * @param prefixLeft 左プリフィックス
     * @param prefixRight 右プリフィックス
     * @throws IOException ファイルにアクセスできない場合
     */
    public TextFileRowCompareResultWriter(
        final String filePath,
        final String charset,
        final boolean isWriteDiffOnly,
        final String prefixLeft,
        final String prefixRight)
        throws IOException {

        super(filePath, charset);
        this.isWriteDiffOnly = isWriteDiffOnly;
        this.prefixLeft = prefixLeft;
        this.prefixRight = prefixRight;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.writer.impl.BaseResultWriter#getWriter(java.io.File, java.lang.String)
     */
    @Override
    protected Closeable getWriter(final File file, final String charset) {
        CsvColumnNameMapWriter writer = null;
        try {
            writer = new CsvColumnNameMapWriter(
                new CsvWriter(
                    new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(file), charset)
                    ),
                    CsvUtils.getCsvConfig()
                ),
                IS_WRITE_HEADER
                );
        } catch (Exception e) {
            throw new CompareFilesException(Const.STREAM_CANTOPEN_OUTPUT, new Object[] {filePath}, e);
        }
        return writer;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.writer.impl.BaseResultWriter#writeRow(java.lang.Object)
     */
    @Override
    protected void writeRow(final TextFileRowCompareResult row) {
        final CsvColumnNameMapWriter csvWriter = (CsvColumnNameMapWriter) writer;

        // --------------------------------------------------------------------------------
        // ヘッダー行出力
        // --------------------------------------------------------------------------------
        if (!isHeaderWrited) {
            final Map<String, String> headerMap = TextFileCompareResultOutput.getHeaderMap(row);
            try {
                csvWriter.writeHeader(headerMap);
            } catch (IOException e) {
                throw new CompareFilesException(
                    Const.FILE_CANTWRITE,
                    new Object[] {filePath + "#header" + "、content：" + headerMap},
                    e);
            }
            // ヘッダー書き出し済フラグを立てる
            isHeaderWrited = true;
            // 出力行番号をインクリメント
            outRowNum++;
        }

        // --------------------------------------------------------------------------------
        // 出力設定確認
        // --------------------------------------------------------------------------------
        if (isWriteDiffOnly && CompareStatus.OK.equals(row.getStatus())) {
            return;
        }

        // --------------------------------------------------------------------------------
        // データ行出力
        // --------------------------------------------------------------------------------
        // 出力行番号をインクリメント
        outRowNum++;
        // データ行を取得
        final Map<String, String> map = TextFileCompareResultOutput.getDataRecordMap(row, prefixLeft, prefixRight);
        // 出力
        try {
            csvWriter.write(map);
        } catch (IOException e) {
            throw new CompareFilesException(
                Const.FILE_CANTWRITE,
                new Object[] {filePath + "#" + outRowNum + "、result：" + row},
                e);
        }

    }

    /**
     * 出力ファイルレイアウト。
     */
    private static final class TextFileCompareResultOutput {

        private static final String COLUMN_NAME_STATUS = "Status";
        private static final String COLUMN_NAME_ROWNUM = "RowNum";
        private static final String COLUMN_NAME_DIFFITEMS = "DiffItems";

        /**
         * コンストラクタ。
         */
        private TextFileCompareResultOutput() {
            super();
        }

        /**
         * ヘッダー行Mapを返します。
         *
         * @param result テキストファイル比較結果
         * @return ヘッダー行Map
         */
        public static Map<String, String> getHeaderMap(final TextFileRowCompareResult result) {
            final Map<String, String> headerMap = new LinkedHashMap<String, String>();

            // --------------------------------------------------
            // 固定項目の登録
            // --------------------------------------------------
            // ステータス
            headerMap.put(COLUMN_NAME_STATUS, COLUMN_NAME_STATUS);
            // 比較した行番号
            headerMap.put(COLUMN_NAME_ROWNUM, COLUMN_NAME_ROWNUM);
            // 差分項目名リスト
            headerMap.put(COLUMN_NAME_DIFFITEMS, COLUMN_NAME_DIFFITEMS);

            // --------------------------------------------------
            // 変動項目の設定
            // --------------------------------------------------
            final FileLayout fileLayout = result.getFileLayout();
            if (fileLayout == null || fileLayout.getRecordList().size() <= 1) {
                // 単一レコードタイプの場合、出力行から項目名を設定
                for (final TextFileItemCompareResult item : result.getItemList()) {
                    final RecordType recordType = result.getRecordType();
                    final String columnName = getDynamicColumnName(fileLayout, recordType, item.getName());
                    headerMap.put(columnName, columnName);
                }
            } else {
                // 複数レコードタイプの場合、全てのレコードタイプから項目名を設定
                for (final RecordLayout recordLayout : fileLayout.getRecordList()) {
                    for (final ItemLayout itemLayout : recordLayout.getItemList()) {
                        final RecordType recordType = recordLayout.getType();
                        final String columnName = getDynamicColumnName(fileLayout, recordType, itemLayout.getName());
                        headerMap.put(columnName, columnName);
                    }
                }
            }
            return headerMap;
        }

        /**
         * データ行Mapを返します。
         *
         * @param result テキストファイル比較結果
         * @param prefixLeft 左差分プリフィックス
         * @param prefixRight 右差分プリフィックス
         * @return データ行Map
         */
        public static Map<String, String> getDataRecordMap(final TextFileRowCompareResult result, final String prefixLeft, final String prefixRight) {
            // データ出力用Map
            final Map<String, String> map = new LinkedHashMap<String, String>();

            // --------------------------------------------------
            // 固定項目の登録
            // --------------------------------------------------
            // ステータス
            final CompareStatus status = result.getStatus();
            map.put(COLUMN_NAME_STATUS, status.toString());

            // 比較した行番号
            map.put(COLUMN_NAME_ROWNUM, getDiffContent(prefixLeft, prefixRight, result.getLeftRowNum(), result.getRightRowNum()));

            // 差分項目名リスト
            if (CompareStatus.NG.equals(status)) {
                map.put(COLUMN_NAME_DIFFITEMS, result.getDiffItemNameList().toString());
            } else {
                map.put(COLUMN_NAME_DIFFITEMS, Const.DUMMY_VALUE);
            }

            // --------------------------------------------------
            // 変動項目の設定
            // --------------------------------------------------
            final FileLayout fileLayout = result.getFileLayout();
            final RecordType recordType = result.getRecordType();
            for (final TextFileItemCompareResult item : result.getItemList()) {
                final String columnName = getDynamicColumnName(fileLayout, recordType, item.getName());
                switch (item.getStatus()) {
                    case OK:
                        map.put(columnName, item.getLeftValue());
                        break;
                    case NG:
                    case Ignore:
                    case LeftOnly:
                    case RightOnly:
                        map.put(columnName, getDiffContent(prefixLeft, prefixRight, item.getLeftValue(), item.getRightValue()));
                        break;
                    default:
                        throw new CompareFilesException(
                            Const.MSGCD_ERROR_COMPARE_ILLEGAL_STATUS,
                            new Object[] {item.getStatus()});
                }
            }
            return map;
        }

        /**
         * ファイルレイアウト、レコードタイプ、項目名から、カラム名を判断します。
         *
         * @param fileLayout ファイルレイアウト
         * @param recordType レコードタイプ
         * @param itemName 項目名
         * @return カラム名
         */
        private static String getDynamicColumnName(final FileLayout fileLayout, final RecordType recordType, final String itemName) {
            String columnName = null;
            if (fileLayout == null || fileLayout.getRecordList().size() <= 1) {
                columnName = itemName;
            } else {
                columnName = recordType.name() + "." + itemName;
            }
            return columnName;
        }

        /**
         * 差分項目の出力文言を返します。
         *
         * @param prefixLeft 左差分プリフィックス
         * @param prefixRight 右差分プリフィックス
         * @param leftValue 左ファイルの値
         * @param rightValue 右ファイルの値
         * @return 差分項目の出力文言
         */
        private static String getDiffContent(final String prefixLeft, final String prefixRight, final Object leftValue, final Object rightValue) {
            String left = "null";
            if (leftValue != null) {
                left = leftValue.toString();
            }

            String right = "null";
            if (rightValue != null) {
                right = rightValue.toString();
            }

            return new StringBuilder()
                .append(prefixLeft).append(left).append('\n')
                .append(prefixRight).append(right)
                .toString();
        }

    }
}
