package me.suwash.tools.comparefiles.sv.da.file.writer.impl;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.da.file.writer.ResultWriter;
import me.suwash.util.FileUtils;

import org.apache.commons.lang3.StringUtils;

/**
 * 比較結果Writerの基底クラス。
 *
 * @param <R> 比較結果
 */
public abstract class BaseResultWriter<R> extends FileWriter implements ResultWriter<R> {

    protected String filePath;
    protected Closeable writer;
    protected long outRowNum;
    protected List<R> chunk = new ArrayList<R>();

    /**
     * コンストラクタ。
     *
     * @param filePath ファイルパス
     * @param charset 文字コード
     * @throws IOException ファイルにアクセスできない場合
     */
    protected BaseResultWriter(final String filePath, final String charset) throws IOException  {
        super(filePath);

        // 出力ファイルパス
        if (StringUtils.isEmpty(filePath)) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"filePath"});
        }
        this.filePath = filePath;

        // ファイル出力チェック
        FileUtils.writeCheck(filePath, charset);

        // 出力準備
        FileUtils.setupOverwrite(filePath);

        // writer
        final File file = new File(filePath);
        this.writer = getWriter(file, charset);
    }

    /**
     * Writerを返します。
     *
     * @param file ファイル
     * @param charset 文字コード
     * @return writer
     */
    protected abstract Closeable getWriter(File file, String charset);

    /*
     * (非 Javadoc)
     * @see tool.comparefiles.dataaccess.ResultWriter#write(RESULT)
     */
    @Override
    public void write(final R row) {
        writeRow(row);
    }

    /**
     * 1行をファイルに出力します。
     *
     * @param row 行単位の比較結果
     */
    protected abstract void writeRow(R row);

    /*
     * (非 Javadoc)
     * @see tool.comparefiles.dataaccess.ResultWriter#flush()
     */
    @Override
    public void flush() {
        if (chunk != null && !chunk.isEmpty()) {
            for (final R curRow : chunk) {
                writeRow(curRow);
            }
            chunk.clear();
        }
    }

    /*
     * (非 Javadoc)
     * @see tool.comparefiles.dataaccess.ResultWriter#close()
     */
    @Override
    public void close() {
        flush();
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                throw new CompareFilesException(Const.STREAM_CANTCLOSE_OUTPUT, new Object[] {filePath}, e);
            }
        }
    }

}
