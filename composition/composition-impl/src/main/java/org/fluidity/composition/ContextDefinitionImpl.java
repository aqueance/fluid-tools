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
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Tibor Varga
 */
final class ContextDefinitionImpl implements ContextDefinition {

    private static final Set<Class<?>> notContext = new HashSet<Class<?>>(Arrays.asList(Component.class, Optional.class, Context.class, ServiceProvider.class));

    private final Map<Class<? extends Annotation>, Annotation[]> defined = new HashMap<Class<? extends Annotation>, Annotation[]>();
    private final Map<Class<? extends Annotation>, Annotation[]> collected = new HashMap<Class<? extends Annotation>, Annotation[]>();

    public ContextDefinitionImpl() {
        // empty
    }

    private ContextDefinitionImpl(final Map<Class<? extends Annotation>, Annotation[]> defined,
                                  final Map<Class<? extends Annotation>, Annotation[]> collected) {
        copy(defined, this.defined);
        copy(collected, this.collected);
    }

    public ContextDefinition expand(final Annotation[] definition) {
        if (definition != null) {
            for (final Annotation value : definition) {
                final Class<? extends Annotation> type = noProxy(value.getClass());

                if (defined.containsKey(type)) {
                    defined.put(type, combine(defined.get(type), value));
                } else {
                    defined.put(type, new Annotation[] { value });
                }
            }

            this.defined.keySet().removeAll(notContext);
        }

        return this;
    }

    public ContextDefinition reduce(final Context accepted) {
        if (accepted != null) {
            collected.putAll(defined);
            collected.keySet().retainAll(Arrays.asList(accepted.value()));
        } else {
            collected.clear();
        }

        return this;
    }

    public ContextDefinition collect(final Collection<ContextDefinition> contexts) {
        for (final ContextDefinition context : contexts) {
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

        collected.keySet().retainAll(defined.keySet());

        return this;
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

            final Class<? extends Annotation> type = noProxy(key);
            out.put(type, entry.getValue().clone());
        }
    }

    private Annotation[] combine(final Annotation[] present, final Annotation... addition) {
        final Collection<Annotation> list = new LinkedHashSet<Annotation>();
        list.addAll(Arrays.asList(present));
        list.addAll(Arrays.asList(addition));
        return list.toArray(new Annotation[list.size()]);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> noProxy(Class<? extends Annotation> key) {
        return Proxy.isProxyClass(key) ? (Class<? extends Annotation>) key.getInterfaces()[0] : key;
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o != null && getClass() == o.getClass() && defined.equals(((ContextDefinitionImpl) o).defined));
    }

    @Override
    public int hashCode() {
        return defined.hashCode();
    }
}
