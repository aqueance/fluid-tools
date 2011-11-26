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

package org.fluidity.foundation;

import org.fluidity.composition.Component;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Log factory that gobbles up messages. This log implementation is meant to be used in test cases.
 *
 * @author Tibor Varga
 */
@Component
@ServiceProvider
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

    /**
     * Default constructor.
     */
    public NoLogFactory() { }

    /**
     * {@inheritDoc}
     */
    public Log createLog(final Class<?> ignored) {
        return sink;
    }
}
