/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.features.DynamicConfiguration;
import org.fluidity.features.Updates;
import org.fluidity.foundation.Configuration;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Proxies;
import org.fluidity.foundation.security.Security;

/**
 * @author Tibor Varga
 */
@Component(api = DynamicConfiguration.class)
@Component.Qualifiers(Component.Reference.class)
final class DynamicConfigurationFactory implements ComponentFactory {

    @SuppressWarnings("unchecked")
    public Instance resolve(final ComponentContext context, final Container dependencies) throws Exception {
        final Component.Reference reference = context.qualifier(Component.Reference.class, DynamicConfiguration.class);
        final Class<?> api = reference.parameter(0);

        for (final Method method : api.getMethods()) {
            if (method.getParameterTypes().length > 0) {
                throw new ComponentContainer.ResolutionException("Cannot load snapshot of %s: method %s has parameters", api, method);
            }
        }

        return dependencies.instance(DynamicConfigurationImpl.class);
    }

    @Component(automatic = false)
    @Component.Qualifiers(Component.Reference.class)
    private static class DynamicConfigurationImpl<T> implements DynamicConfiguration<T> {

        private final Deferred.Reference<Supplier<T>> snapshot;

        DynamicConfigurationImpl(final ComponentContext context, final Configuration<T> delegate, final Configuration<Settings> configuration, final Updates updates) {

            @SuppressWarnings("unchecked")
            final Class<T> type = (Class<T>) Generics.rawType(context.qualifier(Component.Reference.class, null).parameter(0));

            final Configuration.Query<T, T> all = settings -> {
                final Map<Method, Object> cache = new HashMap<>();

                for (final Method method : Security.invoke(type::getMethods)) {
                    assert method.getParameterTypes().length == 0 : method;
                    cache.put(method, Security.access(method).invoke(settings));
                }

                return Proxies.create(type, (proxy, method, arguments) -> cache.get(method));
            };

            this.snapshot = Deferred.shared(() -> updates.snapshot(configuration.settings().period(), () -> delegate.query(all)));
        }

        public Supplier<T> snapshot() {
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
