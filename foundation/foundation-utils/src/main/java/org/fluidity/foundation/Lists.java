/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.foundation;

import java.lang.reflect.Array;
import java.util.Collection;

/**
 * Utilities related to arrays.
 * <h3>Usage Example</h3>
 * <pre>
 * final Constructor&lt;?> constructor = &hellip;
 *
 * final Annotation[] annotations = <span class="hl1">Lists</span>.concatenate(constructor.{@linkplain java.lang.reflect.Constructor#getDeclaringClass() getDeclaringClass}().{@linkplain Class#getAnnotations() getAnnotations}(),
 *                                                    constructor.{@linkplain java.lang.reflect.Constructor#getAnnotations() getAnnotations}(),
 *                                                    constructor.{@linkplain java.lang.reflect.Constructor#getParameterAnnotations() getParameterAnnotations}()[0]);
 * </pre>
 *
 * @author Tibor Varga
 */
public class Lists extends Utility {

    private Lists() { }

    /**
     * Returns either the <code>array</code> parameter or, if it is <code>null</code>, an empty array with the given component <code>type</code>.
     *
     * @param type  the component type of the array.
     * @param array the array.
     * @param <T>   the component type of the array.
     *
     * @return either the <code>array</code> parameter or, if it is <code>null</code>, an empty array with the given component <code>type</code>.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] notNull(final Class<T> type, final T[] array) {
        return array == null ? (T[]) Array.newInstance(type, 0) : array;
    }

    /**
     * Concatenates the given list of <code>arrays</code>.
     *
     * @param type   the component type of the arrays; any item may be <code>null</code>.
     * @param arrays the array list.
     * @param <T>    the component type of the arrays.
     *
     * @return a new array containing the values from all non-<code>null</code> <code>arrays</code>.
     */
    public static <T> T[] concatenate(final Class<T> type, final T[]... arrays) {
        int length = 0;

        for (final T[] array : arrays) {
            length += array == null ? 0 : array.length;
        }

        @SuppressWarnings("unchecked")
        final T[] all = (T[]) Array.newInstance(type, length);

        int tail = 0;
        for (final T[] array : arrays) {
            int size = array == null ? 0 : array.length;

            if (size > 0) {
                System.arraycopy(array, 0, all, tail, size);
                tail += size;
            }
        }

        return all;
    }

    /**
     * Complements {@link java.util.Arrays#asList(Object[])} with a conversion to the opposite direction. This is a convenience method that calls {@link
     * Collection#toArray(Object[])} on the received <code>list</code> with the <code>list</code>'s sized array and, in most cases, does all the unchecked type
     * casts needed for the operation.
     *
     * @param list is the list to convert to an array; may be <code>null</code>.
     * @param type the item type of the list/array; may not be <code>null</code>.
     * @param <T>  the generic item type of the list/array.
     *
     * @return an array containing the elements of the given list; never <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] asArray(final Collection<T> list, final Class<? super T> type) {
        return list == null ? (T[]) Array.newInstance(type, 0) : list.toArray((T[]) Array.newInstance(type, list.size()));
    }
}
