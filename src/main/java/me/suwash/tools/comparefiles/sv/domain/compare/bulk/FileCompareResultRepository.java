package me.suwash.tools.comparefiles.sv.domain.compare.bulk;

import me.suwash.tools.comparefiles.infra.policy.FileRepository;
import me.suwash.tools.comparefiles.sv.domain.compare.file.FileCompareResult;

/**
 * ファイル比較結果リポジトリ。
 */
public interface FileCompareResultRepository extends FileRepository<FileCompareResult> {
}
