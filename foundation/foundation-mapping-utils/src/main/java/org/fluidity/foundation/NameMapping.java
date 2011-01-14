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

package org.fluidity.foundation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.fluidity.foundation.spi.Named;

/**
 * Utilities for {@link Named} objects.
 *
 * @author Tibor Varga
 */
public final class NameMapping {

    private NameMapping() {
        throw new UnsupportedOperationException("No instances allowed");
    }

    /**
     * Converts the given collection of {@link Named} objects to a map whose keys are the objects' name. The returned map retains the original order of the
     * items.
     *
     * @param objects the collection of objects to convert into a map.
     *
     * @return a <code>Map</code>, never <code>null</code>.
     */
    public static <T extends Named> Map<String, T> asOrderedMap(final Collection<T> objects) {
        return asMap(objects, new LinkedHashMap<String, T>());
    }

    /**
     * Converts the given collection of {@link Named} objects to a map whose keys are the objects' name. The order of the items in the returned map is
     * arbitrary.
     *
     * @param objects the collection of objects to convert into a map.
     *
     * @return a <code>Map</code>, never <code>null</code>.
     */
    public static <T extends Named> Map<String, T> asUnorderedMap(final Collection<T> objects) {
        return asMap(objects, new HashMap<String, T>());
    }

    /**
     * Converts the given collection of {@link Named} objects to a map whose keys are the objects' name. The order of the items in the returned map is
     * defined by the given comparator.
     *
     * @param objects    the collection of objects to convert into a map.
     * @param comparator determines the sort ordering of the elements in the returned map.
     *
     * @return a <code>Map</code>, never <code>null</code>.
     */
    public static <T extends Named> Map<String, T> asSortedMap(final Collection<T> objects,
                                                               final Comparator<T> comparator) {
        assert objects != null;
        assert comparator != null;

        final Set<T> set = new TreeSet<T>(comparator);
        set.addAll(objects);

        return asOrderedMap(set);
    }

    /**
     * Converts the given collection of {@link Named} objects to a map whose keys are the objects' name. The returned map retains the original order of the
     * items.
     *
     * @param objects the collection of objects to convert into a map.
     *
     * @return a <code>Map</code>, never <code>null</code>.
     */
    public static <T extends Named> Map<String, T> asOrderedMap(final T[] objects) {
        return asOrderedMap(Arrays.asList(objects));
    }

    /**
     * Converts the given collection of {@link Named} objects to a map whose keys are the objects' name. The order of the items in the returned map is
     * arbitrary.
     *
     * @param objects the collection of objects to convert into a map.
     *
     * @return a <code>Map</code>, never <code>null</code>.
     */
    public static <T extends Named> Map<String, T> asUnorderedMap(final T[] objects) {
        return asUnorderedMap(Arrays.asList(objects));
    }

    /**
     * Converts the given collection of {@link Named} objects to a map whose keys are the objects' name. The order of the items in the returned map is
     * defined by the given comparator.
     *
     * @param objects    the collection of objects to convert into a map.
     * @param comparator determines the sort ordering of the elements in the returned map.
     *
     * @return a <code>Map</code>, never <code>null</code>.
     */
    public static <T extends Named> Map<String, T> asSortedMap(final T[] objects, final Comparator<T> comparator) {
        return asSortedMap(Arrays.asList(objects), comparator);
    }

    /**
     * Extracts the names of the given collection of {@link Named} objects.
     *
     * @param objects the collection of objects to return the names of.
     *
     * @return a <code>Collection</code>, never <code>null</code>.
     */
    public static <T extends Named> Collection<String> asNames(final Collection<T> objects) {
        return Mappings.asKeys(objects, new NameExtractor<T>());
    }

    /**
     * Extracts the names of the given collection of {@link Named} objects.
     *
     * @param objects the collection of objects to return the names of.
     *
     * @return a <code>Collection</code>, never <code>null</code>.
     */
    public static <T extends Named> Collection<String> asNames(final T[] objects) {
        return asNames(Arrays.asList(objects));
    }

    /**
     * Converts the given collection of {@link Named} objects to a map whose keys are the objects' name. The specified map is populated and returned.
     *
     * @param objects the collection of objects to convert into a map.
     * @param result  is the map to be populated and returned.
     *
     * @return a {@link Map}, never <code>null</code>.
     */
    private static <T extends Named> Map<String, T> asMap(final Collection<T> objects, final Map<String, T> result) {
        return Mappings.asMap(objects, result, new NameExtractor<T>());
    }

    private static class NameExtractor<T extends Named> implements Mappings.Extractor<String, T> {

        public String getKey(final Object object) {
            return ((Named) object).name();
        }

        @SuppressWarnings("unchecked")
        public T getValue(final Object object) {
            return (T) object;
        }
    }
}