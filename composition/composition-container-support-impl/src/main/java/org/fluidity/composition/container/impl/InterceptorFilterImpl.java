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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.fluidity.composition.Component;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.spi.ComponentInterceptor;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Methods;
import org.fluidity.foundation.Strings;

/**
 * @author Tibor Varga
 */
@Component(automatic = false)
final class InterceptorFilterImpl implements InterceptorFilter {

    public ComponentInterceptor[] filter(final ContextDefinition context, final ComponentInterceptor[] interceptors) {
        if (interceptors == null) {
            return null;
        } else {
            final Map<Class<? extends Annotation>, Integer> indexes = new HashMap<Class<? extends Annotation>, Integer>();

            int index = 0;
            for (final Class<? extends Annotation> annotation : context.defined().keySet()) {
                indexes.put(annotation, index++);
            }

            final Collection<Descriptor> descriptors = new TreeSet<Descriptor>();

            for (final ComponentInterceptor interceptor : interceptors) {
                final Descriptor descriptor = new Descriptor(interceptor, indexes);

                final Set<Class<? extends Annotation>> annotations = context.copy().accept(descriptor.type).active().keySet();
                final boolean add = annotations.containsAll(descriptor.context);

                if (add) {
                    descriptors.add(descriptor);
                }
            }

            final ComponentInterceptor[] sorted = new ComponentInterceptor[descriptors.size()];

            index = 0;
            for (final Descriptor descriptor : descriptors) {
                sorted[index++] = descriptor.interceptor;
            }

            return sorted;
        }
    }

    /**
     * Describes an interceptor. Used in {@link InterceptorFilter#filter(ContextDefinition, ComponentInterceptor[]) InterceptorFilter.filter()} internally to
     * handle {@link ComponentInterceptor ComponentInterceptors}.
     *
     * @author Tibor Varga
     */
    private static class Descriptor implements Comparable<Descriptor> {

        private static String collection = Methods.get(Component.Context.class, new Methods.Invoker<Component.Context>() {
            public void invoke(final Component.Context capture) throws Throwable {
                capture.collect();
            }
        })[0].getName();

        public final Class<?> type;
        public final Collection<Class<? extends Annotation>> context;
        public final ComponentInterceptor interceptor;

        final Integer index;

        Descriptor(final ComponentInterceptor interceptor, final Map<Class<? extends Annotation>, Integer> indexes) {
            this.type = interceptor.getClass();

            final Component.Context context = this.type.getAnnotation(Component.Context.class);

            this.context = context == null ? Collections.<Class<? extends Annotation>>emptyList() : Arrays.asList(context.value());

            for (final Class<? extends Annotation> annotation : this.context) {
                final Component.Context specification = annotation.getAnnotation(Component.Context.class);

                if (specification == null || specification.collect() != Component.Context.Collection.IMMEDIATE) {
                    throw new IllegalArgumentException(String.format("Context annotation type %s used by component interceptor %s must have a @%s(%s = %s) annotation",
                                                                     Strings.formatClass(false, true, annotation),
                                                                     Strings.formatClass(false, true, this.type),
                                                                     Strings.formatClass(false, false, Component.Context.class),
                                                                     collection,
                                                                     Component.Context.Collection.IMMEDIATE.name()));
                }
            }

            this.interceptor = interceptor;

            this.index = max(this.context, indexes);
        }

        @Override
        public String toString() {
            return toString(true);
        }

        public String toString(final boolean full) {
            final Lists.Delimited annotations = Lists.delimited();

            if (full) {
                for (final Class<? extends Annotation> type : context) {
                    annotations.add("@").append(Strings.formatClass(false, false, type));
                }
            }

            final String kind = Strings.formatClass(false, false, type);
            return annotations.isEmpty() ? kind : String.format("%s (%s)", kind, annotations);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Descriptor that = (Descriptor) o;
            return type == that.type;
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }

        public int compareTo(final Descriptor that) {
            return that.index.compareTo(this.index);
        }

        private Integer max(final Collection<Class<? extends Annotation>> annotations, final Map<Class<? extends Annotation>, Integer> indexes) {
            int value = -1;

            for (final Class<? extends Annotation> annotation : annotations) {
                final Integer index = indexes.get(annotation);
                value = index != null && value < index ? index : value;
            }

            return value;
        }
    }
}
