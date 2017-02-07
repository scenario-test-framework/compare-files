package me.suwash.tools.comparefiles.ap.dto;

import java.text.NumberFormat;
import java.util.Set;

import javax.validation.ConstraintViolation;

import lombok.Getter;
import lombok.Setter;
import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.domain.compare.file.FileCompareResult;
import me.suwash.util.CompareUtils.CompareStatus;

/**
 * ファイル比較機能 出力データモデル。
 */
@Getter
@Setter
@lombok.extern.slf4j.Slf4j
public class FileCompareFacadeOutput extends BaseFacadeOutput<FileCompareFacadeInput> {

    /** 入力データモデル。 */
    private FileCompareFacadeInput input;

    /** ファイル比較結果。 */
    private FileCompareResult result;

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.Output#getInput()
     */
    @Override
    public FileCompareFacadeInput getInput() {
        return input;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.Output#setViolationSet(java.util.Set)
     */
    @Override
    public void setViolationSet(Set<ConstraintViolation<FileCompareFacadeInput>> violationSet) {
        throw new CompareFilesException(Const.UNSUPPORTED);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.Output#getViolationSet()
     */
    @Override
    public Set<ConstraintViolation<FileCompareFacadeInput>> getViolationSet() {
        throw new CompareFilesException(Const.UNSUPPORTED);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.ap.dto.BaseFacadeOutput#setProcessStatus(me.suwash.ddd.classification.ProcessStatus)
     */
    @Override
    public void setProcessStatus(ProcessStatus processStatus) {
        throw new CompareFilesException(Const.UNSUPPORTED);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.ap.dto.BaseFacadeOutput#getProcessStatus()
     */
    @Override
    public ProcessStatus getProcessStatus() {
        if (result == null) {
            return ProcessStatus.Processing;
        }

        switch (result.getStatus()) {
            case OK:
            case Ignore:
                return ProcessStatus.Success;
            case NG:
            case LeftOnly:
            case RightOnly:
                return ProcessStatus.Warning;
            case Error:
            default:
                return ProcessStatus.Failure;
        }
    }

    /**
     * 保持している結果をロガー出力します。
     */
    public void printDetails() {
        final NumberFormat numFormat = NumberFormat.getNumberInstance();

        final FileCompareResult result = this.getResult();
        final CompareStatus status = result.getStatus();
        final String rowCount = numFormat.format(result.getRowCount());
        final String rowOkCount = numFormat.format(result.getOkRowCount());
        final String rowNgCount = numFormat.format(result.getNgRowCount());
        final String rowIgnoreCount = numFormat.format(result.getIgnoreRowCount());
        final String rowLeftOnlyCount = numFormat.format(result.getLeftOnlyRowCount());
        final String rowRightOnlyCount = numFormat.format(result.getRightOnlyRowCount());

        log.info("・比較結果");
        log.info("  ・ファイル単位");
        log.info("    ・比較結果   : " + status);
        log.info("  ・行単位");
        log.info("    ・総件数     : " + rowCount);
        log.info("    ・OK件数     : " + rowOkCount);
        log.info("    ・NG件数     : " + rowNgCount);
        log.info("    ・除外件数   : " + rowIgnoreCount);
        log.info("    ・左のみ件数 : " + rowLeftOnlyCount);
        log.info("    ・右のみ件数 : " + rowRightOnlyCount);
    }

}
