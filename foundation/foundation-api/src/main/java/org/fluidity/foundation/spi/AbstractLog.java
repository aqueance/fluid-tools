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

import org.fluidity.foundation.Log;

/**
 * The recommended super class for log implementations.
 * <p/>
 * The intended boiler-plate code for a subclass implementation, with implementation specific bits <b>highlighted</b>, is as follows:
 * <pre>
 * import <b>external.logging.Log</b>;
 * import <b>external.logging.LogFactory</b>;
 *
 * final class <b>MyLogImpl</b> extends <span class="hl1">AbstractLog</span>&lt;<b>Log</b>> {
 *
 *   public <b>MyLogImpl</b>(final Class&lt;?> source) {
 *     <span class="hl1">super</span>(<b>LoggerFactory</b>.<b>getLogger</b>(source), new <span class="hl1">Levels</span>&lt;<b>Log</b>>() {
 *       public boolean <span class="hl1">trace</span>(final <b>Log</b> log) {
 *         return log.<b>isTraceEnabled</b>();
 *       }
 *
 *       public boolean <span class="hl1">debug</span>(final <b>Log</b> log) {
 *         return log.<b>isDebugEnabled</b>();
 *       }
 *
 *       public boolean <span class="hl1">info</span>(final <b>Log</b> log) {
 *         return log.<b>isInfoEnabled</b>();
 *       }
 *
 *       public boolean <span class="hl1">warning</span>(final <b>Log</b> log) {
 *         return log.<b>isWarnEnabled</b>();
 *       }
 *     });
 *   }
 *
 *   public void trace(final String message, final Object... args) {
 *     if (<span class="hl1">permissions.trace</span>) {
 *       <span class="hl1">log</span>.<b>trace</b>(message, args);
 *     }
 *   }
 *
 *   &hellip;
 * }
 * </pre>
 * <p/>
 * The recommended {@link LogFactory} implementation, with implementation specific bits <b>highlighted</b>, is as follows:
 * <pre>
 * {@linkplain org.fluidity.composition.Component @Component}
 * {@linkplain org.fluidity.composition.ServiceProvider @ServiceProvider}
 * final class <b>MyLogFactory</b> implements <span class="hl1">{@linkplain LogFactory}</span> {
 *
 *   public {@linkplain Log} <span class="hl1">createLog</span>(final Class&lt;?> source) {
 *     return new <b>MyLogImpl</b>(source);
 *   }
 * }
 * </pre>
 *
 * @param <T> the underlying log type.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
public abstract class AbstractLog<T, L> implements Log<L> {

    /**
     * The external log instance passed to the constructor.
     */
    protected final T log;

    /**
     * Flags for the various log level permissions.
     */
    protected final Permissions permissions;

    /**
     * Creates a log instance.
     *
     * @param log    the external log type.
     * @param levels the flags permitting emission of log messages at various levels.
     */
    protected AbstractLog(final T log, final Levels<T> levels) {
        this.log = log;
        this.permissions = new Permissions(levels, log);
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isTraceEnabled() {
        return permissions.trace;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isDebugEnabled() {
        return permissions.debug;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isInfoEnabled() {
        return permissions.info;
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isWarningEnabled() {
        return permissions.info;
    }

    /**
     * Queries the various log levels. Subclasses of {@link AbstractLog} provide an implementation of this interface to show how to query the various log
     * levels in the adapted logging framework.
     * <h3>Usage</h3>
     * See {@link AbstractLog}.
     *
     * @author Tibor Varga
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

    /**
     * Convenience flags to be used in {@link AbstractLog} subclasses to query the enabled log levels.
     * <h3>Usage</h3>
     * See {@link AbstractLog}.
     *
     * @author Tibor Varga
     */
    protected static final class Permissions {

        /**
         * <code>true</code> if trace level logging is enabled; <code>false</code> otherwise.
         */
        public final boolean trace;

        /**
         * <code>true</code> if debug level logging is enabled; <code>false</code> otherwise.
         */
        public final boolean debug;

        /**
         * <code>true</code> if info level logging is enabled; <code>false</code> otherwise.
         */
        public final boolean info;

        /**
         * <code>true</code> if warning level logging is enabled; <code>false</code> otherwise.
         */
        public final boolean warning;

        <T> Permissions(final Levels<T> levels, final T log) {
            trace = levels.trace(log);
            debug = levels.debug(log);
            info = levels.info(log);
            warning = levels.warning(log);
        }
    }
}
