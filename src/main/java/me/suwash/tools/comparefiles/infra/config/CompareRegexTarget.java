package me.suwash.tools.comparefiles.infra.config;

import java.io.File;
import java.util.regex.Pattern;

import lombok.Getter;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;

import org.apache.commons.lang3.StringUtils;

/**
 * ファイル名正規表現指定の比較対象設定。
 */
@Getter
public class CompareRegexTarget {

    /** 左ディレクトリ。 */
    private final File leftDir;

    /** 右ディレクトリ。 */
    private final File rightDir;

    /** ファイル名正規表現。 */
    private final String fileNameRegex;

    /** ファイル名正規表現Pattern。 */
    private Pattern pattern;

    /**
     * コンストラクタ。
     *
     * @param leftDirPath 左ディレクトリパス
     * @param rightDirPath 右ディレクトリパス
     * @param fileNameRegex ファイル名正規表現
     */
    public CompareRegexTarget(final String leftDirPath, final String rightDirPath, final String fileNameRegex) {
        if (StringUtils.isEmpty(leftDirPath)) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"leftDirPath"});
        }
        if (StringUtils.isEmpty(rightDirPath)) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"rightDirPath"});
        }
        if (StringUtils.isEmpty(fileNameRegex)) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"fileNameRegex"});
        }
        try {
            this.pattern = Pattern.compile(fileNameRegex);
        } catch (Exception e) {
            throw new CompareFilesException(Const.MSGCD_ERROR_REGEX_PARSE, new Object[] {fileNameRegex}, e);
        }

        this.leftDir = new File(leftDirPath);
        this.rightDir = new File(rightDirPath);
        this.fileNameRegex = fileNameRegex;
    }

}
