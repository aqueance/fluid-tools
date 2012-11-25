/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.foundation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logs to the standard output. All log levels are enabled.
 *
 * @param <T> identifies the class to which an instance belongs.
 *
 * @author Tibor Varga
 */
final class StandardOutLog<T> implements Log<T> {

    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private final String source;

    StandardOutLog(final Class<?> source) {
        this.source = source.getName();
    }

    private String stackTrace(final Throwable exception) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);

        pw.println();
        exception.printStackTrace(pw);

        return sw.toString();
    }

    private void log(final String level, final String message, final Object... arguments) {
        final String prefix = String.format("%s %s [%s] ", df.format(new Date()), level, source);
        final String suffix = String.format(message, arguments);
        System.out.printf("%s%s%n", prefix, suffix);
    }

    private void log(final String level, final Throwable exception, final String message, final Object... arguments) {
        final String prefix = String.format("%s %s [%s] ", df.format(new Date()), level, source);
        final String suffix = String.format(message, arguments);
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

    public boolean isWarningEnabled() {
        return true;
    }

    public void trace(final String format, final Object... arguments) {
        log("TRACE", format, arguments);
    }

    public void trace(final Throwable exception, final String format, final Object... arguments) {
        log("TRACE", exception, format, arguments);
    }

    public void debug(final String format, final Object... arguments) {
        log("DEBUG", format, arguments);
    }

    public void debug(final Throwable exception, final String format, final Object... arguments) {
        log("DEBUG", exception, format, arguments);
    }

    public void info(final String format, final Object... arguments) {
        log("INFO", format, arguments);
    }

    public void info(final Throwable exception, final String format, final Object... arguments) {
        log("INFO", exception, format, arguments);
    }

    public void warning(final String format, final Object... arguments) {
        log("WARN", format, arguments);
    }

    public void warning(final Throwable exception, final String format, final Object... arguments) {
        log("WARN", exception, format, arguments);
    }

    public void error(final String format, final Object... arguments) {
        log("ERROR", format, arguments);
    }

    public void error(final Throwable exception, final String format, final Object... arguments) {
        log("ERROR", exception, format, arguments);
    }
}
