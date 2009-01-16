package org.fluidity.foundation.logging;

import org.fluidity.foundation.Logging;
import org.fluidity.foundation.SystemSettings;

/**
 * Wraps a standard output logger unless the "container.quiet" System property is set to "true". In that case it
 * suppresses log messages from the bootstrap classes.
 */
public final class BootstrapLog implements Logging {

    public static final String SUPPRESS_LOGS = "quiet-boot";
    public static final String ALL_LOGS = "all";

    private final Logging log;
    private final boolean verbose;

    public BootstrapLog(final String category) {
        verbose = !SystemSettings.isSet(SUPPRESS_LOGS, category, ALL_LOGS);
        log = new StandardOutLogging(null);
    }

    public boolean isTraceEnabled(Class source) {
        return log != null && log.isTraceEnabled(source);
    }

    public void trace(Class source, String message) {
        if (verbose) log.trace(source, message);
    }

    public void trace(Class source, String message, Throwable exception) {
        log.trace(source, message, exception);
    }

    public void debug(Class source, String message) {
        if (verbose) log.debug(source, message);
    }

    public void debug(Class source, String message, Throwable exception) {
        log.debug(source, message, exception);
    }

    public void info(Class source, String message) {
        if (verbose) log.info(source, message);
    }

    public void warning(Class source, String message) {
        log.warning(source, message);
    }

    public void warning(Class source, String message, Throwable exception) {
        log.warning(source, message, exception);
    }

    public void error(Class source, String message) {
        log.error(source, message);
    }

    public void error(Class source, String message, Throwable exception) {
        log.error(source, message, exception);
    }

    public void fatal(Class source, String message) {
        log.fatal(source, message);
    }

    public void fatal(Class source, String message, Throwable exception) {
        log.fatal(source, message, exception);
    }

    public void timer(Class source, String message, long beginStamp) {
        if (verbose) log.timer(source, message, beginStamp);
    }
}
