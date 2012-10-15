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
 * final class <b>MyLogImpl</b>&lt;T> extends <span class="hl1">{@linkplain LogAdapter}</span>&lt;<b>Log</b>, T> {
 *
 *   <b>MyLogImpl</b>(final Class&lt;T> source) {
 *     <span class="hl1">super</span>(<b>LoggerFactory</b>.<b>getLogger</b>(source.getName()));
 *   }
 *
 *   protected Levels levels() {
 *     return new <span class="hl1">{@linkplain org.fluidity.foundation.spi.LogAdapter.Levels Levels}</span>&lt;<b>Log</b>>() {
 *       public boolean <span class="hl1">trace</span>() {
 *         return <span class="hl1">log</span>.<b>isTraceEnabled</b>();
 *       }
 *
 *       public boolean <span class="hl1">debug</span>() {
 *         return <span class="hl1">log</span>.<b>isDebugEnabled</b>();
 *       }
 *
 *       public boolean <span class="hl1">info</span>() {
 *         return <span class="hl1">log</span>.<b>isInfoEnabled</b>();
 *       }
 *
 *       public boolean <span class="hl1">warning</span>() {
 *         return <span class="hl1">log</span>.<b>isWarnEnabled</b>();
 *       }
 *     };
 *   }
 *
 *   public void trace(final String message, final Object... args) {
 *     if (<span class="hl1">{@linkplain LogAdapter#permissions}.trace()</span>) {
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
 * import org.fluidity.foundation.Log;
 * import org.fluidity.foundation.LogFactory;
 *
 * {@linkplain org.fluidity.composition.Component @Component}
 * {@linkplain org.fluidity.composition.ServiceProvider @ServiceProvider}
 * final class <b>MyLogFactory</b> implements <span class="hl1">{@linkplain LogFactory}</span> {
 *
 *   public &lt;T> {@linkplain Log}&lt;T> <span class="hl1">createLog</span>(final Class&lt;T> source) {
 *     return new <b>MyLogImpl</b>&lt;T>(source);
 *   }
 * }
 * </pre>
 *
 * @param <T> the underlying log type.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
public abstract class LogAdapter<T, L> implements Log<L> {

    /**
     * The external log instance passed to the constructor.
     */
    protected final T log;

    /**
     * Flags for the various log level permissions.
     */
    protected final Levels permissions;

    /**
     * Creates a log instance.
     *
     * @param log the external log type.
     */
    protected LogAdapter(final T log) {
        this(DYNAMIC, log);
    }

    /**
     * Used internally to test dynamic log levels.
     *
     * @param dynamic tells whether to support dynamic log levels or not.
     * @param log     the external log type.
     */
    LogAdapter(boolean dynamic, final T log) {
        this.log = log;
        this.permissions = dynamic ? new DynamicLevels(levels()) : new StaticLevels(levels());
    }

    /**
     * Returns the logging framework specific adapter that queries the log levels of the encapsulated logger. Do <i>not</i> call this from concrete subclasses.
     * Use the {@link #permissions} variable instead.
     *
     * @return the logging framework specific adapter that queries the log levels of the encapsulated logger.
     */
    protected abstract Levels levels();

    /**
     * {@inheritDoc}
     */
    public final boolean isTraceEnabled() {
        return permissions.trace();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isDebugEnabled() {
        return permissions.debug();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isInfoEnabled() {
        return permissions.info();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isWarningEnabled() {
        return permissions.warning();
    }

    /**
     * Queries the various log levels. Subclasses of {@link LogAdapter} provide an implementation of this interface to show how to query the various log
     * levels in the adapted logging framework.
     * <h3>Usage</h3>
     * See {@link LogAdapter}.
     *
     * @author Tibor Varga
     */
    public interface Levels {

        /**
         * Returns <code>true</code> if trace level logging is enabled, <code>false</code> otherwise.
         *
         * @return the flag for the log level.
         */
        boolean trace();

        /**
         * Returns <code>true</code> if debug level logging is enabled, <code>false</code> otherwise.
         *
         * @return the flag for the log level.
         */
        boolean debug();

        /**
         * Returns <code>true</code> if info level logging is enabled, <code>false</code> otherwise.
         *
         * @return the flag for the log level.
         */
        boolean info();

        /**
         * Returns <code>true</code> if warning level logging is enabled, <code>false</code> otherwise.
         *
         * @return the flag for the log level.
         */
        boolean warning();
    }

    /**
     * Caches the log levels once queried.
     *
     * @author Tibor Varga
     */
    private static class StaticLevels implements Levels {

        private final boolean trace;
        private final boolean debug;
        private final boolean info;
        private final boolean warning;

        StaticLevels(final Levels levels) {
            assert levels != null;
            trace = levels.trace();
            debug = levels.debug();
            info = levels.info();
            warning = levels.warning();
        }

        public boolean trace() {
            return trace;
        }

        public boolean debug() {
            return debug;
        }

        public boolean info() {
            return info;
        }

        public boolean warning() {
            return warning;
        }
    }

    /**
     * Always queries the log levels.
     *
     * @author Tibor Varga
     */
    private static class DynamicLevels implements Levels {

        private final Levels levels;

        DynamicLevels(final Levels levels) {
            assert levels != null;
            this.levels = levels;
        }

        public boolean trace() {
            return levels.trace();
        }

        public boolean debug() {
            return levels.debug();
        }

        public boolean info() {
            return levels.info();
        }

        public boolean warning() {
            return levels.warning();
        }
    }
}
