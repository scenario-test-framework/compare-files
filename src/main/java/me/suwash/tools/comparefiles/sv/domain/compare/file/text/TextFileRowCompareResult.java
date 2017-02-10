package me.suwash.tools.comparefiles.sv.domain.compare.file.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.FileFormat;
import me.suwash.tools.comparefiles.infra.classification.RecordType;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.config.RecordLayout;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.infra.i18n.CompareFilesMessageSource;
import me.suwash.util.CompareUtils.CompareCriteria;
import me.suwash.util.CompareUtils.CompareStatus;

/**
 * 行単位の比較結果。
 */
@lombok.extern.slf4j.Slf4j
public class TextFileRowCompareResult {

    private static final int RECORDLIST_SIZE_ONLY_ONE_LAYOUT = 1;

    @Getter
    private CompareStatus status = CompareStatus.Processing;

    @Getter
    private FileLayout fileLayout;

    /** レコードタイプ。 */
    @Getter
    private RecordType recordType;

    /** 左ファイルの行番号。 */
    @Getter
    private long leftRowNum;

    /** 右ファイルの行番号。 */
    @Getter
    private long rightRowNum;

    /** 項目単位の比較結果リスト。 */
    @Getter
    private final List<TextFileItemCompareResult> itemList = new ArrayList<TextFileItemCompareResult>();

    private final ComparableRow leftRow;
    private final ComparableRow rightRow;

    /**
     * 固定値の行比較結果を返します。
     *
     * @param status 比較ステータス ※LeftOnly|RightOnly
     * @param fileLayout ファイルレイアウト
     * @param leftRow 左ファイルの行データ
     * @param rightRow 右ファイルの行データ
     * @return 行比較結果
     */
    protected static TextFileRowCompareResult getFixedResult(
        final CompareStatus status,
        final FileLayout fileLayout,
        final ComparableRow leftRow,
        final ComparableRow rightRow) {

        // 必須チェック
        if (status == null) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {
                "status"
            });
        }

        // 行番号判定
        long leftRowNum = -1;
        if (leftRow != null) {
            leftRowNum = leftRow.getRowNum();
        }
        long rightRowNum = -1;
        if (rightRow != null) {
            rightRowNum = rightRow.getRowNum();
        }

        // 結果に登録する行データを判定
        ComparableRow curRow = leftRow;
        if (!CompareStatus.LeftOnly.equals(status)) {
            curRow = rightRow;
        }

        // 固定値を作成
        return new TextFileRowCompareResult(
            fileLayout,
            leftRowNum,
            rightRowNum,
            curRow,
            status);
    }

    /**
     * AggregateEntityからの取得用コンストラクタ。
     *
     * @param fileLayout ファイルレイアウト
     * @param leftRow 左ファイルの行データ
     * @param rightRow 右ファイルの行データ
     */
    protected TextFileRowCompareResult(
        final FileLayout fileLayout,
        final ComparableRow leftRow,
        final ComparableRow rightRow) {

        super();
        this.fileLayout = fileLayout;
        this.leftRow = leftRow;
        this.rightRow = rightRow;
    }

    /**
     * 固定値取得用コンストラクタ。
     *
     * @param fileLayout ファイルレイアウト
     * @param leftRowNum 左ファイル行番号
     * @param rightRowNum 右ファイル行番号
     * @param row 行データ
     * @param status 比較ステータス
     */
    private TextFileRowCompareResult(
        final FileLayout fileLayout,
        final long leftRowNum,
        final long rightRowNum,
        final ComparableRow row,
        final CompareStatus status) {

        this(fileLayout, null, null);
        this.leftRowNum = leftRowNum;
        this.rightRowNum = rightRowNum;
        this.status = status;
        updateFixedItems(status, row);
    }

    /**
     * 固定値をフィールドに設定します。
     *
     * @param status 比較ステータス ※LeftOnly|RightOnly
     * @param row 行データ
     */
    private void updateFixedItems(
        final CompareStatus status,
        final ComparableRow row) {

        // キー項目の設定
        for (final Map.Entry<String, Object> entry : row.getKeyMap().entrySet()) {
            setFixedItem(entry.getKey(), entry.getValue(), status);
        }
        // バリュー項目の設定
        if (row.getValueMap() != null) {
            for (final Map.Entry<String, Object> entry : row.getValueMap().entrySet()) {
                setFixedItem(entry.getKey(), entry.getValue(), status);
            }
        }
    }

    /**
     * 行比較結果に、項目情報群を設定します。
     *
     * @param itemId 項目ID
     * @param itemValue 項目設定値
     * @param status 比較ステータス ※LeftOnly|RightOnly
     */
    private void setFixedItem(final String itemId, final Object itemValue, final CompareStatus status) {
        TextFileItemCompareResult item = null;

        String value = "null";
        if (itemValue != null) {
            value = itemValue.toString();
        }

        if (CompareStatus.LeftOnly.equals(status)) {
            item = TextFileItemCompareResult.getFixedResult(itemId, value, null, null, status);
        } else {
            item = TextFileItemCompareResult.getFixedResult(itemId, null, value, null, status);
        }

        itemList.add(item);
    }

    /**
     * ファイルレイアウトから、行文字列がマッチするレコードレイアウト.比較条件マップ＜項目名, 比較条件＞を返します。
     *
     * @param fileLayout ファイルレイアウト
     * @param rawLine 行文字列
     * @return 比較条件マップ
     */
    private Map<String, CompareCriteria> getCriteriaMap(final FileLayout fileLayout, final String rawLine) {
        // 行文字列から、レコードタイプを取得
        final RecordType recordType = getRecordType(fileLayout, rawLine);
        if (recordType == null) {
            return null;
        }

        // レコードタイプコードがマッチするレイアウトの条件Map
        final List<RecordLayout> recordLayoutList = fileLayout.getRecordList();
        for (final RecordLayout curRecordLayout : recordLayoutList) {
            if (recordType.equals(curRecordLayout.getType())) {
                return curRecordLayout.getCriteriaMap();
            }
        }

        // マッチしなかった場合、エラー
        throw new CompareFilesException(
            Const.MSGCD_ERROR_COMPARE_FILE_TEXT_ROW_LAYOUT_NOTFOUND,
            new Object[] {
                fileLayout.getLogicalFileName(),
                rawLine});
    }

    /**
     * ファイルレイアウトと現在行文字列から、レコードタイプを返します。
     *
     * @param fileLayout ファイルレイアウト
     * @param rawLine 行文字列
     * @return レコードタイプ
     */
    private RecordType getRecordType(final FileLayout fileLayout, final String rawLine) {
        // ファイルレイアウトチェック
        if (fileLayout == null) {
            // 設定されていない場合、レコードタイプなし
            return null;
        }

        // レコードレイアウトチェック
        final List<RecordLayout> recordLayoutList = fileLayout.getRecordList();
        if (recordLayoutList == null || recordLayoutList.isEmpty()) {
            // 設定されていない場合、レコードタイプなし
            return null;

        } else if (recordLayoutList.size() == RECORDLIST_SIZE_ONLY_ONE_LAYOUT) {
            // 1件だけ設定されている場合、設定されているレコードタイプ
            return recordLayoutList.get(0).getType();
        }

        // 固定レコードタイプ判定
        if (fileLayout.getFileFormat().equals(FileFormat.CSV_withHeader) || fileLayout.getFileFormat().equals(FileFormat.TSV_withHeader)) {
            // ヘッダ有のCSV,TSVの場合、レコードタイプにデータを設定
            return RecordType.Data;
        }

        // レコードレイアウトが複数件設定されている場合、レコードタイプコードがマッチするレイアウト
        for (final RecordLayout recordLayout : recordLayoutList) {
            final String codeValue = recordLayout.getCodeValue();
            if (codeValue == null) {
                throw new CompareFilesException(
                    Const.MSGCD_ERROR_COMPARE_FILE_TEXT_ROW_CODEVALUE_NOTFOUND,
                    new Object[] {
                        fileLayout.getLogicalFileName(),
                        recordLayout.getType()});
            }

            String rawLineCodeValue = null;
            switch (fileLayout.getFileFormat()) {
                case CSV_noHeader:
                case CSV_withHeader:
                case TSV_noHeader:
                case TSV_withHeader:
                    // CSV / TSVの場合、括り文字を除去してコード値を取得
                    rawLineCodeValue = rawLine.replaceAll("\"", "").substring(0, codeValue.length());
                    break;
                default:
                    // その他のファイルフォーマットの場合、先頭からコード値を取得
                    rawLineCodeValue = rawLine.substring(0, codeValue.length());
                    break;
            }
            if (codeValue.equals(rawLineCodeValue)) {
                return recordLayout.getType();
            }
        }

        // レコードレイアウトが判断できない場合、データ行として扱う
        log.warn(
            CompareFilesMessageSource.getInstance().getMessage(
                Const.MSGCD_COMPARE_TEXT_ROW_LAYOUT_AS_DATA,
                new Object[] {fileLayout.getLogicalFileName(), rawLine}));
        return RecordType.Data;
    }

    /**
     * 差分を検出した項目IDのリストを返します。
     *
     * @return 差分を検出した項目名のリスト
     */
    public List<String> getDiffItemIdList() {
        final List<String> list = new ArrayList<String>();
        for (final TextFileItemCompareResult item : itemList) {
            if (!CompareStatus.OK.equals(item.getStatus()) && !CompareStatus.Ignore.equals(item.getStatus())) {
                list.add(item.getId());
            }
        }
        return list;
    }

    /**
     * 行の比較を実行します。
     */
    protected void compare() {
        // 条件Mapの確認
        final String leftRawLine = leftRow.getRawLine();
        final Map<String, CompareCriteria> criteriaMap = getCriteriaMap(fileLayout, leftRawLine);
        if (criteriaMap == null) {
            // --------------------------------------------------------------------------------
            // 条件Mapが指定されていない場合
            // --------------------------------------------------------------------------------
            // value1項目に1行のデータがまとめて入っている体で比較
            final String itemId = Const.DEFAULT_ITEM_ID;
            final String leftValue = leftRow.getItemValue(itemId);
            final String rightValue = rightRow.getItemValue(itemId);
            final CompareCriteria criteria = CompareCriteria.Equal;

            // 項目比較
            compareItem(itemId, leftValue, rightValue, criteria);

        } else {
            // --------------------------------------------------------------------------------
            // 条件Mapが指定されている場合
            // --------------------------------------------------------------------------------
            // 条件Mapをベースに比較
            for (final Map.Entry<String, CompareCriteria> curEntry : criteriaMap.entrySet()) {
                final String curItemId = curEntry.getKey();
                final String curLeftValue = leftRow.getItemValue(curItemId);
                final String curRightValue = rightRow.getItemValue(curItemId);
                final CompareCriteria curCriteria = curEntry.getValue();

                // 項目比較
                compareItem(curItemId, curLeftValue, curRightValue, curCriteria);
            }
        }

        // 比較ステータスの更新
        if (CompareStatus.Processing.equals(status)) {
            CompareStatus tempStatus = CompareStatus.OK;
            for (final TextFileItemCompareResult item : itemList) {
                if (CompareStatus.NG.equals(item.getStatus()) ||
                    CompareStatus.LeftOnly.equals(item.getStatus()) ||
                    CompareStatus.RightOnly.equals(item.getStatus())) {
                    tempStatus = CompareStatus.NG;
                    break;
                }
            }
            status = tempStatus;
        }

        // レコードタイプの更新
        this.recordType = getRecordType(fileLayout, leftRawLine);

        // 行番号の更新
        this.leftRowNum = leftRow.getRowNum();
        this.rightRowNum = rightRow.getRowNum();
        if (CompareStatus.LeftOnly.equals(status)) {
            // 左のみの場合、右の行番号に意味は無いのでクリア
            rightRowNum = Const.UNKNOWN_LINE;
        } else if (CompareStatus.RightOnly.equals(status)) {
            // 右のみの場合、左の行番号に意味は無いのでクリア
            leftRowNum = Const.UNKNOWN_LINE;
        }
    }

    /**
     * 項目を比較します。
     *
     * @param itemId 項目ID
     * @param leftValue 左ファイルの値
     * @param rightValue 右ファイルの値
     * @param criteria 比較条件
     */
    private void compareItem(
        final String itemId,
        final String leftValue,
        final String rightValue,
        final CompareCriteria criteria) {

        // 項目比較ステータス
        CompareStatus status = null;

        // nullチェック
        if (leftValue == null && rightValue == null) {
            // どちらもnull
            status = CompareStatus.OK;

        } else if (leftValue != null && rightValue == null) {
            // 左のみ
            status = CompareStatus.LeftOnly;

        } else if (leftValue == null && rightValue != null) {
            // 右のみ
            status = CompareStatus.RightOnly;

        }

        // nullチェック結果を確認
        TextFileItemCompareResult item = null;
        if (status == null) {
            // 項目比較ステータスが確定していない場合、比較を実行
            item = new TextFileItemCompareResult(itemId, leftValue, rightValue, criteria);
            item.compare();

        } else {
            // 項目比較ステータスが確定している場合、固定値を取得
            item = TextFileItemCompareResult.getFixedResult(itemId, leftValue, rightValue, criteria, status);
        }

        // 行比較結果に項目を追加
        itemList.add(item);
    }

}
