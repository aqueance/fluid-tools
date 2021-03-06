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
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.StringJoiner;
import java.util.TreeMap;

import org.fluidity.foundation.Strings;
import org.fluidity.foundation.Utility;

import static java.util.Comparator.comparing;

/**
 * Convenience methods on maps of arrays of annotations.
 */
@SuppressWarnings("WeakerAccess")
final class AnnotationMaps extends Utility {

    private AnnotationMaps() { }

    @SuppressWarnings("EqualsReplaceableByObjectsCall")
    public static boolean equal(Map<Class<? extends Annotation>, Annotation[]> map1, Map<Class<? extends Annotation>, Annotation[]> map2) {
        if (map1 == map2) {
            return true;
        }

        if (!map1.keySet().equals(map2.keySet())) {
            return false;
        }

        for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : map1.entrySet()) {
            final Annotation[] list1 = entry.getValue();
            final Annotation[] list2 = map2.get(entry.getKey());

            if (list1 != list2 && !Arrays.equals(list1, list2)) {
                return false;
            }
        }

        return true;
    }

    public static int hashCode(final Map<Class<? extends Annotation>, Annotation[]> map) {
        int result = map.keySet().hashCode();

        for (final Annotation[] annotations : sorted(map).values()) {
            result = Objects.hash(result, Arrays.hashCode(annotations));
        }

        return result;
    }

    public static String descriptor(final Map<Class<? extends Annotation>, Annotation[]> map) {
        return toString(false, map);
    }

    public static String identity(final Map<Class<? extends Annotation>, Annotation[]> map) {
        return toString(true, map);
    }

    private static String toString(final boolean identity, final Map<Class<? extends Annotation>, Annotation[]> map) {
        final StringJoiner list = new StringJoiner(" ");

        for (final Annotation[] annotations : (map instanceof SortedMap ? map : sorted(map)).values()) {
            for (final Annotation annotation : annotations) {
                list.add(Strings.describeAnnotation(identity, annotation));
            }
        }

        return list.toString();
    }

    private static Map<Class<? extends Annotation>, Annotation[]> sorted(final Map<Class<? extends Annotation>, Annotation[]> map) {
        final Map<Class<? extends Annotation>, Annotation[]> sorted = new TreeMap<>(comparing(Class::getName));
        sorted.putAll(map);
        return sorted;
    }
}
