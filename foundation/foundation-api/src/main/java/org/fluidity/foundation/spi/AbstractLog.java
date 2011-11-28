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

package org.fluidity.foundation.spi;

import org.fluidity.foundation.Configuration;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.Updates;

/**
 * The recommended super class for log implementations. This class provides periodic log level checking functionality, which is configured by the
 * {@link Settings} configuration interface.
 * <p/>
 * The intended boiler-plate code for a subclass implementation, with implementation specific bits
 * highlighted, is as follows:
 * <pre>
 * import external.logging.<b>Log</b>;
 * import external.logging.<b>LogFactory</b>;
 *
 * final class MyLogImpl extends AbstractLog&lt;<b>Log</b>> {
 *
 *     public MyLogImpl(final Class&lt;?> source, final Levels.Snapshot snapshots) {
 *         super(snapshots, <b>LogFactory.getLog</b>(source), new Levels.Factory&lt;<b>Log</b>>() {
 *             public Levels create(final <b>Log</b> log) {
 *                 return new Level() {
 *                     public boolean trace() {
 *                         return log.<b>isTraceEnabled</b>();
 *                     }
 *
 *                     public boolean debug() {
 *                         return log.<b>isDebugEnabled</b>();
 *                     }
 *
 *                     public boolean info() {
 *                         return log.<b>isInfoEnabled</b>();
 *                     }
 *
 *                     public boolean warning() {
 *                         return log.<b>isWarnEnabled</b>();
 *                     }
 *                 };
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
 * The complex constructor boiler-plate is necessary because of the restrictions placed on constructors with respect to accessing instance fields.
 * <p/>
 * The recommended {@link LogFactory} implementation, with implementation specific bits highlighted, is as follows:
 * <pre>
 * &#64;Component
 * &#64;ServiceProvider
 * final class MyLogFactory implements LogFactory {
 *
 *     public Log createLog(final Class&lt;?> source) {
 *         return createLog(source, null);
 *     }
 *
 *     public Log createLog(final Class&lt;?> source, final AbstractLog.Levels.Snapshots levels) {
 *         return new <b>MyLogImpl</b>(source, levels);
 *     }
 * }
 * </pre>
 *
 * @param <T> the external log type.
 *
 * @author Tibor Varga
 */
public abstract class AbstractLog<T> implements Log {

    /**
     * The configuration property that specifies the number of milliseconds during which at most one log level check is performed per logger.
     */
    public static final String LOG_LEVEL_CHECK_PERIOD = "org.fluidity.log.level.check.period.ms";

    /**
     * The external log instance passed to the constructor.
     */
    protected final T log;

    private final Updates.Snapshot<Levels> levels;

    /**
     * Creates a log instance.
     *
     * @param snapshots the log level snapshot factory; may be <code>null</code>, in which case no period log level check will take place.
     * @param log       the external log type.
     * @param factory    the log levels factory.
     */
    protected AbstractLog(final Levels.Snapshots snapshots, final T log, final Levels.Factory<T> factory) {
        this.log = log;

        final Levels levels = factory.create(log);
        this.levels = (snapshots != null ? snapshots.create(levels) : new Updates.Snapshot<Levels>() {
            public Levels get() {
                return levels;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isTraceEnabled() {
        return levels.get().trace();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isDebugEnabled() {
        return levels.get().debug();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isInfoEnabled() {
        return levels.get().info();
    }

    /**
     * {@inheritDoc}
     */
    public final boolean isWarningEnabled() {
        return levels.get().info();
    }

    /**
     * Log settings. Never instantiated directly, this interface is used through the {@link org.fluidity.foundation.Configuration} mechanism.
     */
    public static interface Settings {

        /**
         * Returns the period in milliseconds during which at most one log level check takes place per logger. May be 0 or negative, in which case no periodic
         * log level check will take place.
         *
         * @return the period in milliseconds.
         */
        @Configuration.Property(key = LOG_LEVEL_CHECK_PERIOD)
        long period();
    }

    /**
     * Returns the various log levels. Used by the <code>Factory</code> parameter of {@link AbstractLog#AbstractLog(Snapshots, Object, Levels.Factory)}. See
     * {@link AbstractLog} for use pattern.
     */
    public static interface Levels {

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

        /**
         * Produces log levels objects for some log implementation. See {@link AbstractLog} for use pattern.
         *
         * @param <T> the log implementation class.
         */
        interface Factory<T> {

            /**
             * Return a log levels object for the given log implementation.
             *
             * @param log the log implementation.
             *
             * @return a log levels object for the given log implementation.
             */
            Levels create(T log);
        }

        /**
         * Produces update snapshots. See {@link AbstractLog} for use pattern.
         */
        interface Snapshots {

            /**
             * Returns a new update snapshots object with the given log levels loader.
             *
             * @param levels the loader to use to refresh log levels.
             *
             * @return a new update snapshots object
             */
            Updates.Snapshot<Levels> create(Levels levels);
        }
    }
}
