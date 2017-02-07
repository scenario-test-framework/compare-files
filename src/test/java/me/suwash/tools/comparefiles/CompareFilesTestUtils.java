package me.suwash.tools.comparefiles;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import me.suwash.test.TestUtils;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.util.CsvUtils;
import me.suwash.util.FileUtils;

import org.apache.commons.lang3.StringUtils;

@lombok.extern.slf4j.Slf4j
public class CompareFilesTestUtils {

    private static final String DIR_BASE = "src/test/scripts";

    public static String getBaseDir(final Class<?> testClass) {
        final String targetPackage = testClass.getPackage().getName();
        final String basePackage = CompareFilesTestUtils.class.getPackage().getName();
        final String relPath = targetPackage
            // 対象パッケージから、ベースパッケージを除去
            .replace(basePackage, StringUtils.EMPTY)
            // ドットをスラッシュに置換してパスとして返却
            .replaceAll("\\.", "/");
        return DIR_BASE + relPath + "/" + testClass.getSimpleName();
    }

    public static String getInputDir(final Class<?> testClass) {
        return getBaseDir(testClass) + "/input";
    }

    public static String getExpectDir(final Class<?> testClass) {
        return getBaseDir(testClass) + "/expect";
    }

    public static String getActualDir(final Class<?> testClass) {
        return getBaseDir(testClass) + "/actual";
    }

    public static void initActualDir(final Class<?> testClass) {
        FileUtils.initDir(getActualDir(testClass));
    }

    public static void assertCompareResultFiles(String dirExpect, String dirActual, String summaryFileName) {
        log.warn("▼ここでテストが失敗した場合は、期待値ディレクトリ：" + dirExpect + " を再配置してください。");
        // ファイル比較結果リストは、タイムスタンプが異なるため、個別で比較
        String DIRNAME_TEMP = "temp";
        String dirActualTemp = dirActual + "/" + DIRNAME_TEMP;
        File actualBeforeSummaryFile = new File(dirActual + "/" + summaryFileName);
        File actualSummaryFile = new File(dirActualTemp + "/" + summaryFileName);
        FileUtils.mkdirs(dirActualTemp);
        if (!actualBeforeSummaryFile.renameTo(actualSummaryFile)) {
            fail(actualBeforeSummaryFile + " から、一時ファイルへの移動に失敗しました。");
        }

        String dirExpectTemp = dirExpect + "/" + DIRNAME_TEMP;
        File expectBeforeSummaryFile = new File(dirExpect + "/" + summaryFileName);
        File expectSummaryFile = new File(dirExpectTemp + "/" + summaryFileName);
        FileUtils.mkdirs(dirExpectTemp);
        if (!expectBeforeSummaryFile.renameTo(expectSummaryFile)) {
            fail(expectBeforeSummaryFile + " から、一時ファイルへの移動に失敗しました。");
        }

        // 行比較結果リスト群を一括比較
        TestUtils.assertFilesEquals(dirExpect, dirActual, Const.CHARSET_DEFAULT_CONFIG);
        log.warn("▲ここまで");

        List<String[]> expectLineList = CsvUtils.parseFile(expectSummaryFile.getAbsolutePath(), Const.CHARSET_DEFAULT_CONFIG, CsvUtils.getCsvConfig());
        List<String[]> actualLineList = CsvUtils.parseFile(actualSummaryFile.getAbsolutePath(), Const.CHARSET_DEFAULT_CONFIG, CsvUtils.getCsvConfig());

        StringBuilder failMsgBuilder = new StringBuilder();

        int lineIndex = 0;
        for (String[] curExpectLine : expectLineList) {
            int colIndex = 0;
            for (String curExpect : curExpectLine) {
                String curActual = actualLineList.get(lineIndex)[colIndex];
                if (lineIndex == 0) {
                    // ヘッダーは全項目が一致すること
                    if (! curExpect.equals(curActual)) {
                        appendFail(failMsgBuilder, lineIndex, colIndex, curExpect, curActual);
                    }

                } else {
                    // データ行
                    if (colIndex == 1 || colIndex == 2) {
                        // 左右のパスは、期待値の相対パスと一致すること
                        if (! curActual.endsWith(curExpect)) {
                            appendFail(failMsgBuilder, lineIndex, colIndex, curExpect, curActual);
                        }
                    } else if (colIndex < 10) {
                        // その他のカラムは、開始時刻、終了時刻、処理時間以外が一致すること
                        if (! curExpect.equals(curActual)) {
                            appendFail(failMsgBuilder, lineIndex, colIndex, curExpect, curActual);
                        }
                    }
                }
                colIndex++;
            }
            lineIndex++;
        }

        if (!actualSummaryFile.renameTo(actualBeforeSummaryFile)) {
            fail("一時ファイルから、" + actualBeforeSummaryFile + " への移動に失敗しました。");
        }
        if (!expectSummaryFile.renameTo(expectBeforeSummaryFile)) {
            log.error("■期待値ディレクトリ：" + dirExpect + " を再配置してください。");
            fail("一時ファイルから、" + expectBeforeSummaryFile + " への移動に失敗しました。");
        }
        FileUtils.rmdirs(dirActualTemp);
        FileUtils.rmdirs(dirExpectTemp);

        if (failMsgBuilder.length() > 0) {
            fail(summaryFileName + " で差分を検出しました。\n" + failMsgBuilder.toString());
        }
    }
    private static void appendFail(StringBuilder failMsgBuilder, int lineIndex, int colIndex, String curExpect, String curActual) {
        failMsgBuilder
            .append("行：").append(lineIndex + 1)
            .append(" ")
            .append("列：").append(colIndex + 1)
            .append("が異なります。")
            .append("期待値：<").append(curExpect).append(">")
            .append(" ")
            .append("実績値：<").append(curActual).append(">")
            .append("\n");
    }

}
