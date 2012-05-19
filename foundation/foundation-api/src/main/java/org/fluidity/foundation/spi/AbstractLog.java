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

package org.fluidity.foundation.spi;

import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.foundation.Log;

/**
 * The recommended super class for log implementations.
 * <p/>
 * The intended boiler-plate code for a subclass implementation, with implementation specific bits highlighted, is as
 * follows:
 * <pre>
 * import <b>external.logging.Log</b>;
 * import <b>external.logging.LogFactory</b>;
 *
 * final class MyLogImpl extends AbstractLog&lt;<b>Log</b>> {
 *
 *     public MyLogImpl(final Class&lt;?> source) {
 *         super(LoggerFactory.getLogger(source), new Levels&lt;<b>Log</b>>() {
 *             public boolean trace(final <b>Log</b> log) {
 *                 return log.<b>isTraceEnabled</b>();
 *             }
 *
 *             public boolean debug(final <b>Log</b> log) {
 *                 return log.<b>isDebugEnabled</b>();
 *             }
 *
 *             public boolean info(final <b>Log</b> log) {
 *                 return log.<b>isInfoEnabled</b>();
 *             }
 *
 *             public boolean warning(final <b>Log</b> log) {
 *                 return log.<b>isWarnEnabled</b>();
 *             }
 *         });
 *     }
 *
 *     public void trace(final String message, final Object... args) {
 *         if (isTraceEnabled()) {
 *             log.<b>trace</b>(String.format(message, args));
 *         }
 *     }
 *
 *     ...
 * }
 * </pre>
 * <p/>
 * The recommended {@link LogFactory} implementation, with implementation specific bits highlighted, is as follows:
 * <pre>
 * &#64;Component
 * &#64;ServiceProvider
 * final class MyLogFactory implements LogFactory {
 *
 *     public Log createLog(final Class&lt;?> source) {
 *         return new <b>MyLogImpl</b>(source);
 *     }
 * }
 * </pre>
 *
 * @param <T> the underlying log type.
 * @author Tibor Varga
 */
public abstract class AbstractLog<T> implements Log<T> {

    /**
     * The external log instance passed to the constructor.
     */
    protected final T log;

    private final Levels<T> levels;
    private final AtomicReference<Levels<T>> permissions = new AtomicReference<Levels<T>>();

    /**
     * Creates a log instance.
     *
     * @param log    the external log type.
     * @param levels the flags permitting emission of log messages at various levels.
     */
    protected AbstractLog(final T log, final Levels<T> levels) {
        this.log = log;
        this.levels = levels;

        refresh();
    }

    /**
     * {@inheritDoc}
     */
    public final void refresh() {
        this.permissions.set(new Levels<T>() {

            private final boolean trace = levels.trace(log);
            private final boolean debug = levels.debug(log);
            private final boolean info = levels.info(log);
            private final boolean warning = levels.warning(log);

            public boolean trace(final T log) {
                return trace;
            }

            public boolean debug(final T log) {
                return debug;
            }

            public boolean info(final T log) {
                return info;
            }

            public boolean warning(final T log) {
                return warning;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isTraceEnabled() {
        return permissions.get().trace(log);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isDebugEnabled() {
        return permissions.get().debug(log);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isInfoEnabled() {
        return permissions.get().info(log);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isWarningEnabled() {
        return permissions.get().info(log);
    }

    /**
     * Queries the various log levels.
     */
    public static interface Levels<T> {

        /**
         * Returns <code>true</code> if trace level logging is enabled, <code>false</code> otherwise.
         *
         * @param log the underlying logger object.
         *
         * @return the flag for the log level.
         */
        boolean trace(T log);

        /**
         * Returns <code>true</code> if debug level logging is enabled, <code>false</code> otherwise.
         *
         * @param log the underlying logger object.
         *
         * @return the flag for the log level.
         */
        boolean debug(T log);

        /**
         * Returns <code>true</code> if info level logging is enabled, <code>false</code> otherwise.
         *
         * @param log the underlying logger object.
         *
         * @return the flag for the log level.
         */
        boolean info(T log);

        /**
         * Returns <code>true</code> if warning level logging is enabled, <code>false</code> otherwise.
         *
         * @param log the underlying logger object.
         *
         * @return the flag for the log level.
         */
        boolean warning(T log);
    }
}
