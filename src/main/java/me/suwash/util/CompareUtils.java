package me.suwash.util;

//import java.awt.Rectangle;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.suwash.util.constant.UtilMessageConst;
import me.suwash.util.exception.UtilException;

import org.apache.commons.lang3.StringUtils;

/**
 * 比較関連ユーティリティ。
 */
public final class CompareUtils {

    /**
     * ユーティリティ向けに、コンストラクタはprivate宣言。
     */
    private CompareUtils() {}

    /**
     * 比較条件。
     */
    public enum CompareCriteria {
        /** 除外。 */
        Ignore,
        /** 一致→OK。 */
        Equal,
        /** 不一致→OK。 */
        NotEqual,

        /** 数値：左より大きい→OK。 */
        Number_GreaterThan_Left,
        /** 数値：左以上→OK。 */
        Number_GreaterEqualThan_Left,
        /** 数値：左より小さい→OK。 */
        Number_LessThan_Left,
        /** 数値：左以下→OK。 */
        Number_LessEqualThan_Left,

        /** 年（yyyy）：左より大きい→OK。 */
        Year_GreaterThan_Left,
        /** 年（yyyy）：左以上→OK。 */
        Year_GreaterEqualThan_Left,
        /** 年（yyyy）：左より小さい→OK。 */
        Year_LessThan_Left,
        /** 年（yyyy）：左以下→OK。 */
        Year_LessEqualThan_Left,

        /** 年月（yyyyMM）：左より大きい→OK。 */
        Month_GreaterThan_Left,
        /** 年月（yyyyMM）：左以上→OK。 */
        Month_GreaterEqualThan_Left,
        /** 年月（yyyyMM）：左より小さい→OK。 */
        Month_LessThan_Left,
        /** 年月（yyyyMM）：左以上→OK。 */
        Month_LessEqualThan_Left,

        /** 年月日（yyyyMMdd, yyyy-MM-dd, yyyy/MM/dd）：左より大きい→OK。 */
        Date_GreaterThan_Left,
        /** 年月日（yyyyMMdd, yyyy-MM-dd, yyyy/MM/dd）：左以上→OK。 */
        Date_GreaterEqualThan_Left,
        /** 年月日（yyyyMMdd, yyyy-MM-dd, yyyy/MM/dd）：左より小さい→OK。 */
        Date_LessThan_Left,
        /** 年月日（yyyyMMdd, yyyy-MM-dd, yyyy/MM/dd）：左以下→OK。 */
        Date_LessEqualThan_Left,

        /** 年月日時分秒（yyyyMMddHHmmss, yyyy-MM-dd HH:mm:ss, yyyy/MM/dd HH:mm:ss）：左より大きい→OK。 */
        Datetime_GreaterThan_Left,
        /** 年月日時分秒（yyyyMMddHHmmss, yyyy-MM-dd HH:mm:ss, yyyy/MM/dd HH:mm:ss）：左以上→OK。 */
        Datetime_GreaterEqualThan_Left,
        /** 年月日時分秒（yyyyMMddHHmmss, yyyy-MM-dd HH:mm:ss, yyyy/MM/dd HH:mm:ss）：左より小さい→OK。 */
        Datetime_LessThan_Left,
        /** 年月日時分秒（yyyyMMddHHmmss, yyyy-MM-dd HH:mm:ss, yyyy/MM/dd HH:mm:ss）：左以下→OK。 */
        Datetime_LessEqualThan_Left
    }

    /**
     * 比較ステータス。
     */
    public enum CompareStatus {
        /** 処理中。 */
        Processing,
        /** OK。 */
        OK,
        /** NG。 */
        NG,
        /** 除外。 */
        Ignore,
        /** 左のみ。 */
        LeftOnly,
        /** 右のみ。 */
        RightOnly,
        /** 実行エラー。 */
        Error
    }

    /**
     * オブジェクトツリーのソート向け大小比較。
     * 同じ構造をした、左右のオブジェクトツリーを比較可能な値まで掘り下げて比較します。
     * nullの場合・要素数が少ない場合は、小さい値として扱います。
     *
     * @param left 左オブジェクト
     * @param right 右オブジェクト
     * @return 左が小さい場合 負の値、一致する場合 0、左が大きい場合 正の値
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static int deepCompare(final Object left, final Object right) {
        // null比較 ※nullは最小の値として扱う
        if (left == null && right == null) {
            return 0;
        } else if (left == null && right != null) {
            return -1;
        } else if (left != null && right == null) {
            return 1;
        }

        // 左オブジェクトのプロパティを再帰的に比較
        if (left instanceof Number) {
            // --------------------------------------------------------------------------------
            // Numberの場合
            // --------------------------------------------------------------------------------
            final BigDecimal leftVal = new BigDecimal(left.toString());
            final BigDecimal rightVal = new BigDecimal(right.toString());
            return leftVal.compareTo(rightVal);

        } else if (left instanceof Comparable) {
            // --------------------------------------------------------------------------------
            // その他のComparebleの場合
            // --------------------------------------------------------------------------------
            return ((Comparable) left).compareTo(right);

        } else if (left instanceof List) {
            // --------------------------------------------------------------------------------
            // Listの場合
            // --------------------------------------------------------------------------------
            final List leftList = (List) left;
            final List rightList = (List) right;

            int smallerSize = -1;
            boolean isSameSize = false;
            boolean isLeftSmallerSize = false;
            if (leftList.size() < rightList.size()) {
                smallerSize = leftList.size();
                isLeftSmallerSize = true;
            } else if (leftList.size() > rightList.size()) {
                smallerSize = rightList.size();
            } else {
                smallerSize = leftList.size();
                isSameSize = true;
            }

            for (int listIdx = 0; listIdx < smallerSize; listIdx++) {
                // 再帰呼び出し
                final Object curLeftObj = leftList.get(listIdx);
                final Object curRightObj = rightList.get(listIdx);
                final int curResult = deepCompare(curLeftObj, curRightObj);
                if (curResult != 0) {
                    return curResult;
                }
            }

            if (isSameSize) {
                return 0;
            }
            if (isLeftSmallerSize) {
                return -1;
            } else {
                return 1;
            }

        } else if (left instanceof Map) {
            // --------------------------------------------------------------------------------
            // Mapの場合
            // --------------------------------------------------------------------------------
            final Map leftMap = (Map) left;
            final Map rightMap = (Map) right;

            Set<Map.Entry> loopBaseEntrySet = null;
            boolean isSameSize = false;
            boolean isLeftSmallerSize = false;
            if (leftMap.size() < rightMap.size()) {
                loopBaseEntrySet = leftMap.entrySet();
                isLeftSmallerSize = true;
            } else if (leftMap.size() > rightMap.size()) {
                loopBaseEntrySet = rightMap.entrySet();
            } else {
                loopBaseEntrySet = leftMap.entrySet();
                isSameSize = true;
            }

            for (final Map.Entry loopBaseEntry : loopBaseEntrySet) {
                // 再帰呼び出し
                final Object loopBaseKey = loopBaseEntry.getKey();
                final Object curLeftObj = leftMap.get(loopBaseKey);
                final Object curRightObj = rightMap.get(loopBaseKey);
                final int curResult = deepCompare(curLeftObj, curRightObj);
                if (curResult != 0) {
                    return curResult;
                }
            }

            if (isSameSize) {
                return 0;
            }
            if (isLeftSmallerSize) {
                return -1;
            } else {
                return 1;
            }

        } else {
            // --------------------------------------------------------------------------------
            // その他（Bean）の場合
            // --------------------------------------------------------------------------------
            // 一旦、JSON文字列化して比較
            final String leftJson = JsonUtils.writeString(left);
            final String rightJson = JsonUtils.writeString(right);
            return leftJson.compareTo(rightJson);
        }
    }

    /**
     * 個別項目値のチェック向け比較。
     * 比較条件に従って、左右の値を比較します。
     *
     * @param criteria 比較条件
     * @param left 左の項目値
     * @param right 右の項目値
     * @return 比較結果（OK、NG、Ignore、LeftOnly、RightOnly、Error）
     */
    public static CompareStatus compareInCriteria(final CompareCriteria criteria, final String left, final String right) {
        // 引数チェック
        if (criteria == null) {
            // 比較条件が設定されていない場合、文字列として比較
            return prvCompareEqual(left, right);

        } else if (criteria.equals(CompareCriteria.Ignore)) {
            // 比較条件が「除外」の場合、除外を返却
            return CompareStatus.Ignore;
        }

        // 戻り値
        CompareStatus returnVal = CompareStatus.OK;

        switch (criteria) {
            case Equal:
                returnVal = prvCompareEqual(left, right);
                break;
            case NotEqual:
                returnVal = prvCompareNotEqual(left, right);
                break;
            case Number_GreaterThan_Left:
                returnVal = prvCompareNumGreaterThan(left, right);
                break;
            case Number_GreaterEqualThan_Left:
                returnVal = prvCompareNumGreaterEqualThan(left, right);
                break;
            case Number_LessThan_Left:
                returnVal = prvCompareNumLessThan(left, right);
                break;
            case Number_LessEqualThan_Left:
                returnVal = prvCompareNumLessEqualThan(left, right);
                break;
            case Year_GreaterThan_Left:
                returnVal = prvCompareYearGreaterThan(left, right);
                break;
            case Year_GreaterEqualThan_Left:
                returnVal = prvCompareYearGreaterEqualThan(left, right);
                break;
            case Year_LessThan_Left:
                returnVal = prvCompareYearLessThan(left, right);
                break;
            case Year_LessEqualThan_Left:
                returnVal = prvCompareYearLessEqualThan(left, right);
                break;
            case Month_GreaterThan_Left:
                returnVal = prvCompareMonthGreaterThan(left, right);
                break;
            case Month_GreaterEqualThan_Left:
                returnVal = prvCompareMonthGreaterEqualThan(left, right);
                break;
            case Month_LessThan_Left:
                returnVal = prvCompareMonthLessThan(left, right);
                break;
            case Month_LessEqualThan_Left:
                returnVal = prvCompareMonthLessEqualThan(left, right);
                break;
            case Date_GreaterThan_Left:
                returnVal = prvCompareDateGreaterThan(left, right);
                break;
            case Date_GreaterEqualThan_Left:
                returnVal = prvCompareDateGreaterEqualThan(left, right);
                break;
            case Date_LessThan_Left:
                returnVal = prvCompareDateLessThan(left, right);
                break;
            case Date_LessEqualThan_Left:
                returnVal = prvCompareDateLessEqualThan(left, right);
                break;
            case Datetime_GreaterThan_Left:
                returnVal = prvCompareDatetimeGreaterThan(left, right);
                break;
            case Datetime_GreaterEqualThan_Left:
                returnVal = prvCompareDatetimeGreaterEqualThan(left, right);
                break;
            case Datetime_LessThan_Left:
                returnVal = prvCompareDatetimeLessThan(left, right);
                break;
            case Datetime_LessEqualThan_Left:
                returnVal = prvCompareDatetimeLessEqualThan(left, right);
                break;
            default:
                throw new UtilException(UtilMessageConst.UNSUPPORTED_PATTERN, new Object[] {
                    "criteria", criteria
                });
        }

        // 戻り値を返却
        return returnVal;
    }

    /**
     * Equal比較。
     *
     * @param left 左
     * @param right 右
     * @return 左右の値が一致する場合、OK
     */
    private static CompareStatus prvCompareEqual(final String left, final String right) {
        CompareStatus returnVal = CompareStatus.OK;
        if (StringUtils.isEmpty(left) && StringUtils.isEmpty(right)) {
            returnVal = CompareStatus.OK;
        } else if (!StringUtils.isEmpty(left) && StringUtils.isEmpty(right)) {
            returnVal = CompareStatus.NG;
        } else if (StringUtils.isEmpty(left) && !StringUtils.isEmpty(right)) {
            returnVal = CompareStatus.NG;
        } else { // ! StringUtils.isEmpty(left) && ! StringUtils.isEmpty(right)
            if (left.equals(right)) {
                returnVal = CompareStatus.OK;
            } else {
                returnVal = CompareStatus.NG;
            }
        }
        return returnVal;
    }

    /**
     * NotEqual比較。
     *
     * @param left 左
     * @param right 右
     * @return 左右の値が不一致の場合、OK結果
     */
    private static CompareStatus prvCompareNotEqual(final String left, final String right) {
        CompareStatus returnVal = CompareStatus.OK;
        if (StringUtils.isEmpty(left) && StringUtils.isEmpty(right)) {
            returnVal = CompareStatus.NG;
        } else if (!StringUtils.isEmpty(left) && StringUtils.isEmpty(right)) {
            returnVal = CompareStatus.OK;
        } else if (StringUtils.isEmpty(left) && !StringUtils.isEmpty(right)) {
            returnVal = CompareStatus.OK;
        } else { // ! StringUtils.isEmpty(left) && ! StringUtils.isEmpty(right)
            if (left.equals(right)) {
                returnVal = CompareStatus.NG;
            } else {
                returnVal = CompareStatus.OK;
            }
        }
        return returnVal;
    }

    /**
     * 比較向けの型変換確認。
     *
     * @param left 左
     * @param right 右
     * @return 変換できる場合、true。
     */
    private static boolean canConvertedCompare(final String left, final String right) {
        boolean canConvertedCompare = true;
        if (StringUtils.isEmpty(left) && StringUtils.isEmpty(right)) {
            // どちらも変換できないため、不可
            canConvertedCompare = false;
        } else if (!StringUtils.isEmpty(left) && StringUtils.isEmpty(right)) {
            // 一方が変換できないため、不可
            canConvertedCompare = false;
        } else if (StringUtils.isEmpty(left) && !StringUtils.isEmpty(right)) {
            // 一方が変換できないため、不可
            canConvertedCompare = false;
        }
        return canConvertedCompare;
    }

    /**
     * 数値変換。
     *
     * @param target 対象文字列
     * @return 変換結果
     */
    private static BigDecimal convNum(final String target) {
        try {
            return new BigDecimal(target);
        } catch (Exception e) {
            throw new UtilException(UtilMessageConst.ERRORHANDLE, new Object[] {
                CompareUtils.class.getName() + ".convNum",
                "target=" + target
            }, e);
        }
    }

    /**
     * YYYY変換。
     *
     * @param target 対象文字列
     * @return 変換結果
     */
    private static BigDecimal convYear(final String target) {
        try {
            return new BigDecimal(target.substring(0, 4));
        } catch (Exception e) {
            throw new UtilException(UtilMessageConst.ERRORHANDLE, new Object[] {
                CompareUtils.class.getName() + ".convYear",
                "target=" + target
            }, e);
        }
    }

    /**
     * YYYYMM変換。
     *
     * @param target 対象文字列
     * @return 変換結果
     */
    private static BigDecimal convYearMonth(final String target) {
        try {
            return new BigDecimal(target.substring(0, 6));
        } catch (Exception e) {
            throw new UtilException(UtilMessageConst.ERRORHANDLE, new Object[] {
                CompareUtils.class.getName() + ".convYearMonth",
                "target=" + target
            }, e);
        }
    }

    /**
     * 日付変換（時刻なし）。
     *
     * @param target 対象文字列
     * @return 変換結果
     */
    private static Date convDate(final String target) {
        try {
            return DateUtils.clearTime(DateUtils.toDate(target));
        } catch (Exception e) {
            throw new UtilException(UtilMessageConst.ERRORHANDLE, new Object[] {
                CompareUtils.class.getName() + ".convDate",
                "target=" + target
            }, e);
        }
    }

    /**
     * タイムスタンプ変換。
     *
     * @param target 対象文字列
     * @return 変換結果
     */
    private static Date convDatetime(final String target) {
        try {
            return DateUtils.toDate(target);
        } catch (Exception e) {
            throw new UtilException(UtilMessageConst.ERRORHANDLE, new Object[] {
                CompareUtils.class.getName() + ".convDatetime",
                "target=" + target
            }, e);
        }
    }

    /**
     * 左 < 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareNumGreaterThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final BigDecimal leftVal = convNum(left);
        final BigDecimal rightVal = convNum(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.NG;
        } else if (result < 0) {
            returnVal = CompareStatus.OK;
        } else { // result > 0
            returnVal = CompareStatus.NG;
        }
        return returnVal;
    }

    /**
     * 左 <= 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareNumGreaterEqualThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final BigDecimal leftVal = convNum(left);
        final BigDecimal rightVal = convNum(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.OK;
        } else if (result < 0) {
            returnVal = CompareStatus.OK;
        } else { // result > 0
            returnVal = CompareStatus.NG;
        }
        return returnVal;
    }

    /**
     * 左 > 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareNumLessThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final BigDecimal leftVal = convNum(left);
        final BigDecimal rightVal = convNum(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.NG;
        } else if (result < 0) {
            returnVal = CompareStatus.NG;
        } else { // result > 0
            returnVal = CompareStatus.OK;
        }
        return returnVal;
    }

    /**
     * 左 >= 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareNumLessEqualThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final BigDecimal leftVal = convNum(left);
        final BigDecimal rightVal = convNum(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.OK;
        } else if (result < 0) {
            returnVal = CompareStatus.NG;
        } else { // result > 0
            returnVal = CompareStatus.OK;
        }
        return returnVal;
    }

    /**
     * 左 < 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareYearGreaterThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final BigDecimal leftVal = convYear(left);
        final BigDecimal rightVal = convYear(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.NG;
        } else if (result < 0) {
            returnVal = CompareStatus.OK;
        } else { // result > 0
            returnVal = CompareStatus.NG;
        }
        return returnVal;
    }

    /**
     * 左 <= 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareYearGreaterEqualThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final BigDecimal leftVal = convYear(left);
        final BigDecimal rightVal = convYear(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.OK;
        } else if (result < 0) {
            returnVal = CompareStatus.OK;
        } else { // result > 0
            returnVal = CompareStatus.NG;
        }
        return returnVal;
    }

    /**
     * 左 > 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareYearLessThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final BigDecimal leftVal = convYear(left);
        final BigDecimal rightVal = convYear(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.NG;
        } else if (result < 0) {
            returnVal = CompareStatus.NG;
        } else { // result > 0
            returnVal = CompareStatus.OK;
        }
        return returnVal;
    }

    /**
     * 左 >= 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareYearLessEqualThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final BigDecimal leftVal = convYear(left);
        final BigDecimal rightVal = convYear(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.OK;
        } else if (result < 0) {
            returnVal = CompareStatus.NG;
        } else { // result > 0
            returnVal = CompareStatus.OK;
        }
        return returnVal;
    }

    /**
     * 左 < 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareMonthGreaterThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final BigDecimal leftVal = convYearMonth(left);
        final BigDecimal rightVal = convYearMonth(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.NG;
        } else if (result < 0) {
            returnVal = CompareStatus.OK;
        } else { // result > 0
            returnVal = CompareStatus.NG;
        }
        return returnVal;
    }

    /**
     * 左 <= 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareMonthGreaterEqualThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final BigDecimal leftVal = convYearMonth(left);
        final BigDecimal rightVal = convYearMonth(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.OK;
        } else if (result < 0) {
            returnVal = CompareStatus.OK;
        } else { // result > 0
            returnVal = CompareStatus.NG;
        }
        return returnVal;
    }

    /**
     * 左 > 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareMonthLessThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final BigDecimal leftVal = convYearMonth(left);
        final BigDecimal rightVal = convYearMonth(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.NG;
        } else if (result < 0) {
            returnVal = CompareStatus.NG;
        } else { // result > 0
            returnVal = CompareStatus.OK;
        }
        return returnVal;
    }

    /**
     * 左 >= 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareMonthLessEqualThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final BigDecimal leftVal = convYearMonth(left);
        final BigDecimal rightVal = convYearMonth(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.OK;
        } else if (result < 0) {
            returnVal = CompareStatus.NG;
        } else { // result > 0
            returnVal = CompareStatus.OK;
        }
        return returnVal;
    }

    /**
     * 左 < 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareDateGreaterThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final Date leftVal = convDate(left);
        final Date rightVal = convDate(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.NG;
        } else if (result < 0) {
            returnVal = CompareStatus.OK;
        } else { // result > 0
            returnVal = CompareStatus.NG;
        }
        return returnVal;
    }

    /**
     * 左 <= 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareDateGreaterEqualThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final Date leftVal = convDate(left);
        final Date rightVal = convDate(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.OK;
        } else if (result < 0) {
            returnVal = CompareStatus.OK;
        } else { // result > 0
            returnVal = CompareStatus.NG;
        }
        return returnVal;
    }

    /**
     * 左 > 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareDateLessThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final Date leftVal = convDate(left);
        final Date rightVal = convDate(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.NG;
        } else if (result < 0) {
            returnVal = CompareStatus.NG;
        } else { // result > 0
            returnVal = CompareStatus.OK;
        }
        return returnVal;
    }

    /**
     * 左 >= 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareDateLessEqualThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final Date leftVal = convDate(left);
        final Date rightVal = convDate(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.OK;
        } else if (result < 0) {
            returnVal = CompareStatus.NG;
        } else { // result > 0
            returnVal = CompareStatus.OK;
        }
        return returnVal;
    }

    /**
     * 左 < 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareDatetimeGreaterThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final Date leftVal = convDatetime(left);
        final Date rightVal = convDatetime(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.NG;
        } else if (result < 0) {
            returnVal = CompareStatus.OK;
        } else { // result > 0
            returnVal = CompareStatus.NG;
        }
        return returnVal;
    }

    /**
     * 左 <= 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareDatetimeGreaterEqualThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final Date leftVal = convDatetime(left);
        final Date rightVal = convDatetime(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.OK;
        } else if (result < 0) {
            returnVal = CompareStatus.OK;
        } else { // result > 0
            returnVal = CompareStatus.NG;
        }
        return returnVal;
    }

    /**
     * 左 > 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareDatetimeLessThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final Date leftVal = convDatetime(left);
        final Date rightVal = convDatetime(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.NG;
        } else if (result < 0) {
            returnVal = CompareStatus.NG;
        } else { // result > 0
            returnVal = CompareStatus.OK;
        }
        return returnVal;
    }

    /**
     * 左 >= 右：OK。
     *
     * @param left 左文字列
     * @param right 右文字列
     * @return 比較結果
     */
    private static CompareStatus prvCompareDatetimeLessEqualThan(final String left, final String right) {
        if (!canConvertedCompare(left, right)) {
            return CompareStatus.NG;
        }

        CompareStatus returnVal = CompareStatus.OK;
        final Date leftVal = convDatetime(left);
        final Date rightVal = convDatetime(right);
        final int result = leftVal.compareTo(rightVal);
        if (result == 0) {
            returnVal = CompareStatus.OK;
        } else if (result < 0) {
            returnVal = CompareStatus.NG;
        } else { // result > 0
            returnVal = CompareStatus.OK;
        }
        return returnVal;
    }
}
