package me.suwash.tools.comparefiles.sv.domain.compare;

import java.util.Date;

import lombok.Getter;
import me.suwash.util.CompareUtils.CompareStatus;

/**
 * 比較結果の基底クラス。
 */
public abstract class BaseCompareResult implements CompareResult {

    /** 比較ステータス。 */
    @Getter
    protected CompareStatus status = CompareStatus.Processing;

    /** 処理開始時刻。 */
    protected Date startTime;

    /** 処理終了時刻。 */
    protected Date endTime;

    /** 出力ファイル拡張子。 */
    protected String outputExt;

    /**
     * コンストラクタ。
     */
    protected BaseCompareResult() {
        super();
    }

}
