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

package org.fluidity.composition.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Internal;
import org.fluidity.composition.spi.ContextDefinition;
import org.fluidity.foundation.Generics;

/**
 * @author Tibor Varga
 */
final class ContextDefinitionImpl implements ContextDefinition {

    private final Map<Class<? extends Annotation>, Annotation[]> defined = new HashMap<Class<? extends Annotation>, Annotation[]>();
    private final Map<Class<? extends Annotation>, Annotation[]> active = new HashMap<Class<? extends Annotation>, Annotation[]>();

    private int hashCode;

    public ContextDefinitionImpl() {
        hashCode = AnnotationMaps.hashCode(defined);
    }

    private ContextDefinitionImpl(final Map<Class<? extends Annotation>, Annotation[]> defined,
                                  final Map<Class<? extends Annotation>, Annotation[]> active) {
        copy(defined, this.defined);
        copy(active, this.active);

        this.hashCode = AnnotationMaps.hashCode(this.defined);
    }

    public ContextDefinition expand(final Annotation[] definition, final Type reference) {
        if (definition != null || reference != null) {
            if (definition != null) {

                // first remove all ignored context
                for (final Annotation value : definition) {
                    if (value instanceof Component.Context) {
                        for (final Class<? extends Annotation> ignore : ((Component.Context) value).ignore()) {
                            defined.remove(ignore);
                        }
                    }
                }

                // then define extend context definition
                for (final Annotation value : definition) {
                    final Class<? extends Annotation> type = value.annotationType();

                    if (!type.isAnnotationPresent(Internal.class)) {
                        if (defined.containsKey(type)) {
                            defined.put(type, combine(defined.get(type), value));
                        } else {
                            defined.put(type, new Annotation[] { value });
                        }
                    }
                }
            }

            if (reference != null) {

                // the parameterized dependency type is retained only for the last reference
                defined.put(Component.Reference.class, new Annotation[] { new ComponentReferenceImpl(reference, reference()) });
            }

            hashCode = AnnotationMaps.hashCode(defined);
        }

        return this;
    }

    public ContextDefinition accept(final Class<?> type) {
        if (type != null) {
            active.clear();

            final Component.Context annotation = type.getAnnotation(Component.Context.class);

            if (annotation != null) {
                final Set<Class<? extends Annotation>> context = new HashSet<Class<? extends Annotation>>(Arrays.asList(annotation.value()));

                if (annotation.typed()) {
                    context.add(Component.Reference.class);
                }

                active.putAll(defined);
                active.keySet().retainAll(context);
            }
        }

        return this;
    }

    public ContextDefinition collect(final Collection<ContextDefinition> contexts) {
        for (final ContextDefinition context : contexts) {
            collectOne(context);
        }

        active.keySet().retainAll(defined.keySet());

        return this;
    }

    private void collectOne(final ContextDefinition context) {
        if (context != null) {
            final Map<Class<? extends Annotation>, Annotation[]> map = context.active();

            for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : map.entrySet()) {
                final Class<? extends Annotation> type = entry.getKey();

                // the parameterized dependency type is not propagated backward
                if (!Component.Reference.class.isAssignableFrom(type)) {
                    final Annotation[] annotations = entry.getValue();

                    if (active.containsKey(type)) {
                        active.put(type, combine(active.get(type), annotations));
                    } else {
                        active.put(type, annotations);
                    }
                }
            }
        }
    }

    public Map<Class<? extends Annotation>, Annotation[]> active() {
        return Collections.unmodifiableMap(active);
    }

    public Component.Reference reference() {
        final Annotation[] annotations = defined.get(Component.Reference.class);
        return annotations == null ? null : (Component.Reference) annotations[0];
    }

    public ContextDefinition copy() {
        return new ContextDefinitionImpl(defined, active);
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
        return hashCode;
    }

/*
    @Override
    public String toString() {
        return String.format("defined: [%s] active: [%s]", AnnotationMaps.toString(defined), AnnotationMaps.toString(active));
    }
*/

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
            return String.format("@%s(type=%s)", Component.Reference.class.getName(), reference);
        }
    }
}
