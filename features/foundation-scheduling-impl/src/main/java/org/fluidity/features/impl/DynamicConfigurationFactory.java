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

package org.fluidity.features.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.features.DynamicConfiguration;
import org.fluidity.features.Updates;
import org.fluidity.foundation.Configuration;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Proxies;

import static org.fluidity.foundation.Command.Process;

/**
 * @author Tibor Varga
 */
@Component(api = DynamicConfiguration.class)
@Component.Context(Component.Reference.class)
final class DynamicConfigurationFactory implements ComponentFactory {

    public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {
        final Component.Reference reference = context.annotation(Component.Reference.class, DynamicConfiguration.class);
        final Class<?> api = reference.parameter(0);

        dependencies.discover(DynamicConfigurationImpl.class);

        for (final Method method : api.getMethods()) {
            if (method.getParameterTypes().length > 0) {
                throw new ComponentContainer.ResolutionException("Cannot load snapshot of %s: method %s has parameters", api, method);
            }
        }

        return new Instance() {

            @SuppressWarnings("unchecked")
            public void bind(final Registry registry) throws Exception {
                registry.bindInstance(api, Class.class);
                registry.bindComponent(DynamicConfigurationImpl.class);
            }
        };
    }

    @Component(automatic = false)
    @Component.Context(Component.Reference.class)
    private static class DynamicConfigurationImpl<T> implements DynamicConfiguration<T> {

        private final Deferred.Reference<Updates.Snapshot<T>> snapshot;

        DynamicConfigurationImpl(final Class<T> type, final Configuration<T> delegate, final Configuration<Settings> configuration, final Updates updates) {
            this.snapshot = Deferred.reference(new Deferred.Factory<Updates.Snapshot<T>>() {
                public Updates.Snapshot<T> create() {
                    return updates.snapshot(configuration.settings().period(), new Updates.Snapshot<T>() {
                        private Configuration.Query<T, T> all = new Configuration.Query<T, T>() {
                            public T run(final T settings) throws Exception {
                                final Map<Method, Object> cache = new HashMap<Method, Object>();

                                for (final Method method : type.getMethods()) {
                                    assert method.getParameterTypes().length == 0 : method;

                                    try {
                                        cache.put(method, Exceptions.wrap(new Process<Object, Exception>() {
                                            public Object run() throws Exception {
                                                method.setAccessible(true);
                                                return method.invoke(settings);
                                            }
                                        }));
                                    } catch (final Exceptions.Wrapper e) {
                                        e.rethrow(Exception.class);
                                    }
                                }

                                return Proxies.create(type, new InvocationHandler() {
                                    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                                        return cache.get(method);
                                    }
                                });
                            }
                        };

                        public T get() {
                            return delegate.query(all);
                        }
                    });
                }
            });
        }

        public Updates.Snapshot<T> snapshot() {
            return snapshot.get();
        }
    }

    /**
     * Configuration refresh period settings.
     */
    private interface Settings {

        /**
         * Returns the period in milliseconds during which at most one log level check takes place per logger. May
         * be 0 or negative, in which case no periodic log level check will take place.
         *
         * @return the period in milliseconds.
         */
        @Configuration.Property(key = DynamicConfiguration.CONFIGURATION_REFRESH_PERIOD, undefined = "30000")
        long period();
    }
}