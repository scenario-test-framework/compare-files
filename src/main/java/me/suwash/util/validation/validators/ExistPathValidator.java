package me.suwash.util.validation.validators;

import java.io.File;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import me.suwash.util.validation.constraints.ExistPath;

/**
 * BeanValidation Path存在チェック。
 */
public class ExistPathValidator implements ConstraintValidator<ExistPath, String> {

    @Override
    public void initialize(final ExistPath constraintAnnotation) {
    }

    @Override
    public boolean isValid(final String path, final ConstraintValidatorContext context) {
        if (StringUtils.isEmpty(path)) {
            // 未設定は NotEmpty に移譲。
            return true;
        }

        final File file = new File(path);
        if (!file.exists()) {
            return false;
        }
        return true;
    }

}
