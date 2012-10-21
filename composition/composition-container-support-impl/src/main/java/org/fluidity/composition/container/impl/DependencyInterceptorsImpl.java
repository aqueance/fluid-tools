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

package org.fluidity.composition.container.impl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.container.spi.DependencyResolver;
import org.fluidity.composition.spi.ComponentInterceptor;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.Strings;

/**
 * @author Tibor Varga
 */
@Component(automatic = false)
final class DependencyInterceptorsImpl implements DependencyInterceptors {

    static final ComponentInterceptor[] NO_INTERCEPTORS = new ComponentInterceptor[0];

    // recursion guard: no interception of dependencies of interceptors
    private final ThreadLocal<Boolean> intercepting = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private final InterceptorFilter annotations;
    private final Log log;

    DependencyInterceptorsImpl(final InterceptorFilter annotations, final Log log) {
        this.annotations = annotations;
        this.log = log;
    }

    private ComponentInterceptor[] interceptors(final DependencyResolver container, final DependencyGraph.Traversal traversal) {
        if (!intercepting.get()) {
            intercepting.set(true);

            try {
                final DependencyGraph.Node group = container.resolveGroup(ComponentInterceptor.class, new ContextDefinitionImpl(), traversal, ComponentInterceptor.class);
                return group == null ? NO_INTERCEPTORS : (ComponentInterceptor[]) group.instance(traversal);
            } finally {
                intercepting.set(false);
            }
        } else {
            return NO_INTERCEPTORS;
        }
    }

    public DependencyGraph.Node replace(final DependencyResolver container,
                                        final ContextDefinition context,
                                        final DependencyGraph.Traversal traversal,
                                        final Type reference,
                                        final DependencyGraph.Node node) {
        if (node == null) {
            return node;
        }

        final ComponentInterceptor[] interceptors = annotations.filter(context, interceptors(container, traversal));

        if (interceptors.length > 0) {
            final AtomicReference<ComponentInterceptor.Dependency> last = new AtomicReference<ComponentInterceptor.Dependency>();

            final AtomicReference<ComponentInterceptor.Dependency> next = new AtomicReference<ComponentInterceptor.Dependency>(new ComponentInterceptor.Dependency() {
                public Object create() {
                    final ComponentInterceptor.Dependency dependency = last.get();

                    if (dependency == null) {
                        throw new ComponentContainer.ResolutionException("Dependency access during interception");
                    }

                    return dependency.create();
                }
            });

            final List<String> applied = new ArrayList<String>();

            for (final ComponentInterceptor interceptor : interceptors) {
                final Type type = Generics.typeParameter(Generics.specializedType(interceptor.getClass(), ComponentInterceptor.class), 0);

                if (type == Object.class || Generics.isAssignable(reference, type)) {
                    @SuppressWarnings("unchecked")
                    final ComponentInterceptor.Dependency dependency = interceptor.intercept(reference,
                                                                                             context.copy().accept(interceptor.getClass()).create(),
                                                                                             next.get());

                    if (dependency == null) {
                        return null;
                    }

                    next.set(dependency);
                    applied.add(Strings.formatClass(false, false, interceptor.getClass()));
                }
            }

            log.debug("%s: interceptors for %s: %s", container, context, applied);

            return new DependencyGraph.Node() {
                public Class<?> type() {
                    return node.type();
                }

                public Object instance(final DependencyGraph.Traversal traversal) {
                    last.set(new ComponentInterceptor.Dependency() {
                        public Object create() {
                            return node.instance(traversal);
                        }
                    });

                    return next.get().create();
                }

                public ComponentContext context() {
                    return node.context();
                }
            };
        } else {
            return node;
        }
    }
}
