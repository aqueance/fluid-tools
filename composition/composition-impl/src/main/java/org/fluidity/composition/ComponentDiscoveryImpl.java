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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Uses a class discovery to find classes and a factory to instantiate them.
 *
 * @author Tibor Varga
 */
@Component
final class ComponentDiscoveryImpl implements ComponentDiscovery {

    private final ClassDiscovery discovery;

    public ComponentDiscoveryImpl(final ClassDiscovery discovery) {
        this.discovery = discovery;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] findComponentInstances(final ComponentContainer container, final Class<T> componentClass) {
        final Class<T>[] classes = discovery.findComponentClasses(componentClass, null, false);
        final List<T> instances = new ArrayList<T>();

        final OpenComponentContainer nested = container.makeNestedContainer();
        final ComponentContainer.Registry registry = nested.getRegistry();

        for (final Class<T> theClass : classes) {
            T component = container.getComponent(theClass);

            if (component == null) {
                registry.bindDefault(theClass);
            } else {
                instances.add(component);
            }
        }

        instances.addAll(nested.getAllComponents(componentClass));

        return instances.toArray((T[]) Array.newInstance(componentClass, instances.size()));
    }
}
