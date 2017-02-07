package me.suwash.tools.comparefiles.sv.domain.compare.file.text;

import lombok.Getter;
import me.suwash.util.CompareUtils;
import me.suwash.util.CompareUtils.CompareCriteria;
import me.suwash.util.CompareUtils.CompareStatus;

/**
 * 項目単位の比較結果。
 */
@Getter
public class TextFileItemCompareResult {

    /** 項目名。 */
    private final String name;

    /** 左ファイルの値。 */
    private final String leftValue;

    /** 右ファイルの値。 */
    private final String rightValue;

    /** 比較条件。 */
    private final CompareCriteria criteria;

    /** 比較結果。 */
    private CompareStatus status;

    /**
     * 固定値の項目比較結果を返します。
     *
     * @param name 項目名
     * @param leftValue 左ファイルの値
     * @param rightValue 右ファイルの値
     * @param criteria 比較条件
     * @param status 比較ステータス
     * @return 比較結果
     */
    protected static TextFileItemCompareResult getFixedResult(
        final String name,
        final String leftValue,
        final String rightValue,
        final CompareCriteria criteria,
        final CompareStatus status) {

        return new TextFileItemCompareResult(name, leftValue, rightValue, criteria, status);
    }

    /**
     * 固定値取得用コンストラクタ。
     *
     * @param name 項目名
     * @param leftValue 左ファイルの値
     * @param rightValue 右ファイルの値
     * @param criteria 比較条件
     * @param status 比較ステータス
     */
    private TextFileItemCompareResult(
        final String name,
        final String leftValue,
        final String rightValue,
        final CompareCriteria criteria,
        final CompareStatus status) {

        this(name, leftValue, rightValue, criteria);
        this.status = status;
    }

    /**
     * コンストラクタ。
     *
     * @param name 項目名
     * @param leftValue 左ファイルの値
     * @param rightValue 右ファイルの値
     * @param criteria 比較条件
     */
    protected TextFileItemCompareResult(
        final String name,
        final String leftValue,
        final String rightValue,
        final CompareCriteria criteria) {

        super();
        this.name = name;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        this.criteria = criteria;
    }

    /**
     * 項目の比較を実行します。
     */
    protected void compare() {
        status = CompareUtils.compareInCriteria(criteria, leftValue, rightValue);
    }

}
