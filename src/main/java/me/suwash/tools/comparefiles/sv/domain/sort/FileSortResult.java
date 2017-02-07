package me.suwash.tools.comparefiles.sv.domain.sort;

import java.io.File;
import java.lang.management.ManagementFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.ddd.policy.Input;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.FileFormat;
import me.suwash.tools.comparefiles.infra.classification.LineSp;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.infra.policy.AggregateEntity;
import me.suwash.tools.comparefiles.infra.policy.FileRepository;
import me.suwash.tools.comparefiles.infra.util.ValidateUtils;
import me.suwash.tools.comparefiles.sv.da.file.repository.impl.GenericRowReadRepository;
import me.suwash.tools.comparefiles.sv.da.file.repository.impl.StringFileRepository;
import me.suwash.tools.comparefiles.sv.da.file.repository.impl.TempSortedRowWriteRepository;
import me.suwash.util.FileUtils;
import me.suwash.util.RuntimeUtils;
import me.suwash.util.validation.constraints.Charset;
import me.suwash.util.validation.constraints.ExistPath;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * ファイルのソートサービス。
 * 大容量ファイルに対応するため、マージソートを実施します。
 */
@lombok.extern.slf4j.Slf4j
public class FileSortResult implements AggregateEntity {

    private static final int LINENUM_FIRST = 1;

    /** マージフェーズのステータス。 */
    private enum MergePhaseStatus {
        Compare,
        Sep1GotoUnsortedPos,
        Sep2GotoUnsortedPos
    }

    /** ソート入力データモデル。 */
    private final FileSortInput sortInput;

    /** 処理ステータス。 */
    @Getter
    private ProcessStatus processStatus = ProcessStatus.Processing;

    /**
     * コンストラクタ。
     *
     * @param inputFilePath 入力ファイルパス
     * @param inputCharset 入力ファイル文字コード
     * @param outputDirPath 出力ディレクトリ
     * @param outputCharset 出力ファイル文字コード
     * @param fileLayout ファイルレイアウト
     * @param systemConfig システム設定
     */
    public FileSortResult(
        final String inputFilePath,
        final String inputCharset,
        final String outputDirPath,
        final String outputCharset,
        final FileLayout fileLayout,
        final CompareFilesConfig systemConfig) {

        super();
        final FileSortInput sortInput = new FileSortInput();
        sortInput.setInputFile(inputFilePath);
        sortInput.setInputCharset(inputCharset);
        sortInput.setOutputDir(outputDirPath);
        sortInput.setOutputCharset(outputCharset);
        sortInput.setFileLayout(fileLayout);
        sortInput.setSystemConfig(systemConfig);
        this.sortInput = sortInput;
    }

    /**
     * ソートを実行します。
     */
    public void sort() {
        // --------------------------------------------------------------------------------
        // 事前処理
        // --------------------------------------------------------------------------------

        // ----------------------------------------
        // 単項目チェック
        // ----------------------------------------
        ValidateUtils.validate(sortInput);

        // ----------------------------------------
        // 関連チェック
        // ----------------------------------------
        // 出力ディレクトリ
        final String outputDirPath = sortInput.getOutputDir();
        FileUtils.mkdirs(outputDirPath);

        // 作業ディレクトリの初期化
        final String workDirPath = outputDirPath + File.separator + getWorkDirName(sortInput);
        FileUtils.initDir(workDirPath);

        // --------------------------------------------------------------------------------
        // 本処理
        // --------------------------------------------------------------------------------
        // --------------------------------------------------
        // 設定
        // --------------------------------------------------
        // 入力ファイル
        final String inputFilePath = sortInput.getInputFile();
        final File inputFile = new File(inputFilePath);
        final String inputFileName = inputFile.getName();

        // 出力ファイル
        final String outputFilePath = outputDirPath + File.separator + inputFileName;

        // --------------------------------------------------
        // 0バイトチェック
        // --------------------------------------------------
        if (inputFile.length() == 0) {
            log.warn("        ・SKIP 空ファイル:" + inputFilePath);
            FileUtils.createNewFile(outputFilePath);
            this.processStatus = ProcessStatus.Warning;
            return;
        }

        // --------------------------------------------------
        // ヘッダーファイル出力
        // --------------------------------------------------
        final String inputCharset = sortInput.getInputCharset();
        final String outputCharset = sortInput.getOutputCharset();
        final FileLayout fileLayout = sortInput.getFileLayout();
        final CompareFilesConfig systemConfig = sortInput.getSystemConfig();
        final int chunkSize = systemConfig.getChunkSize();

        // ヘッダー有無を考慮して、データ開始行より前までのファイルを作成
        final String headerFilePath = workDirPath + File.separator + "header";
        final File headerFile = new File(headerFilePath);
        final boolean hasHeader = hasHeader(fileLayout.getFileFormat());
        if (hasHeader) {
            writeHeaderFile(inputFile, inputCharset, systemConfig.getCsvDataStartRow(), headerFile, outputCharset, chunkSize);
        }

        // --------------------------------------------------
        // 部分ソートファイル出力
        // --------------------------------------------------
        // ヘッダー有無を考慮して、入力ファイルをざっくりバブルソート ※高速化
        final String tempSortedFilePath = workDirPath + File.separator + "tempSorted";
        final File tempSortedFile = new File(tempSortedFilePath);
        writeTempSortedFile(inputFilePath, inputCharset, fileLayout, systemConfig, tempSortedFile, outputCharset, hasHeader, chunkSize);

        // --------------------------------------------------
        // マージソート
        // --------------------------------------------------
        // 部分ソートファイルを元にマージソート
        final String mergedFilePath = workDirPath + File.separator + "merged";
        final File mergedFile = new File(mergedFilePath);
        mergeSort(tempSortedFile, workDirPath, mergedFile, outputCharset, fileLayout, systemConfig, chunkSize);

        // --------------------------------------------------
        // 結果ファイル出力
        // --------------------------------------------------
        // ヘッダーファイルとマージソート結果ファイルを連結
        final File outputFile = new File(outputFilePath);
        writeResultFile(headerFile, outputCharset, mergedFile, outputCharset, outputFile, outputCharset, fileLayout, systemConfig, chunkSize);

        // --------------------------------------------------------------------------------
        // 事後処理
        // --------------------------------------------------------------------------------
        // workディレクトリの削除
        FileUtils.rmdirs(workDirPath);

        // 処理ステータスが未設定の場合、正常終了を設定
        if (ProcessStatus.Processing.equals(this.processStatus)) {
            this.processStatus = ProcessStatus.Success;
        }
    }

    /**
     * ヘッダーが存在するファイルフォーマットか確認します。
     *
     * @param fileFormat ファイルフォーマット
     * @return ヘッダーが存在する場合、true
     */
    private boolean hasHeader(final FileFormat fileFormat) {
        boolean hasHeader = false;
        if (FileFormat.CSV_withHeader.equals(fileFormat) ||
            FileFormat.TSV_withHeader.equals(fileFormat)) {
            hasHeader = true;
        }
        return hasHeader;
    }

    /**
     * 作業ディレクトリ名を返します。
     * マルチプロセス、マルチスレッド対応にするため、入力データモデルを利用してIDを保持します。
     *
     * @param input 入力データモデル
     * @return 作業ディレクトリ名
     */
    private String getWorkDirName(final FileSortInput input) {
        final String processId = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        final String threadName = Thread.currentThread().getName();
        final String fileName = new File(input.getInputFile()).getName();
        final String randomStr = RandomStringUtils.randomAlphanumeric(6);

        final StringBuilder workDirNameBuilder = new StringBuilder();
        return workDirNameBuilder
            .append(Const.DIRNAME_WORK).append('_')
            .append(processId).append('_')
            .append(threadName).append('_')
            .append(fileName).append('_')
            .append(randomStr)
            .toString();
    }

    /**
     * ファイルレイアウトにマッチする行データ読み込みリポジトリを返します。
     *
     * @param filePath 入力ファイルパス
     * @param charset 入力文字コード
     * @param fileLayout 入力ファイルレイアウト
     * @param systemConfig システム設定
     * @return 行データ読み込みリポジトリ
     */
    private FileRepository<SortableRow> getRowReadRepository(
        final String filePath,
        final String charset,
        final FileLayout fileLayout,
        final CompareFilesConfig systemConfig) {

        return getRowReadRepository(filePath, charset, fileLayout, systemConfig, false);
    }

    /**
     * ファイルレイアウトにマッチする行データ読み込みリポジトリを返します。
     *
     * @param filePath 入力ファイルパス
     * @param charset 入力文字コード
     * @param fileLayout 入力ファイルレイアウト
     * @param systemConfig システム設定
     * @param isUseConfigDataStartRow システム設定のデータ開始行から読み込むか？
     * @return 行データ読み込みリポジトリ
     */
    private FileRepository<SortableRow> getRowReadRepository(
        final String filePath,
        final String charset,
        final FileLayout fileLayout,
        final CompareFilesConfig systemConfig,
        final boolean isUseConfigDataStartRow) {

        int csvHeaderRow = systemConfig.getCsvHeaderRow();
        int csvDataStartRow = systemConfig.getCsvDataStartRow();
        final String codeValueForOnlyOneRecordType = systemConfig.getCodeValueForOnlyOneRecordType();
        if (!isUseConfigDataStartRow) {
            csvHeaderRow = 0;
            csvDataStartRow = 1;
        }

        final FileRepository<SortableRow> repo = new GenericRowReadRepository<SortableRow>(filePath, charset, fileLayout, csvHeaderRow, csvDataStartRow, codeValueForOnlyOneRecordType);
        repo.begin();
        return repo;
    }

    /**
     * ヘッダー部分を抽出した一時ファイルを作成します。
     * データ部分をマージソートした後で、結合します。
     *
     * @param inputFile 入力ファイル
     * @param inputCharset 入力ファイル文字コード
     * @param dataStartRow データ開始行番号
     * @param headerFile ヘッダーファイル
     * @param headerCharset ヘッダーファイル文字コード
     * @param chunkSize 書き出しバッファ行数
     */
    private void writeHeaderFile(
        final File inputFile,
        final String inputCharset,
        final int dataStartRow,
        final File headerFile,
        final String headerCharset,
        final int chunkSize) {

        log.debug("        ・writeHeaderFile");

        // 入力ファイル
        final FileRepository<String> inputRepo = new StringFileRepository(
            inputFile.getAbsolutePath(), inputCharset, null, chunkSize);
        inputRepo.begin();
        // ヘッダーファイル
        final FileRepository<String> headerRepo = new StringFileRepository(
            headerFile.getAbsolutePath(), headerCharset, null, chunkSize);
        headerRepo.begin();

        // 行番号
        long curLineNum = 0;
        // 現在行コンテンツに1行目を読み込み
        String curLineContent = inputRepo.next();

        // 全行ループ
        while (curLineContent != null) {
            curLineNum++;

            // データ開始行に到達した場合、出力ループを抜ける
            if (curLineNum >= dataStartRow) {
                break;
            }

            headerRepo.write(curLineContent);
            curLineContent = inputRepo.next();
        }

        // ファイル操作を確定
        inputRepo.commit();
        headerRepo.commit();
    }

    /**
     * マージソートの前に、チャンクサイズごとにバブルソートしたファイルを作成して高速化します。
     *
     * @param inputFilePath 入力ファイルパス
     * @param inputCharset 入力ファイル文字コード
     * @param fileLayout ファイルレイアウト
     * @param systemConfig システム設定
     * @param outputFile 出力ファイル
     * @param outputCharset 出力ファイル文字コード
     * @param hasHeader ヘッダー有無
     * @param chunkSize 書き出しバッファ行数
     */
    private void writeTempSortedFile(
        final String inputFilePath,
        final String inputCharset,
        final FileLayout fileLayout,
        final CompareFilesConfig systemConfig,
        final File outputFile,
        final String outputCharset,
        final boolean hasHeader,
        final int chunkSize) {

        log.debug("        ・writeTempSortedFile");

        final FileRepository<SortableRow> inputRepo = getRowReadRepository(inputFilePath, inputCharset, fileLayout, systemConfig, hasHeader);
        int dataStartRow = 0;
        if (hasHeader) {
            // データ開始行以降をバブルソートして出力
            dataStartRow = systemConfig.getCsvDataStartRow();
        } else {
            // すべての行をバブルソートして出力
            dataStartRow = 1;
        }

        final LineSp lineSp = getLineSp(fileLayout);

        // ソート済みファイルリポジトリ
        final FileRepository<SortableRow> tempSortedFileRepo =
            new TempSortedRowWriteRepository<SortableRow>(outputFile, outputCharset, lineSp, chunkSize);
        tempSortedFileRepo.begin();

        // 行番号
        long curLineNum = 0;

        // 現在行コンテンツ
        SortableRow curRow = inputRepo.next();

        // 全行ループ
        while (curRow != null) {
            curLineNum++;

            // データ開始行より前の行はスキップ
            if (curLineNum < dataStartRow) {
                continue;
            }

            tempSortedFileRepo.write(curRow);
            curRow = inputRepo.next();
        }

        // ファイル操作を確定
        inputRepo.commit();
        tempSortedFileRepo.commit();
    }

    /**
     * マージソートを実行します。
     *
     * @param inputFile 入力ファイル
     * @param workDirPath 作業ディレクトリパス
     * @param outputFile 出力ファイル
     * @param charset 入出力文字コード
     * @param fileLayout ファイルレイアウト
     * @param systemConfig システム設定
     * @param chunkSize 書き出しバッファ行数
     */
    private void mergeSort(
        final File inputFile,
        final String workDirPath,
        final File outputFile,
        final String charset,
        final FileLayout fileLayout,
        final CompareFilesConfig systemConfig,
        final int chunkSize) {

        log.debug("        ・mergeSort");

        if (inputFile.length() == 0) {
            // データ部が空ファイルの場合、リネームのみ
            if (!inputFile.renameTo(outputFile)) {
                throw new CompareFilesException(Const.FILE_CANTWRITE, new Object[] {
                    outputFile
                });
            }
            return;
        }

        // 分割ファイル
        final String sep1FilePath = workDirPath + File.separator + "sep1";
        final File sep1File = new File(sep1FilePath);
        final String sep2FilePath = workDirPath + File.separator + "sep2";
        final File sep2File = new File(sep2FilePath);

        // 部分ソートファイルをマージファイルにリネーム
        if (inputFile.renameTo(outputFile)) {
            mergeSortMain(sep1File, sep2File, outputFile, charset, fileLayout, systemConfig, chunkSize);
        } else {
            throw new CompareFilesException(Const.FILE_CANTWRITE, new Object[] {outputFile});
        }
    }

    /**
     * マージソートの本書り。
     *
     * @param sep1File 分割ファイル1
     * @param sep2File 分割ファイル2
     * @param mergedFile マージ済みファイル
     * @param charset 入出力文字コード
     * @param fileLayout ファイルレイアウト
     * @param systemConfig システム設定
     * @param chunkSize 書き出しバッファ行数
     */
    private void mergeSortMain(
        final File sep1File,
        final File sep2File,
        final File mergedFile,
        final String charset,
        final FileLayout fileLayout,
        final CompareFilesConfig systemConfig,
        final int chunkSize) {

        long phaseCount = 0;
        while (true) {
            phaseCount++;

            // 分割フェーズ
            if (!mergeSortSeparatePhase(mergedFile, sep1File, sep2File, charset, fileLayout, systemConfig, chunkSize)) {
                // 分割を実施しなかった場合、処理を終了
                return;
            }

            // マージフェーズ
            mergeSortMergePhase(sep1File, sep2File, mergedFile, charset, fileLayout, systemConfig, chunkSize);

            // 進捗表示
            if (phaseCount % 10 == 0) {
                log.debug(phaseCount + " 回目：" + RuntimeUtils.getMemoryInfo());
            }
            if (phaseCount % Const.RANGE_REPORT == 0) {
                log.info("    " + phaseCount + " merge-sort phase finished.");
            }
        }
    }

    /**
     * マージソート.分割フェーズ。
     *
     * @param mergedFile マージ済みファイル
     * @param sep1File 分割ファイル1
     * @param sep2File 分割ファイル2
     * @param charset 入出力文字コード
     * @param fileLayout ファイルレイアウト
     * @param systemConfig システム設定
     * @param chunkSize 書き出しバッファ行数
     * @return 分割を実施した場合、true
     */
    private boolean mergeSortSeparatePhase(
        final File mergedFile,
        final File sep1File,
        final File sep2File,
        final String charset,
        final FileLayout fileLayout,
        final CompareFilesConfig systemConfig,
        final int chunkSize) {

        log.trace("　・separate phase");

        // 分割元ファイルリポジトリ（マージ済みファイル）
        final FileRepository<SortableRow> mergedRepo = getRowReadRepository(mergedFile.getAbsolutePath(), charset, fileLayout, systemConfig);

        final LineSp lineSp = getLineSp(fileLayout);

        // 分割ファイル1リポジトリ
        final FileRepository<String> sep1Repo = new StringFileRepository(sep1File, charset, lineSp, chunkSize);
        sep1Repo.begin();
        // 分割ファイル2リポジトリ
        final FileRepository<String> sep2Repo = new StringFileRepository(sep2File, charset, lineSp, chunkSize);
        sep2Repo.begin();

        // 分割実施フラグ
        boolean isSeparated = false;

        // 行番号
        long curLineNum = 0;
        // 現在行コンテンツ
        SortableRow curLine = mergedRepo.next();
        // 前行コンテンツ
        SortableRow beforeLine = null;

        // 分割ファイル1への出力フラグ
        boolean isWriteSep1 = true;
        log.trace("　　・write sep1");

        // 全行ループ
        while (curLine != null) {
            // 行番号をインクリメント
            curLineNum++;

            // 1行目は前行コンテンツの設定だけでスキップ
            if (curLineNum == LINENUM_FIRST) {
                beforeLine = curLine;
                curLine = mergedRepo.next();
                continue;
            }

            // 出力先に合わせて、前行をファイル出力
            if (isWriteSep1) {
                sep1Repo.write(beforeLine.getRawLine());
            } else {
                sep2Repo.write(beforeLine.getRawLine());
            }

            // 前行と現在行を比較
            if (beforeLine.compareTo(curLine) > 0) {
                // 現在行の方が小さい場合
                // 出力先ファイルを切替
                isWriteSep1 = !isWriteSep1;
                // 分割実施フラグをON
                isSeparated = true;
                if (log.isTraceEnabled()) {
                    if (isWriteSep1) {
                        log.trace("　　・write sep1");
                    } else {
                        log.trace("　　・write sep2");
                    }
                }
            }

            // 前行を更新
            beforeLine = curLine;
            // 次の行へ
            curLine = mergedRepo.next();
        }

        // 最終行の出力
        if (isWriteSep1) {
            sep1Repo.write(beforeLine.getRawLine());
        } else {
            sep2Repo.write(beforeLine.getRawLine());
        }

        // ファイル操作を確定
        mergedRepo.commit();
        sep1Repo.commit();
        sep2Repo.commit();

        return isSeparated;
    }

    /**
     * マージソート.マージフェーズ。
     *
     * @param sep1File 分割ファイル1
     * @param sep2File 分割ファイル2
     * @param mergedFile マージ済みファイル
     * @param charset 入出力文字コード
     * @param fileLayout ファイルレイアウト
     * @param systemConfig システム設定
     * @param chunkSize 書き出しバッファ行数
     */
    private void mergeSortMergePhase(
        final File sep1File,
        final File sep2File,
        final File mergedFile,
        final String charset,
        final FileLayout fileLayout,
        final CompareFilesConfig systemConfig,
        final int chunkSize) {

        log.trace("　・merge phase");

        // マージ元ファイル1リポジトリ（分割ファイル1）
        final FileRepository<SortableRow> sep1Repo = getRowReadRepository(sep1File.getAbsolutePath(), charset, fileLayout, systemConfig);
        // マージ元ファイル2リポジトリ（分割ファイル2）
        final FileRepository<SortableRow> sep2Repo = getRowReadRepository(sep2File.getAbsolutePath(), charset, fileLayout, systemConfig);

        final LineSp lineSp = getLineSp(fileLayout);

        // マージファイルリポジトリ
        final FileRepository<String> mergedRepo = new StringFileRepository(mergedFile, charset, lineSp, chunkSize);
        mergedRepo.begin();

        // sep1用変数
        SortableRow sep1CurLine = null;
        SortableRow sep1BeforeLine = null;
        // sep2用変数
        SortableRow sep2CurLine = null;
        SortableRow sep2BeforeLine = null;

        // マージフェーズステータス
        MergePhaseStatus status = MergePhaseStatus.Compare;

        // 分割ファイルの1行目を読み込み
        sep1CurLine = sep1Repo.next();
        sep2CurLine = sep2Repo.next();

        // 2つの分割ファイル両方を読み込みきるまでループ
        while (true) {

            // マージフェーズステータスを確認
            if (MergePhaseStatus.Sep1GotoUnsortedPos.equals(status)) {
                // --------------------------------------------------
                // 分割ファイル1のソート未済位置まで出力中
                // --------------------------------------------------
                // 読み込み
                sep1BeforeLine = sep1CurLine;
                sep1CurLine = sep1Repo.next();

                // 前行をファイル出力
                mergedRepo.write(sep1BeforeLine.getRawLine());

                // 空行をスキップ
                while (sep1CurLine != null && StringUtils.EMPTY.equals(sep1CurLine.getRawLine())) {
                    sep1CurLine = sep1Repo.next();
                }

                // 現在行がnullの場合、出力を中断
                if (sep1CurLine == null) {
                    // ソートされていない場合、出力を中断
                    status = MergePhaseStatus.Compare;
                }

                // 現在行と分割ファイル2の現在行を比較
                if (sep1CurLine != null && sep2CurLine != null && sep1CurLine.compareTo(sep2CurLine) > 0) {
                    // 分割ファイル2の方が小さい場合、分割ファイル2のソート未済位置まで出力開始
                    status = MergePhaseStatus.Sep2GotoUnsortedPos;

                } else {
                    // 現在行の方が小さい or 一致する場合、前行と現在行を比較
                    if (sep1BeforeLine.compareTo(sep1CurLine) > 0) {
                        // ソートされていない場合、出力を中断
                        status = MergePhaseStatus.Compare;
                    }
                }

            } else if (MergePhaseStatus.Sep2GotoUnsortedPos.equals(status)) {
                // --------------------------------------------------
                // 分割ファイル2のソート未済位置まで出力中
                // --------------------------------------------------
                // 読み込み
                sep2BeforeLine = sep2CurLine;
                sep2CurLine = sep2Repo.next();

                // 前行をファイル出力
                mergedRepo.write(sep2BeforeLine.getRawLine());

                // 空行をスキップ
                while (sep2CurLine != null && StringUtils.EMPTY.equals(sep2CurLine.getRawLine())) {
                    sep2CurLine = sep2Repo.next();
                }

                // 現在行がnullの場合、出力を中断
                if (sep2CurLine == null) {
                    // ソートされていない場合、出力を中断
                    status = MergePhaseStatus.Compare;
                }

                // 現在行と分割ファイル1の現在行を比較
                if (sep1CurLine != null && sep2CurLine != null && sep2CurLine.compareTo(sep1CurLine) > 0) {
                    // 分割ファイル1の方が小さい場合、分割ファイル1のソート未済位置まで出力開始
                    status = MergePhaseStatus.Sep1GotoUnsortedPos;

                } else {
                    // 現在行の方が小さい or 一致する場合、前行と現在行を比較
                    if (sep2BeforeLine.compareTo(sep2CurLine) > 0) {
                        // ソートされていない場合、出力を中断
                        status = MergePhaseStatus.Compare;
                    }
                }

            } else {
                // --------------------------------------------------
                // 分割ファイル1 / 分割ファイル2の比較中
                // --------------------------------------------------
                // 分割ファイル1現在行 と 分割ファイル2現在行 のデータ存在確認
                if (sep1CurLine == null) {
                    if (sep2CurLine == null) {
                        // ------------------------------
                        // どちらも行データが存在しない場合
                        // ------------------------------
                        // ループを終了
                        break;

                    } else {
                        // ------------------------------
                        // 分割ファイル2の行データが存在する場合
                        // ------------------------------
                        // 分割ファイル2のソート未済位置まで出力開始
                        status = MergePhaseStatus.Sep2GotoUnsortedPos;
                    }

                } else {
                    if (sep2CurLine == null) {
                        // ------------------------------
                        // 分割ファイル1の行データだけが存在する場合
                        // ------------------------------
                        // 分割ファイル1のソート未済位置まで出力開始
                        status = MergePhaseStatus.Sep1GotoUnsortedPos;

                    } else {
                        // ------------------------------
                        // どちらも行データが存在する場合
                        // ------------------------------
                        // 分割ファイル1現在行 と 分割ファイル2現在行 の比較
                        if (sep1CurLine.compareTo(sep2CurLine) <= 0) {
                            // 分割ファイル1現在行の方が小さい場合、分割ファイル1のソート未済位置まで出力開始
                            status = MergePhaseStatus.Sep1GotoUnsortedPos;
                        } else {
                            // 分割ファイル2現在行の方が小さい場合、分割ファイル2のソート未済位置まで出力開始
                            status = MergePhaseStatus.Sep2GotoUnsortedPos;
                        }
                    }
                }

            } // END MargePhaseStatus分岐
        } // END 2つの分割ファイル両方を読み込みきるまでループ

        // ファイル操作を確定
        sep1Repo.commit();
        sep2Repo.commit();
        mergedRepo.commit();
    }

    /**
     * ヘッダーファイルと、マージソート済ファイルを結合して結果ファイルを出力します。
     *
     * @param headerFile ヘッダーファイル
     * @param headerFileCharset ヘッダーファイル文字コード
     * @param mergedFile マージソート済ファイル
     * @param mergedFileCharset マージソート済ファイル文字コード
     * @param outputFile 出力ファイル
     * @param outputCharset 出力ファイル文字コード
     * @param fileLayout ファイルレイアウト
     * @param systemConfig システム設定
     * @param chunkSize 書き出しバッファ行数
     */
    private void writeResultFile(
        final File headerFile,
        final String headerFileCharset,
        final File mergedFile,
        final String mergedFileCharset,
        final File outputFile,
        final String outputCharset,
        final FileLayout fileLayout,
        final CompareFilesConfig systemConfig,
        final int chunkSize) {

        log.debug("        ・writeResultFile");

        final LineSp lineSp = getLineSp(fileLayout);

        // 出力ファイルリポジトリ
        final FileRepository<String> outputRepo = new StringFileRepository(outputFile, outputCharset, lineSp, chunkSize);
        outputRepo.begin();

        // --------------------------------------------------------------------------------
        // ヘッダーファイル部分の出力
        // --------------------------------------------------------------------------------
        if (headerFile.exists()) {
            final FileRepository<String> headerRepo = new StringFileRepository(headerFile, headerFileCharset, lineSp, chunkSize);
            headerRepo.begin();

            String curLineStr = headerRepo.next();
            while (curLineStr != null) {
                outputRepo.write(curLineStr);
                curLineStr = headerRepo.next();
            }

            headerRepo.commit();
        }

        // --------------------------------------------------------------------------------
        // データファイル部分の出力
        // --------------------------------------------------------------------------------
        final FileRepository<SortableRow> mergedRepo = getRowReadRepository(mergedFile.getAbsolutePath(), mergedFileCharset, fileLayout, systemConfig);
        mergedRepo.begin();

        SortableRow curLine = mergedRepo.next();
        while (curLine != null) {
            outputRepo.write(curLine.getRawLine());
            curLine = mergedRepo.next();
        }

        mergedRepo.commit();

        // ファイル操作を確定
        outputRepo.commit();
    }

    /**
     * ファイルレイアウトから、改行コードを判断します。
     *
     * @param fileLayout ファイルレイアウト
     * @return 改行コード
     */
    private LineSp getLineSp(final FileLayout fileLayout) {
        LineSp lineSp = null;
        if (FileFormat.Fixed.equals(fileLayout.getFileFormat())) {
            lineSp = fileLayout.getLineSp();
        }
        return lineSp;
    }

    /**
     * ソート処理の入力データモデル。
     */
    @Getter
    @Setter
    private static class FileSortInput implements Input {

        /** システム設定。 */
        @NotNull
        private CompareFilesConfig systemConfig;

        /** 入力ファイルレイアウト。 */
        @NotNull
        @Valid
        private FileLayout fileLayout;

        /** 入力ファイル。 */
        @NotEmpty
        @ExistPath
        @me.suwash.util.validation.constraints.File
        private String inputFile;

        /** 入力ファイル文字コード。 */
        @NotEmpty
        @Charset
        private String inputCharset;

        /** 出力ファイル配置ディレクトリ。 */
        @NotEmpty
        private String outputDir;

        /** 出力ファイル文字コード。 */
        @NotEmpty
        @Charset
        private String outputCharset;

    }
}
