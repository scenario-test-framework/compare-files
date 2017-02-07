package me.suwash.tools.comparefiles.ap.dto;

import java.util.Set;

import javax.validation.ConstraintViolation;

import lombok.Getter;
import lombok.Setter;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;

/**
 * 対象ファイル名正規表現リスト比較機能 出力データモデル。
 */
@Getter
@Setter
public class FileRegexCompareFacadeOutput extends BaseDirCompareFacadeOutput<FileRegexCompareFacadeInput> {

    /** 入力データモデル。 */
    private FileRegexCompareFacadeInput input;

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.Output#getInput()
     */
    @Override
    public FileRegexCompareFacadeInput getInput() {
        return input;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.Output#setViolationSet(java.util.Set)
     */
    @Override
    public void setViolationSet(Set<ConstraintViolation<FileRegexCompareFacadeInput>> violationSet) {
        throw new CompareFilesException(Const.UNSUPPORTED);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.Output#getViolationSet()
     */
    @Override
    public Set<ConstraintViolation<FileRegexCompareFacadeInput>> getViolationSet() {
        throw new CompareFilesException(Const.UNSUPPORTED);
    }

}
