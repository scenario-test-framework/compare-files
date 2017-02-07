package me.suwash.tools.comparefiles.sv.domain.compare;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * 比較処理入力データモデルの基底クラス。
 */
@Getter
@Setter
public class BaseCompareInput {

    /** システム設定。 */
    @NotNull
    protected CompareFilesConfig systemConfig;

    /** 比較結果出力ディレクトリ。 */
    @NotEmpty
    protected String outputDirPath;

}
