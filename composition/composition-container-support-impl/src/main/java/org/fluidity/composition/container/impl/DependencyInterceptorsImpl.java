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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.ComponentContextDescriptor;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.container.spi.DependencyResolver;
import org.fluidity.composition.spi.ComponentInterceptor;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.Strings;

/**
 * @author Tibor Varga
 */
@Component(automatic = false)
final class DependencyInterceptorsImpl implements DependencyInterceptors {

    public static final InterceptorDescriptor[] NO_INTERCEPTORS = new InterceptorDescriptor[0];

    // recursion guard: no interception of dependencies of interceptors
    private final ThreadLocal<Boolean> intercepting = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private final Log log;

    public DependencyInterceptorsImpl(final Log log) {
        this.log = log;
    }

    private InterceptorDescriptor[] interceptors(final DependencyResolver container, final DependencyGraph.Traversal traversal) {
        if (!intercepting.get()) {
            intercepting.set(true);

            try {
                final DependencyGraph.Node group = container.resolveGroup(ComponentInterceptor.class, new ContextDefinitionImpl(), traversal);
                return descriptors(group == null ? null : (ComponentInterceptor[]) group.instance(traversal));
            } finally {
                intercepting.set(false);
            }
        } else {
            return NO_INTERCEPTORS;
        }
    }

    @SuppressWarnings("ConstantConditions")
    static InterceptorDescriptor[] descriptors(final ComponentInterceptor... instances) {
        final InterceptorDescriptor[] descriptors = new InterceptorDescriptor[instances == null ? 0 : instances.length];

        for (int i = 0, limit = descriptors.length; i < limit; i++) {
            descriptors[i] = new InterceptorDescriptor(instances[i]);
        }

        return descriptors;
    }

    public DependencyGraph.Node replace(final DependencyResolver container,
                                        final ContextDefinition context,
                                        final DependencyGraph.Traversal traversal,
                                        final Type reference,
                                        final DependencyGraph.Node node) {
        if (node == null) {
            return node;
        }

        final InterceptorDescriptor[] interceptors = context.filter(interceptors(container, traversal));

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

            for (final InterceptorDescriptor descriptor : interceptors) {
                final ComponentInterceptor.Dependency dependency = descriptor.interceptor.intercept(reference,
                                                                                                    context.copy().accept(descriptor.type).create(),
                                                                                                    next.get());

                if (dependency == null) {
                    return null;
                }

                next.set(dependency);
                applied.add(descriptor.toString(false));
            }

            log.debug("%s: interceptors for %s: %s", this, context, applied);

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

    static class InterceptorDescriptor extends ComponentContextDescriptor<ComponentInterceptor> {

        public final ComponentInterceptor interceptor;

        @SuppressWarnings("unchecked")
        public InterceptorDescriptor(final ComponentInterceptor interceptor) {
            super((Class<ComponentInterceptor>) interceptor.getClass());
            this.interceptor = interceptor;
        }

        @Override
        public String toString() {
            return toString(true);
        }

        public String toString(final boolean full) {
            final Strings.Listing annotations = Strings.delimited();

            if (full) {
                for (final Class<? extends Annotation> type : context) {
                    annotations.add("@").append(Strings.printClass(false, false, type));
                }
            }

            return annotations.isEmpty()
                   ? Strings.printClass(false, false, type)
                   : String.format("%s (%s)", Strings.printClass(false, false, type), annotations);
        }
    }
}
