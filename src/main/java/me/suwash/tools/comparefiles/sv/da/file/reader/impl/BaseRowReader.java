package me.suwash.tools.comparefiles.sv.da.file.reader.impl;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.FileFormat;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.config.ItemLayout;
import me.suwash.tools.comparefiles.infra.config.RecordLayout;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.da.file.reader.RowReader;
import me.suwash.tools.comparefiles.sv.domain.BaseRow;
import me.suwash.util.FileUtils;

import org.apache.commons.lang3.StringUtils;

/**
 * テキストファイルを行データに変換して読込むReaderの基底クラス。
 *
 * @param <R> 行データクラス
 */
public abstract class BaseRowReader<R extends BaseRow> extends FileReader implements RowReader<R> {

    private static final int RECORDLIST_SIZE_ONLY_ONE_LAYOUT = 1;

    /** 無変換行データ受け渡し用キー。 */
    protected static final String KEY_RAWLINE = "__raw-line__";

    /** Reader。 */
    protected Closeable reader;

    /** 対象ファイルパス。 */
    protected String filePath;

    /** 現在行。 */
    protected long curRowNum;

    /** レイアウト設定。 */
    protected FileLayout fileLayout;

    /** 変換対象クラス。 */
    protected Class<R> targetType;

    /**
     * コンストラクタ。
     *
     * @param filePath 入力ファイルパス
     * @param charset 文字コード
     * @param layout ファイルレイアウト
     * @param dummy 型引数ダミー値
     * @throws FileNotFoundException ファイルが存在しない場合
     */
    @SuppressWarnings("unchecked")
    protected BaseRowReader(final String filePath, final String charset, final FileLayout layout, final R... dummy) throws FileNotFoundException {
        super(filePath);

        // 入力ファイルパス
        if (StringUtils.isEmpty(filePath)) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"filePath"});
        }
        this.filePath = filePath;

        // 文字コード
        if (StringUtils.isEmpty(charset)) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"charset"});
        }

        // 入力ファイル読み込みチェック
        FileUtils.readCheck(filePath, charset);

        // レイアウト設定
        this.fileLayout = layout;

        // 変換対象クラス
        final Class<R> targetType = (Class<R>) dummy.getClass().getComponentType();
        this.targetType = targetType;

        // 初期化処理の実行
        final File file = new File(filePath);
        this.reader = getReader(file, charset);
    }

    /**
     * リーダーを返します。
     *
     * @param file 入力ファイル
     * @param charset 文字コード
     * @return リーダー
     */
    protected abstract Closeable getReader(final File file, final String charset);

    /*
     * (非 Javadoc)
     * @see tool.comparefiles.dataaccess.RowReader#next()
     */
    @Override
    public R next() {
        final Map<String, ?> targetLineMap = readLine();
        if (targetLineMap == null) {
            return null;
        } else {
            String rawLine = null;
            final Object rawLineObj = targetLineMap.get(KEY_RAWLINE);
            if (rawLineObj != null) {
                rawLine = rawLineObj.toString();
            }
            targetLineMap.remove(KEY_RAWLINE);
            return parse(targetLineMap, rawLine);
        }
    }

    /**
     * 行データをMap形式で返します。
     * ファイルのEOFに到達した場合、nullを返します。
     *
     * @return 行データMap
     */
    protected abstract Map<String, ?> readLine();

    /**
     * 行データマップを対象クラスに変換します。
     *
     * @param targetLineMap 対象行マップ
     * @param rawLine 無変換行データ
     * @return 対象行オブジェクト
     */
    @SuppressWarnings("unchecked")
    protected R parse(final Map<String, ?> targetLineMap, final String rawLine) {
        // 変換後オブジェクト
        R parsed = null;
        try {
            parsed = targetType.newInstance();
        } catch (Exception e) {
            throw new CompareFilesException(
                Const.MSGCD_ERROR_CANT_NEW_INSTANCE,
                new Object[] {targetType.getSimpleName(), e.getMessage()},
                e);
        }

        // レイアウト設定の判定
        parsed.setRowNum(curRowNum);
        if (fileLayout == null || FileFormat.Text.equals(fileLayout.getFileFormat())) {
            // --------------------------------------------------------------------------------
            // レイアウト設定が未登録 or テキスト形式 の場合、全て1つの比較項目として保持
            // --------------------------------------------------------------------------------
            final HashMap<String, Object> dummyKeyMap = new HashMap<String, Object>();
            dummyKeyMap.put("dummy", null);
            parsed.setKeyMap(dummyKeyMap);
            parsed.setValueMap((Map<String, Object>) targetLineMap);

        } else {
            // --------------------------------------------------------------------------------
            // レイアウト設定が登録されている場合、比較キーとその他項目を分割して保持
            // --------------------------------------------------------------------------------
            // レコードレイアウトの判定
            RecordLayout recordLayout = null;
            if (fileLayout.getRecordList().isEmpty()) {
                throw new CompareFilesException(
                    Const.CHECK_NOSET,
                    new Object[] {fileLayout.getLogicalFileName(), "recordList"});

            } else if (fileLayout.getRecordList().size() == RECORDLIST_SIZE_ONLY_ONE_LAYOUT) {
                recordLayout = fileLayout.getRecordList().get(0);

            } else {
                for (final RecordLayout curRecordConfig : fileLayout.getRecordList()) {
                    // レコードレイアウトの判定
                    if (isMatchRecordType(curRecordConfig, targetLineMap)) {
                        recordLayout = curRecordConfig;
                        break;
                    }
                }
            }

            // レコードレイアウトの判定結果を確認
            if (recordLayout == null) {
                throw new CompareFilesException(
                    Const.MSGCD_ERROR_COMPARE_FILE_TEXT_ROW_LAYOUT_NOTFOUND,
                    new Object[] {fileLayout.getLogicalFileName(), this.curRowNum + ":" + targetLineMap});
            }

            // レコードレイアウトに合わせて変換後オブジェクトに設定
            final Map<String, Object> keyMap = new LinkedHashMap<String, Object>();
            final Map<String, Object> valueMap = new LinkedHashMap<String, Object>();
            parsed.setKeyMap(keyMap);
            parsed.setValueMap(valueMap);
            for (final ItemLayout curItemConfig : recordLayout.getItemList()) {
                if (curItemConfig.isCompareKey()) {
                    putContent(curItemConfig.getId(), targetLineMap, keyMap);
                } else {
                    putContent(curItemConfig.getId(), targetLineMap, valueMap);
                }
            }
        }

        // 無変換行データを設定
        parsed.setRawLine(rawLine);
        return parsed;
    }

    /**
     * 指定したレコードレイアウトが、パース済みの行データにマッチするか否かを返します。
     *
     * @param recordLayout 対象レコードレイアウト
     * @param targetLineMap パース済みの行データ
     * @return マッチする場合、true
     */
    protected abstract boolean isMatchRecordType(RecordLayout recordLayout, Map<String, ?> targetLineMap);

    /**
     * 項目IDを指定して、Map内の値を転記します。
     *
     * @param itemId 項目ID
     * @param fromMap 転記元Map
     * @param toMap 転記先Map
     */
    protected abstract void putContent(String itemId, Map<String, ?> fromMap, Map<String, Object> toMap);

    /*
     * (非 Javadoc)
     * @see java.io.InputStreamReader#close()
     */
    @Override
    public void close() throws IOException {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (Exception e) {
            throw new CompareFilesException(
                Const.STREAM_CANTCLOSE_INPUT,
                new Object[] {filePath},
                e);
        }
    }

    /*
     * (非 Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return filePath;
    }

}
