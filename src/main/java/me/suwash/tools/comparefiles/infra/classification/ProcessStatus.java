package me.suwash.tools.comparefiles.infra.classification;

/**
 * 処理ステータス。
 */
public enum ProcessStatus {
    /** 処理中。 */
    Processing,
    /** 成功。 */
    Success,
    /** 警告。 */
    Warning,
    /** 失敗。 */
    Failure
}
