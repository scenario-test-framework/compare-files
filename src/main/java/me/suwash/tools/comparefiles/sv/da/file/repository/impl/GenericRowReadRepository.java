package me.suwash.tools.comparefiles.sv.da.file.repository.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.FileFormat;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.config.FileLayoutManager;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.da.file.reader.RowReader;
import me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader;
import me.suwash.tools.comparefiles.sv.da.file.reader.impl.CsvRowReader;
import me.suwash.tools.comparefiles.sv.da.file.reader.impl.FixedRowReader;
import me.suwash.tools.comparefiles.sv.da.file.reader.impl.JsonListRowReader;
import me.suwash.tools.comparefiles.sv.da.file.reader.impl.JsonRowReader;
import me.suwash.tools.comparefiles.sv.da.file.reader.impl.TextRowReader;
import me.suwash.tools.comparefiles.sv.da.file.reader.impl.TsvRowReader;
import me.suwash.tools.comparefiles.sv.domain.BaseRow;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 行データ読み込みリポジトリ。
 *
 * @param <R> 行データ
 */
public class GenericRowReadRepository<R extends BaseRow> extends GenericFileRepository<R> {

    protected FileLayout fileLayout;
    protected int csvHeaderRow;
    protected int csvDataStartRow;
    protected String codeValueForOnlyOneRecordType;
    protected BaseRowReader<R> rowReader;

    /**
     * コンストラクタ。
     *
     * @param filePath 対象ファイルパス
     * @param overrideCharset ファイルレイアウト定義を上書きする文字コード
     * @param fileLayout ファイルレイアウト
     * @param csvHeaderRow CSV/TSVヘッダー行番号
     * @param csvDataStartRow CSV/TSVデータ開始行番号
     * @param codeValueForOnlyOneRecordType レコードタイプが1種類の場合の判定コード
     * @param dummy 行データ型引数ダミー値 ※実行時に指定された型の空配列を、BaseRowReaderまで伝播させてリフレクションでインスタンス化しています。
     */
    @SafeVarargs
    public GenericRowReadRepository(
        final String filePath,
        final String overrideCharset,
        final FileLayout fileLayout,
        final int csvHeaderRow,
        final int csvDataStartRow,
        final String codeValueForOnlyOneRecordType,
        final R... dummy
    ) {
        super();

        setFields(filePath, overrideCharset, fileLayout, csvHeaderRow, csvDataStartRow, codeValueForOnlyOneRecordType, dummy);
    }

    /**
     * フィールドに初期値を設定します。
     *
     * @param filePath 対象ファイルパス
     * @param overrideCharset ファイルレイアウト定義を上書きする文字コード
     * @param fileLayout ファイルレイアウト
     * @param csvHeaderRow CSV/TSVヘッダー行番号
     * @param csvDataStartRow CSV/TSVデータ開始行番号
     * @param codeValueForOnlyOneRecordType レコードタイプが1種類の場合の判定コード
     * @param dummy 行データ型引数ダミー値 ※実行時に指定された型の空配列を、BaseRowReaderまで伝播させてリフレクションでインスタンス化しています。
     */
    @SuppressWarnings("unchecked")
    private void setFields(
        final String filePath,
        final String overrideCharset,
        final FileLayout fileLayout,
        final int csvHeaderRow,
        final int csvDataStartRow,
        final String codeValueForOnlyOneRecordType,
        final R... dummy
    ) {

        // ファイルレイアウトが指定されていない場合、デフォルト値を設定。
        FileLayout decidedLayout = fileLayout;
        if (decidedLayout == null) {
            decidedLayout = FileLayoutManager.getDefaultTextLayout();
        }

        this.filePath = filePath;
        this.txFilePath = filePath + '.' + RandomStringUtils.randomAlphanumeric(10);

        if (StringUtils.isEmpty(overrideCharset)) {
            this.charset = decidedLayout.getCharset();
        } else {
            this.charset = overrideCharset;
        }

        this.lineSp = decidedLayout.getLineSp();

        this.fileLayout = decidedLayout;
        this.csvHeaderRow = csvHeaderRow;
        this.csvDataStartRow = csvDataStartRow;
        this.codeValueForOnlyOneRecordType = codeValueForOnlyOneRecordType;
        this.rowReader = getReader(dummy);
    }

    /**
     * ファイルレイアウト.ファイルフォーマットにマッチする、行データリーダーを返します。
     *
     * @param dummy 行データ型引数ダミー値 ※実行時に指定された型の空配列を、BaseRowReaderまで伝播させてリフレクションでインスタンス化しています。
     * @return 行データリーダー
     */
    @SuppressWarnings("unchecked")
    private BaseRowReader<R> getReader(final R... dummy) {
        final FileFormat fileFormat = fileLayout.getFileFormat();
        try {
            if (FileFormat.CSV_noHeader.equals(fileFormat) || FileFormat.CSV_withHeader.equals(fileFormat)) {
                return new CsvRowReader<R>(filePath, charset, fileLayout, csvHeaderRow, csvDataStartRow, dummy);
            } else if (FileFormat.TSV_noHeader.equals(fileFormat) || FileFormat.TSV_withHeader.equals(fileFormat)) {
                return new TsvRowReader<R>(filePath, charset, fileLayout, csvHeaderRow, csvDataStartRow, dummy);
            } else if (FileFormat.Json.equals(fileFormat)) {
                return new JsonRowReader<R>(filePath, charset, fileLayout, dummy);
            } else if (FileFormat.JsonList.equals(fileFormat)) {
                return new JsonListRowReader<R>(filePath, charset, fileLayout, dummy);
            } else if (FileFormat.Fixed.equals(fileFormat)) {
                return new FixedRowReader<R>(filePath, charset, fileLayout, codeValueForOnlyOneRecordType, dummy);
            } else {
                return new TextRowReader<R>(filePath, charset, fileLayout, dummy);
            }
        } catch (IOException e) {
            throw new CompareFilesException(Const.STREAM_CANTOPEN_INPUT, new Object[] {filePath}, e);
        }
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.repository.impl.GenericFileRepository#getReader()
     */
    @Override
    protected Reader getReader() {
        return this.rowReader;
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
    @SuppressWarnings("unchecked")
    @Override
    public R next() {
        return ((RowReader<R>) reader).next();
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.repository.impl.GenericFileRepository#write(java.lang.Object)
     */
    @Override
    public void write(final R row) {
        throw new CompareFilesException(Const.UNSUPPORTED);
    }

}
