package me.suwash.tools.comparefiles.sv.domain.compare.bulk;

import lombok.Getter;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.domain.compare.BaseCompareResult;
import me.suwash.util.CompareUtils.CompareStatus;
import me.suwash.util.JsonUtils;

/**
 * 一括比較結果の基底クラス。
 */
@Getter
public abstract class BaseBulkCompareResult extends BaseCompareResult {

    /** 比較件数。 */
    private int totalCount;

    /** 成功数。 */
    private int successCount;

    /** エラー数。 */
    private int errorCount;

    /** 比較結果「OK」ファイル数。 */
    private int fileOkCount;

    /** 比較結果「NG」ファイル数。 */
    private int fileNgCount;

    /** 比較結果「除外」ファイル数。 */
    private int fileIgnoreCount;

    /** 比較結果「左のみ」ファイル数。 */
    private int fileLeftOnlyCount;

    /** 比較結果「右のみ」ファイル数。 */
    private int fileRightOnlyCount;

    /**
     * 一括比較結果を返します。
     *
     * @return 比較結果
     */
    public CompareStatus getResult() {
        if (totalCount == 0 && errorCount == 0) {
            // 比較件数、エラー件数が0 → 比較対象が0件
            return CompareStatus.Ignore;

        } else if (totalCount > 0 && totalCount == successCount) {
            // 比較件数が1件以上、成功件数と一致 → 比較結果としてはOK
            return CompareStatus.OK;

        } else {
            return CompareStatus.NG;
        }
    }

    /**
     * ファイル比較結果を追加します。
     *
     * @param status ファイル比較結果ステータス
     */
    protected void addFileResult(final CompareStatus status) {
        totalCount++;

        if (CompareStatus.Error.equals(status)) {
            errorCount++;
            return;
        }

        successCount++;
        switch (status) {
            case OK:
                fileOkCount++;
                break;
            case NG:
                fileNgCount++;
                break;
            case Ignore:
                fileIgnoreCount++;
                break;
            case LeftOnly:
                fileLeftOnlyCount++;
                break;
            case RightOnly:
                fileRightOnlyCount++;
                break;
            default:
                throw new CompareFilesException(Const.MSGCD_ERROR_COMPARE_ILLEGAL_STATUS, new Object[] {status});
        }
    }

    /**
     * ファイル比較のエラー件数をインクリメントします。
     * ※例外発生時にファイル比較結果が取得できない状況で、件数を追加する場合を想定しています。
     */
    protected void addFileError() {
        totalCount++;
        errorCount++;
    }

    /*
     * (非 Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return JsonUtils.writeString(this);
    }

}
