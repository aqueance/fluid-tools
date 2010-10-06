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
package org.fluidity.foundation;

/**
 * Provides facilities to log debug, error, etc. messages to a log sink.
 *
 * @author Tibor Varga
 */
public interface Logging {

    boolean isTraceEnabled(Class source);

    boolean isDebugEnabled(Class source);

    void trace(Class source, String message);

    void trace(Class source, String message, Throwable exception);

    void debug(Class source, String message);

    void debug(Class source, String message, Throwable exception);

    void info(Class source, String message);

    void warning(Class source, String message);

    void warning(Class source, String message, Throwable exception);

    void error(Class source, String message);

    void error(Class source, String message, Throwable exception);

    void fatal(Class source, String message);

    void fatal(Class source, String message, Throwable exception);

    void timer(Class source, String message, long beginStamp);
}
