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

package org.fluidity.deployment.osgi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.Generics;

/**
 * Binds registered service instances.
 *
 * @author Tibor Varga
 */
@Component(automatic = false)
@Component.Context({ Service.class, Component.Reference.class })
final class ServiceComponentFactory implements ComponentFactory {

    private final Map<String, ServiceDescriptor> services;
    private final Class<?>[] api;

    public ServiceComponentFactory(final ServiceDescriptor[] services) {
        final Map<String, ServiceDescriptor> map = new HashMap<String, ServiceDescriptor>();
        final Set<Class<?>> types = new HashSet<Class<?>>();

        for (final ServiceDescriptor descriptor : services) {
            final String filter = descriptor.filter;
            map.put(String.format("%s:%s", descriptor.type, filter == null ? "" : filter), descriptor);
            types.add(descriptor.type);
        }

        this.services = map;
        this.api = types.toArray(new Class[types.size()]);
    }

    public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
        final Service annotation = context.annotation(Service.class, null);
        final Component.Reference reference = context.annotation(Component.Reference.class, null);
        final Class<?> type = annotation.api();
        final ServiceDescriptor descriptor = services.get(String.format("%s:%s",
                                                                        type == Object.class ? Generics.rawType(reference.type()) : type,
                                                                        annotation.filter()));

        if (descriptor.instance() == null) {
            throw new ComponentContainer.ResolutionException(descriptor.toString());
        } else {
            return new Instance() {
                @SuppressWarnings("unchecked")
                public void bind(final Registry registry) throws ComponentContainer.BindingException {
                    registry.bindInstance(descriptor.instance(), (Class) descriptor.type);
                }
            };
        }
    }

    public Class<?>[] api() {
        return api;
    }
}
