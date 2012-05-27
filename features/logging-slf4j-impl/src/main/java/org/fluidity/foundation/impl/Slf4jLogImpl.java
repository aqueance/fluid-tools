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

import org.fluidity.foundation.spi.AbstractLog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses SLF4J as the underlying logging framework.
 */
final class Slf4jLogImpl<T> extends AbstractLog<Logger, T> {

    public Slf4jLogImpl(final Class<?> source) {
        super(LoggerFactory.getLogger(source), new Levels<Logger>() {
            public boolean trace(final Logger log) {
                return log.isTraceEnabled();
            }

            public boolean debug(final Logger log) {
                return log.isDebugEnabled();
            }

            public boolean info(final Logger log) {
                return log.isInfoEnabled();
            }

            public boolean warning(final Logger log) {
                return log.isWarnEnabled();
            }
        });
    }

    public void trace(final String format, final Object... args) {
        if (isTraceEnabled()) {
            log.trace(String.format(format, args));
        }
    }

    public void debug(final String format, final Object... args) {
        if (isDebugEnabled()) {
            log.debug(String.format(format, args));
        }
    }

    public void info(final String format, final Object... args) {
        if (isInfoEnabled()) {
            log.info(String.format(format, args));
        }
    }

    public void warning(final String format, final Object... args) {
        if (isWarningEnabled()) {
            log.warn(String.format(format, args));
        }
    }

    public void error(final String format, final Object... args) {
        log.error(String.format(format, args));
    }

    public void trace(final Throwable exception, final String format, final Object... args) {
        if (isTraceEnabled()) {
            log.trace(String.format(format, args), exception);
        }
    }

    public void debug(final Throwable exception, final String format, final Object... args) {
        if (isDebugEnabled()) {
            log.debug(String.format(format, args), exception);
        }
    }

    public void info(final Throwable exception, final String format, final Object... args) {
        if (isInfoEnabled()) {
            log.info(String.format(format, args), exception);
        }
    }

    public void warning(final Throwable exception, final String format, final Object... args) {
        if (isWarningEnabled()) {
            log.warn(String.format(format, args), exception);
        }
    }

    public void error(final Throwable exception, final String format, final Object... args) {
        log.error(String.format(format, args), exception);
    }
}
