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

import org.fluidity.composition.ComponentContainerAccess;
import org.fluidity.foundation.Logging;

/**
 * Static access to a logging instance.
 */
public final class Log {

    private static Logging log;

    static {
        Logging log = null;

        try {
            log = new ComponentContainerAccess().getComponent(Logging.class);
        } catch (final AssertionError e) {
            // empty
        } catch (final NoClassDefFoundError e) {
            // empty
        } catch (final Exception e) {
            // empty
        }

        Log.log = log == null ? new DefaultLogging(null) : log;
    }

    public static boolean isTraceEnabled(final Class source) {
        assert log != null : Logging.class;
        return log.isTraceEnabled(source);
    }

    public static void trace(final Class source, final String message) {
        assert log != null : Logging.class;
        log.trace(source, message);
    }

    public static void trace(final Class source, final String message, final Throwable exception) {
        assert log != null : Logging.class;
        log.trace(source, message, exception);
    }

    public static void debug(final Class source, final String message) {
        assert log != null : Logging.class;
        log.debug(source, message);
    }

    public static void debug(final Class source, final String message, final Throwable exception) {
        assert log != null : Logging.class;
        log.debug(source, message, exception);
    }

    public static void info(final Class source, final String message) {
        assert log != null : Logging.class;
        log.info(source, message);
    }

    public static void warning(final Class source, final String message) {
        assert log != null : Logging.class;
        log.warning(source, message);
    }

    public static void warning(final Class source, final String message, final Throwable exception) {
        assert log != null : Logging.class;
        log.warning(source, message, exception);
    }

    public static void error(final Class source, final String message) {
        assert log != null : Logging.class;
        log.error(source, message);
    }

    public static void error(final Class source, final String message, final Throwable exception) {
        assert log != null : Logging.class;
        log.error(source, message, exception);
    }

    public static void fatal(final Class source, final String message) {
        assert log != null : Logging.class;
        log.fatal(source, message);
    }

    public static void fatal(final Class source, final String message, final Throwable exception) {
        assert log != null : Logging.class;
        log.fatal(source, message, exception);
    }

    public static void timer(final Class source, final String message, final long beginStamp) {
        assert log != null : Logging.class;
        log.timer(source, message, beginStamp);
    }
}
