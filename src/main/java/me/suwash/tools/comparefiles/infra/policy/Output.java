package me.suwash.tools.comparefiles.infra.policy;

import java.util.Set;

import jakarta.validation.ConstraintViolation;

import me.suwash.tools.comparefiles.infra.classification.ProcessStatus;

/**
 * 出力データモデルインタフェース。
 *
 * @param <I> 入力データモデル
 */
public interface Output<I> {

    /**
     * 入力データモデルを返します。
     *
     * @return 入力データモデル
     */
    I getInput();

    /**
     * バリデーション違反セットを設定します。
     *
     * @param violationSet バリデーション違反セット
     */
    void setViolationSet(Set<ConstraintViolation<I>> violationSet);

    /**
     * バリデーション違反セットを返します。
     *
     * @return バリデーション違反セット
     */
    Set<ConstraintViolation<I>> getViolationSet();

    /**
     * 処理ステータスを設定します。
     *
     * @param processStatus 処理ステータス
     */
    void setProcessStatus(ProcessStatus processStatus);

    /**
     * 処理ステータスを返します。
     *
     * @return 処理ステータス
     */
    ProcessStatus getProcessStatus();
}
