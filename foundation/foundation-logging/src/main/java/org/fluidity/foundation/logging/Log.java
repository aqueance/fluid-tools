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

import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.foundation.Logging;

/**
 * Static access to a logging instance.
 */
public final class Log {

    private static AtomicReference<Logging> log = new AtomicReference<Logging>(new StandardOutLogging(null));

    public static void reset(final ComponentContainer container) {
        final Logging log = container.getComponent(Logging.class);

        if (log != null) {
            Log.log.set(log);
        }
    }

    public static boolean isTraceEnabled(final Class source) {
        assert log.get() != null : Logging.class;
        return log.get().isTraceEnabled(source);
    }

    public static void trace(final Class source, final String message) {
        assert log.get() != null : Logging.class;
        log.get().trace(source, message);
    }

    public static void trace(final Class source, final String message, final Throwable exception) {
        assert log.get() != null : Logging.class;
        log.get().trace(source, message, exception);
    }

    public static void debug(final Class source, final String message) {
        assert log.get() != null : Logging.class;
        log.get().debug(source, message);
    }

    public static void debug(final Class source, final String message, final Throwable exception) {
        assert log.get() != null : Logging.class;
        log.get().debug(source, message, exception);
    }

    public static void info(final Class source, final String message) {
        assert log.get() != null : Logging.class;
        log.get().info(source, message);
    }

    public static void warning(final Class source, final String message) {
        assert log.get() != null : Logging.class;
        log.get().warning(source, message);
    }

    public static void warning(final Class source, final String message, final Throwable exception) {
        assert log.get() != null : Logging.class;
        log.get().warning(source, message, exception);
    }

    public static void error(final Class source, final String message) {
        assert log.get() != null : Logging.class;
        log.get().error(source, message);
    }

    public static void error(final Class source, final String message, final Throwable exception) {
        assert log.get() != null : Logging.class;
        log.get().error(source, message, exception);
    }

    public static void fatal(final Class source, final String message) {
        assert log.get() != null : Logging.class;
        log.get().fatal(source, message);
    }

    public static void fatal(final Class source, final String message, final Throwable exception) {
        assert log.get() != null : Logging.class;
        log.get().fatal(source, message, exception);
    }

    public static void timer(final Class source, final String message, final long beginStamp) {
        assert log.get() != null : Logging.class;
        log.get().timer(source, message, beginStamp);
    }
}
