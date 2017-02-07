package me.suwash.tools.comparefiles.ap;

import me.suwash.tools.comparefiles.ap.dto.DirCompareFacadeInput;
import me.suwash.tools.comparefiles.ap.dto.DirCompareFacadeOutput;
import me.suwash.tools.comparefiles.sv.domain.compare.bulk.DirCompareResult;

/**
 * ディレクトリ同士の比較制御。
 */
// @lombok.extern.slf4j.Slf4j
public class DirCompareFacade extends BaseFacade<DirCompareFacadeInput, DirCompareFacadeOutput> {

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#preExecute(me.suwash.ddd.policy.Input)
     */
    @Override
    protected DirCompareFacadeOutput preExecute(final DirCompareFacadeInput input) {
        // 出力データモデルの初期化
        final DirCompareFacadeOutput outDto = new DirCompareFacadeOutput();
        outDto.setInput(input);
        return outDto;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#mainExecute(me.suwash.ddd.policy.Input, me.suwash.ddd.policy.Output)
     */
    @Override
    protected DirCompareFacadeOutput mainExecute(final DirCompareFacadeInput input, final DirCompareFacadeOutput output) {
        // 比較実行
        final DirCompareResult result = new DirCompareResult(
            input.getLeftDirPath(),
            input.getRightDirPath(),
            input.getOutputDir(),
            input.getConfig()
            );
        result.compare();

        output.setResult(result);
        return output;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#postExecute(me.suwash.ddd.policy.Input, me.suwash.ddd.policy.Output)
     */
    @Override
    protected DirCompareFacadeOutput postExecute(final DirCompareFacadeInput input, final DirCompareFacadeOutput output) {
        // 処理ステータスの確定
        output.fixProcessStatus();
        return output;
    }

}
