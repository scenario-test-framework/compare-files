package me.suwash.tools.comparefiles.sv.domain.compare.file;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.FileFormat;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.config.FileLayoutManager;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.infra.policy.AggregateEntity;
import me.suwash.tools.comparefiles.infra.util.ValidateUtils;
import me.suwash.tools.comparefiles.sv.domain.compare.BaseCompareInput;
import me.suwash.tools.comparefiles.sv.domain.compare.file.image.ImageFileCompareResult;
import me.suwash.tools.comparefiles.sv.domain.compare.file.text.TextFileCompareResult;
import me.suwash.util.CompareUtils.CompareStatus;
import me.suwash.util.FileUtils;
import me.suwash.util.ImageCompareUtils;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * ファイル比較結果。
 */
@lombok.extern.slf4j.Slf4j
public class FileCompareResult extends BaseFileCompareResult implements AggregateEntity {

    /** ファイル比較入力データモデル。 */
    protected FileCompareInput input = new FileCompareInput();

    /**
     * コンストラクタ。
     */
    protected FileCompareResult() {
        super();
    }

    /**
     * ファイル比較結果の固定値を返します。
     *
     * @param status 比較ステータス
     * @param leftFilePath 左ファイルパス
     * @param rightFilePath 右ファイルパス
     * @param startTime 開始時刻
     * @param endTime 終了時刻
     * @return ファイル比較結果の固定値
     */
    public static FileCompareResult getFixedResult(
        final CompareStatus status,
        final String leftFilePath,
        final String rightFilePath,
        final Date startTime,
        final Date endTime) {

        return new FileCompareResult(status, leftFilePath, rightFilePath, startTime, endTime);
    }

    /**
     * 固定値取得用コンストラクタ。
     *
     * @param status 比較結果
     * @param leftFilePath 左ファイルパス
     * @param rightFilePath 右ファイルパス
     * @param startTime 処理開始時刻
     * @param endTime 処理終了時刻
     */
    private FileCompareResult(
        final CompareStatus status,
        final String leftFilePath,
        final String rightFilePath,
        final Date startTime,
        final Date endTime) {

        this();
        this.status = status;
        this.leftFilePath = leftFilePath;
        this.rightFilePath = rightFilePath;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * コンストラクタ。
     *
     * @param leftFilePath 左ファイルパス
     * @param rightFilePath 右ファイルパス
     * @param outputDirPath 出力ディレクトリパス
     * @param systemConfig システム設定
     */
    public FileCompareResult(
        final String leftFilePath,
        final String rightFilePath,
        final String outputDirPath,
        final CompareFilesConfig systemConfig) {

        this();
        input.setLeftFilePath(leftFilePath);
        input.setRightFilePath(rightFilePath);
        input.setOutputDirPath(outputDirPath);
        input.setSystemConfig(systemConfig);
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
        // 実行開始時刻
        this.startTime = new Date();

        // ----------------------------------------
        // 単項目チェック
        // ----------------------------------------
        ValidateUtils.validate(this.input);

        // ----------------------------------------
        // 関連チェック
        // ----------------------------------------
        // 出力ディレクトリが存在しない場合、作成
        final String outputDirPath = input.getOutputDirPath();
        FileUtils.mkdirs(outputDirPath);

        // 出力ファイルが存在する場合、削除
        final CompareFilesConfig systemConfig = input.getSystemConfig();
        final String outputFileName = getOutputFileName(systemConfig);
        final String outputFilePath = outputDirPath + "/" + outputFileName;
        FileUtils.setupOverwrite(outputFilePath);

        // --------------------------------------------------------------------------------
        // 本処理
        // --------------------------------------------------------------------------------
        // ----------------------------------------
        // 設定
        // ----------------------------------------
        // 左ファイル
        final String leftFilePath = input.getLeftFilePath();
        final File leftFile = new File(leftFilePath);
        this.leftFilePath = leftFile.getAbsolutePath();

        // 右ファイル
        final String rightFilePath = input.getRightFilePath();
        final File rightFile = new File(rightFilePath);
        this.rightFilePath = rightFile.getAbsolutePath();

        // ----------------------------------------
        // 共通比較
        // ----------------------------------------
        log.info("  ・ファイル比較 左：" + leftFile.getName() + "、右：" + rightFile.getName());

        // ファイル名除外ルール確認
        log.info("    ・ファイル名除外ルール確認");
        if (isIgnore(leftFile, systemConfig.getIgnoreFileRegexList())) {
            this.status = CompareStatus.Ignore;
            this.endTime = new Date();
            return;
        }

        // ファイルの存在比較
        log.info("    ・ファイルの存在比較");
        final CompareStatus fileExistCheckStatus = fileExistCheck(leftFile, rightFile);
        if (!CompareStatus.OK.equals(fileExistCheckStatus)) {
            this.status = fileExistCheckStatus;
            this.endTime = new Date();
            return;
        }

        // ファイルレイアウト判定
        log.info("    ・ファイルレイアウト判定");
        final FileLayoutManager layoutManager = FileLayoutManager.getInstance();
        final String leftFileName = leftFile.getName();
        this.fileLayout = layoutManager.getLayout(leftFileName, systemConfig);

        // ----------------------------------------
        // ファイル比較
        // ----------------------------------------
        // ファイルフォーマット判定
        FileFormat fileFormat = null;
        final String ext = getExt(leftFileName);
        log.debug("      ・拡張子：" + ext);
        if (this.fileLayout == null && ImageCompareUtils.isAllowedExt(ext)) {
            fileFormat = FileFormat.Image;
        }

        // ファイルフォーマットに合わせて比較実行
        if (FileFormat.Image.equals(fileFormat)) {
            // 画像比較
            final ImageFileCompareResult result = new ImageFileCompareResult(
                this.leftFilePath,
                this.rightFilePath,
                this.fileLayout,
                outputDirPath,
                systemConfig,
                ext);
            result.compare();
            copyResult(result);

        } else {
            // テキスト比較
            final TextFileCompareResult result = new TextFileCompareResult(
                this.leftFilePath,
                this.rightFilePath,
                this.fileLayout,
                outputDirPath,
                systemConfig
                );
            result.compare();
            copyResult(result);
        }

        // --------------------------------------------------------------------------------
        // 事後処理
        // --------------------------------------------------------------------------------
        // workディレクトリの削除
        if (systemConfig.isDeleteWorkDir()) {
            final String workDirPath = outputDirPath + "/" + Const.DIRNAME_WORK;
            FileUtils.rmdirs(workDirPath);
        }
    }

    /**
     * ファイル名から拡張子を返します。
     *
     * @param fileName ファイル名
     * @return 拡張子
     */
    private String getExt(final String fileName) {
        final String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
        // ドットが含まれていない場合、空文字を返す
        if (fileName.equals(ext)) {
            return StringUtils.EMPTY;
        }
        return ext;
    }

    /**
     * 出力ファイル名 を返します。
     *
     * @param systemConfig システム設定
     * @return 出力ファイル名
     */
    protected String getOutputFileName(final CompareFilesConfig systemConfig) {
        final File leftFile = new File(input.getLeftFilePath());
        final File rightFile = new File(input.getRightFilePath());

        final StringBuilder outputFileNameBuilder = new StringBuilder();
        outputFileNameBuilder.append(systemConfig.getCompareDetailFilePrefix()).append(rightFile.getName());

        // --------------------------------------------------
        // サブディレクトリ内の同一ファイル名考慮
        // --------------------------------------------------
        final String leftDirPath = leftFile.getParentFile().getAbsolutePath();
        final String rightDirPath = rightFile.getParentFile().getAbsolutePath();

        // 左右のディレクトリが完全一致する場合は考慮をスキップ
        if (!leftDirPath.equals(rightDirPath)) {
            String[] leftDirs = null;
            String[] rightDirs = null;
            if ("\\".equals(File.separator)) {
                leftDirs = leftDirPath.split("\\\\");
                rightDirs = rightDirPath.split("\\\\");

            } else {
                leftDirs = leftDirPath.split("/");
                rightDirs = rightDirPath.split("/");
            }

            // 左右のディレクトリ階層で不一致になるディレクトリを取得
            // ※左＝期待値、期待値の流用を想定して、比較対象の右ディレクトリを結果ファイル名に設定
            boolean isHitDiff = false;
            int maxLength = 0;
            if (leftDirs.length < rightDirs.length) {
                maxLength = rightDirs.length;
            } else {
                maxLength = leftDirs.length;
            }

            for (int idx = 0; idx < maxLength; idx++) {
                if (!isHitDiff && !leftDirs[idx].equals(rightDirs[idx])) {
                    isHitDiff = true;
                }
                if (isHitDiff) {
                    // 不一致になった階層～右の対象ファイル配置ディレクトリまでを出力ファイル名に付与
                    if (idx < rightDirs.length) {
                        outputFileNameBuilder.append('_').append(rightDirs[idx]);
                    } else {
                        break;
                    }
                }
            }

        }

        // 拡張子を付与
        outputFileNameBuilder.append('.').append(this.outputExt);
        return outputFileNameBuilder.toString();
    }

    /**
     * ファイル名が、除外ファイル名正規表現リストの定義にマッチするか否かを返します。
     *
     * @param file 対象ファイル
     * @param ignoreFileRegexList 除外ファイル名正規表現リスト
     * @return 除外する場合、true
     */
    private boolean isIgnore(final File file, final List<String> ignoreFileRegexList) {
        if (ignoreFileRegexList != null) {
            for (final String ignoreFileRegex : ignoreFileRegexList) {
                try {
                    final Pattern pattern = Pattern.compile(ignoreFileRegex);
                    final Matcher matcher = pattern.matcher(file.getName());
                    log.trace("      ・check regex:" + ignoreFileRegex);
                    if (matcher.matches()) {
                        // マッチした場合、返却用レイアウト定義に設定
                        log.info("      ・[SKIP]正規表現:" + ignoreFileRegex + ", ファイル名:" + file.getName());
                        return true;
                    }
                } catch (Exception e) {
                    log.error("ファイル名除外ルールの正規表現の評価でエラーが発生しました。正規表現:" + ignoreFileRegex, e);
                }
            }
        }

        return false;
    }

    /**
     * ファイルの存在を比較します。
     *
     * @param leftFile 左ファイル
     * @param rightFile 右ファイル
     * @return 比較ステータス
     */
    private CompareStatus fileExistCheck(final File leftFile, final File rightFile) {
        // ----------------------------------------
        // ファイルの存在比較
        // ----------------------------------------
        if (leftFile.exists()) {
            if (!rightFile.exists()) {
                // 左ファイルのみ
                log.info("      ・[左のみ]" + leftFilePath);
                return CompareStatus.LeftOnly;
            }

        } else {
            if (rightFile.exists()) {
                // 右ファイルのみ
                log.info("      ・[右のみ]" + rightFilePath);
                return CompareStatus.RightOnly;

            } else {
                // どちらも存在しない
                throw new CompareFilesException(Const.MSGCD_ERROR_COMPARE_FILE_BOTH_NOTEXIST, new Object[] {leftFile, rightFile});
            }
        }

        // ----------------------------------------
        // 0バイトチェック
        // ----------------------------------------
        if (leftFile.length() == 0 && rightFile.length() == 0) {
            // どちらも0バイトの場合、除外として返却
            log.info("      ・[除外　]どちらも0バイトです。左ファイル=" + leftFilePath + ", 右ファイル=" + rightFilePath);
            return CompareStatus.Ignore;
        }

        return CompareStatus.OK;
    }

    /**
     * 実装クラスの比較結果を、自分にコピーします。
     *
     * @param result 実装クラスの比較結果
     */
    private void copyResult(final FileCompareResult result) {
        this.status = result.getStatus();
        this.fileLayout = result.getFileLayout();
        this.leftFilePath = result.getLeftFilePath();
        this.rightFilePath = result.getRightFilePath();
        this.rowCount = result.getRowCount();
        this.okRowCount = result.getOkRowCount();
        this.ngRowCount = result.getNgRowCount();
        this.ignoreRowCount = result.getIgnoreRowCount();
        this.leftOnlyRowCount = result.getLeftOnlyRowCount();
        this.rightOnlyRowCount = result.getRightOnlyRowCount();
        this.startTime = result.getStartTime();
        this.endTime = result.getEndTime();
    }

    /**
     * ファイル比較の入力データモデル。
     */
    @Getter
    @Setter
    protected static class FileCompareInput extends BaseCompareInput {

        /** 左ファイルパス。 */
        @NotEmpty
        @me.suwash.util.validation.constraints.File
        private String leftFilePath;

        /** 右ファイルパス。 */
        @NotEmpty
        @me.suwash.util.validation.constraints.File
        private String rightFilePath;

        /** ファイルレイアウト。 */
        private FileLayout fileLayout;
    }
}
