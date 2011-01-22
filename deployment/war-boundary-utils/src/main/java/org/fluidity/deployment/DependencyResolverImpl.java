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

package org.fluidity.deployment;

import javax.servlet.ServletException;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.foundation.ClassLoaders;

/**
 * Enables boundary (i.e. environment controlled) objects to acquire their delegate from a component container.
 *
 * @author Tibor Varga
 */
final class DependencyResolverImpl implements DependencyResolver {

    public Object findComponent(final String componentClassName) throws ServletException {
        return findComponent(Bootstrap.container, componentClassName);
    }

    /*
     * Package visible to make accessible to test cases.
     */
    Object findComponent(final ComponentContainer container, final String componentClassName) throws ServletException {
        assert componentClassName != null : COMPONENT_KEY;

        try {
            final ClassLoader classLoader = ClassLoaders.findClassLoader(DependencyResolver.class);
            assert classLoader != null : DependencyResolver.class;

            final Class<?> componentClass = classLoader.loadClass(componentClassName);
            assert componentClass != null : componentClassName;

            assert container != null : ComponentContainer.class;
            Object component = container.getComponent(componentClass);

            if (component == null) {
                component = container.getComponent(componentClass, new ComponentContainer.Bindings() {
                    public void bindComponents(ComponentContainer.Registry registry) {
                        registry.bindDefault(componentClass);
                    }
                });

                assert component != null : componentClassName;
            }

            return component;
        } catch (final ClassNotFoundException e) {
            throw new ServletException(e);
        }
    }

    private static class Bootstrap {
        private static final ComponentContainer container = new ContainerBoundary();
    }
}
