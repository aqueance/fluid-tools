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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.composition.spi.CustomComponentFactory;

/**
 * @author Tibor Varga
 */
@Component(api = Configuration.Updated.class)
@Component.Context(value = { Configuration.Context.class }, typed = true)
final class UpdatedConfigurationFactory implements CustomComponentFactory {

    public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
        final Component.Reference reference = context.annotation(Component.Reference.class, Configuration.class);
        final Class<?> api = reference.parameter(0);

        dependencies.discover(UpdatedConfigurationImpl.class);

        for (final Method method : api.getMethods()) {
            if (method.getParameterTypes().length > 0) {
                throw new ComponentContainer.ResolutionException("Cannot load snapshot of %s: method %s has parameters", api, method);
            }
        }

        return new Instance() {

            @SuppressWarnings("unchecked")
            public void bind(final Registry registry) throws OpenComponentContainer.BindingException {
                registry.bindInstance(api, Class.class);
                registry.bindComponent(UpdatedConfigurationImpl.class);
            }
        };
    }

    @Component(automatic = false)
    private static class UpdatedConfigurationImpl<T> implements Configuration.Updated<T> {

        private final Configuration<T> configuration;
        private final Updates updates;
        private final Class<T> type;

        public UpdatedConfigurationImpl(final Class<T> type, final Configuration<T> configuration, final Updates updates) {
            this.configuration = configuration;
            this.updates = updates;
            this.type = type;
        }

        public Updates.Snapshot<T> snapshot(final long period) {
            return updates.register(period, new Updates.Snapshot<T>() {
                private Configuration.Query<T, T> all = new Configuration.Query<T, T>() {
                    public T read(final T settings) {
                        final Map<Method, Object> cache = new HashMap<Method, Object>();

                        for (final Method method : type.getMethods()) {
                            cache.put(method, Exceptions.wrap(new Exceptions.Command<Object>() {
                                public Object run() throws Throwable {
                                    assert method.getParameterTypes().length == 0 : method;
                                    method.setAccessible(true);
                                    return method.invoke(settings);
                                }
                            }));
                        }

                        return Proxies.create(type, new InvocationHandler() {
                            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                                return cache.get(method);
                            }
                        });
                    }
                };

                public T get() {
                    return configuration.query(all);
                }
            });
        }
    }
}
