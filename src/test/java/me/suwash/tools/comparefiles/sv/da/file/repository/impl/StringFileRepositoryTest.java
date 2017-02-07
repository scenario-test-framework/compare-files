package me.suwash.tools.comparefiles.sv.da.file.repository.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import me.suwash.tools.comparefiles.CompareFilesTestUtils;
import me.suwash.tools.comparefiles.CompareFilesTestWatcher;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.LineSp;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

@lombok.extern.slf4j.Slf4j
public class StringFileRepositoryTest {

//    private static final String DIR_INPUT = CompareFilesTestUtils.getInputDir(StringFileRepositoryTest.class);
//    private static final String DIR_EXPECT = CompareFilesTestUtils.getExpectDir(StringFileRepositoryTest.class);
    private static final String DIR_ACTUAL = CompareFilesTestUtils.getActualDir(StringFileRepositoryTest.class);

    @Rule
    public CompareFilesTestWatcher watcher = new CompareFilesTestWatcher();


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CompareFilesTestUtils.initActualDir(StringFileRepositoryTest.class);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public final void testExecute() {
        String actualFilePath = DIR_ACTUAL + "/target.txt";

        //------------------------------------------------------------------------------------------
        // fileがnullの場合
        //------------------------------------------------------------------------------------------
        File file = null;
        String charset = null;
        LineSp lineSp = null;
        int chunkSize = -1;
        try {
            new StringFileRepository(file, charset, lineSp, chunkSize);
            fail();
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            assertEquals("メッセージコード", Const.CHECK_NOTNULL, e.getMessageId());
            assertEquals("メッセージ引数", "file", e.getMessageArgs()[0]);
        }

        //------------------------------------------------------------------------------------------
        // 新規ファイル作成時
        //------------------------------------------------------------------------------------------
        // トランザクションなしでコミット
        String filePath = actualFilePath;
        charset = "utf8";
        StringFileRepository repo = new StringFileRepository(filePath, charset, lineSp, chunkSize);
        try {
            repo.commit();
            fail();
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            assertEquals("メッセージコード", Const.MSGCD_ERROR_REPOSITORY_TX_NOTEXIST, e.getMessageId());
        }

        // トランザクションなしでロールバック
        file = new File(filePath);
        repo = new StringFileRepository(filePath, charset, lineSp, chunkSize);
        repo.rollback();
        assertFalse("対象ファイルが存在しないこと", file.exists());

        // トランザクション開始後、ロールバック
        repo = new StringFileRepository(filePath, charset, lineSp, chunkSize);
        repo.begin();
        repo.rollback();
        assertTrue("対象ファイルが存在すること", file.exists());
        assertEquals("対象ファイルが空ファイルであること", 0, file.length());
        assertEquals("トランザクションファイルが存在しないこと", 1, file.getParentFile().list().length);

    }

}
