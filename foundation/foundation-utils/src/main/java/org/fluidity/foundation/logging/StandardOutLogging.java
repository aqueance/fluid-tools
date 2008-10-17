package org.fluidity.foundation.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.fluidity.foundation.Logging;

/**
 * Logs to the standard output. Tracing is disabled by default and can be turned on by setting the system property
 * "sysout.trace" to true.
 */
public final class StandardOutLogging implements Logging {

    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final String appName;

    public StandardOutLogging(final String appName) {
        this.appName = appName;
    }

    private String stackTrace(final Throwable exception) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        pw.println();
        exception.printStackTrace(pw);

        return sw.toString();
    }

    private void log(final String level, Class source, String message) {
        final String name = appName != null ? " [" + appName + "]" : "";
        System.out.println(df.format(new Date()) + " "+ level + name  + " [" + source.getName() + "] " + message);
    }

    public boolean isTraceEnabled(Class source) {
        return Boolean.getBoolean("sysout.trace");
    }

    public void trace(Class source, String message) {
        log("TRACE", source, message);
    }

    public void trace(Class source, String message, Throwable exception) {
        log("TRACE", source, message + stackTrace(exception));
    }

    public void debug(Class source, String message) {
        log("DEBUG", source, message);
    }

    public void debug(Class source, String message, Throwable exception) {
        log("DEBUG", source, message + stackTrace(exception));
    }

    public void info(Class source, String message) {
        log("INFO", source, message);
    }

    public void warning(Class source, String message) {
        log("WARN", source, message);
    }

    public void warning(Class source, String message, Throwable exception) {
        log("WARN", source, message + stackTrace(exception));
    }

    public void error(Class source, String message) {
        log("ERROR", source, message);
    }

    public void error(Class source, String message, Throwable exception) {
        log("ERROR", source, message + stackTrace(exception));
    }

    public void fatal(Class source, String message) {
        log("FATAL", source, message);
    }

    public void fatal(Class source, String message, Throwable exception) {
        log("FATAL", source, message + stackTrace(exception));
    }

    public void timer(Class source, String message, long beginStamp) {
        info(source, message + " took " + (System.currentTimeMillis() - beginStamp) + " ms");
    }
}