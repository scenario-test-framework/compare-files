package me.suwash.tools.comparefiles.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.test.TestUtils;
import me.suwash.tools.comparefiles.CompareFilesTestUtils;
import me.suwash.tools.comparefiles.CompareFilesTestWatcher;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

@lombok.extern.slf4j.Slf4j
public class CompareFilesTest {

    private static final String DIR_INPUT = CompareFilesTestUtils.getInputDir(CompareFilesTest.class);
    private static final String DIR_EXPECT = CompareFilesTestUtils.getExpectDir(CompareFilesTest.class);
    private static final String DIR_ACTUAL = CompareFilesTestUtils.getActualDir(CompareFilesTest.class);

    @Rule
    public CompareFilesTestWatcher watcher = new CompareFilesTestWatcher();


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CompareFilesTestUtils.initActualDir(CompareFilesTest.class);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public final void testExecute() {
        String[] args = new String[] {};
        CompareFiles compareFiles = new CompareFiles();
        ProcessStatus processStatus = null;

        //------------------------------------------------------------------------------------------
        // 起動引数なし
        //------------------------------------------------------------------------------------------
        try {
            processStatus = compareFiles.execute(args);
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            assertEquals("メッセージコード", Const.MSGCD_ERROR_ARG, e.getMessageId());
        }


        //------------------------------------------------------------------------------------------
        // ファイル比較（レイアウトなし）
        //------------------------------------------------------------------------------------------
        args = new String[] {
            "-od", DIR_ACTUAL + "/file",
            DIR_INPUT + "/left/undefined.csv",
            DIR_INPUT + "/right/undefined.csv"};

        processStatus = compareFiles.execute(args);
        assertEquals("", ProcessStatus.Warning, processStatus);


        //------------------------------------------------------------------------------------------
        // ファイル比較（レイアウトあり）
        //------------------------------------------------------------------------------------------
        args = new String[] {
            "-od", DIR_ACTUAL + "/file",
            DIR_INPUT + "/left/csv_with_header.csv",
            DIR_INPUT + "/right/csv_with_header.csv"};

        processStatus = compareFiles.execute(args);
        assertEquals("", ProcessStatus.Warning, processStatus);
        TestUtils.assertFilesEquals(DIR_EXPECT + "/file", DIR_ACTUAL + "/file", Const.CHARSET_DEFAULT_CONFIG);


        //------------------------------------------------------------------------------------------
        // ディレクトリ比較
        //------------------------------------------------------------------------------------------
        args = new String[] {
            "-od", DIR_ACTUAL + "/dir",
            DIR_INPUT + "/left",
            DIR_INPUT + "/right"};
        processStatus = compareFiles.execute(args);
        assertEquals("", ProcessStatus.Warning, processStatus);
        CompareFilesTestUtils.assertCompareResultFiles(DIR_EXPECT + "/dir", DIR_ACTUAL + "/dir", "CompareSummary.csv");


        args = new String[] {
            "-od", DIR_ACTUAL + "/dir_left_only",
            DIR_INPUT + "/left",
            DIR_INPUT + "/not_exist"};
        processStatus = compareFiles.execute(args);
        assertEquals("", ProcessStatus.Warning, processStatus);
        CompareFilesTestUtils.assertCompareResultFiles(DIR_EXPECT + "/dir_left_only", DIR_ACTUAL + "/dir_left_only", "CompareSummary.csv");

        args = new String[] {
            "-od", DIR_ACTUAL + "/dir_right_only",
            DIR_INPUT + "/not_exist",
            DIR_INPUT + "/right"};
        processStatus = compareFiles.execute(args);
        assertEquals("", ProcessStatus.Warning, processStatus);
        CompareFilesTestUtils.assertCompareResultFiles(DIR_EXPECT + "/dir_right_only", DIR_ACTUAL + "/dir_right_only", "CompareSummary.csv");

        args = new String[] {
            "-od", DIR_ACTUAL + "/dir_both_not_exist",
            DIR_INPUT + "/not_exist",
            DIR_INPUT + "/not_exist"};
        try {
            compareFiles.execute(args);
            fail();
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            assertEquals("", Const.MSGCD_ERROR_COMPARE_DIR_BOTH_NOTEXIST, e.getMessageId());
        }
        // 空ファイルが作成されること
        CompareFilesTestUtils.assertCompareResultFiles(DIR_EXPECT + "/dir_both_not_exist", DIR_ACTUAL + "/dir_both_not_exist", "CompareSummary.csv");
    }

}
