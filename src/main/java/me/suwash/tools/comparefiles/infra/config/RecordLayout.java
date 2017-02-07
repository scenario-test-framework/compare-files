package me.suwash.tools.comparefiles.infra.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import me.suwash.tools.comparefiles.infra.classification.RecordType;
import me.suwash.util.CompareUtils.CompareCriteria;

/**
 * レコードレイアウト設定。
 */
public class RecordLayout {

    /** レコードタイプ。 */
    @Getter
    @Setter
    private RecordType type;

    /** 当該レコードタイプだと判断する1文字目のコード値 ※0→Header、1→Data、8→Trailer、9→End etc。 */
    @Getter
    @Setter
    private String codeValue;

    /** 項目設定リスト。 */
    @Getter
    @Setter
    private List<ItemLayout> itemList;

    /** バイト長。 */
    @Setter
    private int byteLength;

    /** 比較キー項目リスト。 */
    private List<String> compareKeyItemList;

    /** 全項目リスト。 */
    private List<String> compareItemNameList;

    /** 比較条件マップ。 */
    private Map<String, CompareCriteria> criteriaMap;

    /**
     * 1レコードのバイト長を返します。
     *
     * @return 1レコードのバイト長
     */
    public int getByteLength() {
        if (byteLength <= 0) {
            for (final ItemLayout item : itemList) {
                byteLength = byteLength + item.getByteLength();
            }
        }
        return byteLength;
    }

    /**
     * 比較キー項目をリストで返します。
     *
     * @return 比較キー項目リスト
     */
    public List<String> getCompareKeyItemList() {
        if (compareKeyItemList == null) {
            compareKeyItemList = new ArrayList<String>();
            for (final ItemLayout item : itemList) {
                if (item.isCompareKey()) {
                    compareKeyItemList.add(item.getId());
                }
            }
        }
        return compareKeyItemList;
    }

    /**
     * 全項目をリストで返します。
     *
     * @return 全項目リスト
     */
    public List<String> getCompareItemNameList() {
        if (compareItemNameList == null) {
            compareItemNameList = new ArrayList<String>();
            for (final ItemLayout item : itemList) {
                compareItemNameList.add(item.getId());
            }
        }
        return compareItemNameList;
    }

    /**
     * 項目と対応する比較条件をMapで返します。
     *
     * @return 比較条件マップ
     */
    public Map<String, CompareCriteria> getCriteriaMap() {
        if (criteriaMap == null) {
            criteriaMap = new LinkedHashMap<String, CompareCriteria>();
            for (final ItemLayout item : itemList) {
                criteriaMap.put(item.getId(), item.getCriteria());
            }
        }
        return criteriaMap;
    }
}
