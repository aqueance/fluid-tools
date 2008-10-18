/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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

    private void log(final String level, Class source, final String message) {
        final String name = appName != null ? " [" + appName + "]" : "";
        System.out.println(df.format(new Date()) + " " + level + name + " [" + source.getName() + "] " + message);
    }

    public boolean isTraceEnabled(final Class source) {
        return Boolean.getBoolean("sysout.trace");
    }

    public void trace(final Class source, final String message) {
        log("TRACE", source, message);
    }

    public void trace(final Class source, final String message, final Throwable exception) {
        log("TRACE", source, message + stackTrace(exception));
    }

    public void debug(final Class source, final String message) {
        log("DEBUG", source, message);
    }

    public void debug(final Class source, final String message, final Throwable exception) {
        log("DEBUG", source, message + stackTrace(exception));
    }

    public void info(final Class source, final String message) {
        log("INFO", source, message);
    }

    public void warning(final Class source, final String message) {
        log("WARN", source, message);
    }

    public void warning(final Class source, final String message, final Throwable exception) {
        log("WARN", source, message + stackTrace(exception));
    }

    public void error(final Class source, final String message) {
        log("ERROR", source, message);
    }

    public void error(final Class source, final String message, final Throwable exception) {
        log("ERROR", source, message + stackTrace(exception));
    }

    public void fatal(final Class source, final String message) {
        log("FATAL", source, message);
    }

    public void fatal(final Class source, final String message, final Throwable exception) {
        log("FATAL", source, message + stackTrace(exception));
    }

    public void timer(final Class source, final String message, final long beginStamp) {
        info(source, message + " took " + (System.currentTimeMillis() - beginStamp) + " ms");
    }
}