package me.suwash.tools.comparefiles.infra.util;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.Context;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.infra.exception.Errors;

/**
 * 妥当性チェックユーティリティ。
 */
public final class ValidateUtils {

    /**
     * ユーティリティクラスなので、コンストラクタを隠蔽。
     */
    private ValidateUtils() {}

    /**
     * 指定オブジェクトの妥当性を確認します。
     * 妥当性チェックエラーが存在する場合、Contextに登録して、例外をスローします。
     *
     * @param <T> 対象オブジェクトの型
     * @param target 対象オブジェクト
     * @param groups 妥当性チェックグループ
     */
    public static <T> void validate(final T target, final Class<?>... groups) {
        if (target == null) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"target"});
        }

        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        final Set<ConstraintViolation<T>> violations = validator.validate(target, groups);
        // エラー時は、Contextに保存して、例外をthrow
        if (!violations.isEmpty()) {
            final Errors errors = new Errors();
            errors.addViolations(violations);
            final Context context = Context.getInstance();
            context.setErrors(errors);
            throw new CompareFilesException(Const.MSGCD_ERROR_VALIDATE);
        }
    }

    /**
     * コンテキストに登録された妥当性チェックエラーのメッセージを返します。
     *
     * @return 妥当性チェックエラーメッセージ
     */
    public static String getViolationMessage() {
        String message = null;
        if (Context.getInstance().getErrors() != null && Context.getInstance().getErrors().size() != 0) {
            message = Context.getInstance().getErrors().toString();
        }
        return message;
    }

}
