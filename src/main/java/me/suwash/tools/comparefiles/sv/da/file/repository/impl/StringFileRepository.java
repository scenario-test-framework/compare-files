package me.suwash.tools.comparefiles.sv.da.file.repository.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.LineSp;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * テキストファイルリポジトリ。
 */
public class StringFileRepository extends GenericFileRepository<String> {

    /**
     * コンストラクタ。
     *
     * @param file ファイル
     * @param charset 文字コード
     * @param lineSp 改行コード
     * @param chunkSize 書き出しバッファ行数
     */
    public StringFileRepository(
        final File file,
        final String charset,
        final LineSp lineSp,
        final int chunkSize) {

        super();
        if (file == null) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {
                "file"
            });
        }

        setFields(file.getAbsolutePath(), charset, lineSp, chunkSize);
    }

    /**
     * コンストラクタ。
     *
     * @param filePath ファイルパス
     * @param charset 文字コード
     * @param lineSp 改行コード
     * @param chunkSize 書き出しバッファ行数
     */
    public StringFileRepository(
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
        try {
            return new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), charset));
        } catch (Exception e) {
            throw new CompareFilesException(Const.STREAM_CANTOPEN_INPUT, new Object[] {filePath}, e);
        }
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
            throw new CompareFilesException(Const.STREAM_CANTOPEN_OUTPUT, new Object[] {txFilePath}, e);
        }
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.infra.policy.FileRepository#next()
     */
    @Override
    public String next() {
        try {
            return ((BufferedReader) reader).readLine();
        } catch (IOException e) {
            throw new CompareFilesException(Const.FILE_CANTREAD, new Object[] {filePath}, e);
        }
    }

}
