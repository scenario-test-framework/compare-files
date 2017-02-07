package me.suwash.tools.comparefiles.infra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import lombok.Getter;
import me.suwash.tools.comparefiles.CompareFilesTestUtils;
import me.suwash.tools.comparefiles.CompareFilesTestWatcher;
import me.suwash.tools.comparefiles.infra.classification.LineSp;
import me.suwash.tools.comparefiles.infra.exception.Errors;
import me.suwash.tools.comparefiles.infra.policy.Repository;
import me.suwash.tools.comparefiles.sv.da.file.repository.impl.StringFileRepository;
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
public class ContextTest {

//    private static final String DIR_INPUT = CompareFilesTestUtils.getInputDir(ContextTest.class);
//    private static final String DIR_EXPECT = CompareFilesTestUtils.getExpectDir(ContextTest.class);
    private static final String DIR_ACTUAL = CompareFilesTestUtils.getActualDir(ContextTest.class);

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

    @Getter
    private class Bean {
        @NotEmpty
        private String id;

        @NotEmpty
        @ExistPath
        @Dir
        private String dirPath;

        public Bean(String id, String dirPath) {
            this.id = id;
            this.dirPath = dirPath;
        }
    }

    private Set<ConstraintViolation<Bean>> getViolations(String value) {
        final Bean bean = new Bean(StringUtils.EMPTY, value);

        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        return validator.validate(bean);
    }

    @SuppressWarnings("deprecation")
    @Test
    public final void testSetErrors() {
        Errors errors = null;
        Errors result = null;
        final Context context = Context.getInstance();

        //------------------------------------------------------------------------------------------
        // 空のErrorsの場合
        //------------------------------------------------------------------------------------------
        // 準備
        errors = new Errors();

        // 実行
        context.setErrors(errors);
        result = context.getErrors();
        log.trace("空のErrorsの場合：\n" + result);

        // 確認
        assertNull("", result);

        //------------------------------------------------------------------------------------------
        // 妥当性チェックエラーが含まれる場合
        //------------------------------------------------------------------------------------------
        // 準備
        String notExistPath1 = "/path/to/not_exist_1";
        errors = new Errors();
        errors.addViolations(getViolations(notExistPath1));

        // 実行
        context.setErrors(errors);
        result = context.getErrors();
        log.trace("妥当性チェックエラーが含まれる場合：\n" + result);

        // 確認
        assertEquals("", 2, result.size());
        assertTrue("", result.toString().contains(notExistPath1));

        //------------------------------------------------------------------------------------------
        // 上書きした場合
        //------------------------------------------------------------------------------------------
        // 準備
        String notExistPath2 = "/path/to/not_exist_2";
        errors = new Errors();
        errors.addViolations(getViolations(notExistPath2));

        // 実行
        context.setErrors(errors);
        result = context.getErrors();
        log.trace("上書きした場合：\n" + result);

        // 確認
        assertEquals("", 2, result.size());
        assertTrue("", result.toString().contains(notExistPath2));

        //------------------------------------------------------------------------------------------
        // 他に影響を与えないようにクリア
        //------------------------------------------------------------------------------------------
        context.clearErrors();

    }

    @Test
    public final void testAddRepository() {
        Map<String, Repository> repoMap = null;

        String filePath1 = DIR_ACTUAL + "/file_1";
        String filePath2 = DIR_ACTUAL + "/file_2";
        String filePath3 = DIR_ACTUAL + "/file_3";

        String charset = "utf8";
        LineSp lineSp = LineSp.LF;
        int chunkSize = 10;

        StringFileRepository repo1 = new StringFileRepository(filePath1, charset, lineSp, chunkSize);
        StringFileRepository repo2 = new StringFileRepository(filePath2, charset, lineSp, chunkSize);
        StringFileRepository repo3 = new StringFileRepository(filePath3, charset, lineSp, chunkSize);

        Context context = Context.getInstance();

        //------------------------------------------------------------------------------------------
        // 正常系
        //------------------------------------------------------------------------------------------
        context.addRepository("key", repo1);
        repoMap = context.getRepositoryMap();
        log.trace("登録された場合：\n" + repoMap);
        assertEquals("", repo1, repoMap.get("key"));

        context.removeRepository("key");
        repoMap = context.getRepositoryMap();
        log.trace("削除された場合：\n" + repoMap);
        assertEquals("", 0, repoMap.size());

        //------------------------------------------------------------------------------------------
        // 上書き
        //------------------------------------------------------------------------------------------
        context.addRepository("key", repo2);
        context.addRepository("key", repo3);
        repoMap = context.getRepositoryMap();
        log.trace("上書きされた場合：\n" + repoMap);
        assertEquals("", 1, repoMap.size());
        assertEquals("", repo3, repoMap.get("key"));

        //------------------------------------------------------------------------------------------
        // 他に影響を与えないようにクリア
        //------------------------------------------------------------------------------------------
        context.removeRepository("key");

    }

}
