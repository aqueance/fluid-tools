/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import org.fluidity.composition.spi.ComponentCache;
import org.fluidity.composition.spi.DependencyInjector;
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

    private Constructor<?> checkConstructors(Constructor[] constructors) {

        /*
         * There is no check for any constructor parameter being satisfiable. If there's only one constructor, it is returned. If there is a default constructor and
         * another, the other one is returned. If there are more, the one annotated with @Component is returned. If the above does not yield a single constructor, a
         * ComponentContainer.ResolutionException is thrown.
         */

        if (constructors.length == 1) {
            return constructors[0];
        } else if (constructors.length == 2 && constructors[0].getParameterTypes().length * constructors[1].getParameterTypes().length == 0) {
            return constructors[0].getParameterTypes().length == 0 ? constructors[1] : constructors[0];
        } else {
            final List<Constructor<?>> componentConstructors = new ArrayList<Constructor<?>>();

            for (final Constructor<?> constructor : constructors) {
                if (constructor.getAnnotation(Component.class) != null) {
                    componentConstructors.add(constructor);
                }
            }

            if (componentConstructors.isEmpty()) {
                throw new ComponentContainer.ResolutionException("No @Component constructor found for %s", componentClass);
            } else if (componentConstructors.size() > 1) {
                throw new ComponentContainer.ResolutionException("Multiple @Component constructor found for %s", componentClass);
            } else {
                return componentConstructors.get(0);
            }
        }
    }
}
