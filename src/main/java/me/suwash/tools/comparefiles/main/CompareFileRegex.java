package me.suwash.tools.comparefiles.main;

import java.io.File;
import java.util.List;

import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.tools.comparefiles.ap.FileRegexCompareFacade;
import me.suwash.tools.comparefiles.ap.dto.FileRegexCompareFacadeInput;
import me.suwash.tools.comparefiles.ap.dto.FileRegexCompareFacadeOutput;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;

import org.apache.commons.lang3.StringUtils;

/**
 * CUIバウンダリ。比較対象ファイル名の正規表現指定版。
 */
@lombok.extern.slf4j.Slf4j
public class CompareFileRegex extends BaseCompareFiles {

    private static final int PARAM_SIZE_NO_CONFIG = 1;

    /**
     * デフォルトコンストラクタ。
     * UTからアクセスできる様にprotected公開しています。
     */
    protected CompareFileRegex() {
        super();
    }

    /**
     * コマンドライン引数から設定を判断し、変換をを実行します。
     *
     * @param args コマンドライン引数
     */
    public static void main(final String... args) {
        // 比較実行
        final CompareFileRegex instance = new CompareFileRegex();
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
        // 設定ファイルパスが指定されていない場合、「比較対象設定ファイルパス」が指定されていないければエラー
        if (StringUtils.isEmpty(option.getConfigFilePath()) && paramList.size() != PARAM_SIZE_NO_CONFIG) {
            isValid = false;
        }

        return isValid;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.main.BaseCompareFiles#executeFacade(java.util.List, me.suwash.tools.comparefiles.infra.config.CompareFilesConfig)
     */
    @Override
    protected ProcessStatus executeFacade(final List<String> paramList, final CompareFilesConfig config) {
        // 比較対象設定ファイル
        final String targetConfigFilePath = paramList.get(0);
        final File targetConfigFile = new File(targetConfigFilePath);

        // 出力ディレクトリ
        final String outputDirPath = config.getOutputDir();

        // Facade呼出し
        log.info("・入力情報");
        log.info("  ・比較対象設定ファイル            : " + targetConfigFile.getPath());
        config.printDetails();

        // 比較レイアウト読み込み
        loadLayout(config);

        // Facade呼出し
        final FileRegexCompareFacadeInput inDto = new FileRegexCompareFacadeInput();
        inDto.setTargetConfigFilePath(targetConfigFilePath);
        inDto.setOutputDir(outputDirPath);
        inDto.setConfig(config);
        final FileRegexCompareFacadeOutput outDto = new FileRegexCompareFacade().execute(inDto);

        // 結果出力
        outDto.printDetails();
        return outDto.getProcessStatus();
    }

}
