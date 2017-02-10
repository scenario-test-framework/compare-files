package me.suwash.tools.comparefiles.main;

import java.util.List;

import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;
import me.suwash.tools.comparefiles.infra.config.FileLayoutManager;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.infra.i18n.CompareFilesMessageSource;
import me.suwash.tools.comparefiles.infra.util.ValidateUtils;
import me.suwash.util.ConfigUtils;

import org.apache.commons.lang3.StringUtils;

import com.beust.jcommander.JCommander;

/**
 * CUIバウンダリ基底クラス。
 */
@lombok.extern.slf4j.Slf4j
public abstract class BaseCompareFiles {

    /**
     * 比較実行のTemplateメソッドです。
     * サブクラスのmainメソッドから呼び出せる様に、protectedで公開しています。
     *
     * @param args コマンドライン引数
     * @return 比較結果
     */
    protected ProcessStatus execute(final String... args) {
        log.info(CompareFilesMessageSource.getInstance().getMessage(Const.MSGCD_PROCESS_START));

        // -----------------------------------------------------------------------------------------
        // シャットダウンフック追加
        // -----------------------------------------------------------------------------------------
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                final String violationMessage = ValidateUtils.getViolationMessage();
                if (!StringUtils.isEmpty(violationMessage)) {
                    System.err.println(violationMessage);
                }
            }
        });

        // -----------------------------------------------------------------------------------------
        // 引数のパース
        // -----------------------------------------------------------------------------------------
        final CompareFilesOption option = new CompareFilesOption();
        new JCommander(option, args);
        if (!isValidParam(option)) {
            throw new CompareFilesException(Const.MSGCD_ERROR_ARG);
        }

        // -----------------------------------------------------------------------------------------
        // 設定ファイルの読み込み
        // -----------------------------------------------------------------------------------------
        // デフォルト設定の読み込み
        final CompareFilesConfig defaultConfig = CompareFilesConfig.parse(
            ConfigUtils.getConfigFileClasspath(CompareFiles.class, Const.EXT_CONFIG),
            true);

        // 設定ファイルパスの指定を確認
        CompareFilesConfig config = null;
        if (StringUtils.isEmpty(option.getConfigFilePath())) {
            // 指定されていない場合、デフォルト設定を利用
            config = defaultConfig;

        } else {
            // 設定ファイルパスが指定されている場合、カスタム設定の読み込み
            config = CompareFilesConfig.parse(option.getConfigFilePath(), false);
            config.setDefault(defaultConfig);
        }

        // 設定をコマンドライン引数で上書き
        option.overwriteConfig(config);

        // 設定の妥当性チェック
        ValidateUtils.validate(config);

        // -----------------------------------------------------------------------------------------
        // Facade呼出し
        // -----------------------------------------------------------------------------------------
        final List<String> paramList = option.getParamList();
        return executeFacade(paramList, config);
    }

    /**
     * 起動パラメータの妥当性を確認します。
     *
     * @param option 起動パラメータ
     * @return 妥当な場合、true
     */
    protected abstract boolean isValidParam(CompareFilesOption option);

    /**
     * 比較レイアウトを読み込みます。
     * サブクラスから任意のタイミングで読み込みできる様にprotected公開しています。
     *
     * @param config システム設定
     */
    protected void loadLayout(final CompareFilesConfig config) {
        // --------------------------------------------------
        // レイアウト設定の読み込み
        // --------------------------------------------------
        final FileLayoutManager layoutManager = FileLayoutManager.getInstance();
        final String overwriteLayoutDirs = config.getOverwriteLayoutDir();
        if (!StringUtils.isEmpty(overwriteLayoutDirs)) {
            for (final String curDir : overwriteLayoutDirs.split(",")) {
                layoutManager.addLayoutDir(curDir.trim());
            }
        }
    }

    /**
     * アプリケーション層の処理を呼び出します。
     *
     * @param paramList 引数なしパラメータリスト
     * @param config システム設定
     * @return 処理結果
     */
    protected abstract ProcessStatus executeFacade(List<String> paramList, CompareFilesConfig config);

    /**
     * アプリケーション層の処理結果を判断します。
     * サブクラスから共通で判断できる様にprotected公開しています。
     *
     * @param processStatus アプリケーション層の処理結果
     */
    protected static void exitScript(final ProcessStatus processStatus) {
        switch (processStatus) {
            case Success:
                log.info(CompareFilesMessageSource.getInstance().getMessage(Const.MSGCD_EXIT_SUCCESS));
                System.exit(Const.EXITCODE_SUCCESS);
                break;

            case Warning:
                log.warn(CompareFilesMessageSource.getInstance().getMessage(Const.MSGCD_EXIT_WARN));
                System.exit(Const.EXITCODE_WARN);
                break;

            case Failure:
            default:
                log.error(CompareFilesMessageSource.getInstance().getMessage(Const.MSGCD_EXIT_FAIL));
                System.exit(Const.EXITCODE_ERROR);
                break;
        }

    }
}
