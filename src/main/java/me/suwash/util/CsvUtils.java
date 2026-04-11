package me.suwash.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.suwash.util.constant.UtilMessageConst;
import me.suwash.util.exception.UtilException;

import com.orangesignal.csv.Csv;
import com.orangesignal.csv.CsvConfig;
import com.orangesignal.csv.CsvWriter;
import com.orangesignal.csv.handlers.StringArrayListHandler;
import com.orangesignal.csv.io.CsvColumnPositionMapWriter;

/**
 * CSV関連ユーティリティ。
 */
public final class CsvUtils {

    /**
     * ユーティリティ向けに、コンストラクタはprivate宣言。
     */
    private CsvUtils() {}

    /**
     * デフォルトのCSV設定を返します。
     *
     * @return CSV設定
     */
    public static CsvConfig getCsvConfig() {
        final CsvConfig csvConfig = new CsvConfig(',', '"', '"'); // Excel方式
        csvConfig.setQuoteDisabled(false); // 囲み文字：有効
        csvConfig.setEscapeDisabled(false); // エスケープ文字：有効
        return csvConfig;
    }

    /**
     * デフォルトのTSV設定を返します。
     *
     * @return TSV設定
     */
    public static CsvConfig getTsvConfig() {
        final CsvConfig tsvConfig = new CsvConfig('\t', '"', '\\');
        tsvConfig.setQuoteDisabled(false); // 囲み文字：有効
        tsvConfig.setEscapeDisabled(false); // エスケープ文字：有効
        return tsvConfig;
    }

    /**
     * CSV or TSVファイルを読み込み、文字列配列のリストとして返します。
     *
     * @param filePath 入力ファイルパス
     * @param charset 入力文字コード
     * @param config CSV or TSV設定
     * @return 文字列配列のリスト
     */
    public static List<String[]> parseFile(final String filePath, final String charset, final CsvConfig config) {
        // ファイル読み込み共通チェック
        FileUtils.readCheck(filePath, charset);

        // 戻り値
        List<String[]> inputDataList = null;

        // ファイル読み込み
        try {
            final File file = new File(filePath);
            inputDataList = Csv.load(file, charset, config, new StringArrayListHandler());
        } catch (IOException e) {
            throw new UtilException(UtilMessageConst.FILE_CANTREAD, new Object[] {
                filePath
            }, e);
        }
        return inputDataList;
    }

    /**
     * 出力列番号をキーにしたMapのリストをCSV or TSVファイルに出力します。
     *
     * @param dataList 出力列番号をキーにしたMapのリスト
     * @param dirPath 出力ディレクトリ
     * @param fileName 出力ファイル名
     * @param charset 出力文字コード
     * @param config CSV or TSV設定
     */
    public static void writeFile(
        final List<Map<Integer, String>> dataList,
        final String dirPath,
        final String fileName,
        final String charset,
        final CsvConfig config
    ) {
        // 出力ファイルパス
        final String filePath = dirPath + File.separator + fileName;

        // ファイル書き出し共通チェック
        FileUtils.writeCheck(filePath, charset);

        // ファイル上書き書き共通処理
        FileUtils.setupOverwrite(filePath);

        // ファイル出力
        try {
            final CsvColumnPositionMapWriter writer = new CsvColumnPositionMapWriter(
                new CsvWriter(
                    new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(filePath), charset)
                    ), config
                )
            );

            for (final Map<Integer, String> outputData : dataList) {
                writer.write(outputData);
            }

            writer.close();

        } catch (Exception e) {
            throw new UtilException(UtilMessageConst.FILE_CANTWRITE, new Object[] {
                filePath
            }, e);
        }
    }

    /**
     * CSV or TSVを読み込んだ文字列配列リストの行列を入れ替えて、Mapのリストに変換します。
     *
     * @param dataList CSV or TSVを読み込んだ文字列配列のリスト
     * @return 出力列をキーにしたMapのリスト
     */
    public static List<Map<Integer, String>> convertRowToCol(final List<String[]> dataList) {
        // 文字列配列のリスト
        if (dataList == null) {
            throw new UtilException(UtilMessageConst.CHECK_NOTNULL, new Object[] {
                "dataList"
            });
        }

        // 戻り値
        final List<Map<Integer, String>> outputDataList = new ArrayList<Map<Integer, String>>();

        // input行ループ
        final int inputDataSize = dataList.size();
        for (int curInputRow = 0; curInputRow < inputDataSize; curInputRow++) {
            final String[] curInputRowData = dataList.get(curInputRow);

            // 1行目の場合
            if (curInputRow == 0) {
                // input列分のoutput行を作成
                for (int curInputCol = 0; curInputCol < curInputRowData.length; curInputCol++) {
                    addEmptyMap(outputDataList);
                }
            }

            // input列ループ
            for (int curInputCol = 0; curInputCol < curInputRowData.length; curInputCol++) {
                // 行列を入れ替えてMapのListに登録
                outputDataList.get(curInputCol).put(curInputRow, curInputRowData[curInputCol]);
            }
        }
        return outputDataList;
    }

    /**
     * 出力データリストに空のMapを追加します。
     *
     * @param outputDataList 出力データリスト
     */
    private static void addEmptyMap(final List<Map<Integer, String>> outputDataList) {
        outputDataList.add(new HashMap<Integer, String>());
    }

}
