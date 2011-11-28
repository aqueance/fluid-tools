/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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
final class Slf4jLogImpl extends AbstractLog<Logger> {

    public Slf4jLogImpl(final Class<?> source, final Levels.Snapshots snapshots) {
        super(snapshots, LoggerFactory.getLogger(source), new Levels.Factory<Logger>() {
            public Levels create(final Logger log) {
                return new Levels() {
                    public boolean trace() {
                        return log.isTraceEnabled();
                    }

                    public boolean debug() {
                        return log.isDebugEnabled();
                    }

                    public boolean info() {
                        return log.isInfoEnabled();
                    }

                    public boolean warning() {
                        return log.isWarnEnabled();
                    }
                };
            }
        });
    }

    public void trace(final String message, final Object... args) {
        if (isTraceEnabled()) {
            log.trace(String.format(message, args));
        }
    }

    public void debug(final String message, final Object... args) {
        if (isDebugEnabled()) {
            log.debug(String.format(message, args));
        }
    }

    public void info(final String message, final Object... args) {
        if (isInfoEnabled()) {
            log.info(String.format(message, args));
        }
    }

    public void warning(final String message, final Object... args) {
        if (isWarningEnabled()) {
            log.warn(String.format(message, args));
        }
    }

    public void error(final String message, final Object... args) {
        log.error(String.format(message, args));
    }

    public void trace(final Throwable exception, final String message, final Object... args) {
        if (isTraceEnabled()) {
            log.trace(String.format(message, args), exception);
        }
    }

    public void debug(final Throwable exception, final String message, final Object... args) {
        if (isDebugEnabled()) {
            log.debug(String.format(message, args), exception);
        }
    }

    public void info(final Throwable exception, final String message, final Object... args) {
        if (isInfoEnabled()) {
            log.info(String.format(message, args), exception);
        }
    }

    public void warning(final Throwable exception, final String message, final Object... args) {
        if (isWarningEnabled()) {
            log.warn(String.format(message, args), exception);
        }
    }

    public void error(final Throwable exception, final String message, final Object... args) {
        log.error(String.format(message, args), exception);
    }
}
