package me.suwash.util.i18n;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

/**
 * メッセージ用プロパティファイルの定義保持の基底クラス。
 */
public class MessageSource extends ReloadableLocalazedSource {

    protected static final MessageSource instance = new MessageSource();
    protected transient MessageSource parent;
    protected transient DdSource dataDictionary;

    /**
     * Singletonパターンでインスタンスを返します。
     *
     * @return インスタンス
     */
    public static MessageSource getInstance() {
        return instance;
    }

    /**
     * コンストラクタ。
     */
    protected MessageSource() {
        super();
        parent = getParent();
        dataDictionary = getDd();
    }

    /**
     * 親MessageSourceを返します。
     *
     * @return 親MessageSource
     */
    protected MessageSource getParent() {
        return null;
    }

    /**
     * データディクショナリを返します。
     *
     * @return データディクショナリ
     */
    protected DdSource getDd() {
        return DdSource.getInstance();
    }

    /**
     * メッセージIDを、再帰的に親MessageSourceまで検索し、一致する文言を返します（子の定義を優先します）。
     *
     * @param messageId メッセージID
     * @return メッセージ文言
     */
    public String getMessage(final String messageId) {
        return getMessage(messageId, null, null);
    }

    /**
     * メッセージIDを、再帰的に親MessageSourceまで検索し、一致する文言を返します（子の定義を優先します）。
     *
     * @param messageId メッセージID
     * @param locale ロケール（デフォルトは、マシンロケール）
     * @return メッセージ文言
     */
    public String getMessage(final String messageId, final Locale locale) {
        return getMessage(messageId, null, locale);
    }

    /**
     * メッセージIDを、再帰的に親MessageSourceまで検索し、一致する文言を返します（子の定義を優先します）。
     * メッセージ引数には、データディクショナリIDを設定すると、DDから取得した項目名を設定します。
     *
     * @param messageId メッセージID
     * @param args メッセージ引数
     * @return メッセージ文言
     */
    public String getMessage(final String messageId, final Object[] args) {
        return getMessage(messageId, args, null);
    }

    /**
     * メッセージIDを、再帰的に親MessageSourceまで検索し、一致する文言を返します（子の定義を優先します）。
     * メッセージ引数には、データディクショナリIDを設定すると、DDから取得した項目名を設定します。
     *
     * @param messageId メッセージID
     * @param args メッセージ引数
     * @param paraLocale ロケール（デフォルトは、マシンロケール）
     * @return メッセージ文言
     */
    public String getMessage(final String messageId, final Object[] args, final Locale paraLocale) {
        // 必須チェック
        if (StringUtils.isEmpty(messageId)) {
            throw new IllegalArgumentException("messageId is empty.");
        }

        // Locale判定
        Locale locale = paraLocale;
        if (paraLocale == null) {
            locale = Locale.getDefault();
        }

        // Locale毎のメッセージ取得
        final Properties props = getProperties(locale);
        String message = props.getProperty(messageId);

        // 自身に未定で、親が登録されている場合、親からメッセージを取得
        if (StringUtils.isEmpty(message) && parent != null) {
            message = parent.getMessage(messageId, args, locale);
        }

        // 親MessageSource考慮後の取得結果確認
        if (StringUtils.isEmpty(message)) {
            // 親から取得できない場合、デフォルトを確認
            final Properties defaultProps = getProperties(null);
            message = defaultProps.getProperty(messageId);
            if (StringUtils.isEmpty(message)) {
                // それでも存在しない場合、エラー
                throw new IllegalArgumentException("messageId=" + messageId + " is undefined. properties file:" + getPropFileClasspath(locale));
            }
        }

        // メッセージ引数の適用
        if (args != null) {
            final List<Object> ddReplacedArgList = new ArrayList<Object>();
            for (int i = 0; i < args.length; i++) {

                // DataDictionayからddIdとLocaleで項目名を取得
                String ddId = StringUtils.EMPTY;
                if (args[i] != null) {
                    ddId = args[i].toString();
                }
                final String ddName = dataDictionary.getName(ddId, locale);

                // 取得した項目名の確認
                if (ddName.equals(ddId)) {
                    // DD定義されていない値の場合、引数をそのまま設定
                    ddReplacedArgList.add(args[i]);
                } else {
                    // DD定義された値の場合、DataDictionayから取得した項目名を設定
                    ddReplacedArgList.add(ddName);
                }
            }

            // フォーマットを実行
            final MessageFormat messageFormat = new MessageFormat(message);
            message = messageFormat.format(ddReplacedArgList.toArray());
        }

        // メッセージ文言を返却
        return message;
    }
}
