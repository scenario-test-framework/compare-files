package me.suwash.tools.comparefiles.sv.domain.compare.bulk;

import java.io.File;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;
import me.suwash.tools.comparefiles.infra.config.CompareRegexTarget;
import me.suwash.tools.comparefiles.infra.config.CompareRegexTargetList;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.infra.i18n.CompareFilesMessageSource;
import me.suwash.tools.comparefiles.infra.policy.AggregateEntity;
import me.suwash.tools.comparefiles.infra.util.ValidateUtils;
import me.suwash.tools.comparefiles.sv.da.file.repository.impl.FileCompareResultRepositoryImpl;
import me.suwash.tools.comparefiles.sv.domain.compare.BaseCompareInput;
import me.suwash.tools.comparefiles.sv.domain.compare.file.FileCompareResult;
import me.suwash.util.CompareUtils.CompareStatus;
import me.suwash.util.FileUtils;
import me.suwash.util.validation.constraints.ExistPath;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * 対象ファイル名正規表現指定の比較結果。
 */
@Getter
@lombok.extern.slf4j.Slf4j
public class FileNameRegexCompareResult extends BaseBulkCompareResult implements AggregateEntity {

    private static final String MSG_DIR_NOT_EXIST = "[DirNotExist]";
    private static final String MSG_FILE_UNMATCHED = "[FileUnmatched]";

    private final FileNameRegexCompareInput input;

    /**
     * コンストラクタ。
     *
     * @param targetConfigFilePath 比較対象設定ファイルパス
     * @param outputDirPath 出力ディレクトリパス
     * @param systemConfig システム設定
     */
    public FileNameRegexCompareResult(
        final String targetConfigFilePath,
        final String outputDirPath,
        final CompareFilesConfig systemConfig) {

        super();
        final FileNameRegexCompareInput input = new FileNameRegexCompareInput();
        input.setTargetConfigFilePath(targetConfigFilePath);
        input.setOutputDirPath(outputDirPath);
        input.setSystemConfig(systemConfig);
        this.input = input;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.domain.compare.CompareResult#compare()
     */
    @Override
    public void compare() {
        // --------------------------------------------------------------------------------
        // 事前処理
        // --------------------------------------------------------------------------------
        final String targetConfigFilePath = input.getTargetConfigFilePath();
        final String outputDirPath = input.getOutputDirPath();
        final CompareFilesConfig systemConfig = input.getSystemConfig();

        // 単項目チェック
        ValidateUtils.validate(input);

        // 出力ディレクトリ
        FileUtils.initDir(outputDirPath);

        // --------------------------------------------------------------------------------
        // 本処理
        // --------------------------------------------------------------------------------
        // 実行開始時刻
        final Date startTime = new Date();

        // ファイル比較結果リポジトリ
        final FileCompareResultRepositoryImpl resultRepo = new FileCompareResultRepositoryImpl(
            outputDirPath + "/" + systemConfig.getCompareResultFileName(),
            systemConfig.getOutputCharset(),
            null,
            systemConfig.getChunkSize()
            );
        resultRepo.begin();

        // 比較対象設定の読込み
        log.info("・比較対象設定の読込み");
        final CompareRegexTargetList targetList = new CompareRegexTargetList(targetConfigFilePath, Const.CHARSET_DEFAULT_CONFIG);

        // 比較
        log.info("・比較");
        for (final CompareRegexTarget target : targetList) {
            compareTargetConfig(target, systemConfig, resultRepo, startTime);
        }

        // サマリーファイルwriterをclose
        resultRepo.commit();

        // --------------------------------------------------------------------------------
        // 事後処理
        // --------------------------------------------------------------------------------
        // workディレクトリの削除
        if (systemConfig.isDeleteWorkDir()) {
            final String workDirPath = systemConfig.getOutputDir() + "/" + Const.DIRNAME_WORK;
            FileUtils.rmdirs(workDirPath);
        }
    }

    /**
     * 比較対象設定に従ってファイルを比較します。
     *
     * @param target 比較対象設定
     * @param systemConfig システム設定
     * @param resultRepo ファイル比較結果リポジトリ
     * @param startTime 処理開始時刻
     */
    private void compareTargetConfig(
        final CompareRegexTarget target,
        final CompareFilesConfig systemConfig,
        final FileCompareResultRepositoryImpl resultRepo,
        final Date startTime) {

        // ファイル名正規表現
        final Pattern pattern = target.getPattern();

        // ----------------------------------------------------------------------
        // 左定義チェック
        // ----------------------------------------------------------------------
        // 左ディレクトリ
        final File leftDir = target.getLeftDir();
        final boolean isExistLeftDir = leftDir.isDirectory();

        // 左ファイル
        File leftFile = null;
        if (isExistLeftDir) {
            leftFile = getMatchedTargetFile(leftDir, pattern);
        }
        boolean isLeftFileMatched = false;
        if (leftFile != null) {
            isLeftFileMatched = true;
        }

        // ----------------------------------------------------------------------
        // 右定義チェック
        // ----------------------------------------------------------------------
        // 右ディレクトリ
        final File rightDir = target.getRightDir();
        final boolean isExistRightDir = rightDir.isDirectory();

        // 右ファイル
        File rightFile = null;
        if (isExistRightDir) {
            rightFile = getMatchedTargetFile(rightDir, pattern);
        }
        boolean isRightFileMatched = false;
        if (rightFile != null) {
            isRightFileMatched = true;
        }

        // ----------------------------------------------------------------------
        // 比較実行
        // ----------------------------------------------------------------------
        if (isLeftFileMatched && isRightFileMatched) {
            // 左右どちらも比較チェックを通過した場合、比較を実行
            compareFile(leftFile, rightFile, systemConfig.getOutputDir(), systemConfig, resultRepo);

        } else {
            // 比較チェックにNGが存在する場合、サマリー出力
            final String fileRegex = target.getFileNameRegex();
            writeNgResult(isExistLeftDir, isExistRightDir, leftDir, leftFile, rightDir, rightFile, fileRegex, resultRepo, startTime);
        }
    }

    /**
     * 指定ディレクトリ直下で、はじめに正規表現にマッチしたファイルを返します。
     *
     * @param targetDir 対象ディレクトリ
     * @param pattern 正規表現
     * @return はじめにマッチしたファイル
     */
    private File getMatchedTargetFile(final File targetDir, final Pattern pattern) {
        // 全ファイルループ
        final File[] files = targetDir.listFiles();
        if (files != null) {
            for (final File curFile : files) {
                // 正規表現のマッチを確認
                final Matcher matcher = pattern.matcher(curFile.getName());
                if (matcher.matches()) {
                    // はじめにマッチしたファイルを比較対象に設定
                    return curFile;
                }
            }
        }
        return null;
    }

    /**
     * チェックNGの結果を出力します。
     *
     * @param isExistLeftDir 左ディレクトリが存在するか
     * @param isExistRightDir 右ディレクトリが存在するか
     * @param leftDir 左ディレクトリ
     * @param leftFile 左ファイル
     * @param rightDir 右ディレクトリ
     * @param rightFile 右ファイル
     * @param fileRegex 正規表現
     * @param resultRepo ファイル比較結果リポジトリ
     * @param startTime 処理開始時刻
     */
    private void writeNgResult(
        final boolean isExistLeftDir,
        final boolean isExistRightDir,
        final File leftDir,
        final File leftFile,
        final File rightDir,
        final File rightFile,
        final String fileRegex,
        final FileCompareResultRepositoryImpl resultRepo,
        final Date startTime) {

        String leftFilePath = null;
        String rightFilePath = null;
        CompareStatus compareStatus = null;

        // ----------------------------------------------------------------------
        // 左チェック結果確認
        // ----------------------------------------------------------------------
        if (isExistLeftDir) {
            if (isExistRightDir) {
                // 左Dir：○、右Dir：○
                if (leftFile == null) {
                    leftFilePath = MSG_FILE_UNMATCHED + leftDir.getAbsolutePath() + File.separator + fileRegex;

                    if (rightFile == null) {
                        // 左Dir：○、右Dir：○、左File：×、右ファイル：×
                        rightFilePath = MSG_FILE_UNMATCHED + rightDir.getAbsolutePath() + File.separator + fileRegex;
                        compareStatus = CompareStatus.Error;

                    } else {
                        // 左Dir：○、右Dir：○、左File：×、右ファイル：○
                        rightFilePath = rightFile.getAbsolutePath();
                        compareStatus = CompareStatus.RightOnly;
                    }

                } else {
                    leftFilePath = leftFile.getAbsolutePath();

                    if (rightFile == null) {
                        // 左Dir：○、右Dir：○、左File：○、右ファイル：×
                        rightFilePath = MSG_FILE_UNMATCHED + rightDir.getAbsolutePath() + File.separator + fileRegex;
                        compareStatus = CompareStatus.LeftOnly;

                    } else {
                        // 左Dir：○、右Dir：○、左File：○、右ファイル：○ の場合、この処理は呼ばれない想定
                        rightFilePath = rightFile.getAbsolutePath();
                        throw new CompareFilesException(Const.UNSUPPORTED);
                    }
                }

            } else {
                // 左Dir：○、右Dir：×
                rightFilePath = MSG_DIR_NOT_EXIST + rightDir.getAbsolutePath();
                compareStatus = CompareStatus.LeftOnly;
                if (leftFile == null) {
                    // 左Dir：○、右Dir：×、左File：×、右ファイル：-
                    leftFilePath = leftDir.getAbsolutePath();
                } else {
                    // 左Dir：○、右Dir：×、左File：○、右ファイル：-
                    leftFilePath = leftFile.getAbsolutePath();
                }
            }

        } else {
            leftFilePath = MSG_DIR_NOT_EXIST + leftDir.getAbsolutePath();

            if (isExistRightDir) {
                // 左Dir：×、右Dir：○
                compareStatus = CompareStatus.RightOnly;
                if (rightFile == null) {
                    // 左Dir：×、右Dir：○、左File：-、右ファイル：×
                    rightFilePath = rightDir.getAbsolutePath();
                } else {
                    // 左Dir：×、右Dir：○、左File：-、右ファイル：○
                    rightFilePath = rightFile.getAbsolutePath();
                }

            } else {
                // 左Dir：×、右Dir：×、左File：-、右ファイル：-
                rightFilePath = MSG_DIR_NOT_EXIST + rightDir.getAbsolutePath();
                compareStatus = CompareStatus.Error;
            }
        }

        // ----------------------------------------------------------------------
        // サマリー出力
        // ----------------------------------------------------------------------
        final FileCompareResult result = FileCompareResult.getFixedResult(compareStatus, leftFilePath, rightFilePath, startTime, startTime);
        resultRepo.write(result);

        // ----------------------------------------------------------------------
        // DTO更新
        // ----------------------------------------------------------------------
        switch (compareStatus) {
            case LeftOnly:
            case RightOnly:
                this.addFileResult(compareStatus);
                break;
            default:
                this.addFileError();
                break;
        }
    }

    /**
     * ファイル比較を実行します。
     *
     * @param leftFile 左ファイル
     * @param rightFile 右ファイル
     * @param outputDirPath 出力ディレクトリパス
     * @param systemConfig システム設定
     * @param resultRepo ファイル比較結果リポジトリ
     */
    private void compareFile(
        final File leftFile,
        final File rightFile,
        final String outputDirPath,
        final CompareFilesConfig systemConfig,
        final FileCompareResultRepositoryImpl resultRepo) {

        // 左ファイルパス
        final String leftFilePath = leftFile.getPath();
        // 右ファイルパス
        final String rightFilePath = rightFile.getPath();

        // ファイル比較時の設定上書き項目を退避
        final boolean isSorted = systemConfig.isSorted();

        // ファイル比較実行
        try {
            final FileCompareResult result = new FileCompareResult(leftFilePath, rightFilePath, outputDirPath, systemConfig);
            result.compare();

            // 結果を一括結果ファイルに出力
            resultRepo.write(result);

            // 結果確認
            this.addFileResult(result.getStatus());

        } catch (Exception e) {
            log.error(
                CompareFilesMessageSource.getInstance().getMessage(Const.ERRORHANDLE, new Object[] {"FileCompare", "left=" + leftFilePath + ", right=" + rightFilePath}),
                e);

            // 妥当性チェックエラー表示
            final String violationMessage = ValidateUtils.getViolationMessage();
            if (! StringUtils.isEmpty(violationMessage)) {
                log.error(violationMessage);
            }

            // エラー追加
            final FileCompareResult result = FileCompareResult.getFixedResult(CompareStatus.Error, leftFilePath, rightFilePath, startTime, startTime);
            this.addFileResult(result.getStatus());
            resultRepo.write(result);
        }

        // 退避した設定上書き項目をリストア
        systemConfig.setSorted(isSorted);
    }

    /**
     * 対象ファイル名正規表現指定の比較入力データモデル。
     */
    @Getter
    @Setter
//    @EqualsAndHashCode(callSuper = true)
//    @ToString(callSuper = true)
    private static class FileNameRegexCompareInput extends BaseCompareInput {

        /** 比較対象設定ファイルパス。 */
        @NotEmpty
        @me.suwash.util.validation.constraints.File
        @ExistPath
        private String targetConfigFilePath;

    }
}
