/*
 * Copyright (c) 2006-2009 Tibor Adam Varga (tibor.adam.varga on gmail)
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
 */
package org.fluidity.composition.pico;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;

import org.fluidity.composition.Component;
import org.fluidity.composition.Optional;
import org.picocontainer.PicoContainer;
import org.picocontainer.PicoInitializationException;
import org.picocontainer.PicoIntrospectionException;
import org.picocontainer.defaults.AbstractComponentAdapter;
import org.picocontainer.defaults.AssignabilityRegistrationException;
import org.picocontainer.defaults.UnsatisfiableDependenciesException;

/**
 * Finds, resolves and sets, using the given container, all @{@link org.fluidity.composition.Component} annotated fields of an object that have not yet been
 * set.
 */
public class AnnotatedFieldInjector implements Serializable {

    public <T> T injectFields(final PicoContainer container, final T instance) {
        assert container != null;
        assert instance != null;

        final Class<?> componentType = instance.getClass();

        try {
            injectFields(container, instance, componentType, componentType);
        } catch (IllegalAccessException e) {
            assert false : e;
        }

        return instance;
    }

    private <T> void injectFields(final PicoContainer container, final T instance, final Class<?> componentType, final Class<?> declaringType) throws IllegalAccessException {
        for (final Field field : declaringType.getDeclaredFields()) {
            field.setAccessible(true);

            if ((field.getModifiers() & Modifier.FINAL) != 0 || field.get(instance) != null) continue;

            Class<?> dependencyType = field.getType();
            boolean component = false;
            boolean optional = false;

            for (final Annotation annotation : field.getAnnotations()) {
                final Class<? extends Annotation> type = annotation.annotationType();

                if (type.equals(Component.class)) {
                    component = true;
                    dependencyType = ((Component) annotation).api();
                } else if (type.equals(Optional.class)) {
                    optional = true;
                }
            }

            if (component) {
                if (dependencyType == Object.class) {
                    dependencyType = field.getType();
                }

                final Object dependency = dependencyType.isAssignableFrom(componentType) ? instance : container.getComponentInstance(dependencyType);

                if (dependency == null && !optional) {
                    throw new UnsatisfiableDependenciesException(new FakeComponentAdapter(componentType, instance),
                                                                 dependencyType,
                                                                 Collections.emptySet(),
                                                                 container);
                } else {
                    try {
                        field.set(instance, dependency);
                    } catch (final IllegalAccessException e) {
                        assert false : e;
                    }
                }
            }
        }

        final Class<?> superclass = declaringType.getSuperclass();
        if (superclass != null) {
            injectFields(container, instance, componentType, superclass);
        }
    }

    private static class FakeComponentAdapter extends AbstractComponentAdapter {

        private final Object instance;

        protected FakeComponentAdapter(final Class componentImplementation, final Object instance) throws AssignabilityRegistrationException {

            // TODO: detect component key using @Component annotation of class
            super(componentImplementation, componentImplementation);
            this.instance = instance;
        }

        public Class getComponentImplementation() {
            return instance.getClass();
        }

        public Object getComponentInstance(PicoContainer container) throws PicoInitializationException, PicoIntrospectionException {
            return instance;
        }

        public void verify(PicoContainer container) throws PicoIntrospectionException {
            // empty
        }
    }
}
