package me.suwash.tools.comparefiles.infra.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import me.suwash.tools.comparefiles.CompareFilesTestWatcher;
import me.suwash.util.CompareUtils.CompareCriteria;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

@lombok.extern.slf4j.Slf4j
public class RecordLayoutTest {

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
        // キー項目IDのリストを取得できること
        //------------------------------------------------------------------------------------------
        ItemLayout col1 = new ItemLayout();
        col1.setId("col1");
        col1.setName("カラム1");
        col1.setCompareKey(true);
        col1.setCriteria(CompareCriteria.Equal);
        col1.setByteLength(1);

        ItemLayout col2 = new ItemLayout();
        col2.setId("col2");
        col2.setName("カラム2");
        col2.setCompareKey(true);
        col2.setCriteria(CompareCriteria.Equal);
        col2.setByteLength(2);

        ItemLayout col3 = new ItemLayout();
        col3.setId("col3");
        col3.setName("カラム3");
        col3.setCompareKey(false);
        col3.setCriteria(CompareCriteria.Equal);
        col3.setByteLength(3);

        List<ItemLayout> itemList = new ArrayList<>();
        itemList.add(col1);
        itemList.add(col2);
        itemList.add(col3);

        RecordLayout recordLayout = new RecordLayout();
        recordLayout.setItemList(itemList);

        List<String> keyItemList = recordLayout.getCompareKeyItemList();
        assertTrue("", keyItemList.contains("col1"));
        assertTrue("", keyItemList.contains("col2"));
        assertFalse("", keyItemList.contains("col3"));

    }

}
