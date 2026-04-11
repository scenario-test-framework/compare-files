package me.suwash.util.i18n;

import java.util.Locale;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

/**
 * データディクショナリ用プロパティファイルの定義保持の基底クラス。
 */
public class DdSource extends ReloadableLocalazedSource {

    protected static final DdSource instance = new DdSource();
    protected transient DdSource parent;

    /**
     * Singletonパターンでインスタンスを返します。
     *
     * @return インスタンス
     */
    public static DdSource getInstance() {
        return instance;
    }

    /**
     * コンストラクタ。
     */
    protected DdSource() {
        super();
        parent = getParent();
    }

    /**
     * 親DdSourceを返します。
     * @return 親DdSource
     */
    protected DdSource getParent() {
        return null;
    }

    /**
     * データディクショナリIDを、再帰的に親DdSourceまで検索し、一致する項目名を返します（子の定義を優先します）。
     *
     * @param ddId データディクショナリID
     * @return 項目名
     */
    public String getName(final String ddId) {
        return getName(ddId, null);
    }

    /**
     * データディクショナリIDを、再帰的に親DdSourceまで検索し、一致する項目名を返します（子の定義を優先します）。
     *
     * @param ddId データディクショナリID
     * @param paraLocale ロケール（デフォルトは、マシンロケール）
     * @return 項目名
     */
    public String getName(final String ddId, final Locale paraLocale) {
        // null、空文字チェック
        if (StringUtils.isEmpty(ddId)) {
            return StringUtils.EMPTY;
        }

        // Locale判定
        Locale locale = paraLocale;
        if (paraLocale == null) {
            locale = Locale.getDefault();
        }

        // Locale毎のメッセージ取得
        final Properties props = getProperties(locale);
        String name = props.getProperty(ddId);

        // 自身に未定で、親が登録されている場合、親からメッセージを取得
        if (StringUtils.isEmpty(name) && parent != null) {
            name = parent.getName(ddId, locale);
        }

        // 親メッセージ考慮後の取得結果確認
        if (StringUtils.isEmpty(name)) {
            // 親から取得できない場合、デフォルトを確認
            final Properties defaultProps = getProperties(null);
            name = defaultProps.getProperty(ddId);
            if (StringUtils.isEmpty(name)) {
                // それでも存在しない場合、ddIdをそのまま返却
                name = ddId;
            }
        }

        // メッセージ文言を返却
        return name;
    }

}
