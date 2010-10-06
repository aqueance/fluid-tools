/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.fluidity.tests.MockGroupAbstractTest;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class KeyedUtilsTest extends MockGroupAbstractTest {

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void noInstantation() throws Exception {
        new KeyedUtils();
    }

    @Test
    public void collectionKeyExtraction() throws Exception {
        final Collection<String> keys = new ArrayList<String>();
        final Collection<Keyed> objects = new ArrayList<Keyed>();

        makeCollection(keys, objects);

        assert keys.equals(KeyedUtils.asKeys(objects));
    }

    @Test
    public void arrayKeyExtraction() throws Exception {
        final Collection<String> keys = new ArrayList<String>();
        final Keyed[] objects = makeCollection(keys);

        assert keys.equals(KeyedUtils.asKeys(objects));
    }

    @Test
    public void orderSensitiveCollectionToMapConversion() throws Exception {
        final Collection<String> keys = new ArrayList<String>();
        final Collection<Keyed> objects = new ArrayList<Keyed>();

        makeCollection(keys, objects);

        final Map<String, Keyed> map = KeyedUtils.asOrderedMap(objects);

        assert keys.equals(new ArrayList<String>(map.keySet()));
        assert objects.equals(new ArrayList<Keyed>(map.values()));
    }

    @Test
    public void orderInsensitiveCollectionToMapConversion() throws Exception {
        final Collection<String> keys = new ArrayList<String>();
        final Collection<Keyed> objects = new ArrayList<Keyed>();

        makeCollection(keys, objects);

        final Map<String, Keyed> map = KeyedUtils.asUnorderedMap(objects);

        assert new HashSet<String>(keys).equals(new HashSet<String>(map.keySet()));
        assert new HashSet<Keyed>(objects).equals(new HashSet<Keyed>(map.values()));
    }

    @Test
    public void orderSensitiveArrayToMapConversion() throws Exception {
        final Collection<String> keys = new ArrayList<String>();
        final Keyed[] objects = makeCollection(keys);

        final Map<String, Keyed> map = KeyedUtils.asOrderedMap(objects);

        assert keys.equals(new ArrayList<String>(map.keySet()));
        assert Arrays.asList(objects).equals(new ArrayList<Keyed>(map.values()));
    }

    @Test
    public void orderInsensitiveArrayToMapConversion() throws Exception {
        final Collection<String> keys = new ArrayList<String>();
        final Keyed[] objects = makeCollection(keys);

        final Map<String, Keyed> map = KeyedUtils.asUnorderedMap(objects);

        assert new HashSet<String>(keys).equals(new HashSet<String>(map.keySet()));
        assert new HashSet<Keyed>(Arrays.asList(objects)).equals(new HashSet<Keyed>(map.values()));
    }

    @Test
    public void sortedMapConversion() throws Exception {
        final Collection<String> keys = new HashSet<String>();
        final Collection<Keyed> objects = new HashSet<Keyed>();

        makeCollection(keys, objects);

        final Set<String> orderedKeys = new TreeSet<String>();
        orderedKeys.addAll(keys);

        final Map<String, Keyed> map = KeyedUtils.asSortedMap(objects, new KeyedComparator());

        assert new ArrayList<String>(orderedKeys).equals(new ArrayList<String>(map.keySet()));
        assert new HashSet<Keyed>(objects).equals(new HashSet<Keyed>(map.values()));
    }

    @Test
    public void arrayToSortedMapConversion() throws Exception {
        final Collection<String> keys = new HashSet<String>();
        final Keyed[] objects = makeCollection(keys);

        final Set<String> orderedKeys = new TreeSet<String>();
        orderedKeys.addAll(keys);

        final Map<String, Keyed> map = KeyedUtils.asSortedMap(objects, new KeyedComparator());

        assert new ArrayList<String>(orderedKeys).equals(new ArrayList<String>(map.keySet()));
        assert new HashSet<Keyed>(Arrays.asList(objects)).equals(new HashSet<Keyed>(map.values()));
    }

    private Collection<Keyed> makeCollection(Collection<String> keys, Collection<Keyed> objects) {

        // first create the keys individually to allow the collection to determine their order
        for (int i = 0; i < 6; ++i) {
            keys.add(String.valueOf(i));
        }

        // then create the objects in the order dictated by the key collection to allow reordering
        // by the underlying collection
        for (final String key : keys) {
            objects.add(new Keyed() {
                public String key() {
                    return key;
                }
            });
        }

        return objects;
    }

    private Keyed[] makeCollection(final Collection<String> keys) {
        final Collection<Keyed> collection = makeCollection(keys, new ArrayList<Keyed>());
        return collection.toArray(new Keyed[collection.size()]);
    }
}
