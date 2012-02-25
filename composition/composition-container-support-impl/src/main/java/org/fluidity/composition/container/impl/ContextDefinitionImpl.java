/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.api.ContextDefinition;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Methods;
import org.fluidity.foundation.Strings;

/**
 * @author Tibor Varga
 */
final class ContextDefinitionImpl implements ContextDefinition {

    private final Map<Class<? extends Annotation>, Annotation[]> defined = new HashMap<Class<? extends Annotation>, Annotation[]>();
    private final Map<Class<? extends Annotation>, Annotation[]> active = new HashMap<Class<? extends Annotation>, Annotation[]>();

    private final Deferred.Reference.State<Integer> hashCode = Deferred.state(new Deferred.Factory<Integer>() {
        public Integer create() {
            return AnnotationMaps.hashCode(defined);
        }
    });

    private final Component.Context.Collection defaultSeries = (Component.Context.Collection) Methods.get(Component.Context.class, new Methods.Invoker<Component.Context>() {
        public void invoke(final Component.Context capture) throws Throwable {
            capture.collect();
        }
    }).getDefaultValue();

    public ContextDefinitionImpl() {
        // empty
    }

    private ContextDefinitionImpl(final Map<Class<? extends Annotation>, Annotation[]> defined,
                                  final Map<Class<? extends Annotation>, Annotation[]> active) {
        copy(defined, this.defined);
        copy(active, this.active);
    }

    private ContextDefinitionImpl(final Type reference,
                                  final Map<Class<? extends Annotation>, Annotation[]> defined,
                                  final Map<Class<? extends Annotation>, Annotation[]> active) {
        this(defined, active);

        assert reference != null;
        final Component.Reference inherited = reference();

        // remove all non-inherited context
        for (final Iterator<Class<? extends Annotation>> iterator = this.defined.keySet().iterator(); iterator.hasNext(); ) {
            if (series(iterator.next()) == Component.Context.Collection.IMMEDIATE) {
                iterator.remove();
            }
        }

        this.defined.put(Component.Reference.class, new Annotation[] { new ComponentReferenceImpl(reference, inherited) });
    }

    public ContextDefinition expand(final Annotation[] definition) {
        if (definition != null && definition.length > 0) {

            // first remove all ignored context
            for (final Annotation value : definition) {
                if (value instanceof Component.Context) {
                    for (final Class<? extends Annotation> ignore : ((Component.Context) value).ignore()) {
                        defined.remove(ignore);
                    }
                }
            }

            // then extend the context definition
            for (final Annotation value : definition) {
                final Class<? extends Annotation> type = value.annotationType();
                final Component.Context.Collection series = series(type);

                if (series != Component.Context.Collection.NONE) {
                    final Annotation[] annotations = { value };

                    if (defined.containsKey(type)) {
                        defined.put(type, series == Component.Context.Collection.ALL ? combine(defined.get(type), value) : annotations);
                    } else {
                        defined.put(type, annotations);
                    }
                }
            }

            hashCode.invalidate();
        }

        return this;
    }

    public ContextDefinition copy() {
        return new ContextDefinitionImpl(defined, active);
    }

    public ContextDefinition advance(final Type reference) {
        return new ContextDefinitionImpl(reference, defined, active);
    }

    public ContextDefinition accept(final Class<?> type) {
        active.clear();

        if (type != null) {
            final Component.Context annotation = type.getAnnotation(Component.Context.class);

            if (annotation != null) {
                final Set<Class<? extends Annotation>> context = new HashSet<Class<? extends Annotation>>(Arrays.asList(annotation.value()));

                active.putAll(defined);
                active.keySet().retainAll(context);

                for (final Iterator<Annotation[]> iterator = active.values().iterator(); iterator.hasNext(); ) {
                    if (iterator.next().length == 0) {
                        iterator.remove();
                    }
                }
            }
        }

        return this;
    }

    public ContextDefinition collect(final Collection<ContextDefinition> contexts) {
        for (final ContextDefinition context : contexts) {
            collectOne(context);
        }

        return this;
    }

    private void collectOne(final ContextDefinition context) {
        if (context != null) {
            final Map<Class<? extends Annotation>, Annotation[]> map = context.active();

            for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : map.entrySet()) {
                final Class<? extends Annotation> type = entry.getKey();
                final Component.Context.Collection series = series(type);

                if (series != Component.Context.Collection.IMMEDIATE && defined.containsKey(type)) {
                    final List<Annotation> present = Arrays.asList(defined.get(type));
                    final Annotation[] annotations = entry.getValue();
                    final Set<Annotation> retained = new HashSet<Annotation>(Arrays.asList(annotations));

                    retained.retainAll(present);

                    if (!retained.isEmpty()) {
                        if (series == Component.Context.Collection.ALL) {
                            final Annotation[] updates = retained.toArray(new Annotation[retained.size()]);
                            active.put(type, active.containsKey(type) ? combine(active.get(type), updates) : updates);
                        } else if (series == Component.Context.Collection.LAST) {
                            assert present.size() == 1 : present;
                            active.put(type, annotations);
                        } else {
                            assert false : series;
                        }
                    }
                }
            }
        }
    }

    private Component.Context.Collection series(final Class<? extends Annotation> type) {
        final Component.Context annotation = type.getAnnotation(Component.Context.class);
        return annotation == null ? defaultSeries : annotation.collect();
    }

    public Map<Class<? extends Annotation>, Annotation[]> active() {
        return Collections.unmodifiableMap(active);
    }

    public Component.Reference reference() {
        final Annotation[] annotations = defined.get(Component.Reference.class);
        return annotations == null ? null : (Component.Reference) annotations[0];
    }

    public boolean isEmpty() {
        return defined.isEmpty();
    }

    public ComponentContext create() {
        return create(active);
    }

    public ComponentContext create(final Map<Class<? extends Annotation>, Annotation[]> map) {
        return new ComponentContextImpl(map);
    }

    private void copy(final Map<Class<? extends Annotation>, Annotation[]> in, final Map<Class<? extends Annotation>, Annotation[]> out) {
        for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : in.entrySet()) {
            out.put(entry.getKey(), entry.getValue().clone());
        }
    }

    private Annotation[] combine(final Annotation[] present, final Annotation... addition) {
        final Collection<Annotation> list = new LinkedHashSet<Annotation>(present.length + addition.length);

        list.addAll(Arrays.asList(present));
        list.addAll(Arrays.asList(addition));

        return list.toArray(new Annotation[list.size()]);
    }

    @Override
    public boolean equals(final Object o) {
        final ContextDefinitionImpl that = (ContextDefinitionImpl) o;
        return this == o || (o != null && getClass() == o.getClass() && AnnotationMaps.equal(defined, that.defined));
    }

    @Override
    public int hashCode() {
        return hashCode.get();
    }

    @Override
    public String toString() {
        return AnnotationMaps.toString(defined);
    }

    private static class ComponentReferenceImpl implements Component.Reference {

        private final Type reference;

        public ComponentReferenceImpl(final Type reference, final Component.Reference inbound) {
            this.reference = inbound == null ? reference : Generics.propagate(inbound.type(), reference);
        }

        public Type type() {
            return reference;
        }

        public Class<?> parameter(final int index) {
            final Type type = Generics.typeParameter(reference, index);

            if (type instanceof TypeVariable || type instanceof WildcardType) {
                throw new ComponentContainer.ResolutionException("Type parameter %d could not be resolved in %s dependency", index, Generics.rawType(reference).getName());
            }

            return Generics.rawType(type);
        }

        public Class<? extends Annotation> annotationType() {
            return Component.Reference.class;
        }

        @Override
        public int hashCode() {
            return reference.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Component.Reference && reference.equals(((Component.Reference) obj).type());
        }

        @Override
        public String toString() {
            return Strings.printAnnotation(this);
        }
    }
}