package me.suwash.tools.comparefiles.sv.domain;

import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * 行データの基底クラス。
 */
@Getter
@Setter
@EqualsAndHashCode
public class BaseRow {

    /** 行番号。 */
    protected long rowNum;

    /** 比較キー項目。 */
    protected Map<String, Object> keyMap;

    /** キー以外の項目。 */
    protected Map<String, Object> valueMap;

    /** 元ファイルの無変換行データ。 */
    protected String rawLine;

    /*
     * (非 Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return rawLine;
        // return JsonUtils.writeString(this);
    }
}
