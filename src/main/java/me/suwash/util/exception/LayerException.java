package me.suwash.util.exception;

import java.util.Locale;

import me.suwash.util.i18n.MessageSource;

import org.apache.commons.lang3.StringUtils;

/**
 * レイヤ毎の例外基底クラス。
 * MessageSource、DdSourceを利用して、他言語対応したメッセージを利用できます。
 */
public abstract class LayerException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected String messageId;
    protected Object[] messageArgs;
    protected Locale locale;
    protected MessageSource messageSource;

    /**
     * コンストラクタ。
     */
    protected LayerException() {
        super();
        this.messageId = StringUtils.EMPTY;
//        this.messageArgs = null;
//        this.locale = null;
        this.messageSource = getMessageSource();
    }

    /**
     * コンストラクタ。
     *
     * @param messageId メッセージID
     */
    protected LayerException(final String messageId) {
        super();
        this.messageId = messageId;
//        this.messageArgs = null;
//        this.locale = null;
        this.messageSource = getMessageSource();
    }

    /**
     * コンストラクタ。
     *
     * @param messageId メッセージID
     * @param locale ロケール
     */
    protected LayerException(final String messageId, final Locale locale) {
        super();
        this.messageId = messageId;
//        this.messageArgs = null;
        this.locale = locale;
        this.messageSource = getMessageSource();
    }

    /**
     * コンストラクタ。
     *
     * @param messageId メッセージID
     * @param messageArgs メッセージ引数配列
     */
    protected LayerException(final String messageId, final Object[] messageArgs) {
        super();
        this.messageId = messageId;
        this.messageArgs = messageArgs;
//        this.locale = null;
        this.messageSource = getMessageSource();
    }

    /**
     * コンストラクタ。
     *
     * @param messageId メッセージID
     * @param messageArgs メッセージ引数配列
     * @param locale ロケール
     */
    protected LayerException(final String messageId, final Object[] messageArgs, final Locale locale) {
        super();
        this.messageId = messageId;
        this.messageArgs = messageArgs;
        this.locale = locale;
        this.messageSource = getMessageSource();
    }

    /**
     * コンストラクタ。
     *
     * @param cause 原因となった例外オブジェクト
     */
    protected LayerException(final Throwable cause) {
        super(cause);
        this.messageId = StringUtils.EMPTY;
//        this.messageArgs = null;
//        this.locale = null;
        this.messageSource = getMessageSource();
    }

    /**
     * コンストラクタ。
     *
     * @param messageId メッセージID
     * @param cause 原因となった例外オブジェクト
     */
    protected LayerException(final String messageId, final Throwable cause) {
        super(cause);
        this.messageId = messageId;
//        this.messageArgs = null;
//        this.locale = null;
        this.messageSource = getMessageSource();
    }

    /**
     * コンストラクタ。
     *
     * @param messageId メッセージID
     * @param locale ロケール
     * @param cause 原因となった例外オブジェクト
     */
    protected LayerException(final String messageId, final Locale locale, final Throwable cause) {
        super(cause);
        this.messageId = messageId;
//        this.messageArgs = null;
        this.locale = locale;
        this.messageSource = getMessageSource();
    }

    /**
     * コンストラクタ。
     *
     * @param messageId メッセージID
     * @param messageArgs メッセージ引数配列
     * @param cause 原因となった例外オブジェクト
     */
    protected LayerException(final String messageId, final Object[] messageArgs, final Throwable cause) {
        super(cause);
        this.messageId = messageId;
        this.messageArgs = messageArgs;
//        this.locale = null;
        this.messageSource = getMessageSource();
    }

    /**
     * コンストラクタ。
     *
     * @param messageId メッセージID
     * @param messageArgs メッセージ引数配列
     * @param locale ロケール
     * @param cause 原因となった例外オブジェクト
     */
    protected LayerException(final String messageId, final Object[] messageArgs, final Locale locale, final Throwable cause) {
        super(cause);
        this.messageId = messageId;
        this.messageArgs = messageArgs;
        this.locale = locale;
        this.messageSource = getMessageSource();
    }

    /**
     * 対象レイヤのMessageSourceを返すようにオーバーライドしてください。
     *
     * @return 対象レイヤのMessageSource
     */
    protected abstract MessageSource getMessageSource();

    /**
     * ロケールを設定します。
     *
     * @param locale ロケール
     */
    public void setLocale(final Locale locale) {
        this.locale = locale;
    }

    /**
     * ロケールを返します。
     *
     * @return ロケール
     */
    public Locale getLocale() {
        return this.locale;
    }

    /**
     * メッセージIDを返します。
     *
     * @return メッセージID
     */
    public String getMessageId() {
        return this.messageId;
    }

    /**
     * メッセージ引数配列を返します。
     *
     * @return メッセージ引数配列
     */
    public Object[] getMessageArgs() {
        if (this.messageArgs == null) {
            return null;
        }
        return this.messageArgs.clone();
    }

    @Override
    public String getMessage() {
        if (messageSource == null) {
            throw new RuntimeException("getMessageSourceメソッドを、対象レイヤのMessageSourceを返すようにオーバーライドしてください。");
        }
        return '[' + messageId + ']' + messageSource.getMessage(messageId, messageArgs, locale);
    }
}
