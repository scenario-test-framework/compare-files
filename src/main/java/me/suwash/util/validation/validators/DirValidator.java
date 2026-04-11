package me.suwash.util.validation.validators;

import java.io.File;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import me.suwash.util.validation.constraints.Dir;

/**
 * BeanValidation ディレクトリチェック。
 */
public class DirValidator implements ConstraintValidator<Dir, String> {

    @Override
    public void initialize(final Dir constraintAnnotation) {
    }

    @Override
    public boolean isValid(final String path, final ConstraintValidatorContext context) {
        if (StringUtils.isEmpty(path)) {
            // 未設定は NotEmpty に移譲。
            return true;
        }

        final File dir = new File(path);
        if (!dir.exists()) {
            // 存在しない場合は ExistPath に移譲
            return true;
        }
        if (!dir.isDirectory()) {
            return false;
        }
        return true;
    }

}
