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

package org.fluidity.foundation.impl;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.fluidity.foundation.spi.LogAdapter;

/**
 * Uses <code>java.util.logging</code> as the underlying logging framework.
 */
final class JavaLogImpl<T> extends LogAdapter<Logger, T> {

    JavaLogImpl(final LogManager loggers, final Class<?> source) {
        super(loggers.getLogger(source.getName()));
    }

    @Override
    protected Levels levels() {
        return new Levels() {
            public boolean trace() {
                return log.isLoggable(Level.FINEST);
            }

            public boolean debug() {
                return log.isLoggable(Level.FINE);
            }

            public boolean info() {
                return log.isLoggable(Level.INFO);
            }

            public boolean warning() {
                return log.isLoggable(Level.WARNING);
            }
        };
    }

    public void trace(final String format, final Object... args) {
        if (permissions.trace()) {
            log.log(Level.FINEST, String.format(format, args));
        }
    }

    public void debug(final String format, final Object... args) {
        if (permissions.debug()) {
            log.log(Level.FINE, String.format(format, args));
        }
    }

    public void info(final String format, final Object... args) {
        if (permissions.info()) {
            log.log(Level.INFO, String.format(format, args));
        }
    }

    public void warning(final String format, final Object... args) {
        if (permissions.info()) {
            log.log(Level.WARNING, String.format(format, args));
        }
    }

    public void error(final String format, final Object... args) {
        log.log(Level.SEVERE, String.format(format, args));
    }

    public void trace(final Throwable exception, final String format, final Object... args) {
        if (permissions.trace()) {
            log.log(Level.FINEST, String.format(format, args), exception);
        }
    }

    public void debug(final Throwable exception, final String format, final Object... args) {
        if (permissions.debug()) {
            log.log(Level.FINE, String.format(format, args), exception);
        }
    }

    public void info(final Throwable exception, final String format, final Object... args) {
        if (permissions.info()) {
            log.log(Level.INFO, String.format(format, args), exception);
        }
    }

    public void warning(final Throwable exception, final String format, final Object... args) {
        if (permissions.info()) {
            log.log(Level.WARNING, String.format(format, args), exception);
        }
    }

    public void error(final Throwable exception, final String format, final Object... args) {
        log.log(Level.SEVERE, String.format(format, args), exception);
    }
}
