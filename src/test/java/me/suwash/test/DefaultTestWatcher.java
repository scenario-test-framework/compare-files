package me.suwash.test;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * JUnit TestRule。
 */
@lombok.extern.slf4j.Slf4j
public class DefaultTestWatcher extends TestWatcher {

    /*
     * (非 Javadoc)
     * @see org.junit.rules.TestWatcher#starting(org.junit.runner.Description)
     */
    @Override
    protected void starting(final Description description) {
        System.setProperty("org.jboss.logging.provider", "slf4j");
        log.debug("◼◼ " + description.getTestClass().getSimpleName() + "#" + description.getMethodName() + " Started.");
    }

    /*
     * (非 Javadoc)
     * @see org.junit.rules.TestWatcher#succeeded(org.junit.runner.Description)
     */
    @Override
    protected void succeeded(final Description description) {
        log.debug("◼◼ " + description.getTestClass().getSimpleName() + "#" + description.getMethodName() + " Scceeded.");
    }

    /*
     * (非 Javadoc)
     * @see org.junit.rules.TestWatcher#failed(java.lang.Throwable, org.junit.runner.Description)
     */
    @Override
    protected void failed(final Throwable exception, final Description description) {
        log.error("◼◼ " + description.getTestClass().getSimpleName() + "#" + description.getMethodName() + " Failed.", exception);
    }

}
