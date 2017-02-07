package me.suwash.tools.comparefiles.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import me.suwash.tools.comparefiles.CompareFilesTestWatcher;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.beust.jcommander.JCommander;

@lombok.extern.slf4j.Slf4j
public class CompareFilesOptionTest {

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
    public final void testOverwriteConfig() {
        //------------------------------------------------------------------------------------------
        // 準備
        //------------------------------------------------------------------------------------------
        String[] args = new String[] {
            "-config", "/path/to/config",
            "-d",
            "-ic", "utf8",
            "-s",
            "-ch", "3",
            "-cd", "4",
            "-od", "\"/path/to with_space/output\"",
            "-of", "OutputSummary",
            "-oc", "ms932",
            "-wdo",
            "-dpl", "期待値：",
            "-dpr", "実績値：",
            "-dfp", "OutputDetail_",
            "-chunk", "10",
            "-ignore", "除外項目1,除外項目2",
            "-layout", "'/path/to with_space/layout'",
            "/pah/to/left",
            "/path/to/right",
            };

        CompareFilesConfig systemConfig = new CompareFilesConfig();

        log.info("上書き前");
        systemConfig.printDetails();

        final CompareFilesOption option = new CompareFilesOption();
        new JCommander(option, args);
        assertNotEquals(option.isDeleteWorkDir(), systemConfig.isDeleteWorkDir());
        assertNotEquals(option.getInputCharset(), systemConfig.getDefaultInputCharset());
        assertNotEquals(option.isSorted(), systemConfig.isSorted());
        assertNotEquals(option.getCsvHeaderRow(), systemConfig.getCsvHeaderRow());
        assertNotEquals(option.getCsvDataStartRow(), systemConfig.getCsvDataStartRow());
        assertNotEquals(option.getOutputDir(), systemConfig.getOutputDir());
        assertNotEquals(option.getCompareResultFileName(), systemConfig.getCompareResultFileName());
        assertNotEquals(option.getOutputCharset(), systemConfig.getOutputCharset());
        assertNotEquals(option.isWriteDiffOnly(), systemConfig.isWriteDiffOnly());
        assertNotEquals(option.getLeftPrefix(), systemConfig.getLeftPrefix());
        assertNotEquals(option.getRightPrefix(), systemConfig.getRightPrefix());
        assertNotEquals(option.getCompareDetailFilePrefix(), systemConfig.getCompareDetailFilePrefix());
        assertNotEquals(option.getChunkSize(), systemConfig.getChunkSize());
        assertNotEquals(option.getIgnoreItemList(), systemConfig.getIgnoreItemList());
        assertNotEquals(option.getOverwriteLayoutDir(), systemConfig.getOverwriteLayoutDir());


        //------------------------------------------------------------------------------------------
        // 実行
        //------------------------------------------------------------------------------------------
        option.overwriteConfig(systemConfig);


        //------------------------------------------------------------------------------------------
        // 確認
        //------------------------------------------------------------------------------------------
        log.info("上書き後");
        systemConfig.printDetails();

        assertEquals(option.isDeleteWorkDir(), systemConfig.isDeleteWorkDir());
        assertEquals(option.getInputCharset(), systemConfig.getDefaultInputCharset());
        assertEquals(option.isSorted(), systemConfig.isSorted());
        assertEquals(option.getCsvHeaderRow(), systemConfig.getCsvHeaderRow());
        assertEquals(option.getCsvDataStartRow(), systemConfig.getCsvDataStartRow());
        assertEquals(option.getOutputDir(), systemConfig.getOutputDir());
        assertEquals(option.getCompareResultFileName(), systemConfig.getCompareResultFileName());
        assertEquals(option.getOutputCharset(), systemConfig.getOutputCharset());
        assertEquals(option.isWriteDiffOnly(), systemConfig.isWriteDiffOnly());
        assertEquals(option.getLeftPrefix(), systemConfig.getLeftPrefix());
        assertEquals(option.getRightPrefix(), systemConfig.getRightPrefix());
        assertEquals(option.getCompareDetailFilePrefix(), systemConfig.getCompareDetailFilePrefix());
        assertEquals(option.getChunkSize(), systemConfig.getChunkSize());
        assertEquals(option.getIgnoreItemList(), systemConfig.getIgnoreItemList());
        assertEquals("シングルクォートで括られている場合、エスケープ後の値で上書きされていること。", option.getOverwriteLayoutDir(), "'" + systemConfig.getOverwriteLayoutDir() + "'");
    }

}
