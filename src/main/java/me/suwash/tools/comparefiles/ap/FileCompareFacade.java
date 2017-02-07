package me.suwash.tools.comparefiles.ap;

import me.suwash.tools.comparefiles.ap.dto.FileCompareFacadeInput;
import me.suwash.tools.comparefiles.ap.dto.FileCompareFacadeOutput;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;
import me.suwash.tools.comparefiles.sv.domain.compare.file.FileCompareResult;

/**
 * ファイル同士の比較制御。
 */
public class FileCompareFacade extends BaseFacade<FileCompareFacadeInput, FileCompareFacadeOutput> {

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#preExecute(me.suwash.ddd.policy.Input)
     */
    @Override
    protected FileCompareFacadeOutput preExecute(final FileCompareFacadeInput input) {
        // 出力データモデルの初期化
        final FileCompareFacadeOutput output = new FileCompareFacadeOutput();
        output.setInput(input);
        return output;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#mainExecute(me.suwash.ddd.policy.Input, me.suwash.ddd.policy.Output)
     */
    @Override
    protected FileCompareFacadeOutput mainExecute(final FileCompareFacadeInput input, final FileCompareFacadeOutput output) {
        // ----------------------------------------------------------------------
        // 設定取得
        // ----------------------------------------------------------------------
        // 左ファイル
        final String leftFilePath = input.getLeftFilePath();
        // 右ファイル
        final String rightFilePath = input.getRightFilePath();
        // システム設定
        final CompareFilesConfig config = input.getConfig();

        // ----------------------------------------------------------------------
        // 比較
        // ----------------------------------------------------------------------
        // 実行
        final FileCompareResult result = new FileCompareResult(leftFilePath, rightFilePath, config.getOutputDir(), config);
        result.compare();

        // 結果設定
        output.setResult(result);
        return output;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#postExecute(me.suwash.ddd.policy.Input, me.suwash.ddd.policy.Output)
     */
    @Override
    protected FileCompareFacadeOutput postExecute(final FileCompareFacadeInput input, final FileCompareFacadeOutput output) {
        return output;
    }

}
