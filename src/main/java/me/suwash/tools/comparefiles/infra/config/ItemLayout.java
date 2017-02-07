package me.suwash.tools.comparefiles.infra.config;

import lombok.Getter;
import lombok.Setter;
import me.suwash.util.CompareUtils.CompareCriteria;

/**
 * 項目レイアウト設定。
 */
@Getter
@Setter
public class ItemLayout {

    /**
     * 項目ID。
     * ・項目名があるファイルの場合、物理名 / 論理名
     * → ヘッダー付きCSV / TSV, json, jsonList
     * ・項目名がないファイルの場合、項番
     * → ヘッダーなしCSV / TSV, Fixed
     */
    private String id;

    /** 項目名。 */
    private String name;

    /** バイト長。 */
    private int byteLength;

    /** 比較キーフラグ。 */
    private boolean isCompareKey;

    /** 比較条件。 */
    private CompareCriteria criteria;

    // /** 文字タイプ：全角かな etc。 */
    // private CharType type;
    //
    // /** パディング文字：空白、ゼロ etc。 */
    // private String paddingChar;
    //
    // /** 文字寄せ：左寄せ、右寄せ、なし。 */
    // private Align align;

}
