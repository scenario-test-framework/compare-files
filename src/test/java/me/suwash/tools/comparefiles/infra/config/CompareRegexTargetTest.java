package me.suwash.tools.comparefiles.infra.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
public class CompareRegexTargetTest {

    @Rule
    public CompareFilesTestWatcher watcher = new CompareFilesTestWatcher();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {}

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public final void test() {
        //------------------------------------------------------------------------------------------
        // 準備
        //------------------------------------------------------------------------------------------
        String leftDirPath = "/path/to/left";
        String rightDirPath = "/path/to/right";
        String fileRegex = "file_name_\\d{8}.txt";
        String errorRegex = "file_name_\\d{8.txt";

        //------------------------------------------------------------------------------------------
        // 実行
        //------------------------------------------------------------------------------------------
        try {
            new CompareRegexTarget(null, rightDirPath, fileRegex);
            fail();
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            assertEquals("メッセージコード", Const.CHECK_NOTNULL, e.getMessageId());
        }

        try {
            new CompareRegexTarget(leftDirPath, null, fileRegex);
            fail();
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            assertEquals("メッセージコード", Const.CHECK_NOTNULL, e.getMessageId());
        }

        try {
            new CompareRegexTarget(leftDirPath, rightDirPath, null);
            fail();
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            assertEquals("メッセージコード", Const.CHECK_NOTNULL, e.getMessageId());
        }

        try {
            new CompareRegexTarget(leftDirPath, rightDirPath, errorRegex);
            fail();
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            assertEquals("メッセージコード", Const.MSGCD_ERROR_REGEX_PARSE, e.getMessageId());
        }

    }

}
