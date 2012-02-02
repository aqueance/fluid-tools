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

package org.fluidity.features.impl;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.spi.CustomComponentFactory;
import org.fluidity.features.ReloadingLog;
import org.fluidity.features.Updates;
import org.fluidity.foundation.Configuration;
import org.fluidity.foundation.Log;

/**
 * @author Tibor Varga
 */
@Component(api = ReloadingLog.class)
@Component.Context(Component.Reference.class)
final class ReloadingLogFactory implements CustomComponentFactory {

    public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
        dependencies.discover(RefreshedLogImpl.class);

        return new Instance() {

            @SuppressWarnings("unchecked")
            public void bind(final Registry registry) throws ComponentContainer.BindingException {
                registry.bindComponent(RefreshedLogImpl.class);
            }
        };
    }

    /**
     * @author Tibor Varga
     */
    @Component(automatic = false)
    static final class RefreshedLogImpl<T> implements ReloadingLog<T> {

        private final Updates.Snapshot<Log<T>> log;

        public RefreshedLogImpl(final Log<T> log, final Updates updates, final Configuration<Settings> configuration) {
            this.log = updates.register(configuration.settings().period(), new Updates.Snapshot<Log<T>>() {
                public Log<T> get() {
                    log.refresh();
                    return log;
                }
            });
        }

        public void refresh() {
            // ignore
        }

        public final boolean isTraceEnabled() {
            return log.get().isTraceEnabled();
        }

        public final boolean isDebugEnabled() {
            return log.get().isDebugEnabled();
        }

        public final boolean isInfoEnabled() {
            return log.get().isInfoEnabled();
        }

        public final boolean isWarningEnabled() {
            return log.get().isWarningEnabled();
        }

        public void trace(final String format, final Object... args) {
            log.get().trace(format, args);
        }

        public void debug(final String format, final Object... args) {
            log.get().debug(format, args);
        }

        public void info(final String format, final Object... args) {
            log.get().info(format, args);
        }

        public void warning(final String format, final Object... args) {
            log.get().warning(format, args);
        }

        public void error(final String format, final Object... args) {
            log.get().error(format, args);
        }

        public void trace(final Throwable exception, final String format, final Object... args) {
            log.get().trace(exception, format, args);
        }

        public void debug(final Throwable exception, final String format, final Object... args) {
            log.get().debug(exception, format, args);
        }

        public void info(final Throwable exception, final String format, final Object... args) {
            log.get().info(exception, format, args);
        }

        public void warning(final Throwable exception, final String format, final Object... args) {
            log.get().warning(exception, format, args);
        }

        public void error(final Throwable exception, final String format, final Object... args) {
            log.get().error(exception, format, args);
        }
    }

    /**
     * Log permission refresh period settings.
     */
    private interface Settings {

        /**
         * Returns the period in milliseconds during which at most one log level check takes place per logger. May
         * be 0 or negative, in which case no periodic log level check will take place.
         *
         * @return the period in milliseconds.
         */
        @Configuration.Property(key = ReloadingLog.LOG_LEVEL_REFRESH_PERIOD, undefined = "30000")
        long period();
    }
}
