package me.suwash.tools.comparefiles.ap.dto;

import java.text.NumberFormat;

import lombok.Getter;
import lombok.Setter;
import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.tools.comparefiles.sv.domain.compare.bulk.BaseBulkCompareResult;
import me.suwash.util.CompareUtils.CompareStatus;

/**
 * ディレクトリ比較機能 出力データモデル基底クラス。
 *
 * @param <I> 入力データモデル
 */
@Getter
@Setter
@lombok.extern.slf4j.Slf4j
public abstract class BaseDirCompareFacadeOutput<I extends BaseFacadeInput> extends BaseFacadeOutput<I> {

    /** 成功数。 */
    private int successCount;

    /** エラー数。 */
    private int errorCount;

    /** 比較ファイル数。 */
    private int fileCount;

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
     * 比較結果を返します。
     *
     * @return 比較結果
     */
    public CompareStatus getResult() {
        if (fileCount == 0 && errorCount == 0) {
            // 比較ファイル件数、エラー件数が0 → 比較対象が0件
            return CompareStatus.Ignore;

        } else if (errorCount > 0) {
            // エラー件数が0以外 → エラー
            return CompareStatus.Error;

        } else if (fileCount > 0 && fileCount == (fileOkCount + fileIgnoreCount)) {
            // 比較ファイル件数が1件以上、OK+除外と一致 → 比較結果としてはOK
            return CompareStatus.OK;

        } else {
            return CompareStatus.NG;
        }
    }

    /**
     * 一括比較結果を設定します。
     *
     * @param result 一括比較結果
     */
    public void setResult(BaseBulkCompareResult result) {
        this.fileCount = result.getTotalCount();
        this.successCount = result.getSuccessCount();
        this.errorCount = result.getErrorCount();
        this.fileOkCount = result.getFileOkCount();
        this.fileNgCount = result.getFileNgCount();
        this.fileIgnoreCount = result.getFileIgnoreCount();
        this.fileLeftOnlyCount = result.getFileLeftOnlyCount();
        this.fileRightOnlyCount = result.getFileRightOnlyCount();
    }

    /**
     * 処理ステータス＝処理中の場合、完了ステータスに更新します。
     */
    public void fixProcessStatus() {
        if (ProcessStatus.Processing.equals(processStatus)) {
            switch (getResult()) {
                case Error:
                    processStatus = ProcessStatus.Failure;
                    break;
                case OK:
                case Ignore:
                    processStatus = ProcessStatus.Success;
                    break;
                default:
                    processStatus = ProcessStatus.Warning;
                    break;
            }
        }
    }

    /**
     * 保持している結果をロガー出力します。
     */
    public void printDetails() {
        final NumberFormat numFormat = NumberFormat.getNumberInstance();

        final String successCount = numFormat.format(this.getSuccessCount());
        final String errorCount = numFormat.format(this.getErrorCount());

        final CompareStatus status = this.getResult();
        final String fileCount = numFormat.format(this.getFileCount());
        final String fileOkCount = numFormat.format(this.getFileOkCount());
        final String fileNgCount = numFormat.format(this.getFileNgCount());
        final String fileIgnoreCount = numFormat.format(this.getFileIgnoreCount());
        final String fileLeftOnlyCount = numFormat.format(this.getFileLeftOnlyCount());
        final String fileRightOnlyCount = numFormat.format(this.getFileRightOnlyCount());

        log.info("・処理ファイル件数");
        log.info("  ・成功         : " + successCount);
        log.info("  ・失敗         : " + errorCount);
        log.info("・比較結果");
        log.info("  ・ディレクトリ単位");
        log.info("    ・比較結果   : " + status);
        log.info("  ・ファイル単位");
        log.info("    ・総件数     : " + fileCount);
        log.info("    ・OK件数     : " + fileOkCount);
        log.info("    ・NG件数     : " + fileNgCount);
        log.info("    ・除外件数   : " + fileIgnoreCount);
        log.info("    ・左のみ件数 : " + fileLeftOnlyCount);
        log.info("    ・右のみ件数 : " + fileRightOnlyCount);
    }

}
