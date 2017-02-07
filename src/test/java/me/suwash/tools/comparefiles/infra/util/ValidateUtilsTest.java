package me.suwash.tools.comparefiles.infra.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.validation.groups.Default;

import lombok.Getter;
import me.suwash.tools.comparefiles.CompareFilesTestWatcher;
import me.suwash.tools.comparefiles.infra.Const;
import me.suwash.tools.comparefiles.infra.Context;
import me.suwash.tools.comparefiles.infra.exception.CompareFilesException;
import me.suwash.util.validation.constraints.Dir;
import me.suwash.util.validation.constraints.ExistPath;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

@lombok.extern.slf4j.Slf4j
public class ValidateUtilsTest {

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


    public interface Group1 extends Default {};

    @Getter
    private class Bean {
        @NotEmpty
        private String id;

        @NotEmpty(groups = {Group1.class})
        @ExistPath
        @Dir
        private String dirPath;

        public Bean(String id, String dirPath) {
            this.id = id;
            this.dirPath = dirPath;
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public final void testValidate() {
        Bean target = null;

        //------------------------------------------------------------------------------------------
        // nullの場合
        //------------------------------------------------------------------------------------------
        try {
            ValidateUtils.validate(target);
            fail();
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            assertEquals("メッセージコード", Const.CHECK_NOTNULL, e.getMessageId());
        }

        //------------------------------------------------------------------------------------------
        // チェックエラーが存在しない場合
        //------------------------------------------------------------------------------------------
        target = new Bean("id", "/tmp");
        ValidateUtils.validate(target);
        assertNull("", ValidateUtils.getViolationMessage());

        //------------------------------------------------------------------------------------------
        // group指定なしの場合
        //------------------------------------------------------------------------------------------
        target = new Bean(StringUtils.EMPTY, StringUtils.EMPTY);
        try {
            ValidateUtils.validate(target);
            fail();
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            String violationMessage = ValidateUtils.getViolationMessage();
            log.trace("group指定なしの場合：\n" + violationMessage);
            assertNotNull("", violationMessage);
            assertTrue("dirPathが含まれていないこと", ! violationMessage.contains("Bean.dirPath"));
        }

        try {
            ValidateUtils.validate(target, Group1.class);
            fail();
        } catch (CompareFilesException e) {
            log.trace("目検用", e);
            String violationMessage = ValidateUtils.getViolationMessage();
            log.trace("group指定ありの場合：\n" + violationMessage);
            assertNotNull("", violationMessage);
            assertTrue("dirPathが含まれていること", violationMessage.contains("Bean.dirPath"));
        }

        //------------------------------------------------------------------------------------------
        // 他に影響を与えないようにクリア
        //------------------------------------------------------------------------------------------
        Context.getInstance().clearErrors();

    }

}
