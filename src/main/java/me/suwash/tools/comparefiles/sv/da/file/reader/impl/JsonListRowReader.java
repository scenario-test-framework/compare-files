package me.suwash.tools.comparefiles.sv.da.file.reader.impl;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.FileFormat;
import me.suwash.tools.comparefiles.infra.classification.RecordPattern;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.config.RecordLayout;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.domain.BaseRow;
import me.suwash.util.JsonUtils;

/**
 * JsonList形式のファイルを、行データに変換するReader。
 * 改行区切りで、複数Jsonオブジェクトの形式。
 *
 * @param <R> 行データ
 */
public class JsonListRowReader<R extends BaseRow> extends BaseRowReader<R> {

    /**
     * コンストラクタ。
     *
     * @param filePath ファイルパス
     * @param charset 文字コード
     * @param layout ファイルレイアウト
     * @param dummy 型引数ダミー値
     * @throws FileNotFoundException ファイルが存在しない場合
     */
    @SafeVarargs
    public JsonListRowReader(final String filePath, final String charset, final FileLayout layout, final R... dummy) throws FileNotFoundException {
        super(filePath, charset, layout, dummy);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader#getReader(java.io.File, java.lang.String)
     */
    @Override
    protected Closeable getReader(final File file, final String charset) {
        Closeable reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
        } catch (Exception e) {
            throw new CompareFilesException(Const.STREAM_CANTOPEN_INPUT, new Object[] {file.getPath()}, e);
        }
        return reader;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader#readLine()
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<String, Object> readLine() {
        String targetLine = null;
        try {
            curRowNum++;
            targetLine = ((BufferedReader) reader).readLine();
            if (targetLine == null) {
                return null;
            }
        } catch (IOException e) {
            throw new CompareFilesException(
                Const.FILE_CANTREAD,
                new Object[] {filePath + "#" + curRowNum},
                e);
        }

        final Map<String, Object> returnMap = JsonUtils.parseString(targetLine, Map.class);
        if (returnMap != null) {
            returnMap.put(KEY_RAWLINE, targetLine);
        }

        return returnMap;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader#isMatchRecordType(me.suwash.tools.comparefiles.infra.config.RecordLayout, java.util.Map)
     */
    @Override
    protected boolean isMatchRecordType(final RecordLayout curRecordConfig, final Map<String, ?> targetLineMap) {
        throw new CompareFilesException(Const.MSGCD_ERROR_READER_UNSUPPORTED_RECORDPATTERN, new Object[] {FileFormat.JsonList.ddId(), RecordPattern.DataOnly.ddId()});
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader#putContent(java.lang.String, java.util.Map, java.util.Map)
     */
    @Override
    protected void putContent(final String itemId, final Map<String, ?> fromMap, final Map<String, Object> toMap) {
        deepCopy(itemId, fromMap, toMap);
    }

    /**
     * 項目IDを指定して、Map内の値を再帰的にコピーします。
     *
     * @param itemId 項目ID
     * @param fromMap 転記元Map
     * @param toMap 転記先Map
     */
    @SuppressWarnings("unchecked")
    private void deepCopy(final String itemId, final Map<String, ?> fromMap, final Map<String, Object> toMap) {
        // itemIdの確認
        if (itemId.contains(".")) {
            // --------------------------------------------------------------------------------
            // ドットが含まれている場合
            // --------------------------------------------------------------------------------
            // ドットがなくなるまで再帰的にコピー

            // 変換元の取得
            final String parentItemId = itemId.substring(0, itemId.indexOf('.'));
            final Object parentFromObj = fromMap.get(parentItemId);
            if (parentFromObj == null) {
                // なければエラー
                throw new CompareFilesException(
                    Const.MSGCD_ERROR_READER_JSONLIST_NOTEXIST_ITEM,
                    new Object[] {parentItemId, itemId, fromMap});
            }

            final String childItemId = itemId.substring(itemId.indexOf('.') + 1);
            // 親オブジェクトの型を確認
            if (parentFromObj instanceof List) {
                // 親オブジェクトがListの場合
                final List<Object> parentFromList = (List<Object>) parentFromObj;
                // 変換先への階層登録
                final Object parentToObj = toMap.get(parentItemId);
                List<Object> parentToList = null;
                if (parentToObj == null) {
                    // なければ作ってput
                    parentToList = new ArrayList<Object>();
                    toMap.put(parentItemId, parentToList);
                } else {
                    // あれば流用
                    parentToList = (List<Object>) parentToObj;
                }

                // 変換元のリスト内容をコピー
                for (int index = 0; index < parentFromList.size(); index++) {
                    final Object parentFromSubObj = parentFromList.get(index);
                    // 親リスト内の現在要素の型を確認
                    if (parentFromSubObj instanceof Map) {
                        // Mapの場合、変換先リストのインデックス位置を考慮して、再帰呼び出し
                        final Map<String, Object> parentFromSubMap = (Map<String, Object>) parentFromSubObj;
                        addDeepCopiedObjToListIndex(parentFromSubMap, parentToList, index, childItemId);

                    } else if (parentFromSubObj instanceof List) {
                        // Listの場合、
                        final List<Object> parentFromSubList = (List<Object>) parentFromSubObj;
                        addDeepCopiedList(parentFromSubList, parentToList);

                    } else {
                        // その場の場合、直接コピー
                        parentToList.add(parentFromSubObj);
                    }
                }

            } else {
                // List以外の場合（Mapの場合）
                final Map<String, Object> parentFromMap = (Map<String, Object>) parentFromObj;
                // 変換先への階層登録
                final Object parentToObj = toMap.get(parentItemId);
                Map<String, Object> parentToMap = null;
                if (parentToObj == null) {
                    // なければ作ってput
                    parentToMap = new LinkedHashMap<String, Object>();
                    toMap.put(parentItemId, parentToMap);
                } else {
                    // あれば流用
                    parentToMap = (Map<String, Object>) parentToObj;
                }

                // 再帰呼び出し
                deepCopy(childItemId, parentFromMap, parentToMap);
            }

        } else {
            // --------------------------------------------------------------------------------
            // ドットが含まれていない場合
            // --------------------------------------------------------------------------------
            // 直接コピー
            toMap.put(itemId, fromMap.get(itemId));
        }
    }

    /**
     * リストのインデックス位置に、指定した項目IDの子階層を再帰的にコピーします。
     *
     * @param fromMap 転記元Map
     * @param toList 転記先List
     * @param index Listのインデックス
     * @param itemId 変換する項目ID
     */
    @SuppressWarnings("unchecked")
    private void addDeepCopiedObjToListIndex(final Map<String, Object> fromMap, final List<Object> toList, final int index, final String itemId) {
        Map<String, Object> toMap = null;
        if (index < toList.size()) {
            toMap = (Map<String, Object>) toList.get(index);
        } else {
            // なければ作る
            toMap = new LinkedHashMap<String, Object>();
            // parentToMapに追加済みのsubListに追加
            toList.add(toMap);
        }
        // 再帰呼び出し
        deepCopy(itemId, fromMap, toMap);
    }

    /**
     * 転記先リストに、転記元リストの全ての子要素を再帰的にコピーした結果を追加します。
     *
     * @param fromList 転記元リスト
     * @param toList 転記先リスト
     */
    private void addDeepCopiedList(final List<Object> fromList, final List<Object> toList) {
        for (final Object curFromObj : fromList) {
            addDeepCopiedContent(curFromObj, toList);
        }
    }

    /**
     * 転記先リストに、指定オブジェクトを再帰的にコピーした結果を追加します。
     *
     * @param fromObj 転記元オブジェクト
     * @param toList 追加対象リスト
     */
    private void addDeepCopiedContent(final Object fromObj, final List<Object> toList) {
        // from、toのダミーMapを作成
        final Map<String, Object> fromDummyMap = new HashMap<String, Object>();
        final Map<String, Object> toDummyMap = new HashMap<String, Object>();

        // 再帰呼び出しで、toのダミーMapに設定
        fromDummyMap.put("dummy", fromObj);
        deepCopy("dummy", fromDummyMap, toDummyMap);

        // toのダミーMapにコピーされた結果を、リストに追加
        final Object toObj = toDummyMap.get("dummy");
        toList.add(toObj);
    }

}
