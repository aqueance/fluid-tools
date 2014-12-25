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

package org.fluidity.composition.container.impl;

import org.fluidity.composition.Component;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.foundation.ClassDiscovery;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.ServiceProviders;

/**
 * The component is instantiated by {@link ProductionServices} and picked up at container bootstrap to be made available as a component. This implementation
 * assumes class loaders adhere to the contract of delegating to parent first and only if that fails trying to resolve classes.
 *
 * @author Tibor Varga
 */
@Component(automatic = false)
final class ClassDiscoveryImpl implements ClassDiscovery {

    private final Log log;

    private final ServiceProviders.Log wrapper = new ServiceProviders.Log() {
        public void debug(final String format, final Object... arguments) {
            ClassDiscoveryImpl.this.log.debug(format, arguments);
        }

        public void error(final String format, final Object... arguments) {
            ClassDiscoveryImpl.this.log.error(format, arguments);
        }

        public void error(final Exception error, final String format, final Object... arguments) {
            ClassDiscoveryImpl.this.log.error(error, format, arguments);
        }
    };

    ClassDiscoveryImpl(final Log<ClassDiscoveryImpl> log) {
        this.log = log;
    }

    public <T> Class<? extends T>[] findComponentClasses(final Class<T> api, final ClassLoader classLoader, final boolean strict) {
        final ServiceProvider annotation = api.getAnnotation(ServiceProvider.class);
        final String type = annotation == null ? ServiceProviders.TYPE : annotation.type();
        return ServiceProviders.findClasses(type, api, classLoader, strict, false, true, wrapper);
    }

    public <T> Class<? extends T>[] findComponentClasses(final String type,
                                                         final Class<T> api,
                                                         final ClassLoader classLoader,
                                                         final boolean inherit,
                                                         final boolean strict) {
        return ServiceProviders.findClasses(type, api, classLoader, strict, false, inherit, wrapper);
    }
}
