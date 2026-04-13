package me.suwash.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

import me.suwash.util.constant.UtilMessageConst;
import me.suwash.util.exception.UtilException;
import me.suwash.util.i18n.DdSource;
import me.suwash.util.i18n.MessageSource;

import org.apache.commons.lang3.StringUtils;

/**
 * 日付関連ユーティリティ。
 */
@lombok.extern.slf4j.Slf4j
public final class DateUtils {

    /** 日付文字列の文字数：YYYYMMDD→8。 */
    private static final int DATE_STRING_LENGTH = 8;

    /**
     * 補充する文字列の追加位置。
     */
    private enum FillPosition {
        LEFT,
        RIGHT
    }

    /**
     * ユーティリティ向けに、コンストラクタはprivate宣言。
     */
    private DateUtils() {}

    /**
     * Dateオブジェクトから時刻部分をクリアします。
     * 対応フォーマットは toCalendarメソッドを確認してください。
     *
     * @param target 対象Dateオブジェクト
     * @return 時刻をクリアしたDateオブジェクト
     */
    public static Date clearTime(final Date target) {
        return org.apache.commons.lang3.time.DateUtils.truncate(target, Calendar.DAY_OF_MONTH);
    }

    /**
     * 指定された日付・時刻文字列を、可能であれば Dateオブジェクトに変換します。
     *
     * @param strDate 変換対象の日付文字列
     * @return Dateオブジェクト
     */
    public static Date toDate(final String strDate) {
        // --------------------------------------------------
        // 共通タイムスタンプフォーマット変換
        // --------------------------------------------------
        final Date parsed = parseCommonFormat(strDate);
        if (parsed != null) {
            return parsed;
        }

        // --------------------------------------------------
        // カレンダー変換に移譲
        // --------------------------------------------------
        final Calendar calendar = toCalendar(strDate);
        return calendar.getTime();
    }

    /**
     * 指定された日付・時刻文字列を、可能であれば Calendarオブジェクトに変換します。
     *
     * <pre>
     * 対応フォーマット
     *   ISO-8601形式
     *   apache accesslog timestamp形式
     *   日時フォーマット文字列
     *
     * 日時フォーマット
     *   yyyy/MM/dd [時刻]
     *   yy/MM/dd [時刻]
     *   yyyy-MM-dd [時刻]
     *   yy-MM-dd [時刻]
     *   yyyyMMdd [時刻]
     *
     * 時刻
     *   HH:mm
     *   HH:mm:ss
     *   HH:mm:ss.SSS
     * </pre>
     *
     * @param strDate 日時文字列
     * @return Calendarオブジェクト
     */
    public static Calendar toCalendar(final String strDate) {
        // --------------------------------------------------
        // 共通タイムスタンプフォーマット変換
        // --------------------------------------------------
        Calendar result = null;
        final Date parsed = parseCommonFormat(strDate);
        if (parsed != null) {
            result = Calendar.getInstance();
            result.setTime(parsed);
            return result;
        }

        // --------------------------------------------------
        // カレンダー変換
        // --------------------------------------------------
        try {
            result = toCalendarMain(strDate);
            // 日付の妥当性チェック
            result.getTime();
        } catch (RuntimeException e) {
            throw new UtilException(UtilMessageConst.UNSUPPORTED_PATTERN, new Object[] {
                DateUtils.class.getName() + ".toCalendar", strDate
            }, e);
        }

        return result;
    }

    /**
     * カレンダー変換の内部処理。
     *
     * @param strDate 対象文字列
     * @return 変換したカレンダー
     */
    private static Calendar toCalendarMain(final String strDate) {
        // --------------------------------------------------
        // 共通文字列フォーマット変換
        // --------------------------------------------------
        final String formattedStrDate = format(strDate);

        // --------------------------------------------------
        // カレンダー変換
        // --------------------------------------------------
        final Calendar cal = Calendar.getInstance();
        cal.setLenient(false);

        final int intYear = Integer.parseInt(formattedStrDate.substring(0, 4));
        final int intMonth = Integer.parseInt(formattedStrDate.substring(5, 7));
        final int intDate = Integer.parseInt(formattedStrDate.substring(8, 10));
        int intHours = cal.get(Calendar.HOUR_OF_DAY);
        int intMinutes = cal.get(Calendar.MINUTE);
        int intSeconds = cal.get(Calendar.SECOND);
        int intMilleSeconds = cal.get(Calendar.MILLISECOND);
        cal.clear();
        cal.set(intYear, intMonth - 1, intDate);
        final int len = formattedStrDate.length();
        switch (len) {
            case 10:
                break;
// 共通フォーマット変換で「yyyy/MM/dd」か「yyyy/MM/dd HH:mm:ss」に変換される
//            case 16: // yyyy/MM/dd HH:mm
//                intHours = Integer.parseInt(formattedStrDate.substring(11, 13));
//                intMinutes = Integer.parseInt(formattedStrDate.substring(14, 16));
//                cal.set(Calendar.HOUR_OF_DAY, intHours);
//                cal.set(Calendar.MINUTE, intMinutes);
//                break;
//            case 19: // yyyy/MM/dd HH:mm:ss
//                intHours = Integer.parseInt(formattedStrDate.substring(11, 13));
//                intMinutes = Integer.parseInt(formattedStrDate.substring(14, 16));
//                intSeconds = Integer.parseInt(formattedStrDate.substring(17, 19));
//                cal.set(Calendar.HOUR_OF_DAY, intHours);
//                cal.set(Calendar.MINUTE, intMinutes);
//                cal.set(Calendar.SECOND, intSeconds);
//                break;
            case 23: // yyyy/MM/dd HH:mm:ss.SSS
                intHours = Integer.parseInt(formattedStrDate.substring(11, 13));
                intMinutes = Integer.parseInt(formattedStrDate.substring(14, 16));
                intSeconds = Integer.parseInt(formattedStrDate.substring(17, 19));
                intMilleSeconds = Integer.parseInt(formattedStrDate.substring(20, 23));
                cal.set(Calendar.HOUR_OF_DAY, intHours);
                cal.set(Calendar.MINUTE, intMinutes);
                cal.set(Calendar.SECOND, intSeconds);
                cal.set(Calendar.MILLISECOND, intMilleSeconds);
                break;
            default:
                throw new UtilException(UtilMessageConst.UNSUPPORTED_PATTERN, new Object[] {
                    DateUtils.class.getName() + ".format", formattedStrDate
                });
        }

        return cal;
    }

    /**
     * 共通フォーマットをDateオブジェクトに変換します。
     *
     * @param strDate 日時文字列
     * @return Dateオブジェクト
     */
    private static Date parseCommonFormat(final String strDate) {
        final String funcName = DdSource.getInstance().getName(DateUtils.class.getName() + ".format");

        Date parsed = null;
        String parseTarget = StringUtils.EMPTY;
        String format = StringUtils.EMPTY;

        // --------------------------------------------------
        // ISO-8601
        // --------------------------------------------------
        if (!strDate.contains(" ") && strDate.contains("-") && strDate.contains("T") && strDate.contains(":")) {
            // スペースなし、-あり、Tあり、:ありの場合、変換を実行
            parseTarget = strDate;
            if (strDate.endsWith(":")) {
                // yyyy-mm-dd'T'hh:mm:ss.SSS+0900: 形式 → :除去
                parseTarget = strDate.substring(0, strDate.length() - 1);
            }
            // ミリ秒あり(.区切り)で変換
            format = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
            try {
                parsed = new SimpleDateFormat(format, Locale.getDefault(Locale.Category.FORMAT)).parse(parseTarget);
            } catch (ParseException e) {
                if (log.isTraceEnabled()) {
                    log.trace(MessageSource.getInstance().getMessage(UtilMessageConst.UNSUPPORTED_PATTERN, new Object[] {
                        funcName + '(' + format + ')', strDate
                    }), e);
                }
            }
            if (parsed != null) {
                return parsed;
            }

            // ミリ秒あり（,区切り）で変換
            format = "yyyy-MM-dd'T'HH:mm:ss,SSSX";
            try {
                parsed = new SimpleDateFormat(format, Locale.getDefault(Locale.Category.FORMAT)).parse(parseTarget);
            } catch (ParseException e) {
                if (log.isTraceEnabled()) {
                    log.trace(MessageSource.getInstance().getMessage(UtilMessageConst.UNSUPPORTED_PATTERN, new Object[] {
                        funcName + '(' + format + ')', strDate
                    }), e);
                }
            }
            if (parsed != null) {
                return parsed;
            }

            // ミリ秒なしで変換
            format = "yyyy-MM-dd'T'HH:mm:ssX";
            try {
                parsed = new SimpleDateFormat(format, Locale.getDefault(Locale.Category.FORMAT)).parse(parseTarget);
            } catch (ParseException e) {
                if (log.isTraceEnabled()) {
                    log.trace(MessageSource.getInstance().getMessage(UtilMessageConst.UNSUPPORTED_PATTERN, new Object[] {
                        funcName + '(' + format + ')', strDate
                    }), e);
                }
            }
            if (parsed != null) {
                return parsed;
            }
        }

        // --------------------------------------------------
        // apache access log
        // --------------------------------------------------
        if (strDate.charAt(0) == '[' && strDate.charAt(strDate.length() - 1) == ']' && strDate.contains(" ") && strDate.contains("/") && strDate.contains(":")) {
            // [ ]で括られている、スペースあり、/あり、:ありの場合、変換を実行
            // 括り文字を除去
            parseTarget = strDate.substring(1, strDate.length() - 1);
            // 変換
            format = "dd/MMM/yyyy:HH:mm:ss X";
            try {
                parsed = new SimpleDateFormat(format, Locale.US).parse(parseTarget);
            } catch (ParseException e) {
                if (log.isTraceEnabled()) {
                    log.trace(MessageSource.getInstance().getMessage(UtilMessageConst.UNSUPPORTED_PATTERN, new Object[] {
                        funcName + '(' + format + ')', strDate
                    }), e);
                }
            }
            if (parsed != null) {
                return parsed;
            }
        }

        return parsed;
    }

    /**
     * 日時文字列を、デフォルトの日付・時刻フォーマットに変換します。
     *
     * <pre>
     * デフォルトフォーマット
     *   日付のみ場合：yyyy/MM/dd
     *   日付+時刻の場合：yyyy/MM/dd HH:mm:ss.SSS
     * </pre>
     *
     * @param paraTargetDate 変換対象の文字列
     * @return フォーマット後の文字列
     */
    private static String format(final String paraTargetDate) {
        if (paraTargetDate == null || paraTargetDate.trim().length() < DATE_STRING_LENGTH) {
            throw new UtilException(UtilMessageConst.UNSUPPORTED_PATTERN, new Object[] {
                DateUtils.class.getName() + ".format", paraTargetDate
            });
        }

        final String targetDate = paraTargetDate.trim();
        String strYear = null;
        String strMonth = null;
        String strDate = null;
        String strHours = "00";
        String strMinutes = "00";
        String strSeconds = "00";
        String strMilleSeconds = "000";

        // "-" or "/" が無い場合
        if (targetDate.indexOf('/') == -1 && targetDate.indexOf('-') == -1) {
            if (targetDate.length() == DATE_STRING_LENGTH) {
                // yyyyMMdd形式
                strYear = targetDate.substring(0, 4);
                strMonth = targetDate.substring(4, 6);
                strDate = targetDate.substring(6, 8);
                return strYear + "/" + strMonth + "/" + strDate;
            }

            // yyyyMMddHHmmss形式
            strYear = targetDate.substring(0, 4);
            strMonth = targetDate.substring(4, 6);
            strDate = targetDate.substring(6, 8);
            strHours = targetDate.substring(9, 11);
            strMinutes = targetDate.substring(12, 14);
            strSeconds = targetDate.substring(15, 17);
            final StringBuffer result = new StringBuffer();
            result.append(strYear)
                .append('/').append(strMonth)
                .append('/').append(strDate)
                .append(' ').append(strHours)
                .append(':').append(strMinutes)
                .append(':').append(strSeconds)
                .append('.').append(strMilleSeconds);
            return result.toString();
        }

        final StringTokenizer token = new StringTokenizer(targetDate, "_/-:.,T ");
        for (int i = 0; token.hasMoreTokens(); i++) {
            final String temp = token.nextToken();
            switch (i) {
                case 0:// 年の部分
                    strYear = fillString(temp, FillPosition.LEFT, 4, "20");
                    break;
                case 1:// 月の部分
                    strMonth = fillString(temp, FillPosition.LEFT, 2, "0");
                    break;
                case 2:// 日の部分
                    strDate = fillString(temp, FillPosition.LEFT, 2, "0");
                    break;
                case 3:// 時間の部分
                    strHours = fillString(temp, FillPosition.LEFT, 2, "0");
                    break;
                case 4:// 分の部分
                    strMinutes = fillString(temp, FillPosition.LEFT, 2, "0");
                    break;
                case 5:// 秒の部分
                    strSeconds = fillString(temp, FillPosition.LEFT, 2, "0");
                    break;
                case 6:// ミリ秒の部分
                    strMilleSeconds = fillString(temp, FillPosition.RIGHT, 3, "0");
                    break;
                default:
                    throw new UtilException(UtilMessageConst.UNSUPPORTED_PATTERN, new Object[] {
                        DateUtils.class.getName() + ".format", paraTargetDate
                    });
            }
        }

        final StringBuffer result = new StringBuffer();
        result.append(strYear)
            .append('/').append(strMonth)
            .append('/').append(strDate)
            .append(' ').append(strHours)
            .append(':').append(strMinutes)
            .append(':').append(strSeconds)
            .append('.').append(strMilleSeconds);
        return result.toString();
    }

    /**
     * 文字列[paraStr]に対して、補充する文字列[paraAddStr]を、[position]の位置に、[len]を満たすまで挿入します。
     * ※[paraStr]がnullや空文字の場合でも、[paraAddStr]を、[len]を満たすまで挿入した結果を返します。
     *
     * @param paraStr 対象文字列
     * @param position 前に挿入 ⇒ LEFT 後に挿入 ⇒ RIGHT
     * @param len 補充するまでの桁数
     * @param paraAddStr 挿入する文字列
     * @return 変換後の文字列
     */
    private static String fillString(
        final String paraStr,
        final FillPosition position,
        final int len,
        final String paraAddStr
    ) {
        String addStr = paraAddStr;
        String str = StringUtils.EMPTY;
        if (!StringUtils.isEmpty(paraStr)) {
            str = paraStr;
        }

        final StringBuffer buffer = new StringBuffer(str);
        while (len > buffer.length()) {
            if (FillPosition.LEFT.equals(position)) {
                final int sum = buffer.length() + addStr.length();
                if (sum > len) {
                    addStr = addStr.substring(0, addStr.length() - (sum - len));
                    buffer.insert(0, addStr);
                } else {
                    buffer.insert(0, addStr);
                }

            } else {
                buffer.append(addStr);
            }
        }

        if (buffer.length() == len) {
            return buffer.toString();
        }

        return buffer.toString().substring(0, len);
    }
}
