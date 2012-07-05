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

package org.fluidity.deployment.web;

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
    @SuppressWarnings("unchecked")
    Object findComponent(final ComponentContainer container, final String componentClassName) throws ServletException {
        assert componentClassName != null : COMPONENT_KEY;

        try {
            final ClassLoader classLoader = ClassLoaders.findClassLoader(DependencyResolver.class, true);
            assert classLoader != null : DependencyResolver.class;

            final Class componentClass = classLoader.loadClass(componentClassName);
            assert componentClass != null : componentClassName;

            assert container != null : ComponentContainer.class;
            Object component = container.getComponent(componentClass);

            if (component == null) {
                component = container.instantiate(componentClass);
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
