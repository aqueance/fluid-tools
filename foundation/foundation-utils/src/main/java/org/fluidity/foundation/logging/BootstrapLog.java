/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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
package org.fluidity.foundation.logging;

import org.fluidity.foundation.Logging;
import org.fluidity.foundation.SystemSettings;

/**
 * Wraps a standard output logger unless the "container.quiet" System property is set to "true". In that case it suppresses log messages from the bootstrap
 * classes.
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

    public boolean isTraceEnabled(final Class source) {
        return log != null && log.isTraceEnabled(source);
    }

    public boolean isDebugEnabled(Class source) {
        return log != null && log.isDebugEnabled(source);
    }

    public void trace(final Class source, final String message) {
        if (verbose) {
            log.trace(source, message);
        }
    }

    public void trace(final Class source, final String message, final Throwable exception) {
        log.trace(source, message, exception);
    }

    public void debug(final Class source, final String message) {
        if (verbose) {
            log.debug(source, message);
        }
    }

    public void debug(final Class source, final String message, final Throwable exception) {
        log.debug(source, message, exception);
    }

    public void info(Class source, String message) {
        if (verbose) {
            log.info(source, message);
        }
    }

    public void warning(final Class source, final String message) {
        log.warning(source, message);
    }

    public void warning(final Class source, final String message, final Throwable exception) {
        log.warning(source, message, exception);
    }

    public void error(final Class source, final String message) {
        log.error(source, message);
    }

    public void error(final Class source, final String message, final Throwable exception) {
        log.error(source, message, exception);
    }

    public void fatal(final Class source, final String message) {
        log.fatal(source, message);
    }

    public void fatal(final Class source, final String message, final Throwable exception) {
        log.fatal(source, message, exception);
    }

    public void timer(final Class source, final String message, final long beginStamp) {
        if (verbose) {
            log.timer(source, message, beginStamp);
        }
    }
}
