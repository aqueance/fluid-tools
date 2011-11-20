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

package org.fluidity.composition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.spi.ContextDefinition;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Proxies;

/**
 * @author Tibor Varga
 */
final class ContextDefinitionImpl implements ContextDefinition {

    private final Map<Class<? extends Annotation>, Annotation[]> defined = new HashMap<Class<? extends Annotation>, Annotation[]>();
    private final Map<Class<? extends Annotation>, Annotation[]> active = new HashMap<Class<? extends Annotation>, Annotation[]>();

    private int hashCode;

    public ContextDefinitionImpl() {
        hashCode = AnnotationMaps.hashCode(clean(defined));
    }

    private ContextDefinitionImpl(final Map<Class<? extends Annotation>, Annotation[]> defined,
                                  final Map<Class<? extends Annotation>, Annotation[]> active) {
        copy(defined, this.defined);
        copy(active, this.active);

        this.hashCode = AnnotationMaps.hashCode(clean(this.defined));
    }

    public ContextDefinition expand(final Annotation[] definition, final Type reference) {
        if (definition != null || reference != null) {
            if (definition != null) {
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
                defined.put(Component.Reference.class, new Annotation[] { new ComponentReferenceImpl(reference) });
            }

            hashCode = AnnotationMaps.hashCode(clean(defined));
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

    public ContextDefinition copy() {
        return new ContextDefinitionImpl(defined, active);
    }

    public ComponentContext create() {
        return new ComponentContextImpl(active);
    }

    private void copy(final Map<Class<? extends Annotation>, Annotation[]> in, final Map<Class<? extends Annotation>, Annotation[]> out) {
        for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : in.entrySet()) {
            out.put(Proxies.api(entry.getKey()), entry.getValue().clone());
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
        return this == o || (o != null && getClass() == o.getClass() && AnnotationMaps.equal(clean(defined), clean(((ContextDefinitionImpl) o).defined)));
    }

    /*
     * The Component.Reference annotation is a special case in that
     *  - it tracks only the last reference rather than all dependency references in the instantiation path,
     *  - it should not contribute to context definition identity (hash code and equality check)
     */
    private Map<Class<? extends Annotation>, Annotation[]> clean(final Map<Class<? extends Annotation>, Annotation[]> map) {
        final Map<Class<? extends Annotation>, Annotation[]> clean = new HashMap<Class<? extends Annotation>, Annotation[]>(map);
        clean.remove(Component.Reference.class);
        return clean;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
/*

    @Override
    public String toString() {
        return String.format("Context %d%n  defined: %s%n  collected: %s",
                             System.identityHashCode(this),
                             AnnotationMaps.toString(defined),
                             AnnotationMaps.toString(collected));
    }
*/

    private static class ComponentReferenceImpl implements Component.Reference {
        private final Type reference;

        public ComponentReferenceImpl(final Type reference) {
            this.reference = reference;
        }

        public Type type() {
            return reference;
        }

        public Class<?> parameter(final int index) {
            final Class parameter = Generics.rawType(Generics.typeParameter(reference, index));

            if (parameter == null) {
                throw new ComponentContainer.ResolutionException("Type parameter %d missing from %s dependency", index, Generics.rawType(reference).getName());
            }

            return parameter;
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
