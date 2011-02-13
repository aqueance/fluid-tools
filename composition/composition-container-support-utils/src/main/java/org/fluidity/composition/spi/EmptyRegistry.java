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

package org.fluidity.composition.spi;

import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.OpenComponentContainer;

/**
 * Implements basic method relationships and useful functionality to registry implementations.
 */
final class EmptyRegistry implements ComponentContainer.Registry {

    private final ComponentRegistry delegate;

    EmptyRegistry(final ComponentRegistry delegate) {
        this.delegate = delegate;
    }

    public OpenComponentContainer makeChildContainer() {
        return delegate.makeChildContainer();
    }

    public final <T> void bindComponent(final Class<T> implementation) throws ComponentContainer.BindingException {
        final Class<?>[] groups = groups(implementation);
        delegate.bindComponent(implementation, interfaces(implementation, groups != null && groups.length > 0), groups);
    }

    public final <T> void bindComponent(final Class<T> implementation, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
        delegate.bindComponent(implementation, interfaces, groups(implementation));
    }

    @SuppressWarnings("unchecked")
    public final <T> void bindInstance(final T instance) throws ComponentContainer.BindingException {
        final Class<?> implementation = instance.getClass();
        final Class<?>[] groups = groups(implementation);
        delegate.bindInstance(instance, interfaces(implementation, groups != null && groups.length > 0), groups);
    }

    @SuppressWarnings("unchecked")
    public final <T> void bindInstance(final T instance, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
        delegate.bindInstance(instance, interfaces, groups(instance.getClass()));
    }

    public final void bindFactory(final Class<?> factory, final Class<?>... interfaces) throws ComponentContainer.BindingException {
        delegate.bindComponent(factory, interfaces, groups(factory));
    }

    public <T> void bindGroup(final Class<T> api, final Class<? extends T>... implementations) throws ComponentContainer.BindingException {
        for (final Class<? extends T> implementation : implementations) {
            delegate.bindComponent(implementation, null, new Class<?>[] { api });
        }
    }

    @SuppressWarnings("unchecked")
    public final <T> OpenComponentContainer makeChildContainer(final Class<T> implementation) throws ComponentContainer.BindingException {
        final Class<?>[] groups = groups(implementation);
        return delegate.makeChildContainer(implementation, interfaces(implementation, groups != null && groups.length > 0), groups);
    }

    public final <T> OpenComponentContainer makeChildContainer(final Class<T> implementation, final Class<? super T>... interfaces) {
        return delegate.makeChildContainer(implementation, interfaces, groups(implementation));
    }

    /**
     * Returns the configured component interfaces for the given component class.
     *
     * @param implementation the component class.
     * @param group          tells if the component has or inherits a group annotation.
     *
     * @return the list of interfaces configured for the component.
     */
    private Class<?>[] interfaces(final Class<?> implementation, final boolean group) {
        final Component component = implementation.getAnnotation(Component.class);
        return component == null && group ? null : interfaces(!group, implementation, component == null ? null : component.api());
    }

    /**
     * Returns the configured component group interfaces for the given component class.
     *
     * @param implementation the component class.
     *
     * @return the list of group interfaces configured for the component.
     */
    private Class<?>[] groups(final Class<?> implementation) {
        final Component component = implementation.getAnnotation(Component.class);
        final List<Class<?>> groups = groupInterfaces(implementation);
        return groups.isEmpty() ? null : interfaces(component == null, implementation, groups.toArray(new Class[groups.size()]));
    }

    /**
     * Finds the possible single API that the given component class should be bound against.
     *
     * @param discover       specifies whether suitable interfaces should be found if none are specified.
     * @param implementation the component class.
     * @param specified      the list of interfaces specified for the component. Discovery is performed if empty or <code>null</code>.
     *
     * @return an array of class objects, never <code>null</code>. When no suitable interface is found, the implementation class itself is returned.
     */
    private Class<?>[] interfaces(final boolean discover, final Class<?> implementation, final Class<?>... specified) {
        if ((specified != null && specified.length > 0) || !discover) {
            return specified;
        }

        final Class<?>[] implemented = implementation.isArray() ? null : findComponentInterfaces(implementation);
        return implemented == null ? new Class[] { implementation } : implemented;
    }

    private List<Class<?>> groupInterfaces(final Class<?> current) {
        final List<Class<?>> list = new ArrayList<Class<?>>();
        final ComponentGroup direct = current.getAnnotation(ComponentGroup.class);
        assert ComponentGroup.class.getAnnotation(Inherited.class) != null;

        if (direct != null) {
            list.addAll(Arrays.asList(interfaces(true, current, direct.api())));
        }

        for (final Class<?> implemented : current.getInterfaces()) {
            list.addAll(groupInterfaces(implemented));
        }

        return list;
    }

    private Class<?>[] findComponentInterfaces(final Class<?> implementation) {
        final Class<?>[] interfaces = implementation.getInterfaces();

        if (interfaces.length > 0) {
            return interfaces;
        } else {
            final Class<?> ancestor = implementation.getSuperclass();

            if (ancestor != null && ancestor != Object.class) {
                return findComponentInterfaces(ancestor);
            }
        }

        return null;
    }
}
