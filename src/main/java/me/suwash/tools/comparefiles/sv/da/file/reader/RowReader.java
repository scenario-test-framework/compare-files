package me.suwash.tools.comparefiles.sv.da.file.reader;

import java.io.Closeable;

import me.suwash.tools.comparefiles.sv.domain.BaseRow;

/**
 * 行データに変換して読込むReader。
 *
 * @param <R> 行データクラス
 */
public interface RowReader<R extends BaseRow> extends Readable, Closeable {

    /**
     * 次の行を読み込み、対象クラスに変換して返します。
     * ファイルのEOFに到達した場合、nullを返します。
     *
     * @return 対象行オブジェクト
     */
    R next();

}
