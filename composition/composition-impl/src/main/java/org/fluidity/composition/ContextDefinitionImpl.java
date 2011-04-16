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
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Tibor Varga
 */
final class ContextDefinitionImpl implements ContextDefinition {

    private final Map<Class<? extends Annotation>, Annotation[]> defined = new HashMap<Class<? extends Annotation>, Annotation[]>();
    private final Map<Class<? extends Annotation>, Annotation[]> collected = new HashMap<Class<? extends Annotation>, Annotation[]>();
    private int hashCode;

    public ContextDefinitionImpl() {
        hashCode = AnnotationMaps.hashCode(defined);
    }

    private ContextDefinitionImpl(final Map<Class<? extends Annotation>, Annotation[]> defined,
                                  final Map<Class<? extends Annotation>, Annotation[]> collected) {
        copy(defined, this.defined);
        copy(collected, this.collected);
        hashCode = AnnotationMaps.hashCode(this.defined);
    }

    public ContextDefinition expand(final Annotation[] definition) {
        if (definition != null) {
            for (final Annotation value : definition) {
                final Class<? extends Annotation> type = annotationType(value.getClass());

                if (!type.isAnnotationPresent(Internal.class)) {
                    if (defined.containsKey(type)) {
                        defined.put(type, combine(defined.get(type), value));
                    } else {
                        defined.put(type, new Annotation[] { value });
                    }
                }
            }

            hashCode = AnnotationMaps.hashCode(this.defined);
        }

        return this;
    }

    public ContextDefinition reduce(final Set<Class<? extends Annotation>> accepted) {
        collected.clear();

        if (accepted != null) {
            collected.putAll(defined);
            collected.keySet().retainAll(accepted);
        }

        return this;
    }

    public ContextDefinition collect(final Collection<ContextDefinition> contexts) {
        for (final ContextDefinition context : contexts) {
            collectOne(context);
        }

        collected.keySet().retainAll(defined.keySet());

        return this;
    }

    public void collect(final ContextDefinition context) {
        collectOne(context);
        collected.keySet().retainAll(defined.keySet());
    }

    private void collectOne(final ContextDefinition context) {
        if (context != null) {
            final Map<Class<? extends Annotation>, Annotation[]> map = context.collected();

            for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : map.entrySet()) {
                final Class<? extends Annotation> type = entry.getKey();
                final Annotation[] annotations = entry.getValue();

                if (collected.containsKey(type)) {
                    collected.put(type, combine(collected.get(type), annotations));
                } else {
                    collected.put(type, annotations);
                }
            }
        }
    }

    public Map<Class<? extends Annotation>, Annotation[]> defined() {
        return Collections.unmodifiableMap(defined);
    }

    public Map<Class<? extends Annotation>, Annotation[]> collected() {
        return Collections.unmodifiableMap(collected);
    }

    public ContextDefinition copy() {
        return new ContextDefinitionImpl(this.defined, this.collected);
    }

    public ComponentContext create() {
        return new ComponentContextImpl(collected);
    }

    private void copy(final Map<Class<? extends Annotation>, Annotation[]> in, final Map<Class<? extends Annotation>, Annotation[]> out) {
        for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : in.entrySet()) {
            final Class<? extends Annotation> key = entry.getKey();

            final Class<? extends Annotation> type = annotationType(key);
            out.put(type, entry.getValue().clone());
        }
    }

    private Annotation[] combine(final Annotation[] present, final Annotation... addition) {
        final Collection<Annotation> list = new LinkedHashSet<Annotation>(present.length + addition.length);
        list.addAll(Arrays.asList(present));
        list.addAll(Arrays.asList(addition));
        return list.toArray(new Annotation[list.size()]);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> annotationType(Class<? extends Annotation> key) {
        return Proxy.isProxyClass(key) ? (Class<? extends Annotation>) key.getInterfaces()[0] : key;
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o != null && getClass() == o.getClass() && AnnotationMaps.equal(defined, ((ContextDefinitionImpl) o).defined));
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return String.format("Context %d%n  defined: %s%n  collected: %s",
                             System.identityHashCode(this),
                             AnnotationMaps.toString(defined),
                             AnnotationMaps.toString(collected));
    }
}
