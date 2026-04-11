package me.suwash.util;

import java.util.Locale;


/**
 * 設定関連ユーティリティ。
 */
public final class ConfigUtils {

    /** クラスパスのセパレータ。 */
    private static final char CLASSPATH_SEP = '/';

    /**
     * ユーティリティ向けに、コンストラクタはprivate宣言。
     */
    private ConfigUtils() {}

    /**
     * 設定ファイルクラスパスを返します。
     *
     * <pre>
     * IN:
     *   SampleClassName
     *   json
     * OUT:
     *   /sample_class_name.json
     * </pre>
     *
     * @param type 対象クラス
     * @param ext 拡張子
     * @return 設定ファイルクラスパス
     */
    public static String getConfigFileClasspath(final Class<?> type, final String ext) {
        return CLASSPATH_SEP + getConfigFileName(type, ext);
    }

    /**
     * パッケージ階層を含めた設定ファイルクラスパスを返します。
     *
     * <pre>
     * IN:
     *   me.suwash.util.SampleClassName
     *   json
     * OUT:
     *   /me/suwash/util/sample_class_name.json
     * </pre>
     *
     * @param type 対象クラス
     * @param ext 拡張子
     * @return 設定ファイルクラスパス
     */
    public static String getConfigFileClasspathWithPackage(final Class<?> type, final String ext) {
        final StringBuilder pathBuilder = new StringBuilder();

        pathBuilder.append(CLASSPATH_SEP)
            .append(type.getPackage().getName().replace('.', CLASSPATH_SEP))
            .append(CLASSPATH_SEP)
            .append(getConfigFileName(type, ext));
        return pathBuilder.toString();
    }

    /**
     * 指定されたクラスの設定ファイル名を返します。
     *
     * @param type 対象クラス
     * @param ext 拡張子
     * @return 設定ファイル名
     */
    private static String getConfigFileName(final Class<?> type, final String ext) {
        final StringBuilder fileNameBuilder = new StringBuilder();

        // クラス名のUpperCamelCaseから、小文字の"_"区切りに置換
        final String className = type.getSimpleName();
        final String lowerCaseClassName = className.toLowerCase(Locale.getDefault());
        final int classNameLength = className.length();
        for (int pos = 0; pos < classNameLength; pos++) {
            if (pos > 1 && Character.isUpperCase(className.charAt(pos))) {
                fileNameBuilder.append('_');
            }
            fileNameBuilder.append(lowerCaseClassName.charAt(pos));
        }

        fileNameBuilder.append('.').append(ext);
        return fileNameBuilder.toString().replace("_config", "");
    }

}
