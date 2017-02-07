package me.suwash.tools.comparefiles.infra.config;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * ファイルレイアウト設定の集合。
 */
@Getter
@Setter
public class FileLayoutList {

    /** ファイルレイアウト定義リスト。 */
    private List<FileLayout> layoutList;
}
