package me.suwash.util;

import java.text.DecimalFormat;

import me.suwash.util.constant.UtilMessageConst;
import me.suwash.util.i18n.MessageSource;

/**
 * Runtime関連ユーティリティ。
 */
public final class RuntimeUtils {

    /**
     * ユーティリティ向けに、コンストラクタはprivate宣言。
     */
    private RuntimeUtils() {}

    /**
     * 実行時点のJVMメモリ利用状況を文字列で返します。
     *
     * @return JVMメモリ利用状況
     */
    public static String getMemoryInfo() {
        final DecimalFormat numFormat = new DecimalFormat("#,###");
        final DecimalFormat ratioFormat = new DecimalFormat("##.0");

        final long max = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        final long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        final long free = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        final long used = total - free;
        final double ratio = used * 100 / (double) total;

        // final StringBuilder info = new StringBuilder();
        // info.append("used=").append(f2.format(ratio)).append("% ").append(f1.format(used));
        // return info.toString();
        return MessageSource.getInstance().getMessage(
            UtilMessageConst.MEMORY_INFO,
            new Object[] {
                numFormat.format(max),
                numFormat.format(total),
                numFormat.format(used),
                ratioFormat.format(ratio)
            });
    }
}
