package me.suwash.tools.comparefiles.sv.domain.compare.file;

import java.util.Date;

import lombok.Getter;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.domain.compare.BaseCompareResult;

/**
 * ファイル比較結果の基底クラス。
 */
public abstract class BaseFileCompareResult extends BaseCompareResult {

    /** 左ファイルパス。 */
    @Getter
    protected String leftFilePath;

    /** 右ファイルパス。 */
    @Getter
    protected String rightFilePath;

    /** 比較レイアウト。 */
    @Getter
    protected FileLayout fileLayout;

    /** 比較した行数。 */
    @Getter
    protected long rowCount;

    /** 比較結果が「OK」の行数。 */
    @Getter
    protected long okRowCount;

    /** 比較結果が「NG」の行数。 */
    @Getter
    protected long ngRowCount;

    /** 比較結果が「除外」の行数。 */
    @Getter
    protected long ignoreRowCount;

    /** 比較結果が「左のみ」の行数。 */
    @Getter
    protected long leftOnlyRowCount;

    /** 比較結果が「右のみ」の行数。 */
    @Getter
    protected long rightOnlyRowCount;

    /**
     * 処理開始時刻 を返します。
     *
     * @return 処理開始時刻
     */
    public Date getStartTime() {
        if (startTime == null) {
            return null;
        } else {
            return (Date) startTime.clone();
        }
    }

    /**
     * 処理終了時刻 を返します。
     *
     * @return 処理終了時刻
     */
    public Date getEndTime() {
        if (endTime == null) {
            return null;
        } else {
            return (Date) endTime.clone();
        }
    }

    /**
     * 処理時間 を返します。
     *
     * @return length 処理時間
     */
    public long getLength() {
        if (startTime == null) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"startTime"});
        }
        if (endTime == null) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"endTime"});
        }
        return endTime.getTime() - startTime.getTime();
    }

}
