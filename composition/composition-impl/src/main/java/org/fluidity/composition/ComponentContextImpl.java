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
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Tibor Varga
 */
final class ComponentContextImpl implements ComponentContext {

    private final Map<Class<? extends Annotation>, Annotation[]> annotations = new HashMap<Class<? extends Annotation>, Annotation[]>();
    private final int hashCode;

    public ComponentContextImpl(final Map<Class<? extends Annotation>, Annotation[]> map) {
        for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : map.entrySet()) {
            annotations.put(entry.getKey(), entry.getValue().clone());
        }

        hashCode = AnnotationMaps.hashCode(annotations);
    }

    @SuppressWarnings({ "unchecked", "SuspiciousSystemArraycopy" })
    public <T extends Annotation> T[] annotations(final Class<T> type) {
        final Annotation[] array = annotations.get(type);

        if (array == null) {
            return null;
        } else {
            final T[] cast = (T[]) Array.newInstance(type, array.length);
            System.arraycopy(array, 0, cast, 0, cast.length);
            return cast;
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> T annotation(final Class<T> type, final Class<?> reference) {
        final Annotation[] annotations = annotations(type);

        if (annotations != null && annotations.length > 0) {
            return (T) annotations[annotations.length - 1];
        } else if (reference == null) {
            return null;
        } else {
            throw new ComponentContainer.ResolutionException("Annotation %s is not found for, or accepted by, the %s dependency", type.getName(), reference);
        }
    }

    public boolean defines(final Class<? extends Annotation> type) {
        final Annotation[] annotations = annotations(type);
        return annotations != null && annotations.length > 0;
    }

    public Set<Class<? extends Annotation>> types() {
        return Collections.unmodifiableSet(annotations.keySet());
    }

    @Override
    public String toString() {
        return AnnotationMaps.toString(annotations);
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o != null && getClass() == o.getClass() && AnnotationMaps.equal(annotations, ((ComponentContextImpl) o).annotations));
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
