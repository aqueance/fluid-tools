/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package org.fluidity.composition.web;

import javax.servlet.ServletException;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.foundation.ClassLoaderUtils;

/**
 * Enables boundary (i.e. environment controlled) objects to acquire their delegate from a component container.
 *
 * @author Tibor Varga
 */
final class DependencyResolverImpl implements DependencyResolver {

    public Object findComponent(final String containerClassName, final String componentClassName)
        throws ServletException {
        assert containerClassName != null : CONTAINER_CLASS;
        assert componentClassName != null : COMPONENT_KEY;

        try {
            final ClassLoader classLoader = ClassLoaderUtils.findClassLoader(DependencyResolver.class);
            assert classLoader != null : DependencyResolver.class;

            final ComponentContainer container = (ComponentContainer) classLoader.loadClass(containerClassName).newInstance();
            assert container != null : containerClassName;

            final Class<?> componentClass = classLoader.loadClass(componentClassName);
            assert componentClass != null : componentClassName;

            Object component = container.getComponent(componentClass);

            if (component == null) {
                component = container.getComponent(componentClass, new ComponentContainer.Bindings() {
                    public void registerComponents(ComponentContainer.Registry registry) {
                        registry.bind(componentClass);
                    }
                });

                assert component != null : componentClassName;
            }

            return component;
        } catch (final InstantiationException e) {
            throw (ServletException) new ServletException(e).initCause(e);
        } catch (final IllegalAccessException e) {
            throw (ServletException) new ServletException(e).initCause(e);
        } catch (final ClassNotFoundException e) {
            throw (ServletException) new ServletException(e).initCause(e);
        }
    }
}
