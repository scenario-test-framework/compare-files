package me.suwash.tools.comparefiles.ap.dto;

import java.util.Set;

import jakarta.validation.ConstraintViolation;

import lombok.Getter;
import lombok.Setter;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;

/**
 * ディレクトリ比較機能 出力データモデル。
 */
@Getter
@Setter
public class DirCompareFacadeOutput extends BaseDirCompareFacadeOutput<DirCompareFacadeInput> {

    /** 入力データモデル。 */
    private DirCompareFacadeInput input;

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.infra.policy.Output#getInput()
     */
    @Override
    public DirCompareFacadeInput getInput() {
        return input;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.infra.policy.Output#setViolationSet(java.util.Set)
     */
    @Override
    public void setViolationSet(Set<ConstraintViolation<DirCompareFacadeInput>> violationSet) {
        throw new CompareFilesException(Const.UNSUPPORTED);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.infra.policy.Output#getViolationSet()
     */
    @Override
    public Set<ConstraintViolation<DirCompareFacadeInput>> getViolationSet() {
        throw new CompareFilesException(Const.UNSUPPORTED);
    }

}
