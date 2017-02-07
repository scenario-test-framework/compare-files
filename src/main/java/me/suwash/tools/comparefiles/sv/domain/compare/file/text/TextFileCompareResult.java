package me.suwash.tools.comparefiles.sv.domain.compare.file.text;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import lombok.Getter;
import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.FileFormat;
import me.suwash.tools.comparefiles.infra.classification.LineSp;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.infra.policy.FileRepository;
import me.suwash.tools.comparefiles.infra.util.ValidateUtils;
import me.suwash.tools.comparefiles.sv.da.file.repository.impl.GenericRowReadRepository;
import me.suwash.tools.comparefiles.sv.da.file.repository.impl.TextFileRowCompareResultRepositoryImpl;
import me.suwash.tools.comparefiles.sv.domain.compare.file.FileCompareResult;
import me.suwash.tools.comparefiles.sv.domain.sort.FileSortResult;
import me.suwash.util.CompareUtils.CompareStatus;

import org.apache.commons.lang3.StringUtils;

/**
 * ファイル単位の比較結果。
 */
@Getter
@lombok.extern.slf4j.Slf4j
public class TextFileCompareResult extends FileCompareResult {

    private static final String OUTPUT_EXT = "csv";

    /**
     * コンストラクタ。
     *
     * @param leftFilePath 左ファイルパス
     * @param rightFilePath 右ファイルパス
     * @param fileLayout ファイルレイアウト
     * @param outputDirPath 出力ディレクトリパス
     * @param systemConfig システム設定
     */
    public TextFileCompareResult(
        final String leftFilePath,
        final String rightFilePath,
        final FileLayout fileLayout,
        final String outputDirPath,
        final CompareFilesConfig systemConfig) {

        super(leftFilePath, rightFilePath, outputDirPath, systemConfig);
        this.outputExt = OUTPUT_EXT;
        this.input.setFileLayout(fileLayout);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.domain.compare.file.FileCompareResult#compare()
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

        // --------------------------------------------------------------------------------
        // 本処理
        // --------------------------------------------------------------------------------
        // ----------------------------------------
        // 設定
        // ----------------------------------------
        // 左ファイル
        this.leftFilePath = input.getLeftFilePath();
        final File leftFile = new File(leftFilePath);
        // 右ファイル
        this.rightFilePath = input.getRightFilePath();
        final File rightFile = new File(rightFilePath);
        // 出力ファイル
        final CompareFilesConfig systemConfig = input.getSystemConfig();
        final String outputDirPath = input.getOutputDirPath();
        // ファイルレイアウト
        this.fileLayout = input.getFileLayout();

        // 実行開始時刻
        this.startTime = new Date();

        // 文字コード判定
        // ファイルレイアウト > システムデフォルト > 設定読み込み固定値
        String charset = null;
        if (fileLayout != null) {
            charset = fileLayout.getCharset();
        }
        if (StringUtils.isEmpty(charset)) {
            charset = systemConfig.getDefaultInputCharset();
        }
        if (StringUtils.isEmpty(charset)) {
            charset = Const.CHARSET_DEFAULT_CONFIG;
        }

        // ----------------------------------------
        // ソート
        // ----------------------------------------
        log.info("    ・ソート");

        String leftSortedPath = null;
        String rightSortedPath = null;

        // ソートフェーズの実施判定
        if (isSkipSort(systemConfig)) {
            log.info("      ・Skip");
            leftSortedPath = leftFilePath;
            rightSortedPath = rightFilePath;

        } else {
            // 左右のソート処理をFork
            final ForkJoinPool pool = new ForkJoinPool(2);

            // 左ファイルのソート
            log.info("      ・左ファイル:" + leftFilePath);
            final String leftSortedDir = outputDirPath + "/" + Const.DIRNAME_WORK + "/left";
            leftSortedPath = leftSortedDir + "/" + leftFile.getName();
            final SortTask leftSortTask = new SortTask(leftFilePath, leftSortedDir, charset, fileLayout, systemConfig);
            pool.invoke(leftSortTask);

            // 右ファイルのソート
            log.info("      ・右ファイル:" + rightFilePath);
            final String rightSortedDir = outputDirPath + "/" + Const.DIRNAME_WORK + "/right";
            rightSortedPath = rightSortedDir + "/" + rightFile.getName();
            final SortTask rightSortTask = new SortTask(rightFilePath, rightSortedDir, charset, fileLayout, systemConfig);
            pool.invoke(rightSortTask);

            // ソート処理をJoin
            if (ProcessStatus.Failure.equals(leftSortTask.join())) {
                throw new CompareFilesException(
                    Const.ERRORHANDLE,
                    new Object[] {
                        this.getClass().getSimpleName() + ".Sort.Left",
                        "file:" + leftFile.getName()
                    });
            }
            if (ProcessStatus.Failure.equals(rightSortTask.join())) {
                throw new CompareFilesException(
                    Const.ERRORHANDLE,
                    new Object[] {
                        this.getClass().getSimpleName() + ".Sort.Right",
                        "file:" + rightFile.getName()
                    });
            }
        }


        // ----------------------------------------
        // ファイル比較
        // ----------------------------------------
        log.info("    ・テキスト比較");
        final String outputFileName = getOutputFileName(systemConfig);
        final String outputFilePath = outputDirPath + "/" + outputFileName;
        compare(leftSortedPath, rightSortedPath, charset, outputFilePath, fileLayout, systemConfig);

        // --------------------------------------------------------------------------------
        // 事後処理
        // --------------------------------------------------------------------------------
        // 終了時刻を設定
        this.endTime = new Date();
    }

    /**
     * ソートフェーズをスキップするか判断します。
     *
     * @param systemConfig システム設定
     * @return ソートフェーズをスキップする場合、true
     */
    private boolean isSkipSort(final CompareFilesConfig systemConfig) {
        // レイアウトから強制スキップ判定
        if (fileLayout == null) {
            // レイアウトなしの場合、スキップ
            return true;
        }

        if (FileFormat.Fixed.equals(fileLayout.getFileFormat()) && LineSp.None.equals(fileLayout.getLineSp())) {
            // 固定長、改行コードなしの場合、スキップ
            return true;
        } else if (FileFormat.Json.equals(fileLayout.getFileFormat())) {
            // Jsonの場合、スキップ
            return true;
        }

        // レイアウトによる強制スキップ以外の場合、設定を利用
        return systemConfig.isSorted();
    }

    /**
     * ソート処理を並走スレッドで実行するクラス。
     */
    @lombok.extern.slf4j.Slf4j
    private static class SortTask extends RecursiveTask<ProcessStatus> {
        private static final long serialVersionUID = 1L;

        private final String inputFilePath;
        private final String outputDir;
        private final String charset;
        private final transient FileLayout fileLayout;
        private final transient CompareFilesConfig systemConfig;

        /**
         * コンストラクタ。
         *
         * @param inputFilePath 入力ファイルパス
         * @param outputDir 出力ディレクトリ
         * @param charset 文字コード
         * @param fileLayout ファイルレイアウト
         * @param systemConfig システム設定
         */
        public SortTask(
            final String inputFilePath,
            final String outputDir,
            final String charset,
            final FileLayout fileLayout,
            final CompareFilesConfig systemConfig) {

            super();
            this.inputFilePath = inputFilePath;
            this.outputDir = outputDir;
            this.charset = charset;
            this.fileLayout = fileLayout;
            this.systemConfig = systemConfig;
        }

        /*
         * (非 Javadoc)
         * @see java.util.concurrent.RecursiveTask#compute()
         */
        @Override
        protected ProcessStatus compute() {
            final FileSortResult sortResult = new FileSortResult(inputFilePath, charset, outputDir, charset, fileLayout, systemConfig);
            sortResult.sort();
            final ProcessStatus processStatus = sortResult.getProcessStatus();
            log.trace("        ・ソート結果：" + processStatus + ", 対象ファイル:" + inputFilePath);
            return processStatus;
        }

    }

    /**
     * 比較を実行します。
     *
     * @param leftFilePath 左ファイルパス
     * @param rightFilePath 右ファイルパス
     * @param charset 文字コード
     * @param outputFilePath 出力ファイルパス
     * @param fileLayout ファイルレイアウト
     * @param systemConfig システム設定
     */
    private void compare(final String leftFilePath, final String rightFilePath, final String charset, final String outputFilePath, final FileLayout fileLayout, final CompareFilesConfig systemConfig) {
        // --------------------------------------------------------------------------------
        // リポジトリ作成
        // --------------------------------------------------------------------------------
        final int csvHeaderRow = systemConfig.getCsvHeaderRow();
        final int csvDataStartRow = systemConfig.getCsvDataStartRow();
        final String codeValueForOnlyOneRecordType = systemConfig.getCodeValueForOnlyOneRecordType();

        // 左ファイルリポジトリ
        final FileRepository<ComparableRow> leftRepo = new GenericRowReadRepository<ComparableRow>(leftFilePath, charset, fileLayout, csvHeaderRow, csvDataStartRow, codeValueForOnlyOneRecordType);
        leftRepo.begin();

        // 右ファイルリポジトリ
        final FileRepository<ComparableRow> rightRepo = new GenericRowReadRepository<ComparableRow>(rightFilePath, charset, fileLayout, csvHeaderRow, csvDataStartRow, codeValueForOnlyOneRecordType);
        rightRepo.begin();

        // 行比較結果リポジトリ
        final TextFileRowCompareResultRepository rowResultRepo = new TextFileRowCompareResultRepositoryImpl(
            outputFilePath,
            systemConfig.getOutputCharset(),
            null,
            systemConfig.getChunkSize(),
            systemConfig.isWriteDiffOnly(),
            systemConfig.getLeftPrefix(),
            systemConfig.getRightPrefix()
            );
        rowResultRepo.begin();

        // --------------------------------------------------
        // 左右ともにEOFまでループして比較
        // --------------------------------------------------
        // 左ファイル データ読込み
        ComparableRow leftCurRow = leftRepo.next();
        // 右ファイル データ読込み
        ComparableRow rightCurRow = rightRepo.next();

        // 対象行0件として処理
        if (leftCurRow == null && rightCurRow == null) {
            this.status = CompareStatus.Ignore;
        }

        // 左右の現在行が両方EOFに到達するまで（nullになるまで）ループ
        long compareCount = 0;
        while (true) {
            compareCount++;
            if (compareCount % 1000 == 0) {
                long leftRowNum = Const.UNKNOWN_LINE;
                long rightRowNum = Const.UNKNOWN_LINE;
                if (leftCurRow != null) {
                    leftRowNum = leftCurRow.getRowNum();
                }
                if (rightCurRow != null) {
                    rightRowNum = rightCurRow.getRowNum();
                }
                log.debug("      ・CompareCount:" + compareCount + ", left:" + leftRowNum + ", right:" + rightRowNum);
            }

            // EOFチェック
            if (leftCurRow == null) {
                if (rightCurRow == null) {
                    // --------------------------------------------------
                    // 左右両方がEOFに到達した場合
                    // --------------------------------------------------
                    // ループを抜ける
                    log.debug("      ・break");
                    break;

                } else {
                    // --------------------------------------------------
                    // 左ファイルがEOFに到達した場合
                    // --------------------------------------------------
                    log.trace("  ・[LEFT ]reache EOF");
                    // 比較結果：右のみ
                    addFixedRowResult(fileLayout, rowResultRepo, CompareStatus.RightOnly, null, rightCurRow);
                    // 右ファイル データ読込み
                    rightCurRow = rightRepo.next();
                    // 次の比較へ
                    continue;
                }

            } else {
                if (rightCurRow == null) {
                    // --------------------------------------------------
                    // 右ファイルがEOFに到達した場合
                    // --------------------------------------------------
                    log.trace("  ・[RIGHT]reache EOF");
                    // 比較結果：左のみ
                    addFixedRowResult(fileLayout, rowResultRepo, CompareStatus.LeftOnly, leftCurRow, null);
                    // 左ファイル データ読込み
                    leftCurRow = leftRepo.next();
                    // 次の比較へ
                    continue;
                }
            }

            // どちらもEOFに到達していない場合、キーを比較
            final int keyCompareResult = leftCurRow.compareTo(rightCurRow);
            if (keyCompareResult < 0) {
                // --------------------------------------------------
                // 左キーの方が小さい場合
                // --------------------------------------------------
                log.trace("  ・[LEFT ]Only");
                // 比較結果：左のみ
                addFixedRowResult(fileLayout, rowResultRepo, CompareStatus.LeftOnly, leftCurRow, null);
                // 左ファイル データ読込み
                leftCurRow = leftRepo.next();
                // 次の比較へ
                continue;

            } else if (keyCompareResult > 0) {
                // --------------------------------------------------
                // 右キーの方が小さい場合
                // --------------------------------------------------
                log.trace("  ・[RIGHT]Only");
                // 比較結果：右のみ
                addFixedRowResult(fileLayout, rowResultRepo, CompareStatus.RightOnly, null, rightCurRow);
                // 右ファイル データ読込み
                rightCurRow = rightRepo.next();
                // 次の比較へ
                continue;

            } else {
                // --------------------------------------------------
                // キーが一致した場合
                // --------------------------------------------------
                // 全項目を比較
                addComparedResult(fileLayout, rowResultRepo, leftCurRow, rightCurRow);

                // 左ファイル データ読込み
                leftCurRow = leftRepo.next();
                // 右ファイル データ読込み
                rightCurRow = rightRepo.next();
                // 次の比較へ
                continue;
            }
        }

        // ファイル操作を確定
        rowResultRepo.commit();
        leftRepo.commit();
        rightRepo.commit();
    }

    /**
     * 固定ステータスの行比較結果を追加します。
     *
     * @param fileLayout ファイルレイアウト
     * @param rowResultRepo 行比較結果リポジトリ
     * @param status 比較ステータス
     * @param leftRow 左ファイルの行データ
     * @param rightRow 右ファイルの行データ
     */
    private void addFixedRowResult(
        final FileLayout fileLayout,
        final TextFileRowCompareResultRepository rowResultRepo,
        final CompareStatus status,
        final ComparableRow leftRow,
        final ComparableRow rightRow) {

        final TextFileRowCompareResult rowResult = TextFileRowCompareResult.getFixedResult(status, fileLayout, leftRow, rightRow);
        this.addRow(rowResult, rowResultRepo);

    }

    /**
     * 行比較を実行し、結果を追加します。
     *
     * @param fileLayout ファイルレイアウト
     * @param rowResultRepo 行比較結果リポジトリ
     * @param leftRow 左ファイルの行データ
     * @param rightRow 右ファイルの行データ
     */
    private void addComparedResult(
        final FileLayout fileLayout,
        final TextFileRowCompareResultRepository rowResultRepo,
        final ComparableRow leftRow,
        final ComparableRow rightRow) {

        final TextFileRowCompareResult rowResult = new TextFileRowCompareResult(fileLayout, leftRow, rightRow);
        rowResult.compare();
        this.addRow(rowResult, rowResultRepo);

    }

    /**
     * 行比較結果を追加します。
     *
     * @param rowResult 行比較結果
     * @param rowResultRepo 行比較結果リポジトリ
     */
    private void addRow(final TextFileRowCompareResult rowResult, final TextFileRowCompareResultRepository rowResultRepo) {
        // --------------------------------------------------
        // サマリー項目の更新
        // --------------------------------------------------
        final CompareStatus rowStatus = rowResult.getStatus();
        // ファイル比較結果
        updateFileStatus(rowStatus);
        // 行サマリー項目の更新
        updateRowSummaryFields(rowStatus);

        // 行比較結果を出力
        rowResultRepo.write(rowResult);
    }

    /**
     * 実行時点のファイル比較結果と、現在行の比較結果から、ファイル比較結果を更新します。
     *
     * @param rowStatus 行比較ステータス
     */
    private void updateFileStatus(final CompareStatus rowStatus) {
        // 行ステータスからサマリー用ステータス(OK or NG)に変換
        CompareStatus tempSummaryStatus = null;
        switch (rowStatus) {
            case OK:
            case Ignore:
                tempSummaryStatus = CompareStatus.OK;
                break;
            case NG:
            case LeftOnly:
            case RightOnly:
                tempSummaryStatus = CompareStatus.NG;
                break;
            default:
                throw new CompareFilesException(Const.UNSUPPORTED);
        }

        // 現在のサマリーステータスの状況に合わせて、今回のステータスを適用
        switch (status) {
            case Processing:
            case OK:
                status = tempSummaryStatus;
                break;
            case NG:
                // 1件でもNGが存在する場合は、更新なし
                break;
            default:
                throw new CompareFilesException(Const.UNSUPPORTED);
        }
    }

    /**
     * 行単位の集計項目を更新します。
     *
     * @param rowStatus 行比較ステータス
     */
    private void updateRowSummaryFields(final CompareStatus rowStatus) {
        rowCount++;

        switch (rowStatus) {
            case OK:
                okRowCount++;
                break;
            case NG:
                ngRowCount++;
                break;
            case Ignore:
                ignoreRowCount++;
                break;
            case LeftOnly:
                leftOnlyRowCount++;
                break;
            case RightOnly:
                rightOnlyRowCount++;
                break;
            default:
                throw new CompareFilesException(Const.UNSUPPORTED);
        }
    }

}
