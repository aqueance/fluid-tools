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

package org.fluidity.foundation;

import org.fluidity.composition.Component;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Log factory that to produce loggers that gobble up messages. This log implementation is meant to be used when you don't want any log message actually
 * emitted, e.g., in test cases.
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

        public boolean isWarningEnabled() {
            return false;
        }

        public void trace(final String format, final Object... args) {
            // empty
        }

        public void debug(final String format, final Object... args) {
            // empty
        }

        public void info(final String format, final Object... args) {
            // empty
        }

        public void warning(final String format, final Object... args) {
            // empty
        }

        public void error(final String format, final Object... args) {
            // empty
        }

        public void trace(final Throwable exception, final String format, final Object... args) {
            // empty
        }

        public void debug(final Throwable exception, final String format, final Object... args) {
            // empty
        }

        public void info(final Throwable exception, final String format, final Object... args) {
            // empty
        }

        public void warning(final Throwable exception, final String format, final Object... args) {
            // empty
        }

        public void error(final Throwable exception, final String format, final Object... args) {
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
    @SuppressWarnings("unchecked")
    public <T> Log<T> createLog(final Class<T> ignored) {
        return (Log<T>) sink;
    }

    /**
     * Returns the log sink for the given class.
     *
     * @param ignored the class to use the log sink.
     * @param <T>     the type of the class.
     *
     * @return the log sink.
     */
    @SuppressWarnings({ "unchecked", "UnusedParameters" })
    public static <T> Log<T> consume(final Class<T> ignored) {
        return (Log<T>) sink;
    }
}
