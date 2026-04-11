package me.suwash.util.validation.validators;

import java.nio.charset.Charset;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

/**
 * BeanValidation 文字コードチェック。
 */
public class CharsetValidator implements ConstraintValidator<me.suwash.util.validation.constraints.Charset, String> {

    @Override
    public void initialize(final me.suwash.util.validation.constraints.Charset constraintAnnotation) {
    }

    @Override
    public boolean isValid(final String charset, final ConstraintValidatorContext context) {
        if (StringUtils.isEmpty(charset)) {
            // 未設定は NotEmpty に移譲。
            return true;
        }

        try {
            Charset.forName(charset);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
