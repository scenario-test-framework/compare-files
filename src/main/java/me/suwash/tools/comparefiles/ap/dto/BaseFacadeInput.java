package me.suwash.tools.comparefiles.ap.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import me.suwash.ddd.policy.Input;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * アプリケーション層 入力データモデル 基底クラス。
 */
@Setter
@Getter
public class BaseFacadeInput implements Input {

    /** 出力ディレクトリ。 */
    @NotEmpty
    private String outputDir;

    /** システム設定。 */
    @Valid
    @NotNull
    private CompareFilesConfig config;

}
