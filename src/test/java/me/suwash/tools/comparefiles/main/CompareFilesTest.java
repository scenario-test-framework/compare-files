package me.suwash.tools.comparefiles.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageOutputStream;

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
            DIR_INPUT + "/left/TEXT_PLAINTEXT/compare_layout_undefined.csv",
            DIR_INPUT + "/right/TEXT_PLAINTEXT/compare_layout_undefined.csv"};

        processStatus = compareFiles.execute(args);
        assertEquals("", ProcessStatus.Warning, processStatus);


        //------------------------------------------------------------------------------------------
        // ファイル比較（レイアウトあり）
        //------------------------------------------------------------------------------------------
        args = new String[] {
            "-od", DIR_ACTUAL + "/file",
            DIR_INPUT + "/left/TEXT_CSV/csv_with-header_ng.csv",
            DIR_INPUT + "/right/TEXT_CSV/csv_with-header_ng.csv"};

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

//    @Test
    public void generateDiffImage() throws IOException {
        int fontSize = 48;
        generateDiffImage(
            DIR_INPUT + "/right/IMAGE_BMP/bmp_ok.bmp",
            DIR_INPUT + "/right/IMAGE_BMP/bmp_ng.bmp",
            "bmp",
            fontSize, 256, 512);
        generateDiffImage(
            DIR_INPUT + "/right/IMAGE_GIF/gif_ok.gif",
            DIR_INPUT + "/right/IMAGE_GIF/gif_ng.gif",
            "gif",
            fontSize, 256, 512);
        generateDiffImage(
            DIR_INPUT + "/right/IMAGE_PNG/png_ok.png",
            DIR_INPUT + "/right/IMAGE_PNG/png_ng.png",
            "png",
            fontSize, 256, 512);
        generateDiffImage(
            DIR_INPUT + "/right/IMAGE_JPG/jpg_ok.jpg",
            DIR_INPUT + "/right/IMAGE_JPG/jpg_ng.jpg",
            "jpg",
            fontSize, 256, 512);
        generateSameImage(
            DIR_INPUT + "/right/IMAGE_JPG/jpg_ok.jpg",
            DIR_INPUT + "/left/IMAGE_JPG/jpg_ng.jpg",
            "jpg");
    }

    private void generateSameImage(String srcPath, String destPath, String formatName) throws IOException {
        File srcFile = new File(srcPath);
        File destFile = new File(destPath);
        BufferedImage image = ImageIO.read(srcFile);
        ImageIO.write(image, formatName, destFile);
    }

    private void generateDiffImage(String srcPath, String destPath, String formatName, int fontSize, int left, int top) throws IOException {
        File srcFile = new File(srcPath);
        File destFile = new File(destPath);

        BufferedImage image = ImageIO.read(srcFile);
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(new Color(0,0,0,255));
        graphics.setFont(new Font("Arial", Font.PLAIN, fontSize));
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawString("Diff-1", left, top);
        graphics.drawString("Diff-2", top, left);

        ImageIO.write(image, formatName, destFile);
    }

    @Test
    public void testImage() {
        //------------------------------------------------------------------------------------------
        // ファイル比較（レイアウトなし）
        //------------------------------------------------------------------------------------------
        String [] args = new String[] {
            "-od", DIR_ACTUAL + "/file_image",
            DIR_INPUT + "/left/IMAGE_BMP/bmp_ng.bmp",
            DIR_INPUT + "/right/IMAGE_BMP/bmp_ng.bmp"};

        ProcessStatus processStatus = new CompareFiles().execute(args);
        assertEquals("", ProcessStatus.Warning, processStatus);
    }

}
