package me.suwash.tools.comparefiles.sv.da.file.reader.impl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.FileFormat;
import me.suwash.tools.comparefiles.infra.classification.RecordPattern;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.config.RecordLayout;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.domain.BaseRow;
import me.suwash.util.JsonUtils;

/**
 * Json形式のファイルを、行データに変換するReader。
 * 1ファイルで、1Jsonオブジェクトの形式。
 *
 * @param <R> 行データ
 */
public class JsonRowReader<R extends BaseRow> extends JsonListRowReader<R> {

    /**
     * コンストラクタ。
     *
     * @param filePath ファイルパス
     * @param charset 文字コード
     * @param layout ファイルレイアウト
     * @param dummy 型引数ダミー値
     * @throws FileNotFoundException ファイルが存在しない場合
     */
    @SafeVarargs
    public JsonRowReader(final String filePath, final String charset, final FileLayout layout, final R... dummy) throws FileNotFoundException {
        super(filePath, charset, layout, dummy);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.JsonListRowReader#readLine()
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<String, Object> readLine() {
        String content = null;
        try {
            // 1行として処理
            curRowNum++;

            // 全行を読み込んで返す
            final BufferedReader bufferedReader = (BufferedReader) reader;
            final StringBuilder contentBuilder = new StringBuilder();
            String curLine = null;
            curLine = bufferedReader.readLine();
            while (curLine != null) {
                contentBuilder.append(curLine);
                curLine = bufferedReader.readLine();
            }
            content = contentBuilder.toString();

        } catch (IOException e) {
            throw new CompareFilesException(
                Const.FILE_CANTREAD,
                new Object[] {filePath + "#" + curRowNum},
                e);
        }

        final Map<String, Object> returnMap = JsonUtils.parseString(content, Map.class);
        if (returnMap != null) {
            returnMap.put(KEY_RAWLINE, content);
        }
        return returnMap;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.JsonListRowReader#isMatchRecordType(me.suwash.tools.comparefiles.infra.config.RecordLayout, java.util.Map)
     */
    @Override
    protected boolean isMatchRecordType(final RecordLayout curRecordConfig, final Map<String, ?> targetLineMap) {
        throw new CompareFilesException(Const.MSGCD_ERROR_READER_UNSUPPORTED_RECORDPATTERN, new Object[] {FileFormat.Json.ddId(), RecordPattern.DataOnly.ddId()});
    }

}
