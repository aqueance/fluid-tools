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

import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Returns a {@link Log} object that simply gobbles up messages.
 *
 * @author Tibor Varga
 */
public final class NoLogFactory implements LogFactory {

    /**
     * A log sink that gobbles up all log messages.
     */
    public static final Log sink = new Log() {
        public boolean isTraceEnabled() {
            return false;
        }

        public boolean isDebugEnabled() {
            return false;
        }

        public boolean isInfoEnabled() {
            return false;
        }

        public void trace(final String message, final Object... args) {
            // empty
        }

        public void debug(final String message, final Object... args) {
            // empty
        }

        public void info(final String message, final Object... args) {
            // empty
        }

        public void warning(final String message, final Object... args) {
            // empty
        }

        public void error(final String message, final Object... args) {
            // empty
        }

        public void trace(final Throwable exception, final String message, final Object... args) {
            // empty
        }

        public void debug(final Throwable exception, final String message, final Object... args) {
            // empty
        }

        public void info(final Throwable exception, final String message, final Object... args) {
            // empty
        }

        public void warning(final Throwable exception, final String message, final Object... args) {
            // empty
        }

        public void error(final Throwable exception, final String message, final Object... args) {
            // empty
        }
    };

    public Log createLog(final Class<?> ignored) {
        return sink;
    }
}
