package me.suwash.tools.comparefiles.sv.da.file.reader.impl;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.LineSp;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.config.ItemLayout;
import me.suwash.tools.comparefiles.infra.config.RecordLayout;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.domain.BaseRow;

/**
 * Fixed形式のファイルを、行データに変換するReader。
 * 固定長テキスト形式。レイアウト定義が必須です。
 *
 * @param <R> 行データ
 */
@lombok.extern.slf4j.Slf4j
public class FixedRowReader<R extends BaseRow> extends BaseFlatRowReader<R> {

    private String charset;
    private final String codeValueForOnlyOneRecordType;

    /**
     * コンストラクタ。
     *
     * @param filePath ファイルパス
     * @param charset 文字コード
     * @param layout ファイルレイアウト
     * @param codeValueForOnlyOneRecordType レコードタイプが1種類の場合の判定コード
     * @param dummy 型引数ダミー値
     * @throws FileNotFoundException ファイルが存在しない場合
     */
    @SafeVarargs
    public FixedRowReader(final String filePath, final String charset, final FileLayout layout, final String codeValueForOnlyOneRecordType, final R... dummy) throws FileNotFoundException {
        super(filePath, charset, layout, dummy);
        if (layout == null) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"layout"});
        } else if (layout.getRecordList().size() == 0) {
            throw new CompareFilesException(Const.CHECK_NOTNULL, new Object[] {"layout.recordList"});
        }

        // レコードタイプが１つのみの場合のレコードタイプ判別値
        this.codeValueForOnlyOneRecordType = codeValueForOnlyOneRecordType;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader#getReader(java.io.File, java.lang.String)
     */
    @Override
    protected Closeable getReader(final File file, final String charset) {
        Closeable reader = null;
        try {
            reader = new BufferedInputStream(new FileInputStream(file));
            this.charset = charset;
        } catch (Exception e) {
            throw new CompareFilesException(
                Const.STREAM_CANTOPEN_INPUT,
                new Object[] {file.getPath()},
                e);
        }
        return reader;
    }

    /*
     * (非 Javadoc)
     * @see tool.comparefiles.dataaccess.BaseRowReader#next()
     */
    @Override
    public R next() {
        final BufferedInputStream stream = (BufferedInputStream) reader;

        // 1件目のレコードタイプから、レコードバイト長を取得
        final int recordByteLength = fileLayout.getFirstRecordByteLength();

        // レコードバイト長分読み込み
        final byte[] lineBytes = new byte[recordByteLength];
        int readLength;
        try {
            readLength = stream.read(lineBytes);
        } catch (IOException e) {
            throw new CompareFilesException(
                Const.FILE_CANTREAD,
                new Object[] {filePath + "#" + curRowNum},
                e);
        }

        // EOF判定
        if (readLength <= 0) {
            // EOFの場合、nullを返却
            return null;

        } else {
            // EOF以外の場合
            // レコードタイプに合わせてbyteで分割
            final RecordLayout recordLayout = getRecordLayout(lineBytes);
            final Map<String, Object> recordMap = getRecordMap(lineBytes, recordLayout);
            final String rawLine = getRawLine(lineBytes, recordLayout);

            // 改行コード分読み進める
            skipLineSp(stream);
            // 読み込み行番号をインクリメント
            curRowNum++;

            // 行データに変換
            return parse(recordMap, rawLine);
        }
    }

    /**
     * 入力ストリームから改行コード分読み捨てます。
     *
     * @param stream 入力ストリーム
     */
    private void skipLineSp(final BufferedInputStream stream) {
        try {
            int readed = Integer.MIN_VALUE;
            if (LineSp.CR.equals(fileLayout.getLineSp()) || LineSp.LF.equals(fileLayout.getLineSp())) {
                readed = stream.read(new byte[1]);
            } else if (LineSp.CRLF.equals(fileLayout.getLineSp())) {
                readed = stream.read(new byte[2]);
            }
            if (log.isTraceEnabled()) {
                log.trace("レイアウトの改行コード:" + fileLayout.getLineSp() + ", 読み捨てた改行コード:" + readed);
            }
        } catch (IOException e) {
            throw new CompareFilesException(
                Const.MSGCD_ERROR_READER_FIXED_CANT_SKIP_LINESP,
                new Object[] {fileLayout.getLineSp()},
                e);
        }
    }

    /**
     * レコードレイアウトを返します。
     *
     * @param lineBytes バイト配列の行データ
     * @return レコードレイアウト
     */
    private RecordLayout getRecordLayout(final byte[] lineBytes) {
        RecordLayout recordLayout = null;
        for (final RecordLayout curRecordLayout : fileLayout.getRecordList()) {
            // 現在行から、レコードタイプコードと同じ文字数分、先頭から文字列変換
            final String recordTypeCode = getRecordTypeCode(lineBytes, curRecordLayout);

            // レイアウト定義と現在行の、レコードタイプコードの一致を確認
            if (recordTypeCode.equals(curRecordLayout.getCodeValue())) {
                // System.out.println(this.filePath + ", 行番号:" + (curRowNum +1) + ", レコードタイプコード:" + recordTypeCode + ", レコードタイプ:" + curRecordLayout.getType());
                recordLayout = curRecordLayout;
                break;
            }
        }
        // 判別できず、レコードタイプが1種類だけ登録されていてコード値が存在しない("-")場合、それを利用
        if (recordLayout == null && fileLayout.getRecordList().size() == 1 && fileLayout.getRecordList().get(0).getCodeValue().equals(codeValueForOnlyOneRecordType)) {
            recordLayout = fileLayout.getRecordList().get(0);
        }
        if (recordLayout == null) {
            throw new CompareFilesException(
                Const.MSGCD_ERROR_COMPARE_FILE_TEXT_ROW_LAYOUT_NOTFOUND,
                new Object[] {fileLayout.getLogicalFileName(), "#" + (curRowNum + 1) });
        }
        return recordLayout;
    }

    /**
     * レコードタイプ判定コードを返します。
     *
     * @param lineBytes バイト配列の行データ
     * @param curRecordLayout レコードレイアウト
     * @return 判定コード
     */
    private String getRecordTypeCode(final byte[] lineBytes, final RecordLayout curRecordLayout) {
        final int curCodeValueLength = curRecordLayout.getCodeValue().length();
        final StringBuilder recordTypeCodeBuilder = new StringBuilder();
        for (int i = 0; i < curCodeValueLength; i++) {
            recordTypeCodeBuilder.append(parseBytes2String(lineBytes, i, i + 1));
        }
        return recordTypeCodeBuilder.toString();
    }

    /**
     * バイト配列の行データを項目をキーにしたMapに変換します。
     *
     * @param lineBytes バイト配列の行データ
     * @param recordLayout レコードレイアウト
     * @return 行データMap
     */
    private Map<String, Object> getRecordMap(final byte[] lineBytes, final RecordLayout recordLayout) {
        final Map<String, Object> recordMap = new LinkedHashMap<String, Object>();
        int readedPos = 0;
        for (final ItemLayout curItemLayout : recordLayout.getItemList()) {
            final int endPos = readedPos + curItemLayout.getByteLength();
            recordMap.put(curItemLayout.getId(), parseBytes2String(lineBytes, readedPos, endPos));
            readedPos = endPos;
        }
        return recordMap;
    }

    /**
     * バイト配列の行データを文字列に変換します。
     *
     * @param lineBytes バイト配列の行データ
     * @param recordLayout レコードレイアウト
     * @return 行文字列
     */
    private String getRawLine(final byte[] lineBytes, final RecordLayout recordLayout) {
        final StringBuilder rawLineBuilder = new StringBuilder();
        int readedPos = 0;
        for (final ItemLayout curItemLayout : recordLayout.getItemList()) {
            final int endPos = readedPos + curItemLayout.getByteLength();
            rawLineBuilder.append(parseBytes2String(lineBytes, readedPos, endPos));
            readedPos = endPos;
        }
        return rawLineBuilder.toString();
    }

    /**
     * バイト配列の指定インデックス間を文字列に変換します。
     *
     * @param lineBytes バイト配列
     * @param startPos 変換開始インデックス
     * @param endPos 変換終了インデックス
     * @return 変換後文字列
     */
    private String parseBytes2String(final byte[] lineBytes, final int startPos, final int endPos) {
        final byte[] targetBytes = Arrays.copyOfRange(lineBytes, startPos, endPos);
        try {
            return new String(targetBytes, charset);
        } catch (UnsupportedEncodingException e) {
            throw new CompareFilesException(
                Const.MSGCD_ERROR_READER_FIXED_CANT_PARSE_BYTES,
                new Object[] {charset, Arrays.toString(targetBytes)},
                e);
        }
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader#readLine()
     */
    @Override
    protected Map<String, ?> readLine() {
        throw new CompareFilesException(Const.UNSUPPORTED);
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader#isMatchRecordType(me.suwash.tools.comparefiles.infra.config.RecordLayout, java.util.Map)
     */
    @Override
    protected boolean isMatchRecordType(final RecordLayout curRecordConfig, final Map<String, ?> targetLineMap) {
        // 1項目目の値を取得
        final String firstValue = targetLineMap.entrySet().iterator().next().getValue().toString();

        // 1項目目から、レコードタイプコードと同じ文字数分抽出
        final String recordTypeCode = firstValue.substring(0, curRecordConfig.getCodeValue().length());
        return recordTypeCode.equals(curRecordConfig.getCodeValue());
    }

}
