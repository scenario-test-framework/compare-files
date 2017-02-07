package me.suwash.tools.comparefiles.ap.dto;

import lombok.Getter;
import lombok.Setter;
import me.suwash.util.validation.constraints.Dir;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * ディレクトリ比較機能 入力データモデル。
 */
@Setter
@Getter
public class DirCompareFacadeInput extends BaseFacadeInput {

    /** 左ディレクトリパス。 */
    @NotEmpty
    @Dir
    private String leftDirPath;

    /** 右ディレクトリパス。 */
    @NotEmpty
    @Dir
    private String rightDirPath;

}
