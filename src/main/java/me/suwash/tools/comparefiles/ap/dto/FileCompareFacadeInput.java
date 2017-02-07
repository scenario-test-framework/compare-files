package me.suwash.tools.comparefiles.ap.dto;

import lombok.Getter;
import lombok.Setter;
import me.suwash.util.validation.constraints.File;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * ファイル比較機能 入力データモデル。
 */
@Getter
@Setter
public class FileCompareFacadeInput extends BaseFacadeInput {

    /** 左ファイルパス。 */
    @NotEmpty
    @File
    private String leftFilePath;

    /** 右ファイルパス。 */
    @NotEmpty
    @File
    private String rightFilePath;

}
