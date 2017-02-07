package me.suwash.tools.comparefiles.sv.da.file.repository.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.LineSp;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.da.file.writer.impl.TextFileRowCompareResultWriter;
import me.suwash.tools.comparefiles.sv.domain.compare.file.text.TextFileRowCompareResult;
import me.suwash.tools.comparefiles.sv.domain.compare.file.text.TextFileRowCompareResultRepository;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * テキストファイル行比較結果リポジトリ。
 */
public class TextFileRowCompareResultRepositoryImpl extends GenericFileRepository<TextFileRowCompareResult> implements TextFileRowCompareResultRepository {

    private boolean isWriteDiffOnly;
    private String prefixLeft = Const.DEFAULT_PREFIX_LEFT;
    private String prefixRight = Const.DEFAULT_PREFIX_RIGHT;

    /**
     * コンストラクタ。
     *
     * @param filePath ファイルパス
     * @param charset 文字コード
     * @param lineSp 改行コード
     * @param chunkSize 出力バッファ行数
     * @param isWriteDiffOnly 差分のみ出力するか
     * @param prefixLeft 左差分プリフィックス
     * @param prefixRight 右差分プリフィックス
     */
    public TextFileRowCompareResultRepositoryImpl(
        final String filePath,
        final String charset,
        final LineSp lineSp,
        final int chunkSize,
        final boolean isWriteDiffOnly,
        final String prefixLeft,
        final String prefixRight) {

        super();
        setFields(filePath, charset, lineSp, chunkSize, isWriteDiffOnly, prefixLeft, prefixRight);
    }

    /**
     * フィールドに初期値を設定します。
     *
     * @param filePath ファイルパス
     * @param charset 文字コード
     * @param lineSp 改行コード
     * @param chunkSize 出力バッファ行数
     * @param isWriteDiffOnly 差分のみ出力するか
     * @param prefixLeft 左差分プリフィックス
     * @param prefixRight 右差分プリフィックス
     */
    public void setFields(
        final String filePath,
        final String charset,
        final LineSp lineSp,
        final int chunkSize,
        final boolean isWriteDiffOnly,
        final String prefixLeft,
        final String prefixRight) {

        this.filePath = filePath;
        this.txFilePath = filePath + '.' + RandomStringUtils.randomAlphanumeric(10);
        this.charset = charset;
        this.lineSp = lineSp;
        this.chunkSize = chunkSize;
        this.isWriteDiffOnly = isWriteDiffOnly;
        this.prefixLeft = prefixLeft;
        this.prefixRight = prefixRight;
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
            return new TextFileRowCompareResultWriter(
                txFilePath, charset, isWriteDiffOnly, prefixLeft, prefixRight);
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
    public TextFileRowCompareResult next() {
        throw new CompareFilesException(Const.UNSUPPORTED);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.repository.impl.GenericFileRepository#writeRow(java.lang.Object)
     */
    @Override
    protected void writeRow(final TextFileRowCompareResult row) {
        ((TextFileRowCompareResultWriter) writer).write(row);
    }

}
