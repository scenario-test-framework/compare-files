package me.suwash.tools.comparefiles.infra.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
public class CompareRegexTargetListTest {

    private static final String DIR_INPUT = CompareFilesTestUtils.getInputDir(CompareRegexTargetListTest.class);

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
        String columnNumOverFilePath = DIR_INPUT + "/column_num_over.csv";
        String regexErrorFilePath = DIR_INPUT + "/regex_error.csv";

        //------------------------------------------------------------------------------------------
        // 実行
        //------------------------------------------------------------------------------------------
        try {
            new CompareRegexTargetList(columnNumOverFilePath, Const.CHARSET_DEFAULT_CONFIG);
            fail();
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            assertEquals("メッセージコード", Const.MSGCD_ERROR_FILE_LAYOUT, e.getMessageId());
        }

        try {
            new CompareRegexTargetList(regexErrorFilePath, Const.CHARSET_DEFAULT_CONFIG);
            fail();
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            assertEquals("メッセージコード", Const.MSGCD_ERROR_FILE_PARSE, e.getMessageId());
        }

    }

}
