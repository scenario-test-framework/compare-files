package me.suwash.tools.comparefiles.sv.da.file.reader.impl;

import java.io.FileNotFoundException;

import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.sv.domain.BaseRow;
import me.suwash.util.CsvUtils;

import com.orangesignal.csv.CsvConfig;

/**
 * TSV形式のファイルを、行データに変換するReader。
 * <pre>
 * ヘッダーあり、なしどちらにも対応。
 * ・ヘッダーありの場合
 * 　　項目はレイアウト定義か、ヘッダー行番号で指定できます。
 * ・ヘッダーなしの場合
 * 　　項目はレイアウト定義で指定できます。
 * 　　レイアウト定義がない場合は、行番号でマッピングします。
 * </pre>
 *
 * @param <R> 行データ
 */
public class TsvRowReader<R extends BaseRow> extends CsvRowReader<R> {

    /**
     * コンストラクタ。
     *
     * @param filePath ファイルパス
     * @param charset 文字コード
     * @param layout ファイルレイアウト
     * @param csvHeaderRow ヘッダー行番号
     * @param csvDataStartRow データ開始行番号
     * @param dummy 型引数ダミー値
     * @throws FileNotFoundException ファイルが存在しない場合
     */
    @SafeVarargs
    public TsvRowReader(final String filePath, final String charset, final FileLayout layout, final int csvHeaderRow, final int csvDataStartRow, final R... dummy) throws FileNotFoundException {
        super(filePath, charset, layout, csvHeaderRow, csvDataStartRow, dummy);
    }

    /*
     * (非 Javadoc)
     * @see tool.comparefiles.dataaccess.CsvRowReader#getCsvConfig()
     */
    @Override
    protected CsvConfig getCsvConfig() {
        return CsvUtils.getTsvConfig();
    }

}
