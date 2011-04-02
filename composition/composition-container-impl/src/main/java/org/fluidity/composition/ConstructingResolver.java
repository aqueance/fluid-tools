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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.fluidity.foundation.spi.LogFactory;

/**
 * Component resolver that works by instantiating a class.
 *
 * @author Tibor Varga
 */
final class ConstructingResolver extends AbstractResolver {

    private final DependencyInjector injector;

    private final Class<?> componentClass;

    private final Constructor<?> constructor;
    private final boolean ignoreContext;

    public ConstructingResolver(final int priority,
                                final Class<?> api,
                                final Class<?> componentClass,
                                final boolean ignoreContext,
                                final ComponentCache cache,
                                final DependencyInjector injector,
                                final LogFactory logs) {
        super(priority, api, cache, logs);
        this.ignoreContext = ignoreContext;
        this.injector = injector;
        this.componentClass = componentClass;
        this.constructor = findComponentConstructor();
    }

    public Annotation[] annotations() {
        return ignoreContext ? null : componentClass.getAnnotations();
    }

    public Set<Class<? extends Annotation>> acceptedContext() {
        return ignoreContext ? null : AbstractResolver.acceptedContext(componentClass);
    }

    private synchronized Constructor<?> constructor() {
        return constructor;
    }

    private Constructor<?> findComponentConstructor() {

        /*
         * There is no check for any constructor parameter being satisfiable. Synthetic constructors are ignored. If there is any constructor annotated with
         * @Component, all other constructors are ignored. If there's only one constructor, it is returned. If there is a default constructor
         * and another, and neither is annotated with @Component, the other one is returned. If there are more constructors, the only one annotated with
         * @Component is returned. If these checks do not yield a single constructor, the same is repeated for public constructors only. If that yields no or
         * multiple constructors, a ComponentContainer.ResolutionException is thrown.
         *
         * For synthetic constructors see http://java.sun.com/docs/books/jls/third_edition/html/binaryComp.html#13.1, "synthetic"
         */

        Constructor<?> designated = null;

        final List<Constructor<?>> validConstructors = new ArrayList<Constructor<?>>();
        for (final Constructor<?> constructor : componentClass.getDeclaredConstructors()) {
            if (!constructor.isSynthetic()) {
                if (designated == null) {
                    validConstructors.add(constructor);
                }

                if (constructor.isAnnotationPresent(Inject.class)) {
                    if (designated != null) {
                        throw new ComponentContainer.ResolutionException("Multiple @%s constructors found for %s", Inject.class, componentClass);
                    } else {
                        designated = constructor;
                    }
                }
            }
        }

        if (designated != null) {
            return designated;
        }

        try {
            return selectConstructor(validConstructors);
        } catch (final MultipleConstructorsException e) {
            final List<Constructor<?>> publicConstructors = new ArrayList<Constructor<?>>();

            for (final Constructor<?> constructor1 : validConstructors) {
                if (Modifier.isPublic(constructor1.getModifiers())) {
                    publicConstructors.add(constructor1);
                }
            }

            return selectConstructor(publicConstructors);
        }
    }

    private Constructor<?> selectConstructor(final List<Constructor<?>> constructors) {
        switch (constructors.size()) {
        case 0:
            throw new NoConstructorException(componentClass);
        case 1:
            return constructors.get(0);
        case 2:

            // return the one with more than 0 parameters
            final int parameterCount0 = constructors.get(0).getParameterTypes().length;
            final int parameterCount1 = constructors.get(1).getParameterTypes().length;

            if (parameterCount0 == 0 && parameterCount1 != 0) {
                return constructors.get(1);
            } else if (parameterCount0 != 0 && parameterCount1 == 0) {
                return constructors.get(0);
            }

            // fall through
        default:
            throw new MultipleConstructorsException(componentClass);
        }
    }

    public DependencyGraph.Node resolve(final DependencyGraph.Traversal traversal, final SimpleContainer container, final ContextDefinition context) {
        return traversal.follow(container, context, new DependencyGraph.Node.Reference() {
            public Class<?> api() {
                return api;
            }

            public DependencyGraph.Node resolve(final DependencyGraph.Traversal traversal, final ContextDefinition context) {
                return cachingNode(injector.constructor(traversal, container, ConstructingResolver.this, context, constructor()), container);
            }
        });
    }

    @Override
    public String toString() {
        return componentClass.getName();
    }

    private static class MultipleConstructorsException extends ComponentContainer.ResolutionException {

        public MultipleConstructorsException(final Class<?> componentClass) {
            super("Multiple constructors found for %s", componentClass);
        }
    }

    private static class NoConstructorException extends ComponentContainer.ResolutionException {

        public NoConstructorException(final Class<?> componentClass) {
            super("No suitable constructor found for %s", componentClass);
        }
    }
}
