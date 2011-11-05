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
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import org.fluidity.foundation.Strings;
import org.fluidity.foundation.Utilities;

/**
 * Convenience methods on maps of arrays of annotations.
 */
final class AnnotationMaps extends Utilities {

    public static boolean equal(Map<Class<? extends Annotation>, Annotation[]> map1, Map<Class<? extends Annotation>, Annotation[]> map2) {
        if (map1 == map2) {
            return true;
        }

        if (!map1.keySet().equals(map1.keySet())) {
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
            result += 31 * Arrays.hashCode(annotations);
        }

        return result;
    }

    public static String toString(final Map<Class<? extends Annotation>, Annotation[]> map) {
        final StringBuilder builder = new StringBuilder();

        boolean multiple = false;
        for (final Map.Entry<Class<? extends Annotation>, Annotation[]> entry : sorted(map).entrySet()) {
            if (builder.length() > 0) {
                builder.append(", ");
                multiple = true;
            }

            for (final Annotation annotation : entry.getValue()) {
                builder.append(Strings.simpleNotation(annotation));
            }
        }

        return (multiple ? builder.insert(0, '[').append(']') : builder).toString();
    }

    private static Map<Class<? extends Annotation>, Annotation[]> sorted(final Map<Class<? extends Annotation>, Annotation[]> map) {
        final Map<Class<? extends Annotation>, Annotation[]> sorted = new TreeMap<Class<? extends Annotation>, Annotation[]>(new Comparator<Class<? extends Annotation >> () {
            public int compare(final Class<? extends Annotation> o1, final Class<? extends Annotation> o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        sorted.putAll(map);
        return sorted;
    }
}
