package me.suwash.tools.comparefiles.ap.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import me.suwash.tools.comparefiles.infra.policy.Input;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;

import jakarta.validation.constraints.NotEmpty;

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
