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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

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
        final Class<?>[] types = reference(implementation);
        final Class<?>[] groups = groups(types);
        final Class<?>[] interfaces = interfaces(types, groups);
        delegate.bindComponent(implementation, interfaces, groups);
    }

    public final <T> void bindComponent(final Class<T> implementation, final Class<? super T>... classes) throws ComponentContainer.BindingException {
        final Class<?>[] groups = groups(reference(implementation));
        final Class<?>[] interfaces = interfaces(implementation, classes, groups);
        delegate.bindComponent(implementation, interfaces, groups);
    }

    @SuppressWarnings("unchecked")
    public final <T> void bindInstance(final T instance) throws ComponentContainer.BindingException {
        assert instance != null;
        final Class<?> implementation = instance.getClass();
        final Class<?>[] types = reference(implementation);
        final Class<?>[] groups = groups(types);
        final Class<?>[] interfaces = interfaces(types, groups);
        delegate.bindInstance(instance, interfaces, groups);
    }

    @SuppressWarnings("unchecked")
    public final <T> void bindInstance(final T instance, final Class<? super T>... classes) throws ComponentContainer.BindingException {
        assert instance != null;
        final Class<?> implementation = instance.getClass();
        final Class<?>[] groups = groups(reference(implementation));
        final Class<?>[] interfaces = interfaces(isFactory(implementation) ? null : implementation, classes, groups);
        delegate.bindInstance(instance, interfaces, groups);
    }

    public final void bindFactory(final Class<?> factory, final Class<?>... classes) throws ComponentContainer.BindingException {
        final Class<?>[] groups = groups(reference(factory));
        final Class<?>[] interfaces = interfaces(null, classes, groups);
        delegate.bindComponent(factory, interfaces, groups);
    }

    @SuppressWarnings("unchecked")
    public final <T> OpenComponentContainer makeChildContainer(final Class<T> implementation) throws ComponentContainer.BindingException {
        final Class<?>[] types = reference(implementation);
        final Class<?>[] groups = groups(types);
        final Class<?>[] interfaces = interfaces(types, groups);
        return delegate.makeChildContainer(implementation, interfaces, groups);
    }

    public final <T> OpenComponentContainer makeChildContainer(final Class<T> implementation, final Class<? super T>... classes) {
        final Class<?>[] groups = groups(reference(implementation));
        final Class<?>[] interfaces = interfaces(implementation, classes, groups);
        return delegate.makeChildContainer(implementation, interfaces, groups);
    }

    private Class<?>[] reference(final Class<?> implementation) {
        return isFactory(implementation) ? delegate(implementation) : new Class<?>[] { implementation };
    }

    private Class<?>[] groups(final Class<?>[] types) {
        final Set<Class<?>> list = new LinkedHashSet<Class<?>>();

        for (final Class<?> type : types) {
            final Class<?>[] groups = groups(type);

            if (groups != null) {
                list.addAll(Arrays.asList(groups));
            }
        }

        return list.isEmpty() ? null : list.toArray(new Class<?>[list.size()]);
    }

    private Class<?>[] interfaces(final Class<?>[] types, final Class<?>[] groups) {
        final Set<Class<?>> list = new LinkedHashSet<Class<?>>();

        for (final Class<?> type : types) {
            list.add(type);

            final Class<?>[] interfaces = interfaces(type);

            if (interfaces != null) {
                list.addAll(Arrays.asList(interfaces));
            }
        }

        if (groups != null) {
            list.removeAll(Arrays.asList(groups));
        }

        return list.toArray(new Class<?>[list.size()]);
    }

    private Class<?>[] interfaces(final Class<?> implementation, final Class<?>[] interfaces, final Class<?>[] groups) {
        final Set<Class<?>> types = new LinkedHashSet<Class<?>>();

        if (implementation != null) {
            types.add(implementation);
        }

        types.addAll(Arrays.asList(interfaces));

        if (groups != null) {
            types.removeAll(Arrays.asList(groups));
        }

        return types.toArray(new Class<?>[types.size()]);
    }

    private boolean isFactory(final Class<?> implementation) {
        return ComponentFactory.class.isAssignableFrom(implementation) || ComponentVariantFactory.class.isAssignableFrom(implementation);
    }

    private Class<?>[] delegate(final Class<?> implementation) {
        final Component annotation = implementation.getAnnotation(Component.class);
        final Class<?>[] api = annotation == null ? null : annotation.api();

        if (api != null) {
            for (final Class<?> type : api) {
                if (isFactory(type)) {
                    throw new ComponentContainer.BindingException("Factory %s cannot stand for another factory %s", implementation, type);
                }
            }
        }

        return api == null ? new Class<?>[0] : api;
    }

    /**
     * Returns the configured component interfaces for the given component class.
     *
     * @param implementation the component class.
     * @return the list of interfaces configured for the component.
     */
    private Class<?>[] interfaces(final Class<?> implementation) {
        final Component component = implementation.getAnnotation(Component.class);
        return component == null ? null : interfaces(implementation, component.api());
    }

    /**
     * Returns the configured component group interfaces for the given component class.
     *
     * @param implementation the component class.
     *
     * @return the list of group interfaces configured for the component.
     */
    private Class<?>[] groups(final Class<?> implementation) {
        final Set<Class<?>> groups = groupInterfaces(implementation);
        return groups.isEmpty() ? null : interfaces(implementation, groups.toArray(new Class[groups.size()]));
    }

    /**
     * Finds the possible single API that the given component class should be bound against.
     *
     * @param implementation the component class.
     * @param specified      the list of interfaces specified for the component. Discovery is performed if empty or <code>null</code>.
     *
     * @return an array of class objects, never <code>null</code>. When no suitable interface is found, the implementation class itself is returned.
     */
    private Class<?>[] interfaces(final Class<?> implementation, final Class<?>... specified) {
        if ((specified != null && specified.length > 0)) {
            for (final Class<?> api : specified) {
                if (!api.isAssignableFrom(implementation)) {
                    throw new ComponentContainer.BindingException("%s is not assignable to %s", implementation, api);
                }
            }

            return specified;
        }

        final Class<?>[] implemented = implementation.isArray() ? null : findComponentInterfaces(implementation);
        return implemented == null ? new Class[] { implementation } : implemented;
    }

    private Set<Class<?>> groupInterfaces(final Class<?> current) {
        final Set<Class<?>> list = new LinkedHashSet<Class<?>>();
        final ComponentGroup direct = current.getAnnotation(ComponentGroup.class);
        assert ComponentGroup.class.isAnnotationPresent(Inherited.class);

        if (direct != null) {
            list.addAll(Arrays.asList(interfaces(current, direct.api())));
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
