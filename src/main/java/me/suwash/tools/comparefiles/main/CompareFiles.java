package me.suwash.tools.comparefiles.main;

import java.io.File;
import java.util.List;

import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.tools.comparefiles.ap.DirCompareFacade;
import me.suwash.tools.comparefiles.ap.FileCompareFacade;
import me.suwash.tools.comparefiles.ap.dto.DirCompareFacadeInput;
import me.suwash.tools.comparefiles.ap.dto.DirCompareFacadeOutput;
import me.suwash.tools.comparefiles.ap.dto.FileCompareFacadeInput;
import me.suwash.tools.comparefiles.ap.dto.FileCompareFacadeOutput;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;

import org.apache.commons.lang3.StringUtils;

/**
 * CUIバウンダリ。
 */
@lombok.extern.slf4j.Slf4j
public class CompareFiles extends BaseCompareFiles {

    private static final int PARAM_SIZE_NO_CONFIG = 2;

    /**
     * デフォルトコンストラクタ。
     * UTからアクセスできる様にprotected公開しています。
     */
    protected CompareFiles() {
        super();
    }

    /**
     * コマンドラインのエントリポイント。
     *
     * @param args コマンドライン引数
     */
    public static void main(final String... args) {
        // 比較実行
        final CompareFiles instance = new CompareFiles();
        final ProcessStatus processStatus = instance.execute(args);

        // 結果判定
        exitScript(processStatus);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.main.BaseCompareFiles#isValidParam(me.suwash.tools.comparefiles.main.CompareFilesOption)
     */
    @Override
    protected boolean isValidParam(final CompareFilesOption option) {
        boolean isValid = true;

        final List<String> paramList = option.getParamList();
        // 設定ファイルパスが指定されていない場合、「左パス」「右パス」が指定されていないければエラー
        if (StringUtils.isEmpty(option.getConfigFilePath()) && paramList.size() != PARAM_SIZE_NO_CONFIG) {
            isValid = false;
            final StringBuilder usageBuilder = new StringBuilder();
            usageBuilder
                .append("Usage: compare_files [Options] LEFT_PATH RIGHT_PATH").append(StringUtils.LF)
                .append(StringUtils.LF)
                .append(option.usage());
            System.err.println(usageBuilder.toString());
        }

        return isValid;
    }


    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.main.BaseCompareFiles#executeFacade(java.util.List, me.suwash.tools.comparefiles.infra.config.CompareFilesConfig)
     */
    @Override
    protected ProcessStatus executeFacade(final List<String> paramList, final CompareFilesConfig config) {
        // 左パス
        final String leftPath = paramList.get(0);
        final File left = new File(leftPath);

        // 右パス
        final String rightPath = paramList.get(1);
        final File right = new File(rightPath);

        // 出力ディレクトリ
        final String outputDirPath = config.getOutputDir();

        // 実行情報出力
        log.info("・入力情報");
        log.info("  ・左パス                          : " + left.getPath());
        log.info("  ・右パス                          : " + right.getPath());
        config.printDetails();

        // 比較レイアウト読み込み
        loadLayout(config);

        // 実行
        ProcessStatus processStatus = null;
        if (left.isFile()) {
            final FileCompareFacadeOutput outDto = executeFileFacade(leftPath, rightPath, outputDirPath, config);
            processStatus = outDto.getProcessStatus();
        } else {
            final DirCompareFacadeOutput outDto = executeDirFacade(leftPath, rightPath, outputDirPath, config);
            processStatus = outDto.getProcessStatus();
        }
        return processStatus;
    }

    /**
     * ファイル比較を実行します。
     *
     * @param leftFilePath 左ファイルパス
     * @param rightFilePath 右ファイルパス
     * @param outputDirPath 出力ディレクトリパス
     * @param config システム設定
     * @return 比較結果データモデル
     */
    private FileCompareFacadeOutput executeFileFacade(
        final String leftFilePath,
        final String rightFilePath,
        final String outputDirPath,
        final CompareFilesConfig config) {

        log.info("  ・比較モード                      : ファイル比較");

        // Facade呼出し
        final FileCompareFacadeInput inDto = new FileCompareFacadeInput();
        inDto.setLeftFilePath(leftFilePath);
        inDto.setRightFilePath(rightFilePath);
        inDto.setOutputDir(outputDirPath);
        inDto.setConfig(config);
        final FileCompareFacadeOutput outDto = new FileCompareFacade().execute(inDto);

        // 結果出力
        outDto.printDetails();
        return outDto;
    }

    /**
     * ディレクトリ比較を実行します。
     *
     * @param leftDirPath 左ディレクトリパス
     * @param rightDirPath 右ディレクトリパス
     * @param outputDirPath 出力ディレクトリパス
     * @param config システム設定
     * @return 比較結果データモデル
     */
    private DirCompareFacadeOutput executeDirFacade(final String leftDirPath, final String rightDirPath, final String outputDirPath, final CompareFilesConfig config) {
        log.info("  ・比較モード                      : ディレクトリ比較");

        // Facade呼出し
        final DirCompareFacadeInput inDto = new DirCompareFacadeInput();
        inDto.setLeftDirPath(leftDirPath);
        inDto.setRightDirPath(rightDirPath);
        inDto.setOutputDir(outputDirPath);
        inDto.setConfig(config);
        final DirCompareFacadeOutput outDto = new DirCompareFacade().execute(inDto);

        // 結果出力
        outDto.printDetails();
        return outDto;
    }

}
