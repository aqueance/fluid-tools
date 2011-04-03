/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.composition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.spi.ContainerProvider;
import org.fluidity.composition.spi.PackageBindings;

/**
 * @author Tibor Varga
 */
final class ContainerProviderImpl implements ContainerProvider {

    public OpenComponentContainer newContainer(final ContainerServices services) {
        return new ComponentContainerShell(services);
    }

    @SuppressWarnings("unchecked")
    public List<PackageBindings> instantiateBindings(final ContainerServices services,
                                                     final Map properties,
                                                     final Collection<Class<PackageBindings>> bindings) {
        final SimpleContainer container = new SimpleContainerImpl(services);

        if (properties != null) {
            container.bindInstance(properties, Components.inspect(properties.getClass(), Map.class));
        }

        /*
         * Add each to the container
         */
        final Collection<Class<?>> groups = Collections.<Class<?>>singletonList(PackageBindings.class);
        for (final Class<PackageBindings> binding : bindings) {
            container.bindComponent(new Components.Interfaces(binding, false, false, new Components.Specification[] { new Components.Specification(binding, groups) }));
        }

        /*
         * Get the instances in instantiation order
         */
        final DependencyGraph.Traversal traversal = services.graphTraversal();
        final PackageBindings[] instances = (PackageBindings[]) container.resolveGroup(PackageBindings.class, services.emptyContext(), traversal).instance(traversal);
        assert instances != null : PackageBindings.class;
        return Arrays.asList(instances);
    }
}
