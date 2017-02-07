package me.suwash.tools.comparefiles.sv.da.file.reader.impl;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.FileFormat;
import me.suwash.tools.comparefiles.infra.classification.RecordPattern;
import me.suwash.tools.comparefiles.infra.classification.RecordType;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.tools.comparefiles.infra.config.RecordLayout;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.tools.comparefiles.sv.domain.BaseRow;
import me.suwash.util.CsvUtils;

import org.apache.commons.lang3.StringUtils;

import com.orangesignal.csv.CsvConfig;
import com.orangesignal.csv.CsvReader;
import com.orangesignal.csv.io.CsvColumnNameMapReader;

/**
 * CSV形式のファイルを、行データに変換するReader。
 * <pre>
 * ヘッダーあり、なしどちらにも対応。
 * ・ヘッダーありの場合
 * 　　項目はレイアウト定義か、ヘッダー行番号で指定できます。
 * ・ヘッダーなしの場合
 * 　　項目はレイアウト定義で指定できます。
 * 　　レイアウト定義がない場合は、行番号でマッピングします。
 * </pre>
 *
 * @param <R> 行データ
 */
public class CsvRowReader<R extends BaseRow> extends BaseFlatRowReader<R> {

    protected RecordPattern recordPattern;
    protected int csvHeaderRow;
    protected int csvDataStartRow;

    protected Map<RecordType, CsvColumnNameMapReader> mapperMap = new HashMap<RecordType, CsvColumnNameMapReader>();

    /**
     * コンストラクタ。
     *
     * @param filePath ファイルパス
     * @param charset 文字コード
     * @param layout ファイルレイアウト
     * @param csvHeaderRow ヘッダー行番号
     * @param csvDataStartRow データ開始行番号
     * @param dummy 型引数ダミー値
     * @throws FileNotFoundException ファイルが存在しない場合
     */
    @SafeVarargs
    public CsvRowReader(final String filePath, final String charset, final FileLayout layout, final int csvHeaderRow, final int csvDataStartRow, final R... dummy) throws FileNotFoundException {
        super(filePath, charset, layout, dummy);

        // --------------------------------------------------------------------------------
        // レコードパターンの判定
        // --------------------------------------------------------------------------------
        if (layout == null) {
            recordPattern = RecordPattern.None;
        } else {
            recordPattern = layout.getRecordPattern();
        }

        // --------------------------------------------------------------------------------
        // ヘッダー行、データ開始行の設定
        // --------------------------------------------------------------------------------
        this.csvHeaderRow = csvHeaderRow;
        this.csvDataStartRow = csvDataStartRow;
        if (this.csvDataStartRow <= 0) {
            // データ開始行が未設定の場合は、1行目に初期化
            this.csvDataStartRow = 1;
        }

        // --------------------------------------------------------------------------------
        // CSVヘッダーの設定
        // --------------------------------------------------------------------------------
        if (layout == null) {
            setCsvHeaderByHeaderRow();
        } else {
            setCsvHeaderByLayout();
        }
    }

    /**
     * ファイルレイアウトを元にヘッダーを設定します。
     */
    private void setCsvHeaderByLayout() {
        final CsvReader cReader = (CsvReader) reader;

        // --------------------------------------------------------------------------------
        // mapper登録
        // --------------------------------------------------------------------------------
        // 全てのレコードタイプをループ
        for (final RecordLayout curRecordLayout : fileLayout.getRecordList()) {
            // レコードタイプごとのレイアウトに合わせたReaderをmapperMapに登録
            putMapperMap(cReader, curRecordLayout);
        }

        // --------------------------------------------------------------------------------
        // データ開始の前行まで読み進める
        // --------------------------------------------------------------------------------
        if (FileFormat.CSV_withHeader.equals(fileLayout.getFileFormat()) ||
            FileFormat.TSV_withHeader.equals(fileLayout.getFileFormat())) {
            try {
                while (cReader.getLineNumber() < csvDataStartRow - 1) {
                    final List<String> curLine = cReader.readValues();
                    if (curLine == null || curLine.isEmpty()) {
                        break;
                    }
                }
            } catch (Exception e) {
                throw new CompareFilesException(
                    Const.MSGCD_ERROR_READER_CSV_CANT_SKIP_HEADER,
                    new Object[] {filePath, fileLayout},
                    e);
            }
        }
    }

    /**
     * レコードレイアウト毎にMapperを保持します。
     *
     * @param reader CsvReader
     * @param recordLayout レコードレイアウト
     */
    private void putMapperMap(final CsvReader reader, final RecordLayout recordLayout) {
        final CsvColumnNameMapReader curReader = new CsvColumnNameMapReader(reader, recordLayout.getCompareItemNameList());
        mapperMap.put(recordLayout.getType(), curReader);
    }

    /**
     * ヘッダー行番号の文字列から、ヘッダーを設定します。
     */
    private void setCsvHeaderByHeaderRow() {
        // --------------------------------------------------------------------------------
        // データ開始の前行まで読み進める
        // --------------------------------------------------------------------------------
        final CsvReader csvReader = (CsvReader) reader;
        List<String> headerNameList = null;
        try {
            // データ開始の前行まで読み進める
            while (csvReader.getLineNumber() < csvDataStartRow - 1) {
                final List<String> curLine = csvReader.readValues();
                if (curLine == null || curLine.isEmpty()) {
                    break;
                }

                // ヘッダー項目名リストを、ヘッダー行から取得 ※ヘッダー行が指定されていない場合は、読み込み時にカラム番号でマッピング
                if (csvReader.getLineNumber() == csvHeaderRow) {
                    headerNameList = curLine;
                }
            }
        } catch (Exception e) {
            throw new CompareFilesException(
                Const.MSGCD_ERROR_READER_CSV_CANT_SKIP_HEADER,
                new Object[] {filePath, fileLayout},
                e);
        }

        // mapperMapに登録
        final RecordType recordType = RecordType.Data;
        if (headerNameList == null) {
            mapperMap.put(recordType, null);
        } else {
            mapperMap.put(recordType, new CsvColumnNameMapReader(csvReader, headerNameList));
        }
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader#getReader(java.io.File, java.lang.String)
     */
    @Override
    protected Closeable getReader(final File file, final String charset) {
        CsvReader reader = null;
        try {
            final FileInputStream inStream = new FileInputStream(file);
            reader = new CsvReader(new BufferedReader(new InputStreamReader(inStream, charset)), getCsvConfig());
        } catch (Exception e) {
            throw new CompareFilesException(
                Const.STREAM_CANTOPEN_INPUT,
                new Object[] {file.getPath()},
                e);
        }
        return reader;
    }

    /**
     * CSV設定を返します。
     *
     * @return CSV設定
     */
    protected CsvConfig getCsvConfig() {
        return CsvUtils.getCsvConfig();
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader#readLine()
     */
    @Override
    protected Map<String, ?> readLine() {
        final CsvReader csvReader = (CsvReader) reader;

        List<String> contentList = null;
        try {
            contentList = csvReader.readValues();
            curRowNum = csvReader.getLineNumber();
            if (contentList == null ||
                contentList.size() == 1 && StringUtils.isEmpty(contentList.get(0))) {
                return null;
            }
        } catch (Exception e) {
            throw new CompareFilesException(
                Const.FILE_CANTREAD,
                new Object[] {filePath},
                e);
        }

        final StringBuilder rawLineBuilder = new StringBuilder();
        boolean isFirstElem = true;
        for (final String curContent : contentList) {
            if (isFirstElem) {
                rawLineBuilder.append("\"" + curContent + "\"");
                isFirstElem = false;
            } else {
                rawLineBuilder.append(getCsvConfig().getSeparator() + "\"" + curContent + "\"");
            }
        }

        // レコードタイプの判定
        RecordType recordType = null;
        if (RecordPattern.DataOnly.equals(recordPattern)) {
            // データのみの場合
            recordType = RecordType.Data;

        } else {
            if (fileLayout == null) {
                // ファイルレイアウトが存在しない場合
                recordType = RecordType.Data;

            } else {
                // ファイルレイアウトが存在する場合
                // 1項目目から、レコードタイプコードと同じ文字数分抽出
                final String firstValue = contentList.get(0);
                for (final RecordLayout curRecordLayout : fileLayout.getRecordList()) {
                    final String curCodeValue = curRecordLayout.getCodeValue();
                    final String recordTypeCode = firstValue.substring(0, curCodeValue.length());
                    if (recordTypeCode.equals(curCodeValue)) {
                        recordType = curRecordLayout.getType();
                        break;
                    }
                }
            }
        }

        // Mapに変換して返却
        return toMap(recordType, contentList, rawLineBuilder.toString());
    }

    /**
     * 行データリストをMapに変換します。
     *
     * @param recordType レコードタイプ
     * @param contentList 行データリスト
     * @param rawLine 行文字列
     * @return 変換後のMap
     */
    private Map<String, String> toMap(final RecordType recordType, final List<String> contentList, final String rawLine) {
        Map<String, String> returnMap = null;
        CsvColumnNameMapReader mapper = null;

        mapper = mapperMap.get(recordType);
        if (mapper == null) {
            // mapperが存在しない場合
            returnMap = new LinkedHashMap<String, String>();
            for (int colNum = 1; colNum <= contentList.size(); colNum++) {
                returnMap.put(String.valueOf(colNum), contentList.get(colNum - 1));
            }

        } else {
            // mapperが存在する場合
            try {
                returnMap = mapper.toMap(contentList);
            } catch (IOException e) {
                throw new CompareFilesException(
                    Const.MSGCD_ERROR_READER_CSV_CANT_PARSE_ROW,
                    new Object[] {contentList},
                    e);
            }
        }

        returnMap.put(KEY_RAWLINE, rawLine);
        return returnMap;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.tools.comparefiles.sv.da.file.reader.impl.BaseRowReader#isMatchRecordType(me.suwash.tools.comparefiles.infra.config.RecordLayout, java.util.Map)
     */
    @Override
    protected boolean isMatchRecordType(final RecordLayout curRecordConfig, final Map<String, ?> targetLineMap) {
        // データ or ヘッダー/データの場合
        if (RecordPattern.DataOnly.equals(recordPattern) || RecordPattern.HeaderData.equals(recordPattern)) {
            // データと一致するか、で判断
            return RecordType.Data.equals(curRecordConfig.getType());
        }

        // 1項目目の値を取得
        final String firstValue = targetLineMap.entrySet().iterator().next().getValue().toString();

        // 1項目目から、レコードタイプコードと同じ文字数分抽出
        final String recordTypeCode = firstValue.substring(0, curRecordConfig.getCodeValue().length());
        return recordTypeCode.equals(curRecordConfig.getCodeValue());
    }

}
