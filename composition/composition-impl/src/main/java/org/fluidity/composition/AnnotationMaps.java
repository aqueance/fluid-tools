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
import java.util.Arrays;
import java.util.Map;

/**
 * Convenience methods on maps of arrays of annotations.
 */
final class AnnotationMaps {

    public AnnotationMaps() {
        throw new UnsupportedOperationException("No instance allowed");
    }

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

        for (final Annotation[] annotations : map.values()) {
            result += Arrays.hashCode(annotations);
        }

        return result;
    }
}