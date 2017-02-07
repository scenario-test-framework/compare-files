package me.suwash.tools.comparefiles.sv.domain.compare.file.image;

import static org.junit.Assert.assertEquals;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.suwash.tools.comparefiles.CompareFilesTestUtils;
import me.suwash.tools.comparefiles.CompareFilesTestWatcher;
import me.suwash.tools.comparefiles.infra.config.CompareFilesConfig;
import me.suwash.tools.comparefiles.infra.config.FileLayout;
import me.suwash.util.CompareUtils.CompareStatus;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class ImageFileCompareResultTest {

    private static final String DIR_INPUT = CompareFilesTestUtils.getInputDir(ImageFileCompareResultTest.class);
    private static final String DIR_ACTUAL = CompareFilesTestUtils.getActualDir(ImageFileCompareResultTest.class);

    @Rule
    public CompareFilesTestWatcher watcher = new CompareFilesTestWatcher();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CompareFilesTestUtils.initActualDir(ImageFileCompareResultTest.class);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public final void test() throws IOException {
        String inputExt = "jpg";
        String leftFilePath = DIR_INPUT + "/left/image01." + inputExt;
        String rightFilePath = DIR_INPUT + "/right/image01." + inputExt;

        //------------------------------------------------------------------------------------------
        // 除外リストがnullの場合
        //------------------------------------------------------------------------------------------
        String leftPrefix = "L:";
        String rightPrefix = "R:";
        boolean isWriteDiffOnly = false;
        List<Rectangle> ignoreAreaList = null;

        CompareFilesConfig systemConfig = new CompareFilesConfig();
        systemConfig.setCompareDetailFilePrefix("CompareDetail_");
        systemConfig.setLeftPrefix(leftPrefix);
        systemConfig.setRightPrefix(rightPrefix);
        systemConfig.setWriteDiffOnly(isWriteDiffOnly);
        systemConfig.setIgnoreAreaList(ignoreAreaList);

        FileLayout fileLayout = null;

        ImageFileCompareResult target = new ImageFileCompareResult(
            leftFilePath,
            rightFilePath,
            fileLayout,
            DIR_ACTUAL,
            systemConfig,
            inputExt);
        target.compare();

        assertEquals("除外リストがnullの場合", CompareStatus.NG, target.getStatus());

        //------------------------------------------------------------------------------------------
        // 際のある部分が除外されている場合
        //------------------------------------------------------------------------------------------
        leftPrefix = "期待値:";
        rightPrefix = "実績値:";
        ignoreAreaList = new ArrayList<Rectangle>();
        ignoreAreaList.add(new Rectangle(280, 80, 50, 50));
        systemConfig.setIgnoreAreaList(ignoreAreaList);
        systemConfig.setLeftPrefix(leftPrefix);
        systemConfig.setRightPrefix(rightPrefix);

        target = new ImageFileCompareResult(
            leftFilePath,
            rightFilePath,
            fileLayout,
            DIR_ACTUAL,
            systemConfig,
            inputExt);
        target.compare();

        assertEquals("差異のある部分が除外されている場合", CompareStatus.OK, target.getStatus());

        //------------------------------------------------------------------------------------------
        // 元画像より除外エリアが大きい場合
        //------------------------------------------------------------------------------------------
        ignoreAreaList = new ArrayList<Rectangle>();
        ignoreAreaList.add(new Rectangle(0, 0, 1000, 1000));
        systemConfig.setIgnoreAreaList(ignoreAreaList);

        target = new ImageFileCompareResult(
            leftFilePath,
            rightFilePath,
            fileLayout,
            DIR_ACTUAL,
            systemConfig,
            inputExt);
        target.compare();

        assertEquals("元画像より除外エリアが大きい場合", CompareStatus.OK, target.getStatus());

    }

}
