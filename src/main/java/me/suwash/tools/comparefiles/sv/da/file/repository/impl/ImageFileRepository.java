package me.suwash.tools.comparefiles.sv.da.file.repository.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.imageio.ImageIO;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 画像ファイルリポジトリ。
 */
public class ImageFileRepository extends GenericFileRepository<BufferedImage> {

    private String formatName;
    private boolean isReaded;
    private boolean isWrited;

    /**
     * コンストラクタ。
     *
     * @param file ファイル
     * @param formatName フォーマット名
     */
    public ImageFileRepository(
        final File file,
        final String formatName) {

        super();
        if (file == null) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"file"});
        }

        setFields(file.getAbsolutePath(), formatName);
    }

    /**
     * コンストラクタ。
     *
     * @param filePath ファイルパス
     * @param formatName フォーマット名
     */
    public ImageFileRepository(
        final String filePath,
        final String formatName) {

        super();
        setFields(filePath, formatName);
    }

    /**
     * フィールドに初期値を設定します。
     *
     * @param filePath ファイルパス
     * @param formatName フォーマット名
     */
    private void setFields(
        final String filePath,
        final String formatName) {

        if (StringUtils.isEmpty(formatName)) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"formatName"});
        }

        this.filePath = filePath;
        this.txFilePath = filePath + '.' + RandomStringUtils.randomAlphanumeric(10);
        this.formatName = formatName;

        // ダミー値
        this.charset = Const.CHARSET_DEFAULT_CONFIG;
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
        return null;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.infra.policy.FileRepository#next()
     */
    @Override
    public BufferedImage next() {
        BufferedImage image = null;
        if (! this.isReaded) {
            final File inputFile = new File(this.filePath);
            try {
                image = ImageIO.read(inputFile);
                this.isReaded = true;
            } catch (IOException e) {
                throw new CompareFilesException(Const.FILE_CANTREAD, new Object[] {filePath}, e);
            }
        }
        return image;
    }

    /* (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.repository.impl.GenericFileRepository#write(java.lang.Object)
     */
    @Override
    public void write(final BufferedImage image) {
        // トランザクションが開始していない場合、エラー
        if (!isBegin) {
            throw new CompareFilesException(Const.MSGCD_ERROR_REPOSITORY_TX_NOTEXIST, new Object[] {filePath});
        }

        if (! this.isWrited) {
            final File txFile = new File(this.txFilePath);
            try {
                ImageIO.write(image, formatName, txFile);
            } catch (IOException e) {
                throw new CompareFilesException(Const.FILE_CANTWRITE, new Object[] {txFilePath}, e);
            }
        }
    }

}
