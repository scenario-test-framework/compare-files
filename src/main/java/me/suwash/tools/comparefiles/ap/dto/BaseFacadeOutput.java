package me.suwash.tools.comparefiles.ap.dto;

import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.ddd.policy.Output;

/**
 * アプリケーション層 出力データモデル 基底クラス。
 *
 * @param <I> 入力データモデル
 */
public abstract class BaseFacadeOutput<I extends BaseFacadeInput> implements Output<I> {

    /** 処理ステータス。 */
    protected ProcessStatus processStatus = ProcessStatus.Processing;

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.Output#setProcessStatus(me.suwash.ddd.classification.ProcessStatus)
     */
    @Override
    public void setProcessStatus(ProcessStatus processStatus) {
        this.processStatus = processStatus;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.Output#getProcessStatus()
     */
    @Override
    public ProcessStatus getProcessStatus() {
        return processStatus;
    }

}
