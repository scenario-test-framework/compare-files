package me.suwash.tools.comparefiles.sv.da.file.repository.impl;

import java.io.File;
import java.util.Collections;

import me.suwash.tools.comparefiles.infra.classification.LineSp;
import me.suwash.tools.comparefiles.sv.domain.sort.SortableRow;

/**
 * chunk単位のバブルソート出力リポジトリ。
 *
 * @param <R> 行データ
 */
public class TempSortedRowWriteRepository<R extends SortableRow> extends GenericRowWriteRepository<R> {

    /**
     * コンストラクタ。
     *
     * @param file ファイル
     * @param charset 文字コード
     * @param lineSp 改行コード
     * @param chunkSize 書き出しバッファ行数
     */
    public TempSortedRowWriteRepository(
        final File file,
        final String charset,
        final LineSp lineSp,
        final int chunkSize) {

        super(file, charset, lineSp, chunkSize);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.repository.impl.GenericFileRepository#writeChunk()
     */
    @Override
    protected void writeChunk() {
        Collections.sort(chunk);
        super.writeChunk();
    }

}
