package me.suwash.tools.comparefiles.sv.da.file.writer;

import java.io.Closeable;

/**
 * 比較結果Writer。
 *
 * @param <E> 比較結果
 */
public interface ResultWriter<E> extends Closeable {

    /**
     * 1行を出力します。
     *
     * @param row 行単位の比較結果
     */
    void write(E row);

    /**
     * 出力バッファを全てファイル出力し、クリアします。
     */
    void flush();

}
