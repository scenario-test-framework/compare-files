package me.suwash.tools.comparefiles.sv.domain.compare.file.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import me.suwash.tools.comparefiles.CompareFilesTestWatcher;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

@lombok.extern.slf4j.Slf4j
public class ComparableRowTest {

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
    public final void testCompareTo() throws ParseException {
        ComparableRow leftRow = null;
        ComparableRow rightRow = null;

        Date timestamp = new Date();

        // key, valueが一致
        leftRow = getRow("key1", 100, "value1", 1.0, timestamp);
        rightRow = getRow("key1", 100, "value1", 1.0, timestamp);
        assertTrue("", leftRow.compareTo(rightRow) == 0);

        // keyが異なる
        rightRow = getRow("key2", 100, "value1", 1.0, timestamp);
        assertTrue("", leftRow.compareTo(rightRow) < 0);

        rightRow = getRow("key1", 101, "value1", 1.0, timestamp);
        assertTrue("", leftRow.compareTo(rightRow) < 0);

        rightRow = getRow("key0", 100, "value1", 1.0, timestamp);
        assertTrue("", leftRow.compareTo(rightRow) > 0);

        rightRow = getRow("key1", 99, "value1", 1.0, timestamp);
        assertTrue("", leftRow.compareTo(rightRow) > 0);

        // valueが異なる
        rightRow = getRow("key1", 100, "value2", 1.0, timestamp);
        assertTrue("", leftRow.compareTo(rightRow) == 0);

        rightRow = getRow("key1", 100, "value1", 1.1, timestamp);
        assertTrue("", leftRow.compareTo(rightRow) == 0);

        rightRow = getRow("key1", 100, "value1", 1.0, new Date());
        assertTrue("", leftRow.compareTo(rightRow) == 0);

        rightRow = getRow("key1", 100, "value0", 1.0, timestamp);
        assertTrue("", leftRow.compareTo(rightRow) == 0);

        rightRow = getRow("key1", 100, "value1", 0.9, timestamp);
        assertTrue("", leftRow.compareTo(rightRow) == 0);

        rightRow = getRow("key1", 100, "value1", 1.0, DateUtils.parseDate("2017-01-01 00:00:00", "YYYY-MM-dd HH:mm:ss"));
        assertTrue("", leftRow.compareTo(rightRow) == 0);
    }

    @Test
    public final void testHashCode() {
        ComparableRow leftRow = null;
        ComparableRow rightRow = null;

        Date timestamp = new Date();

        leftRow = getRow("key1", 100, "value1", 1.0, timestamp);
        rightRow = getRow("key1", 100, "value1", 1.0, timestamp);

        assertEquals("", leftRow.hashCode(), rightRow.hashCode());
        log.trace("leftRow.hashCode:" + leftRow.hashCode());
    }

    @Test
    public final void testEquals() throws ParseException {
        ComparableRow leftRow = null;
        ComparableRow rightRow = null;

        Date timestamp = new Date();

        leftRow = getRow("key1", 100, "value1", 1.0, timestamp);
        rightRow = getRow("key1", 100, "value1", 1.0, timestamp);
        assertTrue("key, valueが同じ値の場合", leftRow.equals(rightRow));

        rightRow = getRow("key2", 100, "value1", 1.0, timestamp);
        assertFalse("key1が異なる場合", leftRow.equals(rightRow));

        rightRow = getRow("key1", 101, "value1", 1.0, timestamp);
        assertFalse("key2が異なる場合", leftRow.equals(rightRow));

        rightRow = getRow("key1", 100, "value2", 1.0, timestamp);
        assertFalse("value1が異なる場合（文字列）", leftRow.equals(rightRow));

        rightRow = getRow("key1", 100, "value1", 1.1, timestamp);
        assertFalse("value2が異なる場合（数値）", leftRow.equals(rightRow));

        rightRow = getRow("key1", 100, "value1", 1.0, DateUtils.parseDate("2017-01-01 00:00:00", "YYYY-MM-dd HH:mm:ss"));
        assertFalse("value3が異なる場合（日時）", leftRow.equals(rightRow));
    }

    private ComparableRow getRow(Object key1, Object key2, Object value1, Object value2, Object value3) {
        Map<String, Object> keyMap = new HashMap<String, Object>();
        keyMap.put("key1", key1);
        keyMap.put("Key2", key2);

        Map<String, Object> valueMap = new HashMap<String, Object>();
        valueMap.put("Value1", value1);
        valueMap.put("Value2", value2);
        valueMap.put("Value3", value3);

        ComparableRow row = new ComparableRow();
        row.setRowNum(1);
        row.setKeyMap(keyMap);
        row.setValueMap(valueMap);
        row.setRawLine(row.toString());
        return row;
    }

}
