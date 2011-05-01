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

package org.fluidity.deployment.osgi;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Context;
import org.fluidity.composition.spi.ComponentFactory;

/**
 * Handles {@link org.fluidity.deployment.osgi.ServiceTracker.Dependency} dependencies. See {@link ServiceTracker} for details.
 *
 * @author Tibor Varga
 */
@Component(api = ServiceTracker.Dependency.class)
@Context(Service.class)
final class ServiceDependencyFactory implements ComponentFactory {

    private final ThreadLocal<Map<Service, MutableReference>> collector = new ThreadLocal<Map<Service, ServiceDependencyFactory.MutableReference>>();

    public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
        final Map<Service, ServiceDependencyFactory.MutableReference> references = this.collector.get();

        if (references == null) {
            throw new ComponentContainer.ResolutionException("Service dependencies can only be resolved via %s.%s()", ServiceDependencyFactory.class, "instantiate");
        } else {
            final Service service = context.annotation(Service.class, ServiceTracker.Dependency.class);

            final ServiceTracker.Dependency reference;
            if (!references.containsKey(service)) {
                reference = new ServiceReference();
                references.put(service, (MutableReference) reference);
            } else {
                reference = references.get(service);
            }

            return new Instance() {
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindInstance(reference, ServiceTracker.Dependency.class);
                }
            };
        }
    }

    public <T> T instantiate(final Class<T> api, final ComponentContainer container, final Map<Service, ServiceDependencyFactory.MutableReference> collector) {
        this.collector.set(collector);

        try {
            return container.getComponent(api);
        } finally {
            this.collector.remove();
        }
    }

    public static interface MutableReference extends ServiceTracker.Dependency {

        void set(Object instance);

        void remove();
    }

    @SuppressWarnings("unchecked")
    private static class ServiceReference implements MutableReference {

        private final AtomicReference reference = new AtomicReference();

        public void set(final Object instance) {
            reference.set(instance);
        }

        public void remove() {
            reference.set(null);
        }

        public boolean resolved() {
            return reference.get() != null;
        }

        public Object get() {
            return reference.get();
        }
    }
}
