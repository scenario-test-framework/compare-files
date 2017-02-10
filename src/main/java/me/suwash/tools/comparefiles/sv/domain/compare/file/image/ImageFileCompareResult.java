package me.suwash.tools.comparefiles.sv.domain.compare.file.image;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Date;
import java.util.List;

import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.config.FileLayoutManager;
import me.suwash.tools.comparefiles.infra.util.ValidateUtils;
import me.suwash.tools.comparefiles.sv.da.file.repository.impl.ImageFileRepository;
import me.suwash.tools.comparefiles.sv.domain.compare.file.FileCompareResult;
import me.suwash.util.CompareUtils.CompareStatus;
import me.suwash.util.ImageCompareUtils;
import me.suwash.util.ImageCompareUtils.DiffAreas;

/**
 * ファイル単位の比較結果。
 */
@lombok.extern.slf4j.Slf4j
public class ImageFileCompareResult extends FileCompareResult {

    private final String inputExt;

    /**
     * コンストラクタ。
     *
     * @param leftFilePath 左ファイルパス
     * @param rightFilePath 右ファイルパス
     * @param fileLayout ファイルレイアウト
     * @param outputDirPath 出力ディレクトリパス
     * @param systemConfig システム設定
     * @param inputExt ファイル拡張子
     */
    public ImageFileCompareResult(
        final String leftFilePath,
        final String rightFilePath,
        final FileLayout fileLayout,
        final String outputDirPath,
        final CompareFilesConfig systemConfig,
        final String inputExt) {

        super(leftFilePath, rightFilePath, outputDirPath, systemConfig);
        this.outputExt = "png";
        this.input.setFileLayout(fileLayout);

        this.inputExt = inputExt;
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
        // システム設定
        final CompareFilesConfig systemConfig = input.getSystemConfig();

        // 左ファイル
        final String leftFilePath = input.getLeftFilePath();
        final File leftFile = new File(leftFilePath);
        this.leftFilePath = leftFile.getAbsolutePath();
        // 右ファイル
        final String rightFilePath = input.getRightFilePath();
        final File rightFile = new File(rightFilePath);
        this.rightFilePath = rightFile.getAbsolutePath();
        // 入力拡張子
        final String inputExt = this.inputExt;
        // 出力ファイル
        final String outputFileName = getOutputFileName(systemConfig);
        final String outputDirPath = input.getOutputDirPath();
        final String outputFilePath = outputDirPath + "/" + outputFileName;
        // 出力拡張子
        final String outputExt = this.outputExt;
        // ファイルレイアウト
        FileLayout fileLayout = input.getFileLayout();
        if (fileLayout == null) {
            // 指定されていない場合、画像ファイルのデフォルトを設定
            fileLayout = FileLayoutManager.getDefaultImageLayout(systemConfig);
        }
        this.fileLayout = fileLayout;

        // 実行開始時刻
        this.startTime = new Date();

        // ----------------------------------------
        // ファイル比較
        // ----------------------------------------
        log.info("    ・画像比較");
        this.status = compare(
            leftFilePath,
            rightFilePath,
            inputExt,
            outputFilePath,
            outputExt,
            fileLayout,
            systemConfig);

        // --------------------------------------------------------------------------------
        // 事後処理
        // --------------------------------------------------------------------------------
        // 終了時刻を設定
        this.endTime = new Date();
    }

    /**
     * 画像を比較します。
     *
     * @param leftFilePath 左ファイルパス
     * @param rightFilePath 右ファイルパス
     * @param inputExt 入力拡張子
     * @param outputFilePath 出力ファイルパス
     * @param outputExt 出力拡張子
     * @param fileLayout ファイルレイアウト
     * @param systemConfig システム設定
     * @return 比較結果
     */
    private CompareStatus compare(
        final String leftFilePath,
        final String rightFilePath,
        final String inputExt,
        final String outputFilePath,
        final String outputExt,
        final FileLayout fileLayout,
        final CompareFilesConfig systemConfig) {

        // 左ファイル
        final File leftFile = new File(leftFilePath);
        // 右ファイル
        final File rightFile = new File(rightFilePath);

        // 除外エリア
        final List<Rectangle> ignoreAreaList = fileLayout.getIgnoreAreaList();

        // 左ファイル
        final ImageFileRepository leftRepo = new ImageFileRepository(leftFile, inputExt);
        leftRepo.begin();
        final BufferedImage leftImage = leftRepo.next();
        leftRepo.commit();

        // 左ファイル
        final ImageFileRepository rightRepo = new ImageFileRepository(rightFile, inputExt);
        rightRepo.begin();
        final BufferedImage rightImage = rightRepo.next();
        rightRepo.commit();

        // 比較
        final DiffAreas diffAreas = ImageCompareUtils.compare(leftImage, rightImage, ignoreAreaList);

        // 結果出力
        this.ignoreRowCount = ignoreAreaList.size();
        this.ngRowCount = diffAreas.size();

        final ImageFileRepository resultRepo = new ImageFileRepository(new File(outputFilePath), outputExt);
        resultRepo.begin();
        CompareStatus comparedStatus = null;
        if (diffAreas.hasDiff()) {
            comparedStatus = CompareStatus.NG;
            // 比較結果を出力
            final BufferedImage result = ImageCompareUtils.getNgImage(
                leftImage,
                rightImage,
                diffAreas,
                systemConfig.getLeftPrefix(),
                systemConfig.getRightPrefix(),
                systemConfig.getNgImageStyle());
            resultRepo.write(result);

        } else {
            comparedStatus = CompareStatus.OK;
            if (!systemConfig.isWriteDiffOnly()) {
                // 差分なしでも出力する場合、比較結果を出力
                final BufferedImage result = ImageCompareUtils.getOkImage(
                    leftImage,
                    rightImage,
                    ignoreAreaList,
                    systemConfig.getLeftPrefix(),
                    systemConfig.getRightPrefix(),
                    systemConfig.getOkImageStyle());
                resultRepo.write(result);
            }

        }

        // ファイル操作を確定
        resultRepo.commit();

        // 比較結果を返す
        return comparedStatus;
    }

}
