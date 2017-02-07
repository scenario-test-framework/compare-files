package me.suwash.tools.comparefiles.sv.domain.sort;

import me.suwash.tools.comparefiles.sv.domain.BaseRow;
import me.suwash.util.CompareUtils;

/**
 * ソート用の行データ。
 */
public class SortableRow extends BaseRow implements Comparable<SortableRow> {

    /*
     * (非 Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final SortableRow targetObject) {
        // 相手がnullの場合、nullを小さいものとして扱う。
        if (targetObject == null) {
            return -1;
        }

        // キーで比較
        int result = CompareUtils.deepCompare(this.getKeyMap(), targetObject.getKeyMap());
        if (result == 0) {
            // キーが一致している場合は、その他の項目で比較
            result = CompareUtils.deepCompare(this.getValueMap(), targetObject.getValueMap());
        }
        return result;
    }

}
