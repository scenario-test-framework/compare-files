package me.suwash.tools.comparefiles.sv.da.file.repository.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.LineSp;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.da.file.writer.impl.FileCompareResultWriter;
import me.suwash.tools.comparefiles.sv.domain.compare.bulk.FileCompareResultRepository;
import me.suwash.tools.comparefiles.sv.domain.compare.file.FileCompareResult;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * ファイル比較結果リポジトリ。
 */
public class FileCompareResultRepositoryImpl extends GenericFileRepository<FileCompareResult> implements FileCompareResultRepository {

    /**
     * コンストラクタ。
     *
     * @param filePath ファイルパス
     * @param charset 文字コード
     * @param lineSp 改行コード
     * @param chunkSize 出力バッファ行数
     */
    public FileCompareResultRepositoryImpl(
        final String filePath,
        final String charset,
        final LineSp lineSp,
        final int chunkSize) {
        super();

        setFields(filePath, charset, lineSp, chunkSize);
    }

    /**
     * フィールドに初期値を設定します。
     *
     * @param filePath ファイルパス
     * @param charset 文字コード
     * @param lineSp 改行コード
     * @param chunkSize 出力バッファ行数
     */
    public void setFields(
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
            return new FileCompareResultWriter(txFilePath, charset);
        } catch (IOException e) {
            throw new CompareFilesException(Const.FILE_CANTWRITE, new Object[] {
                txFilePath
            }, e);
        }
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.repository.FileRepository#next()
     */
    @Override
    public FileCompareResult next() {
        throw new CompareFilesException(Const.UNSUPPORTED);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.repository.impl.GenericFileRepository#writeRow(java.lang.Object)
     */
    @Override
    protected void writeRow(final FileCompareResult row) {
        ((FileCompareResultWriter) writer).write(row);
    }

}
