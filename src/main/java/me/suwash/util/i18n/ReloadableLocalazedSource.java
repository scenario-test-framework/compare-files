package me.suwash.util.i18n;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import me.suwash.util.ConfigUtils;

/**
 * ロケールを考慮したプロパティファイルの定義保持クラス。
 * <p>
 * プロパティファイルのキャッシュは、リロードすることができます。
 *
 * 読み込み対象のプロパティファイルは、クラスパス直下の下記のファイル名です。
 * 「実装クラス名の小文字表現("_"区切り) + _ + ロケール + .properties」
 * ※XxxMessageSource → xxx_message_source_ja.properties, xxx_message_source_en.properties など
 * </p>
 */
public class ReloadableLocalazedSource {

    private static final String DEFAULT_LOCALE = "__default__";
    private static final String FILE_EXT = "properties";

    /** Locale毎のプロパティMap。 */
    protected transient Map<String, Properties> propsMap = new ConcurrentHashMap<String, Properties>();

    /**
     * ロケールに合わせたプロパティファイルをクラスパス直下から読み込みます。
     * ロケールごとにプロパティファイルの内容をキャッシュします。
     * キャッシュのクリアはclearCacheメソッドを利用してください。
     *
     * @param locale ロケール
     * @return 対象ロケールのプロパティファイル。
     */
    protected Properties getProperties(final Locale locale) {
        // デフォルトロケールでのプロパティ読み込み判定
        if (locale == null ) {
            if (!propsMap.containsKey(DEFAULT_LOCALE)) {
                final Properties props = new Properties();
                final String filePath = getPropFileClasspath(null);
                try (
                    Reader reader = new InputStreamReader(getClass().getResourceAsStream(filePath), "UTF-8")) {
                    props.load(reader);
                } catch (Exception e) {
                    throw new IllegalArgumentException("can not load properties file. path=" + filePath, e);
                }
                propsMap.put(DEFAULT_LOCALE, props);
            }
            return propsMap.get(DEFAULT_LOCALE);
        }

        // 指定ロケールでのプロパティ読み込み判定
        if (!propsMap.containsKey(locale.getLanguage())) {
            final Properties props = new Properties();
            final String filePath = getPropFileClasspath(locale);
            try (
                Reader reader = new InputStreamReader(getClass().getResourceAsStream(filePath), "UTF-8")) {
                props.load(reader);
            } catch (Exception e) {
                throw new IllegalArgumentException("can not load properties file. path=" + filePath, e);
            }
            propsMap.put(locale.getLanguage(), props);
        }
        return propsMap.get(locale.getLanguage());
    }

    /**
     * ロケールに合わせて、対象のプロパティファイル名を返します。
     * 「実装クラス名の小文字表現("_"区切り) + _ + ロケール + .properties」
     *
     * @param locale ロケール
     * @return 指定されたロケールの対象プロパティファイル名
     */
    protected String getPropFileClasspath(final Locale locale) {
        // 設定ファイル共通ルールでデフォルトのパスを取得（LowerSnakeCase）
        final String defaultPath = ConfigUtils.getConfigFileClasspath(this.getClass(), FILE_EXT);
        if (locale == null) {
            return defaultPath;
        }

        // localeが指定されている場合、languageを付与したファイル名
        return defaultPath.replace('.' + FILE_EXT, '_' + locale.getLanguage() + '.' + FILE_EXT);
    }

    /**
     * 読み込んだpropertiesファイルのキャッシュをリフレッシュします。
     */
    public void clearCache() {
        propsMap.clear();
    }
}
