/*
 * Copyright (c) 2006-2009 Tibor Adam Varga (tibor.adam.varga on gmail)
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
