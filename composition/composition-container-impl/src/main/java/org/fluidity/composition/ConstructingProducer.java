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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Reflection;

/**
 * Component producer that works by instantiating a class.
 *
 * @author Tibor Varga
 */
final class ConstructingProducer extends AbstractProducer {

    private final DependencyInjector injector;

    private final Class<?> componentInterface;
    private final Class<?> componentClass;

    private final Constructor<?> constructor;

    public ConstructingProducer(final Class<?> componentInterface,
                                final Class<?> componentClass,
                                final ComponentCache cache,
                                final DependencyInjector injector) {
        super(cache);
        this.injector = injector;
        this.componentInterface = componentInterface;
        this.componentClass = componentClass;
        this.constructor = findComponentConstructor();
    }

    public Class<?> componentInterface() {
        return componentInterface;
    }

    public Class<?> componentClass() {
        return componentClass;
    }

    private synchronized Constructor<?> constructor() {
        return constructor;
    }

    @Override
    protected ComponentCache.Command createCommand(final SimpleContainer container) {
        return new ComponentCache.Command() {
            public Object run(final ComponentContext context) {
                final Constructor constructor = constructor();
                final ComponentContext componentContext = container.contextFactory().deriveContext(context, componentClass());

                return Exceptions.wrap(String.format("instantiating %s", componentClass()), new Exceptions.Command<Object>() {
                    public Object run() throws Exception {
                        final boolean accessible = Reflection.isAccessible(constructor);

                        if (!accessible) {
                            constructor.setAccessible(true);
                        }

                        try {
                            return constructor.newInstance(injector.injectConstructor(container, componentInterface, componentContext, constructor));
                        } finally {
                            if (!accessible) {
                                constructor.setAccessible(accessible);
                            }
                        }
                    }
                });
            }
        };
    }

    private Constructor<?> findComponentConstructor() {
        try {
            return checkConstructors(componentClass.getConstructors());
        } catch (final ComponentContainer.ResolutionException e) {
            return checkConstructors(componentClass.getDeclaredConstructors());
        }
    }

    private Constructor<?> checkConstructors(final Constructor[] constructors) {

        /*
         * There is no check for any constructor parameter being satisfiable. Synthetic constructors are ignored. If there is any constructor annotated with
         * @Component, all other constructors are ignored. If there's only one constructor, it is returned. If there is a default constructor
         * and another, and neither is annotated with @Component, the other one is returned. If there are more constructors, the only one annotated with
         * @Component is returned. If these checks do not yield a single constructor, a ComponentContainer.ResolutionException is thrown.
         *
         * For synthetic constructors see http://java.sun.com/docs/books/jls/third_edition/html/binaryComp.html#13.1, "synthetic"
         */

        Constructor<?> designed = null;

        final List<Constructor<?>> list = new ArrayList<Constructor<?>>();
        for (final Constructor constructor : constructors) {
            if (!constructor.isSynthetic()) {
                if (designed == null) {
                    list.add(constructor);
                }

                @SuppressWarnings("unchecked")
                final boolean annotated = constructor.getAnnotation(Component.class) != null;
                if (designed != null && annotated) {
                    throw new ComponentContainer.ResolutionException("Multiple @Component constructors found for %s", componentClass);
                } else if (annotated) {
                    designed = constructor;
                }
            }
        }

        if (designed != null) {
            return designed;
        }

        switch (list.size()) {
        case 0:
            throw new ComponentContainer.ResolutionException("No suitable constructor found for %s", componentClass);
        case 1:
            return list.get(0);
        case 2:

            // return the one with more than 0 parameters
            final int parameters0 = list.get(0).getParameterTypes().length;
            final int parameters1 = list.get(1).getParameterTypes().length;

            if (parameters0 == 0 && parameters1 != 0) {
                return list.get(1);
            } else if (parameters0 != 0 && parameters1 == 0) {
                return list.get(0);
            }
        default:
            throw new ComponentContainer.ResolutionException("Multiple constructors found for %s", componentClass);
        }
    }
}
