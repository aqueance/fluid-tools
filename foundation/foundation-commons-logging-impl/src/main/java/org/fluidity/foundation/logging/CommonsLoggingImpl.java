package org.fluidity.foundation.logging;

import org.apache.commons.logging.LogFactory;
import org.fluidity.composition.Component;
import org.fluidity.foundation.Logging;

/**
 * Uses commons-logging as the underlying logging framework.
 */
@Component
final class CommonsLoggingImpl implements Logging {

    public boolean isTraceEnabled(Class source) {
        return LogFactory.getLog(source).isTraceEnabled();
    }

    public void trace(Class source, String message) {
        LogFactory.getLog(source).trace(message);
    }

    public void trace(Class source, String message, Throwable exception) {
        LogFactory.getLog(source).trace(message, exception);
    }

    public void debug(Class source, String message) {
        LogFactory.getLog(source).debug(message);
    }

    public void debug(Class source, String message, Throwable exception) {
        LogFactory.getLog(source).debug(message, exception);
    }

    public void info(Class source, String message) {
        LogFactory.getLog(source).info(message);
    }

    public void warning(Class source, String message) {
        LogFactory.getLog(source).warn(message);
    }

    public void warning(Class source, String message, Throwable exception) {
        LogFactory.getLog(source).warn(message, exception);
    }

    public void error(Class source, String message) {
        LogFactory.getLog(source).error(message);
    }

    public void error(Class source, String message, Throwable exception) {
        LogFactory.getLog(source).error(message, exception);
    }

    public void fatal(Class source, String message) {
        LogFactory.getLog(source).fatal(message);
    }

    public void fatal(Class source, String message, Throwable exception) {
        LogFactory.getLog(source).fatal(message, exception);
    }

    public void timer(Class source, String message, long beginStamp) {
        info(source, message + " took " + (System.currentTimeMillis() - beginStamp) + " ms");
    }
}
