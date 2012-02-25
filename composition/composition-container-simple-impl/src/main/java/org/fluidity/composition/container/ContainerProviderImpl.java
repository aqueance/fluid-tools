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

package org.fluidity.composition.container;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.Components;
import org.fluidity.composition.container.api.ContainerServices;
import org.fluidity.composition.container.spi.ContainerProvider;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.container.spi.OpenComponentContainer;
import org.fluidity.composition.container.spi.PlatformContainer;
import org.fluidity.composition.spi.PackageBindings;

/**
 * @author Tibor Varga
 */
final class ContainerProviderImpl implements ContainerProvider {

    public OpenComponentContainer newContainer(final ContainerServices services, final PlatformContainer platform) {
        return new ComponentContainerShell(services, platform);
    }

    @SuppressWarnings("unchecked")
    public List<PackageBindings> instantiateBindings(final ContainerServices services, final Map properties, final Class<PackageBindings>[] bindings) {
        final SimpleContainer container = new SimpleContainerImpl(services, null);

        if (properties != null) {
            container.bindInstance(properties, Components.inspect(properties.getClass(), Map.class));
        }

        /*
         * Add each to the container
         */
        final Collection<Class<?>> groups = Collections.<Class<?>>singletonList(PackageBindings.class);
        for (final Class<PackageBindings> binding : bindings) {
            container.bindComponent(new Components.Interfaces(binding, new Components.Specification[] {
                    new Components.Specification(binding, groups)
            }));
        }

        /*
         * Get the instances in instantiation order
         */
        final DependencyGraph.Traversal traversal = services.graphTraversal();
        final PackageBindings[] instances = (PackageBindings[]) container.resolveGroup(PackageBindings.class, services.emptyContext(), traversal).instance(
                traversal);
        assert instances != null : PackageBindings.class;
        return Arrays.asList(instances);
    }
}
