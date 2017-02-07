package me.suwash.tools.comparefiles.ap;

import me.suwash.ddd.policy.Input;
import me.suwash.ddd.policy.Output;
import me.suwash.ddd.policy.layer.ap.Facade;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.Context;
import me.suwash.tools.comparefiles.infra.i18n.CompareFilesMessageSource;
import me.suwash.tools.comparefiles.infra.policy.BaseLayerSuperType;
import me.suwash.tools.comparefiles.infra.policy.Repository;

/**
 * アプリケーション層のLayerSuperType基底クラス。
 *
 * @param <I> 入力データモデル
 * @param <O> 出力データモデル
 */
@lombok.extern.slf4j.Slf4j
public abstract class BaseFacade<I extends Input, O extends Output<I>> extends BaseLayerSuperType<I, O> implements Facade<I, O> {

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.infra.policy.BaseLayerSuperType#execute(me.suwash.ddd.policy.Input)
     */
    @Override
    public O execute(I input) {
        boolean hasError = false;
        O output = null;
        try {
            output = super.execute(input);

        } catch (Exception e) {
            hasError = true;
            log.error(
                CompareFilesMessageSource.getInstance().getMessage(
                    Const.ERRORHANDLE,
                    new Object[] {this.getClass().getSimpleName(), e.getMessage()}));
            for (final Repository curRepo : Context.getInstance().getRepositoryList()) {
                log.warn("  force-rollback: " + curRepo.toString());
                curRepo.rollback();
            }
            // 再throw
            throw e;

        } finally {
            if (!hasError) {
                for (final Repository curRepo : Context.getInstance().getRepositoryList()) {
                    log.warn("  force-commit:" + curRepo.toString());
                    curRepo.commit();
                }
            }

        }

        return output;
    }

}
