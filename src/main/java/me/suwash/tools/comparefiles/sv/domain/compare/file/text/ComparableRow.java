package me.suwash.tools.comparefiles.sv.domain.compare.file.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.suwash.tools.comparefiles.sv.domain.BaseRow;
import me.suwash.util.CompareUtils;

/**
 * 比較用の行データ。
 */
public class ComparableRow extends BaseRow implements Comparable<ComparableRow> {

    private static final String KEY_DUMMY = "dummy";

    /*
     * (非 Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final ComparableRow targetObject) {
        // 相手がnullの場合、nullを小さいものとして扱う。
        if (targetObject == null) {
            return -1;
        }

        // キーで比較
        return CompareUtils.deepCompare(this.getKeyMap(), targetObject.getKeyMap());
    }

    /**
     * 指定された項目IDの設定値を返します。
     * ※キー項目、その他の項目を再帰的に指定キーで走査します。
     *
     * @param itemId 項目ID
     * @return 値
     */
    protected String getItemValue(final String itemId) {
        // キー項目から、対象項目を取得
        String value = getItemValueMain(this.getKeyMap(), itemId);
        if (value == null) {
            // キーから取得できない場合、その他の項目から取得
            value = getItemValueMain(this.getValueMap(), itemId);
        }
        return value;
    }

    /**
     * 再帰呼び出し用。
     *
     * @param contentMap 対象Map
     * @param itemId 項目ID
     * @return 値
     */
    @SuppressWarnings("unchecked")
    private String getItemValueMain(final Map<String, Object> contentMap, final String itemId) {
        // itemIdの確認
        if (itemId.contains(".")) {
            // --------------------------------------------------------------------------------
            // ドットが含まれている場合
            // --------------------------------------------------------------------------------
            // ドットがなくなるまで再帰的にvalueを検索
            final String parentItemId = itemId.substring(0, itemId.indexOf('.'));
            final String childItemId = itemId.substring(itemId.indexOf('.') + 1);

            final Object parentObj = contentMap.get(parentItemId);
            if (parentObj == null) {
                return null;

            } else {
                if (parentObj instanceof Map) {
                    // 再帰呼出し
                    final Map<String, Object> parentMap = (Map<String, Object>) parentObj;
                    return getItemValueMain(parentMap, childItemId);

                } else if (parentObj instanceof List) {
                    final List<Object> parentList = (List<Object>) parentObj;
                    final List<Object> returnList = new ArrayList<Object>();
                    for (final Object parentSubObj : parentList) {
                        // ダミーMapに詰めて再帰呼び出し
                        final Map<String, Object> dummyMap = getDummyMap(parentSubObj);
                        returnList.add(getItemValueMain((Map<String, Object>) dummyMap, KEY_DUMMY + "." + childItemId));
                    }
                    return returnList.toString();

                } else {
                    return parentObj.toString();
                }
            }

        } else {
            // --------------------------------------------------------------------------------
            // ドットが含まれていない場合
            // --------------------------------------------------------------------------------
            // 対象Mapから直接valueを検索
            final Object valueObj = contentMap.get(itemId);
            if (valueObj == null) {
                return null;
            } else {
                return valueObj.toString();
            }
        }
    }

    /**
     * 再帰呼び出し用のダミーMapを返します。
     *
     * @param targetObj ダミーMapに詰めるオブジェクト
     * @return 対象オブジェクトを設定したダミーMap
     */
    private Map<String, Object> getDummyMap(final Object targetObj) {
        final Map<String, Object> dummyMap = new HashMap<String, Object>();
        dummyMap.put(KEY_DUMMY, targetObj);
        return dummyMap;
    }

}
