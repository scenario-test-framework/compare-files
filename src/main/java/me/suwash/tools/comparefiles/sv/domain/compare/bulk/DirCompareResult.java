package me.suwash.tools.comparefiles.sv.domain.compare.bulk;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.infra.i18n.CompareFilesMessageSource;
import me.suwash.tools.comparefiles.infra.policy.AggregateEntity;
import me.suwash.tools.comparefiles.infra.util.ValidateUtils;
import me.suwash.tools.comparefiles.sv.da.file.repository.impl.FileCompareResultRepositoryImpl;
import me.suwash.tools.comparefiles.sv.domain.compare.BaseCompareInput;
import me.suwash.tools.comparefiles.sv.domain.compare.file.FileCompareResult;
import me.suwash.util.CompareUtils.CompareStatus;
import me.suwash.util.FileUtils;
import me.suwash.util.validation.constraints.Dir;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * ディレクトリ比較結果。
 */
@Getter
@lombok.extern.slf4j.Slf4j
public class DirCompareResult extends BaseBulkCompareResult implements AggregateEntity {

    private final DirCompareInput input;

    /**
     * コンストラクタ。
     *
     * @param leftDirPath 左ディレクトリパス
     * @param rightDirPath 右ディレクトリパス
     * @param outputDirPath 出力ディレクトリパス
     * @param systemConfig システム設定
     */
    public DirCompareResult(
        final String leftDirPath,
        final String rightDirPath,
        final String outputDirPath,
        final CompareFilesConfig systemConfig) {

        super();
        final DirCompareInput input = new DirCompareInput();
        input.setLeftDirPath(leftDirPath);
        input.setRightDirPath(rightDirPath);
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
        // ----------------------------------------
        // 単項目チェック
        // ----------------------------------------
        ValidateUtils.validate(input);

        // ----------------------------------------
        // 相関チェック
        // ----------------------------------------
        // なし

        // --------------------------------------------------------------------------------
        // 本処理
        // --------------------------------------------------------------------------------
        final String leftDirPath = input.getLeftDirPath();
        final String rightDirPath = input.getRightDirPath();
        final String outputDirPath = input.getOutputDirPath();
        final CompareFilesConfig systemConfig = input.getSystemConfig();

        // 出力ディレクトリ
        FileUtils.initDir(outputDirPath);

        // ----------------------------------------------------------------------
        // 設定取得
        // ----------------------------------------------------------------------
        // 左ディレクトリ
        final File leftDir = new File(leftDirPath);
        // 右ディレクトリ
        final File rightDir = new File(rightDirPath);

        // 実行開始時刻
        final Date startTime = new Date();

        // ファイル比較結果リポジトリ
        final FileCompareResultRepository resultRepo = new FileCompareResultRepositoryImpl(
            outputDirPath + "/" + systemConfig.getCompareResultFileName(),
            systemConfig.getOutputCharset(),
            null,
            systemConfig.getChunkSize()
            );
        resultRepo.begin();

        // ----------------------------------------------------------------------
        // ディレクトリの存在比較
        // ----------------------------------------------------------------------
        if (leftDir.exists() && !rightDir.exists()) {
            // 左ディレクトリのみ
            log.info("・[左のみ]" + leftDir);
            final FileCompareResult result = FileCompareResult.getFixedResult(CompareStatus.LeftOnly, leftDirPath, rightDirPath, startTime, startTime);
            this.addFileResult(result.getStatus());
            resultRepo.write(result);
            resultRepo.commit();
            return;

        } else if (!leftDir.exists() && rightDir.exists()) {
            // 右ディレクトリのみ
            log.info("・[右のみ]" + rightDir);
            final FileCompareResult result = FileCompareResult.getFixedResult(CompareStatus.RightOnly, leftDirPath, rightDirPath, startTime, startTime);
            this.addFileResult(result.getStatus());
            resultRepo.write(result);
            resultRepo.commit();
            return;

        } else if (!leftDir.exists() && !rightDir.exists()) {
            // どちらも存在しない
            resultRepo.rollback();
            throw new CompareFilesException(Const.MSGCD_ERROR_COMPARE_DIR_BOTH_NOTEXIST, new Object[] {leftDir, rightDir});
        }

        // ----------------------------------------------------------------------
        // ファイル走査
        // ----------------------------------------------------------------------
        log.info("・ファイル走査");
        // 左ファイルの相対パスリスト作成
        final List<String> leftRelPathList = getRelPathList(leftDir);
        // 右ファイルの相対パスリスト作成
        final List<String> rightRelPathList = getRelPathList(rightDir);
        // マージした相対パスリスト作成
        final List<String> mergedRelPathList = new ArrayList<String>();
        mergedRelPathList.addAll(leftRelPathList);
        for (final String rightRelPath : rightRelPathList) {
            if (!mergedRelPathList.contains(rightRelPath)) {
                mergedRelPathList.add(rightRelPath);
            }
        }
        Collections.sort(mergedRelPathList);

        // ----------------------------------------------------------------------
        // ディレクトリ比較
        // ----------------------------------------------------------------------
        log.info("・ディレクトリ比較");
        final boolean isSorted = systemConfig.isSorted();
        // マージした相対パスリストを全件ループ
        for (final String relPath : mergedRelPathList) {
            // 左ファイルパス
            final String leftFilePath = leftDirPath + relPath;
            // 右ファイルパス
            final String rightFilePath = rightDirPath + relPath;

            // システム設定を初期化
            systemConfig.setSorted(isSorted);

            // ファイル比較
            compareFile(leftFilePath, rightFilePath, outputDirPath, systemConfig, startTime, resultRepo);
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
     * ファイルを比較します。
     *
     * @param leftFilePath 左ファイルパス
     * @param rightFilePath 右ファイルパス
     * @param outputDirPath 出力ディレクトリパス
     * @param systemConfig システム設定
     * @param startTime 処理開始時刻
     * @param resultRepo ファイル比較結果リポジトリ
     */
    private void compareFile(
        final String leftFilePath,
        final String rightFilePath,
        final String outputDirPath,
        final CompareFilesConfig systemConfig,
        final Date startTime,
        final FileCompareResultRepository resultRepo) {

        // ファイル比較実行
        try {
            final FileCompareResult result = new FileCompareResult(leftFilePath, rightFilePath, outputDirPath, systemConfig);
            result.compare();

            // 結果を出力
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
    }

    /**
     * サブディレクトリを含めて、指定ディレクトリ配下全てのファイルパスを、相対パスで返します。
     *
     * @param dir 基準ディレクトリ
     * @return 相対ファイルパスリスト
     */
    private List<String> getRelPathList(final File dir) {
        final List<String> filePathList = new ArrayList<String>();
        if (dir == null) {
            return filePathList;
        } else if (!dir.isDirectory()) {
            return filePathList;
        }
        getRelPathListMain(dir, dir, filePathList);
        return filePathList;
    }

    /**
     * 再帰呼び出し。
     *
     * @param rootDir 基準ディレクトリ
     * @param targetDir 対象ディレクトリ
     * @param filePathList 相対ファイルパスリスト
     */
    private void getRelPathListMain(final File rootDir, final File targetDir, final List<String> filePathList) {
        final File[] targetFiles = targetDir.listFiles();
        if (targetFiles == null) {
            return;
        }
        for (final File targetFile : targetFiles) {
            if (targetFile.isDirectory()) {
                getRelPathListMain(rootDir, targetFile, filePathList);
            } else {
                final String targetPath = targetFile.getPath();
                final String relPath = targetPath.replace(rootDir.getPath(), StringUtils.EMPTY);
                filePathList.add(relPath);
            }
        }
    }

    /**
     * ディレクトリ比較入力データモデル。
     */
    @Getter
    @Setter
//    @EqualsAndHashCode(callSuper = true)
//    @ToString(callSuper = true)
    private static class DirCompareInput extends BaseCompareInput {

        /** 左ディレクトリパス。 */
        @NotEmpty
        @Dir
        private String leftDirPath;

        /** 右ディレクトリパス。 */
        @NotEmpty
        @Dir
        private String rightDirPath;

    }

}
