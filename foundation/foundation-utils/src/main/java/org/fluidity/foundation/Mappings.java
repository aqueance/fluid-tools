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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * Logic common to the various utility classes in this package, such as {@link KeyMapping}, {@link NameMapping}, etc. You can also use this utility class
 * directly to implement various mapping and filtering functions on collections of your objects.
 *
 * @author Tibor Varga
 */
public final class Mappings {

    public Mappings() {
        throw new UnsupportedOperationException("No instances allowed");
    }

    /**
     * Extracts the keys from each item in the given collection.
     *
     * @param objects   the collection of objects to return the keys of.
     * @param extractor knows how to extract the key from an item.
     *
     * @return a {@link Collection}, never <code>null</code>.
     */
    public static <K> Collection<K> asKeys(final Collection objects, final Extractor<K, ?> extractor) {
        assert objects != null;

        final Collection<K> result = new ArrayList<K>();

        for (final Object object : objects) {
            result.add(extractor.getKey(object));
        }

        return result;
    }

    /**
     * Turns the given collection in to a map. Keys in the map will be objects returned by {@link Extractor#getKey(Object)} while the values will be objects
     * returned by the {@link Extractor#getValue(Object)}.
     *
     * @param objects   the collection of objects to extract the keys of.
     * @param result    is where the results should go.
     * @param extractor knows how to extract the key and the value from an item.
     *
     * @return the value of the <code>result</code> parameter.
     */
    public static <K, V> Map<K, V> asMap(final Collection<?> objects, final Map<K, V> result, final Extractor<K, V> extractor) {
        if (objects != null) {
            for (final Object object : objects) {
                result.put(extractor.getKey(object), extractor.getValue(object));
            }
        }

        return result;
    }

    /**
     * Knows how to extract the key and the value from an object passed on to the above methods. This interface is used when processing collections to extract
     * some property to produce a new collection or two properties, a key and a value, to produce a maps.
     */
    static interface Extractor<K, V> {

        /**
         * Extracts the key of the given object.
         *
         * @param object the object to be extracted the key of.
         *
         * @return an object, may be <code>null</code>.
         */
        K getKey(Object object);

        /**
         * Extracts the value of the given object.
         *
         * @param object the object to be extracted the value of.
         *
         * @return an object, may be <code>null</code>.
         */
        V getValue(Object object);
    }
}
