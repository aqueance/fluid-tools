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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.spi.ContextDefinition;
import org.fluidity.foundation.Proxies;

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

    public ContextDefinition expand(final Annotation[] definition) {
        if (definition != null) {
            for (final Annotation value : definition) {
                final Class<? extends Annotation> type = Proxies.api(value.getClass());

                if (!type.isAnnotationPresent(Internal.class)) {
                    if (defined.containsKey(type)) {
                        defined.put(type, combine(defined.get(type), value));
                    } else {
                        defined.put(type, new Annotation[] { value });
                    }
                }
            }

            hashCode = AnnotationMaps.hashCode(defined);
        }

        return this;
    }

    public ContextDefinition accept(final Set<Class<? extends Annotation>> consumed) {
        active.clear();

        if (consumed != null) {
            active.putAll(defined);
            active.keySet().retainAll(consumed);
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
                final Annotation[] annotations = entry.getValue();

                if (active.containsKey(type)) {
                    active.put(type, combine(active.get(type), annotations));
                } else {
                    active.put(type, annotations);
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
        return this == o || (o != null && getClass() == o.getClass() && AnnotationMaps.equal(defined, ((ContextDefinitionImpl) o).defined));
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
}
