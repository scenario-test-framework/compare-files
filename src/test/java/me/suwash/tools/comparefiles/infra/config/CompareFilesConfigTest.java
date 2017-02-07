package me.suwash.tools.comparefiles.infra.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import me.suwash.tools.comparefiles.CompareFilesTestUtils;
import me.suwash.tools.comparefiles.CompareFilesTestWatcher;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.util.CompareUtils;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

@lombok.extern.slf4j.Slf4j
public class CompareFilesConfigTest {

    private static final String DIR_INPUT = CompareFilesTestUtils.getInputDir(CompareFilesConfigTest.class);
//    private static final String DIR_EXPECT = CompareFilesTestUtils.getExpectDir(CompareFilesConfigTest.class);
//    private static final String DIR_ACTUAL = CompareFilesTestUtils.getActualDir(CompareFilesConfigTest.class);

    private static final String FILENAME_DEFAULT = "default.json";
    private static final String FILENAME_DIFF = "diff.json";
    private static final String FILENAME_EMPTY = "empty.json";
    private static final String FILENAME_NOTEXIST = "not_exist.json";

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
    public final void testParse() {
        //------------------------------------------------------------------------------------------
        // 準備
        //------------------------------------------------------------------------------------------
        final String filePath = DIR_INPUT + "/" + FILENAME_DEFAULT;
        final boolean isClasspath = false;

        //------------------------------------------------------------------------------------------
        // 実行
        //------------------------------------------------------------------------------------------
        final CompareFilesConfig systemConfig = CompareFilesConfig.parse(filePath, isClasspath);

        //------------------------------------------------------------------------------------------
        // 確認
        //------------------------------------------------------------------------------------------
        systemConfig.printDetails();
    }

    @Test
    public final void testParse_NotExist() {
        //------------------------------------------------------------------------------------------
        // 準備
        //------------------------------------------------------------------------------------------
        final String filePath = DIR_INPUT + "/" + FILENAME_NOTEXIST;
        final boolean isClasspath = false;

        //------------------------------------------------------------------------------------------
        // 実行
        //------------------------------------------------------------------------------------------
        try {
            CompareFilesConfig.parse(filePath, isClasspath);
            fail();

        } catch (CompareFilesException e) {
            //--------------------------------------------------------------------------------------
            // 確認
            //--------------------------------------------------------------------------------------
            log.trace("目検用", e);
            assertEquals("メッセージコード", Const.MSGCD_ERROR_PARSE, e.getMessageId());
            assertEquals("メッセージ引数", filePath, e.getMessageArgs()[0]);
        }

    }

    @Test
    public final void testSetDefault() {
        //------------------------------------------------------------------------------------------
        // 準備
        //------------------------------------------------------------------------------------------
        final String defaultFilePath = DIR_INPUT + "/" + FILENAME_DEFAULT;
        final String diffFilePath = DIR_INPUT + "/" + FILENAME_DIFF;
        final boolean isClasspath = false;

        //------------------------------------------------------------------------------------------
        // 実行
        //------------------------------------------------------------------------------------------
        final CompareFilesConfig defaultConfig = CompareFilesConfig.parse(defaultFilePath, isClasspath);
        final CompareFilesConfig diffConfig = CompareFilesConfig.parse(diffFilePath, isClasspath);
        diffConfig.setDefault(defaultConfig);

        //------------------------------------------------------------------------------------------
        // 確認
        //------------------------------------------------------------------------------------------
        diffConfig.printDetails();

        assertEquals("Diffファイルのみに定義", "DIFF_ONLY", diffConfig.getOverwriteLayoutDir());
        assertEquals("Diff/Defaultどちらも定義", "BOTH", diffConfig.getIgnoreItemList().get(0));
        assertEquals("Defaultファイルのみに定義", "-", diffConfig.getCodeValueForOnlyOneRecordType());
    }


    @Test
    public final void testSetDefaultFromEmpty() {
        //------------------------------------------------------------------------------------------
        // 準備
        //------------------------------------------------------------------------------------------
        final String defaultFilePath = DIR_INPUT + "/" + FILENAME_DEFAULT;
        final String emptyFilePath = DIR_INPUT + "/" + FILENAME_EMPTY;
        final boolean isClasspath = false;

        //------------------------------------------------------------------------------------------
        // 実行
        //------------------------------------------------------------------------------------------
        final CompareFilesConfig defaultConfig = CompareFilesConfig.parse(defaultFilePath, isClasspath);
        final CompareFilesConfig emptyConfig = CompareFilesConfig.parse(emptyFilePath, isClasspath);
        emptyConfig.setDefault(defaultConfig);

        //------------------------------------------------------------------------------------------
        // 確認
        //------------------------------------------------------------------------------------------
        emptyConfig.printDetails();

        assertTrue(CompareUtils.deepCompare(defaultConfig.toString(), emptyConfig.toString()) == 0);
    }
}
