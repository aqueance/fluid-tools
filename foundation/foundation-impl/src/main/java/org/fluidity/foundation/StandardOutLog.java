/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.foundation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.fluidity.foundation.logging.Log;

/**
 * Logs to the standard output. All log levels are enabled.
 *
 * @author Tibor Varga
 */
final class StandardOutLog implements Log {

    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private final String source;

    public StandardOutLog(final Class<?> source) {
        this.source = source.getName();
    }

    private String stackTrace(final Throwable exception) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        pw.println();
        exception.printStackTrace(pw);

        return sw.toString();
    }

    private void log(final String level, final String message, final Object... args) {
        final String prefix = String.format("%s %s [%s] ", df.format(new Date()), level, source);
        final String suffix = String.format(message, args);
        System.out.printf("%s%s%n", prefix, suffix);
    }

    private void log(final String level, final Throwable exception, final String message, final Object... args) {
        final String prefix = String.format("%s %s [%s] ", df.format(new Date()), level, source);
        final String suffix = String.format(message, args);
        System.out.printf("%s%s%s%n", prefix, suffix, stackTrace(exception));
    }

    public boolean isTraceEnabled() {
        return true;
    }

    public boolean isDebugEnabled() {
        return true;
    }

    public boolean isInfoEnabled() {
        return true;
    }

    public void trace(final String message, final Object... args) {
        log("TRACE", message, args);
    }

    public void trace(final Throwable exception, final String message, final Object... args) {
        log("TRACE", exception, message, args);
    }

    public void debug(final String message, final Object... args) {
        log("DEBUG", message, args);
    }

    public void debug(final Throwable exception, final String message, final Object... args) {
        log("DEBUG", exception, message, args);
    }

    public void info(final String message, final Object... args) {
        log("INFO", message, args);
    }

    public void info(final Throwable exception, final String message, final Object... args) {
        log("INFO", exception, message, args);
    }

    public void warning(final String message, final Object... args) {
        log("WARN", message, args);
    }

    public void warning(final Throwable exception, final String message, final Object... args) {
        log("WARN", exception, message, args);
    }

    public void error(final String message, final Object... args) {
        log("ERROR", message, args);
    }

    public void error(final Throwable exception, final String message, final Object... args) {
        log("ERROR", exception, message, args);
    }
}