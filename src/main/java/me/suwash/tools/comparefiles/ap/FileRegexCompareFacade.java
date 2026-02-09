package me.suwash.tools.comparefiles.ap;

import me.suwash.tools.comparefiles.ap.dto.FileRegexCompareFacadeInput;
import me.suwash.tools.comparefiles.ap.dto.FileRegexCompareFacadeOutput;
import me.suwash.tools.comparefiles.sv.domain.compare.bulk.FileNameRegexCompareResult;

/**
 * 対象ファイル名正規表現リストの比較制御。
 */
public class FileRegexCompareFacade extends BaseFacade<FileRegexCompareFacadeInput, FileRegexCompareFacadeOutput> {

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.infra.policy.BaseLayerSuperType#preExecute(me.suwash.tools.comparefiles.infra.policy.Input)
     */
    @Override
    protected FileRegexCompareFacadeOutput preExecute(final FileRegexCompareFacadeInput input) {
        // 出力データモデルの初期化
        final FileRegexCompareFacadeOutput outDto = new FileRegexCompareFacadeOutput();
        outDto.setInput(input);
        return outDto;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.infra.policy.BaseLayerSuperType#mainExecute(me.suwash.tools.comparefiles.infra.policy.Input, me.suwash.tools.comparefiles.infra.policy.Output)
     */
    @Override
    protected FileRegexCompareFacadeOutput mainExecute(final FileRegexCompareFacadeInput input, final FileRegexCompareFacadeOutput output) {
        // 比較実行
        final FileNameRegexCompareResult result = new FileNameRegexCompareResult(
            input.getTargetConfigFilePath(),
            input.getOutputDir(),
            input.getConfig()
            );
        result.compare();

        // FacadeOutputに一括比較結果を設定
        output.setResult(result);
        return output;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.infra.policy.BaseLayerSuperType#postExecute(me.suwash.tools.comparefiles.infra.policy.Input, me.suwash.tools.comparefiles.infra.policy.Output)
     */
    @Override
    protected FileRegexCompareFacadeOutput postExecute(final FileRegexCompareFacadeInput input, final FileRegexCompareFacadeOutput output) {
        // 処理ステータスの確定
        output.fixProcessStatus();
        return output;
    }

}
