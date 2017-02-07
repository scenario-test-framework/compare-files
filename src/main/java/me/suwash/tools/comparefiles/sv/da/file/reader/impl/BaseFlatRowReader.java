package me.suwash.tools.comparefiles.sv.da.file.reader.impl;

import java.io.FileNotFoundException;
import java.util.Map;

import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.sv.domain.BaseRow;

/**
 * 階層なしテキストファイルを行データに変換して読込むReaderの基底クラス。
 *
 * @param <R> 行データクラス
 */
public abstract class BaseFlatRowReader<R extends BaseRow> extends BaseRowReader<R> {

    /**
     * コンストラクタ。
     *
     * @param filePath ファイルパス
     * @param charset 文字コード
     * @param layout ファイルレイアウト
     * @param dummy 型引数ダミー値
     * @throws FileNotFoundException ファイルが存在しない場合
     */
    @SuppressWarnings("unchecked")
    protected BaseFlatRowReader(final String filePath, final String charset, final FileLayout layout, final R... dummy) throws FileNotFoundException {
        super(filePath, charset, layout, dummy);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader#putContent(java.lang.String, java.util.Map, java.util.Map)
     */
    @Override
    protected void putContent(final String itemId, final Map<String, ?> fromMap, final Map<String, Object> toMap) {
        // 階層はないので、直接コピー
        toMap.put(itemId, fromMap.get(itemId));
    }

}
