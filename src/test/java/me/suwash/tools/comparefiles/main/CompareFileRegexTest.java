package me.suwash.tools.comparefiles.main;

import static org.junit.Assert.assertEquals;
import me.suwash.ddd.classification.ProcessStatus;
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
public class CompareFileRegexTest {

    private static final String DIR_INPUT = CompareFilesTestUtils.getInputDir(CompareFileRegexTest.class);
    private static final String DIR_EXPECT = CompareFilesTestUtils.getExpectDir(CompareFileRegexTest.class);
    private static final String DIR_ACTUAL = CompareFilesTestUtils.getActualDir(CompareFileRegexTest.class);

    @Rule
    public CompareFilesTestWatcher watcher = new CompareFilesTestWatcher();


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CompareFilesTestUtils.initActualDir(CompareFileRegexTest.class);
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
        CompareFileRegex compareFileRegex = new CompareFileRegex();
        ProcessStatus processStatus = null;

        //------------------------------------------------------------------------------------------
        // 起動引数なし
        //------------------------------------------------------------------------------------------
        try {
            processStatus = compareFileRegex.execute(args);
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            assertEquals("メッセージコード", Const.MSGCD_ERROR_ARG, e.getMessageId());
        }

        //------------------------------------------------------------------------------------------
        // 起動引数あり
        //------------------------------------------------------------------------------------------
        args = new String[] {
            "-od", DIR_ACTUAL,
            DIR_INPUT + "/compare_target.csv"};

        processStatus = compareFileRegex.execute(args);
        assertEquals("", ProcessStatus.Failure, processStatus);
        CompareFilesTestUtils.assertCompareResultFiles(DIR_EXPECT, DIR_ACTUAL, "CompareSummary.csv");

    }

}
