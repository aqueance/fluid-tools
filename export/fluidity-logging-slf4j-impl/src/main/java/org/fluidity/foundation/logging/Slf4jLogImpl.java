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

package org.fluidity.foundation.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses commons-logging as the underlying logging framework.
 */
final class Slf4jLogImpl implements Log {

    private final Logger log;

    Slf4jLogImpl(final Class<?> source) {
        this.log = LoggerFactory.getLogger(source);
    }

    public boolean isTraceEnabled() {
        return log.isTraceEnabled();
    }

    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    public void trace(final String message, final Object... args) {
        log.trace(String.format(message, args));
    }

    public void debug(final String message, final Object... args) {
        log.debug(String.format(message, args));
    }

    public void info(final String message, final Object... args) {
        log.info(String.format(message, args));
    }

    public void warning(final String message, final Object... args) {
        log.warn(String.format(message, args));
    }

    public void error(final String message, final Object... args) {
        log.error(String.format(message, args));
    }

    public void trace(final Throwable exception, final String message, final Object... args) {
        log.trace(String.format(message, args), exception);
    }

    public void debug(final Throwable exception, final String message, final Object... args) {
        log.debug(String.format(message, args), exception);
    }

    public void info(final Throwable exception, final String message, final Object... args) {
        log.info(String.format(message, args), exception);
    }

    public void warning(final Throwable exception, final String message, final Object... args) {
        log.warn(String.format(message, args), exception);
    }

    public void error(final Throwable exception, final String message, final Object... args) {
        log.error(String.format(message, args), exception);
    }

    public void timer(final String message, final long beginMillis) {
        info("%s took %d ms", message, +(System.currentTimeMillis() - beginMillis));
    }
}
