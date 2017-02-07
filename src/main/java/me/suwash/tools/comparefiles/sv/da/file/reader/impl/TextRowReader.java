package me.suwash.tools.comparefiles.sv.da.file.reader.impl;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.FileFormat;
import me.suwash.tools.comparefiles.infra.classification.RecordPattern;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.config.RecordLayout;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.domain.BaseRow;

/**
 * text形式のファイルを、行データに変換するReader。
 *
 * @param <R> 行データ
 */
public class TextRowReader<R extends BaseRow> extends BaseFlatRowReader<R> {

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
    public TextRowReader(final String filePath, final String charset, final FileLayout layout, final R... dummy) throws FileNotFoundException {
        super(filePath, charset, layout, dummy);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader#getReader(java.io.File, java.lang.String)
     */
    @Override
    protected Closeable getReader(final File file, final String charset) {
        Closeable reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
        } catch (Exception e) {
            throw new CompareFilesException(Const.STREAM_CANTOPEN_INPUT, new Object[] {file.getPath()}, e);
        }
        return reader;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader#readLine()
     */
    @Override
    protected Map<String, Object> readLine() {
        String targetLine = null;
        try {
            curRowNum++;
            targetLine = ((BufferedReader) reader).readLine();
            if (targetLine == null) {
                return null;
            }
        } catch (IOException e) {
            throw new CompareFilesException(
                Const.FILE_CANTREAD,
                new Object[] {filePath + "#" + curRowNum},
                e);
        }

        final Map<String, Object> returnMap = new LinkedHashMap<String, Object>();
        returnMap.put(Const.DEFAULT_ITEM_ID, targetLine);
        returnMap.put(KEY_RAWLINE, targetLine);
        return returnMap;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader#isMatchRecordType(me.suwash.tools.comparefiles.infra.config.RecordLayout, java.util.Map)
     */
    @Override
    protected boolean isMatchRecordType(final RecordLayout curRecordConfig, final Map<String, ?> targetLineMap) {
        throw new CompareFilesException(Const.MSGCD_ERROR_READER_UNSUPPORTED_RECORDPATTERN, new Object[] {FileFormat.Text.ddId(), RecordPattern.DataOnly.ddId()});
    }

}
