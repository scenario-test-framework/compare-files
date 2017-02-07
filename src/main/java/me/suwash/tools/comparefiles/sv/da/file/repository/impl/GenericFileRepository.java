package me.suwash.tools.comparefiles.sv.da.file.repository.impl;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.Context;
import me.suwash.tools.comparefiles.infra.classification.LineSp;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.infra.i18n.CompareFilesMessageSource;
import me.suwash.tools.comparefiles.infra.policy.FileRepository;
import me.suwash.util.FileUtils;

/**
 * ファイルリポジトリ基底クラス。
 *<pre>
 * 新規ファイル：
 * 　読み込み：空ファイル。
 * 　書き出し：トランザクション中は、一時ファイル。
 * 　　　　　　トランザクション確定で、対象ファイルにリネーム。
 * 既存ファイル：
 * 　読み込み：既存ファイル。
 * 　書き出し：トランザクション中は、一時ファイル。
 * 　　　　　　トランザクション確定で、対象ファイルにリネーム。
 * 　　　　　　※compare-filesでは追記の用途がないため、上書きだけを実装しています。
 *</pre>
 *
 * @param <E> データモデル
 */
@lombok.extern.slf4j.Slf4j
public abstract class GenericFileRepository<E> implements FileRepository<E> {

    protected String filePath;
    protected String charset;
    protected LineSp lineSp;

    protected boolean isBegin;

    protected String txFilePath;

    protected Reader reader;
    protected Writer writer;

    protected int chunkSize;
    protected List<E> chunk = new ArrayList<E>();

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.infra.policy.Repository#begin()
     */
    @Override
    public void begin() {
        // すでにトランザクションを開始している場合、処理をスキップ
        if (isBegin) {
            return;
        } else {
            isBegin = true;
        }

        // ファイルの存在確認
        final File file = new File(filePath);
        if (!file.exists()) {
            // 存在しない場合、親ディレクトリ作成
            final File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                FileUtils.mkdirs(parentDir.getAbsolutePath());
            }
            // 空ファイル作成
            try {
                if (!file.createNewFile()) {
                    throw new CompareFilesException(Const.FILE_CANTWRITE, new Object[] {file});
                }
            } catch (IOException e) {
                throw new CompareFilesException(Const.FILE_CANTWRITE, new Object[] {file}, e);
            }
        }

        FileUtils.readCheck(filePath, charset);
        reader = getReader();

        FileUtils.writeCheck(txFilePath, charset);
        try {
            writer = getWriter();
        } catch (Exception e) {
            try {
                reader.close();
            } catch (Exception e1) {
                log.error(
                    CompareFilesMessageSource.getInstance().getMessage(
                        Const.STREAM_CANTCLOSE_INPUT,new Object[] {file}),e1);
            }
            throw e;
        }

        // コンテキストに追加
        Context.getInstance().addRepository(this.toString(), this);

        // chunkSizeの初期化
        if (chunkSize <= 0) {
            chunkSize = Const.DEFAULT_CHUNK_SIZE;
        }
    }

    /**
     * Readerを返します。
     *
     * @return Reader
     */
    protected abstract Reader getReader();

    /**
     * Writerを返します。
     *
     * @return Writer
     */
    protected abstract Writer getWriter();

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.infra.policy.Repository#commit()
     */
    @Override
    public void commit() {
        // トランザクションが開始していない場合、エラー
        if (!isBegin) {
            throw new CompareFilesException(Const.MSGCD_ERROR_REPOSITORY_TX_NOTEXIST, new Object[] {filePath});
        }

        // chunkを書き出し
        writeChunk();

        // ストリームを閉じる
        closeStream();

        // トランザクションファイルへの書き込みを確認
        final File file = new File(filePath);
        final File txFile = new File(txFilePath);
        if (txFile.length() > 0) {
            // 書き込みがある場合、対象ファイルを置き換え
            if (!file.delete()) {
                throw new CompareFilesException(Const.FILE_CANTDELETE, new Object[] {filePath});
            }
            if (!txFile.renameTo(file)) {
                throw new CompareFilesException(Const.FILE_CANTWRITE, new Object[] {filePath});
            }

        } else {
            // 書き込みがない場合、トランザクションファイルを削除
            if (txFile.exists() && !txFile.delete()) {
                throw new CompareFilesException(Const.FILE_CANTDELETE, new Object[] {txFilePath});
            }
        }

        // コンテキストから削除
        Context.getInstance().removeRepository(this.toString());
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.infra.policy.Repository#rollback()
     */
    @Override
    public void rollback() {
        // トランザクションが開始していない場合、処理なし
        if (!isBegin) {
            return;
        }

        // ストリームを閉じる
        closeStream();

        // トランザクションファイルを削除
        final File txFile = new File(txFilePath);
        if (!txFile.delete()) {
            throw new CompareFilesException(Const.FILE_CANTDELETE, new Object[] {txFilePath});
        }

        // コンテキストから削除
        Context.getInstance().removeRepository(this.toString());
    }

    /**
     * ストリームを閉じます。
     */
    private void closeStream() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                throw new CompareFilesException(Const.STREAM_CANTCLOSE_INPUT, new Object[] {filePath}, e);
            }
        }

        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                throw new CompareFilesException(Const.STREAM_CANTCLOSE_OUTPUT, new Object[] {txFilePath}, e);
            }
        }
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.infra.policy.FileRepository#write(java.lang.Object)
     */
    @Override
    public void write(final E row) {
        // トランザクションが開始していない場合、エラー
        if (!isBegin) {
            throw new CompareFilesException(Const.MSGCD_ERROR_REPOSITORY_TX_NOTEXIST, new Object[] {filePath});
        }
        chunk.add(row);
        if (chunk.size() >= chunkSize) {
            writeChunk();
        }
    }

    /**
     * 出力バッファの内容を全て書き出します。
     */
    protected void writeChunk() {
        for (final E row : chunk) {
            writeRow(row);
        }
        chunk.clear();
    }

    /**
     * 行データを書き出します。
     *
     * @param row 行データ
     */
    protected void writeRow(final E row) {
        try {
            // 文字列を出力
            writer.write(row.toString());

            // 改行コードを出力
            if (lineSp == null) {
                // 改行コードが設定されていない場合、システムに合わせる
                writer.write(System.getProperty("line.separator"));

            } else {
                // 設定されている場合、ファイルフォーマットに合わせて改行コードを追記 ※改行なしも考慮
                if (LineSp.CR.equals(lineSp)) {
                    writer.write('\r');
                } else if (LineSp.LF.equals(lineSp)) {
                    writer.write('\n');
                } else if (LineSp.CRLF.equals(lineSp)) {
                    writer.write("\r\n");
                }
            }

        } catch (IOException e) {
            throw new CompareFilesException(Const.FILE_CANTWRITE, new Object[] {txFilePath}, e);
        }
    }

    /*
     * (非 Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return filePath;
    }
}
