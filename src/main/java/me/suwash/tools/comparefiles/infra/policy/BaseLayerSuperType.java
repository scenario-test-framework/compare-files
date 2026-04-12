package me.suwash.tools.comparefiles.infra.policy;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import me.suwash.tools.comparefiles.infra.classification.ProcessStatus;
import me.suwash.tools.comparefiles.infra.util.ValidateUtils;

/**
 * CUIツール向けLayerSuperTypeパターン基底クラス。
 * validationエラー発生時は、Contextに保存して、例外をthrowします。
 *
 * @param <I> Input
 * @param <O> Output
 */
@lombok.extern.slf4j.Slf4j
public abstract class BaseLayerSuperType<I extends Input, O extends Output<I>> {

    private static final String MSG_START = "START ";
    private static final String MSG_END = "END   ";

    /**
     * 処理を実行します。
     *
     * @param input 入力データモデル
     * @return 出力データモデル
     */
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

    /**
     * 入力データモデルのバリデーションを実行します。
     *
     * @param input 入力データモデル
     * @return バリデーション違反セット
     */
    public Set<ConstraintViolation<I>> validate(final I input) {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        return validator.validate(input);
    }

    /**
     * 事前処理を実行します。
     *
     * @param input 入力データモデル
     * @return 出力データモデル
     */
    protected abstract O preExecute(I input);

    /**
     * 主処理を実行します。
     *
     * @param input 入力データモデル
     * @param preExecuteOutput 事前処理の出力データモデル
     * @return 出力データモデル
     */
    protected abstract O mainExecute(I input, O preExecuteOutput);

    /**
     * 事後処理を実行します。
     *
     * @param input 入力データモデル
     * @param mainExecuteOutput 主処理の出力データモデル
     * @return 出力データモデル
     */
    protected abstract O postExecute(I input, O mainExecuteOutput);

}
