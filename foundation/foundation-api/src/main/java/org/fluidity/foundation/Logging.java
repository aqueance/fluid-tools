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
package org.fluidity.foundation;

/**
 * Provides facilities to log debug, error, etc. messages to a log sink.
 *
 * @author Tibor Varga
 */
public interface Logging {

    boolean isTraceEnabled(final Class source);

    void trace(final Class source, final String message);

    void trace(final Class source, final String message, final Throwable exception);

    void debug(final Class source, final String message);

    void debug(final Class source, final String message, final Throwable exception);

    void info(final Class source, final String message);

    void warning(final Class source, final String message);

    void warning(final Class source, final String message, final Throwable exception);

    void error(final Class source, final String message);

    void error(final Class source, final String message, final Throwable exception);

    void fatal(final Class source, final String message);

    void fatal(final Class source, final String message, final Throwable exception);

    void timer(final Class source, final String message, long beginStamp);
}
