package me.suwash.tools.comparefiles;

import me.suwash.test.DefaultTestWatcher;
import me.suwash.tools.comparefiles.infra.util.ValidateUtils;

import org.apache.commons.lang3.StringUtils;
import org.junit.runner.Description;

@lombok.extern.slf4j.Slf4j
public class CompareFilesTestWatcher extends DefaultTestWatcher {

    /* (非 Javadoc)
     * @see me.suwash.test.DefaultTestWatcher#failed(java.lang.Throwable, org.junit.runner.Description)
     */
    @Override
    protected void failed(Throwable exception, Description description) {
        super.failed(exception, description);

        // バリデーションエラーの出力
        final String violationMessage = ValidateUtils.getViolationMessage();
        if (!StringUtils.isEmpty(violationMessage)) {
            log.error("バリデーションエラー：\n" + violationMessage);
        }
    }

}
