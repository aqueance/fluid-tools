/*
 * Copyright (c) 2006-2018 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Qualifier;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Methods;
import org.fluidity.foundation.Strings;

/**
 * @author Tibor Varga
 */
final class ContextDefinitionImpl implements ContextDefinition {

    private static final Qualifier.Composition DEFAULT_COMPOSITION = (Qualifier.Composition) Methods.get(Qualifier.class, Qualifier::value)[0].getDefaultValue();

    private final Map<Class<? extends Annotation>, Annotation[]> defined = new LinkedHashMap<>();
    private final Map<Class<? extends Annotation>, Annotation[]> active = new HashMap<>();

    private final Deferred.Reference<Integer> hashCode = Deferred.shared(() -> AnnotationMaps.hashCode(defined));

    ContextDefinitionImpl() {
        // empty
    }

    private ContextDefinitionImpl(final Map<Class<? extends Annotation>, Annotation[]> defined,
                                  final Map<Class<? extends Annotation>, Annotation[]> active) {
        copy(defined, this.defined);
        copy(active, this.active);
    }

    private ContextDefinitionImpl(final Type reference,
                                  final Map<Class<? extends Annotation>, Annotation[]> defined,
                                  final Map<Class<? extends Annotation>, Annotation[]> active,
                                  final boolean refine) {
        this(defined, active);

        assert reference != null;
        final Component.Reference inherited = reference();

        if (!refine) {

            // remove all non-inherited context
            this.defined.keySet().removeIf(annotation -> composition(annotation) == Qualifier.Composition.IMMEDIATE);
        }

        this.defined.put(Component.Reference.class, new Annotation[] { new ComponentReference(reference, inherited) });
    }

    @Override
    public ContextDefinition expand(final Annotation[] definition) {
        if (definition != null && definition.length > 0) {

            // first remove all ignored context
            for (final Annotation value : definition) {
                if (value instanceof Component.Qualifiers) {
                    for (final Class<? extends Annotation> ignore : ((Component.Qualifiers) value).ignore()) {
                        defined.remove(ignore);
                    }
                }
            }

            // then extend the context definition
            for (final Annotation value : definition) {
                final Class<? extends Annotation> type = value.annotationType();
                final Qualifier.Composition composition = composition(type);

                if (composition != Qualifier.Composition.NONE) {
                    final Annotation[] annotations = { value };

                    final Annotation[] present = defined.remove(type);

                    if (present != null) {
                        defined.put(type, composition == Qualifier.Composition.ALL ? combine(present, value) : annotations);
                    } else {
                        defined.put(type, annotations);
                    }
                }
            }

            hashCode.invalidate();
        }

        return this;
    }

    @Override
    public ContextDefinition copy() {
        return new ContextDefinitionImpl(defined, active);
    }

    @Override
    public ContextDefinition advance(final Type reference, final boolean refine) {
        return new ContextDefinitionImpl(reference, defined, active, refine);
    }

    @Override
    public ContextDefinition accept(final Class<?> type) {
        active.clear();

        if (type != null) {
            final Component.Qualifiers annotation = type.getAnnotation(Component.Qualifiers.class);

            if (annotation != null) {
                final Set<Class<? extends Annotation>> context = new HashSet<>(Arrays.asList(annotation.value()));

                active.putAll(defined);
                active.keySet().retainAll(context);

                active.values().removeIf(array -> array.length == 0);
            }
        }

        return this;
    }

    @Override
    public ContextDefinition collect(final Collection<ContextDefinition> contexts) {
        contexts.forEach(this::collectOne);
        return this;
    }

    private void collectOne(final ContextDefinition context) {
        if (context != null) {
            final Map<Class<? extends Annotation>, Annotation[]> map = context.active();

            for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : map.entrySet()) {
                final Class<? extends Annotation> type = entry.getKey();
                final Qualifier.Composition composition = composition(type);

                if (composition != Qualifier.Composition.IMMEDIATE && defined.containsKey(type)) {
                    final List<Annotation> present = Arrays.asList(defined.get(type));
                    final Annotation[] annotations = entry.getValue();
                    final Set<Annotation> retained = new HashSet<>(Arrays.asList(annotations));

                    retained.retainAll(present);

                    if (!retained.isEmpty()) {
                        if (composition == Qualifier.Composition.ALL) {
                            final Annotation[] updates = Lists.asArray(Annotation.class, retained);
                            active.put(type, active.containsKey(type) ? combine(active.get(type), updates) : updates);
                        } else if (composition == Qualifier.Composition.LAST) {
                            assert present.size() == 1 : present;
                            active.put(type, annotations);
                        } else {
                            assert false : composition;
                        }
                    }
                }
            }
        }
    }

    private Qualifier.Composition composition(final Class<? extends Annotation> type) {
        final Qualifier annotation = type.getAnnotation(Qualifier.class);
        return annotation == null ? DEFAULT_COMPOSITION : annotation.value();
    }

    @Override
    public Map<Class<? extends Annotation>, Annotation[]> defined() {
        return Collections.unmodifiableMap(defined);
    }

    @Override
    public Map<Class<? extends Annotation>, Annotation[]> active() {
        return Collections.unmodifiableMap(active);
    }

    @Override
    public Component.Reference reference() {
        final Annotation[] annotations = defined.get(Component.Reference.class);
        return annotations == null ? null : (Component.Reference) annotations[0];
    }

    @Override
    public boolean isEmpty() {
        return defined.isEmpty();
    }

    @Override
    public ComponentContext create() {
        return create(active);
    }

    @Override
    public ComponentContext create(final Map<Class<? extends Annotation>, Annotation[]> map) {
        return new ComponentContextImpl(map);
    }

    private void copy(final Map<Class<? extends Annotation>, Annotation[]> in, final Map<Class<? extends Annotation>, Annotation[]> out) {
        for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : in.entrySet()) {
            out.put(entry.getKey(), entry.getValue().clone());
        }
    }

    private Annotation[] combine(final Annotation[] present, final Annotation... addition) {
        final Collection<Annotation> list = new LinkedHashSet<>(present.length + addition.length);

        list.addAll(Arrays.asList(present));
        list.addAll(Arrays.asList(addition));

        return Lists.asArray(Annotation.class, list);
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
        return AnnotationMaps.descriptor(defined);
    }

    private static class ComponentReference implements Component.Reference {

        private final Type reference;
        private final Type canonical;

        ComponentReference(final Type reference, final Component.Reference inbound) {
            this.reference = inbound == null ? reference : Generics.propagate(inbound.type(), reference, true);
            this.canonical = Generics.canonicalType(this.reference);
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
            return canonical.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Component.Reference && Objects.equals(canonical, ((Component.Reference) obj).type());
        }

        @Override
        public String toString() {
            return  '@' + Strings.formatClass(false, false, annotationType()) + '(' + Generics.toString(true, canonical) + ')';
        }
    }
}
