package org.fluidity.foundation.logging;

import org.apache.commons.logging.LogFactory;
import org.fluidity.composition.Component;
import org.fluidity.foundation.Logging;

/**
 * Uses commons-logging as the underlying logging framework.
 */
@Component
final class CommonsLoggingImpl implements Logging {

    public boolean isTraceEnabled(final Class source) {
        return LogFactory.getLog(source).isTraceEnabled();
    }

    public void trace(final Class source, final String message) {
        LogFactory.getLog(source).trace(message);
    }

    public void trace(final Class source, final String message, final Throwable exception) {
        LogFactory.getLog(source).trace(message, exception);
    }

    public void debug(final Class source, final String message) {
        LogFactory.getLog(source).debug(message);
    }

    public void debug(final Class source, final String message, final Throwable exception) {
        LogFactory.getLog(source).debug(message, exception);
    }

    public void info(final Class source, final String message) {
        LogFactory.getLog(source).info(message);
    }

    public void warning(final Class source, final String message) {
        LogFactory.getLog(source).warn(message);
    }

    public void warning(final Class source, final String message, final Throwable exception) {
        LogFactory.getLog(source).warn(message, exception);
    }

    public void error(final Class source, final String message) {
        LogFactory.getLog(source).error(message);
    }

    public void error(final Class source, final String message, final Throwable exception) {
        LogFactory.getLog(source).error(message, exception);
    }

    public void fatal(final Class source, final String message) {
        LogFactory.getLog(source).fatal(message);
    }

    public void fatal(final Class source, final String message, final Throwable exception) {
        LogFactory.getLog(source).fatal(message, exception);
    }

    public void timer(final Class source, final String message, final long beginStamp) {
        info(source, message + " took " + (System.currentTimeMillis() - beginStamp) + " ms");
    }
}
