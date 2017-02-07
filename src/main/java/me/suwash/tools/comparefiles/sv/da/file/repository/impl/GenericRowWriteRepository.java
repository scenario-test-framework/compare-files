package me.suwash.tools.comparefiles.sv.da.file.repository.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.LineSp;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.domain.BaseRow;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * 行データ書き出しリポジトリ。
 *
 * @param <R> 行データ
 */
public class GenericRowWriteRepository<R extends BaseRow> extends GenericFileRepository<R> {

    /**
     * コンストラクタ。
     *
     * @param file 対象ファイル
     * @param charset 文字コード
     * @param lineSp 改行コード
     * @param chunkSize 書き出しバッファ行数
     */
    protected GenericRowWriteRepository(
        final File file,
        final String charset,
        final LineSp lineSp,
        final int chunkSize) {

        super();
        if (file == null) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"file"});
        }
        setFields(file.getAbsolutePath(), charset, lineSp, chunkSize);
    }

    /**
     * フィールドに初期値を設定します。
     *
     * @param filePath 対象ファイルパス
     * @param charset 文字コード
     * @param lineSp 改行コード
     * @param chunkSize 書き出しバッファ行数
     */
    private void setFields(
        final String filePath,
        final String charset,
        final LineSp lineSp,
        final int chunkSize) {

        this.filePath = filePath;
        this.txFilePath = filePath + '.' + RandomStringUtils.randomAlphanumeric(10);
        this.charset = charset;
        this.lineSp = lineSp;
        this.chunkSize = chunkSize;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.repository.impl.GenericFileRepository#getReader()
     */
    @Override
    protected Reader getReader() {
        return null;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.repository.impl.GenericFileRepository#getWriter()
     */
    @Override
    protected Writer getWriter() {
        try {
            return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(txFilePath)), charset));
        } catch (Exception e) {
            throw new CompareFilesException(Const.STREAM_CANTOPEN_OUTPUT, new Object[] {
                txFilePath
            }, e);
        }
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.infra.policy.FileRepository#next()
     */
    @Override
    public R next() {
        throw new CompareFilesException(Const.UNSUPPORTED);
    }

}
