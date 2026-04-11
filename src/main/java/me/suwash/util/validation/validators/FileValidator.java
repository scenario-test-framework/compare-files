package me.suwash.util.validation.validators;

import java.io.File;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

/**
 * BeanValidation ファイルチェック。
 */
public class FileValidator implements ConstraintValidator<me.suwash.util.validation.constraints.File, String> {

    @Override
    public void initialize(final me.suwash.util.validation.constraints.File constraintAnnotation) {
    }

    @Override
    public boolean isValid(final String path, final ConstraintValidatorContext context) {
        if (StringUtils.isEmpty(path)) {
            // 未設定は NotEmpty に移譲
            return true;
        }

        final File file = new File(path);
        if (!file.exists()) {
            // 存在しない場合は ExistPath に移譲
            return true;
        }
        if (!file.isFile()) {
            return false;
        }
        return true;
    }

}
