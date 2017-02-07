package me.suwash.tools.comparefiles.infra.exception;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.i18n.CompareFilesMessageSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.hateoas.VndErrors;
import org.springframework.hateoas.VndErrors.VndError;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * チェックエラー。
 */
@lombok.NoArgsConstructor
public class Errors extends VndErrors implements Iterable<VndError> {

    /** エラー。 */
    private VndErrors errors;

    /**
     * コンストラクタ。
     *
     * @param logref パス
     * @param message メッセージ
     */
    public Errors(final String logref, final String message) {
        super();
        errors = new VndErrors(logref, message);
    }

    /**
     * コンストラクタ。
     *
     * @param exception 基底例外
     */
    public Errors(final CompareFilesException exception) {
        super();

        String logref = StringUtils.EMPTY;
        final Object[] messageArgs = exception.getMessageArgs();
        if (messageArgs != null) {
            logref = messageArgs[0].toString();
        }
        errors = new VndErrors(logref, exception.getMessage());
    }

    /**
     * 登録されているチェックエラー群を返します。
     *
     * @return VndErrors チェックエラー群
     */
    @JsonValue
    public VndErrors getErrors() {
        return errors;
    }

    /**
     * チェックエラーを追加します。
     *
     * @param error チェックエラー
     */
    @Override
    public VndErrors add(final VndError error) {
        if (error == null) {
            throw new CompareFilesException("check.notNull", new Object[] {
                "error"
            });
        }

        if (errors == null) {
            errors = new VndErrors(error);
        } else {
            errors = errors.add(error);
        }
        return errors;
    }

    /**
     * チェックエラーを追加します。
     *
     * @param logref パス
     * @param messageId メッセージID
     * @param messageArgs メッセージ引数
     */
    public void add(final String logref, final String messageId, final Object... messageArgs) {
        if (StringUtils.isEmpty(logref)) {
            throw new CompareFilesException("check.notNull", new Object[] {
                "logref"
            });
        }
        if (StringUtils.isEmpty(messageId)) {
            throw new CompareFilesException("check.notNull", new Object[] {
                "messageId"
            });
        }

        final VndError error = new VndError(logref, getMessageSource().getMessage(messageId, messageArgs));
        if (errors == null) {
            errors = new VndErrors(error);
        } else {
            errors = errors.add(error);
        }
    }

    /**
     * MessageSourceを返します。
     *
     * @return MessageSource
     */
    protected CompareFilesMessageSource getMessageSource() {
        return CompareFilesMessageSource.getInstance();
    }

    /*
     * (非 Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<VndError> iterator() {
        return errors.iterator();
    }

    /**
     * 登録されているエラー数を返します。
     *
     * @return エラー数
     */
    public int size() {
        if (errors == null) {
            return 0;
        }
        int size = 0;
        for (@SuppressWarnings("unused") final VndError error : errors) {
            size++;
        }
        return size;
    }

    /**
     * エラーをクリアします。
     */
    public void clear() {
        if (errors != null) {
            errors = null;
        }
    }

    /**
     * バリデーション結果をエラーとして追加します。
     *
     * @param <T> バリデート対象
     * @param violations バリデーション結果
     */
    public <T> void addViolations(final Set<ConstraintViolation<T>> violations) {
        if (violations == null) {
            throw new CompareFilesException("check.notNull", new Object[] {"violations"});
        }

        // ValidationMessages.properties で 実際に利用するメッセージIDをvalueとして設定している。
        // → BeanValidator#validateで、messageIdと、その引数リストを自動設定
        // → BeanValidator#getMessageで、設定値を取得
        // → ここでメッセージ変換
        for (final ConstraintViolation<T> curViolation : violations) {
            final String messageId = curViolation.getMessage();
            final Object invalidValue = curViolation.getInvalidValue();
            final String curMessage = getMessageSource().getMessage(messageId, curViolation.getExecutableParameters());
            this.add(newVndError(
                curViolation.getRootBean().getClass().getSimpleName() + "." + curViolation.getPropertyPath().toString(),
                getMessageSource().getMessage(Const.MSGCD_WRAP_VALIDATE_ERROR, new Object[] {
                    curMessage, invalidValue
                })
                ));
        }
    }

    /**
     * logrefにプリフィックスを付与します。
     *
     * @param prefix logrefに付与するprefix
     * @return prefixを付与したエラーオブジェクト
     */
    public Errors addLogrefPrefix(final String prefix) {
        if (StringUtils.isEmpty(prefix)) {
            return this;
        }

        final List<VndError> errorList = new ArrayList<VndError>();
        for (final VndError error : errors) {
            errorList.add(newVndError(prefix + "." + error.getLogref(), error.getMessage()));
        }
        errors = new VndErrors(errorList);
        return this;
    }

    /**
     * VndErrorを返します。
     *
     * @param logref パス
     * @param message メッセージ
     * @return VndError
     */
    private VndError newVndError(final String logref, final String message) {
        return new VndError(logref, message);
    }

    /*
     * (非 Javadoc)
     * @see org.springframework.hateoas.VndErrors#toString()
     */
    @Override
    public String toString() {
        if (errors == null) {
            return "null";
        }

        final StringBuilder sb = new StringBuilder();
        for (final VndError curError : errors) {
            sb.append(curError.getLogref()).append(':').append(curError.getMessage()).append('\n');
        }
        return sb.toString();
    }

    /*
     * (非 Javadoc)
     * @see org.springframework.hateoas.VndErrors#hashCode()
     */
    @Override
    public int hashCode() {
        if (errors == null) {
            return super.hashCode();
        }
        return errors.hashCode();
    }

    /*
     * (非 Javadoc)
     * @see org.springframework.hateoas.VndErrors#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (errors == null) {
            return super.equals(obj);
        }
        return errors.equals(obj);
    }

}
