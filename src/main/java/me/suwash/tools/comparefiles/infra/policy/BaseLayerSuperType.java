package me.suwash.tools.comparefiles.infra.policy;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.ddd.policy.GenericLayerSuperType;
import me.suwash.ddd.policy.Input;
import me.suwash.ddd.policy.LayerSuperType;
import me.suwash.ddd.policy.Output;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.infra.util.ValidateUtils;

/**
 * CUIツール向けLayerSuperTypeパターン基底クラス。
 * validationエラー発生時は、Contextに保存して、例外をthrowします。
 *
 * @param <I> Input
 * @param <O> Output
 */
@lombok.extern.slf4j.Slf4j
public abstract class BaseLayerSuperType<I extends Input, O extends Output<I>> extends GenericLayerSuperType<I, O> implements LayerSuperType<I, O> {

    private static final String MSG_START = "START ";
    private static final String MSG_END = "END   ";

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#execute(me.suwash.ddd.policy.Input)
     */
    @Override
    public O execute(final I input) {
        final String className = this.getClass().getSimpleName();
        // validate
        log.debug(MSG_START + className + "#validate");
        ValidateUtils.validate(input);
        log.debug(MSG_END + className + "#validate");

        // preExecute
        log.debug(MSG_START + className + "#preExecute");
        final O preExecuteOutput = preExecute(input);
        log.debug(MSG_END + className + "#preExecute");
        if (ProcessStatus.Failure.equals(preExecuteOutput.getProcessStatus())) {
            return preExecuteOutput;
        }

        // main
        log.debug(MSG_START + className + "#mainExecute");
        final O mainExecuteOutput = mainExecute(input, preExecuteOutput);
        log.debug(MSG_END + className + "#mainExecute");
        if (ProcessStatus.Failure.equals(mainExecuteOutput.getProcessStatus())) {
            return mainExecuteOutput;
        }

        // postExecute
        log.debug(MSG_START + className + "#postExecute");
        final O postExecuteOutput = postExecute(input, mainExecuteOutput);
        log.debug(MSG_END + className + "#postExecute");
        return postExecuteOutput;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.LayerSuperType#validate(me.suwash.ddd.policy.Input)
     */
    @Override
    public Set<ConstraintViolation<I>> validate(final I input) {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        return validator.validate(input);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#getOutput(me.suwash.ddd.policy.Input)
     */
    @Override
    protected O getOutput(final I input) {
        // validationエラー発生時は、Context保存するので、不使用。
        throw new CompareFilesException(Const.UNSUPPORTED);
    }

}
