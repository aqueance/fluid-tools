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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Tibor Varga
 */
final class ComponentContextImpl implements ComponentContext {

    private final Map<Class<? extends Annotation>, Annotation[]> map = new HashMap<Class<? extends Annotation>, Annotation[]>();

    public ComponentContextImpl(final Map<Class<? extends Annotation>, Annotation[]> map) {
        for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : map.entrySet()) {
            final Class<? extends Annotation> key = entry.getKey();

            @SuppressWarnings("unchecked")
            final Class<? extends Annotation> type = Proxy.isProxyClass(key) ? (Class<? extends Annotation>) key.getInterfaces()[0] : key;
            this.map.put(type, entry.getValue());
        }
    }

    public ComponentContextImpl(final ComponentContext parent, final ComponentContext child) {
        if (parent != null) {
            for (final Class<? extends Annotation> type : parent.types()) {
                map.put(type, parent.annotations(type));
            }
        }

        assert child != null;
        for (final Class<? extends Annotation> type : child.types()) {
            final Annotation[] addition = child.annotations(type);

            if (map.containsKey(type)) {
                map.put(type, combine(map.get(type), addition));
            } else {
                map.put(type, addition);
            }
        }
    }

    private Annotation[] combine(final Annotation[] present, final Annotation[] addition) {
        final Collection<Annotation> list = new LinkedHashSet<Annotation>();
        list.addAll(Arrays.asList(present));
        list.addAll(Arrays.asList(addition));
        return list.toArray(new Annotation[list.size()]);
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> T[] annotations(final Class<T> type) {
        return (T[]) map.get(type);
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> T annotation(final Class<T> type, final Class<?> reference) {
        final Annotation[] annotations = annotations(type);

        if (annotations != null && annotations.length > 0) {
            return (T) annotations[annotations.length - 1];
        } else if (reference == null) {
            return null;
        } else {
            throw new ComponentContainer.ResolutionException("Annotation %s is missing from %s dependency", type, reference);
        }
    }

    public boolean defines(final Class<? extends Annotation> type) {
        final Annotation[] annotations = annotations(type);
        return annotations != null && annotations.length > 0;
    }

    public Set<Class<? extends Annotation>> types() {
        return Collections.unmodifiableSet(map.keySet());
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o != null && getClass() == o.getClass() && map.equals(((ComponentContextImpl) o).map));
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : map.entrySet()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }

            builder.append(Arrays.asList(entry.getValue()));
        }

        return builder.toString();
    }
}
