package me.suwash.tools.comparefiles.infra.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import me.suwash.tools.comparefiles.CompareFilesTestWatcher;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.classification.RecordPattern;
import me.suwash.tools.comparefiles.infra.classification.RecordType;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

@lombok.extern.slf4j.Slf4j
public class FileLayoutTest {

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
        // レコードリストが指定されていない場合
        //------------------------------------------------------------------------------------------
        FileLayout fileLayout = new FileLayout();
        try {
            fileLayout.getFirstRecordByteLength();
            fail();
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            assertEquals("メッセージコード", Const.CHECK_NOTNULL, e.getMessageId());
        }
        assertEquals("定義内容から、レコードパターン＝なし を返すこと", RecordPattern.None, fileLayout.getRecordPattern());

        //------------------------------------------------------------------------------------------
        // レコードリストにヘッダー、データが指定されている場合
        //------------------------------------------------------------------------------------------
        RecordLayout headerRecord = new RecordLayout();
        headerRecord.setType(RecordType.Header);
        RecordLayout dataRecord = new RecordLayout();
        dataRecord.setType(RecordType.Data);

        List<RecordLayout> recordList = new ArrayList<RecordLayout>();
        recordList.add(headerRecord);
        recordList.add(dataRecord);

        fileLayout.setRecordList(recordList);
        assertEquals("定義内容から、レコードパターン＝HeaderData を返すこと", RecordPattern.HeaderData, fileLayout.getRecordPattern());

    }

}
