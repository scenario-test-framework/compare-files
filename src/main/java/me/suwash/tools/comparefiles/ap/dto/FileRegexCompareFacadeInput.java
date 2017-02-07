package me.suwash.tools.comparefiles.ap.dto;

import lombok.Getter;
import lombok.Setter;
import me.suwash.util.validation.constraints.ExistPath;
import me.suwash.util.validation.constraints.File;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * 対象ファイル名正規表現リスト比較機能 入力データモデル。
 */
@Setter
@Getter
public class FileRegexCompareFacadeInput extends BaseFacadeInput {

    /** 比較対象設定ファイルパス。 */
    @NotEmpty
    @File
    @ExistPath
    private String targetConfigFilePath;

}
