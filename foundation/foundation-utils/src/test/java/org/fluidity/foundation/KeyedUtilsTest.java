/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
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
        Collection<String> keys = new ArrayList<String>();
        Collection<Keyed> objects = new ArrayList<Keyed>();

        makeCollection(keys, objects);

        assert keys.equals(KeyedUtils.asKeys(objects));
    }

    @Test
    public void arrayKeyExtraction() throws Exception {
        Collection<String> keys = new ArrayList<String>();
        Keyed[] objects = makeCollection(keys);

        assert keys.equals(KeyedUtils.asKeys(objects));
    }

    @Test
    public void orderSensitiveCollectionToMapConversion() throws Exception {
        Collection<String> keys = new ArrayList<String>();
        Collection<Keyed> objects = new ArrayList<Keyed>();

        makeCollection(keys, objects);

        Map<String, Keyed> map = KeyedUtils.asOrderedMap(objects);

        assert keys.equals(new ArrayList<String>(map.keySet()));
        assert objects.equals(new ArrayList<Keyed>(map.values()));
    }

    @Test
    public void orderInsensitiveCollectionToMapConversion() throws Exception {
        Collection<String> keys = new ArrayList<String>();
        Collection<Keyed> objects = new ArrayList<Keyed>();

        makeCollection(keys, objects);

        Map<String, Keyed> map = KeyedUtils.asUnorderedMap(objects);

        assert new HashSet<String>(keys).equals(new HashSet<String>(map.keySet()));
        assert new HashSet<Keyed>(objects).equals(new HashSet<Keyed>(map.values()));
    }

    @Test
    public void orderSensitiveArrayToMapConversion() throws Exception {
        Collection<String> keys = new ArrayList<String>();
        Keyed[] objects = makeCollection(keys);

        Map<String, Keyed> map = KeyedUtils.asOrderedMap(objects);

        assert keys.equals(new ArrayList<String>(map.keySet()));
        assert Arrays.asList(objects).equals(new ArrayList<Keyed>(map.values()));
    }

    @Test
    public void orderInsensitiveArrayToMapConversion() throws Exception {
        Collection<String> keys = new ArrayList<String>();
        Keyed[] objects = makeCollection(keys);

        Map<String, Keyed> map = KeyedUtils.asUnorderedMap(objects);

        assert new HashSet<String>(keys).equals(new HashSet<String>(map.keySet()));
        assert new HashSet<Keyed>(Arrays.asList(objects)).equals(new HashSet<Keyed>(map.values()));
    }

    @Test
    public void sortedMapConversion() throws Exception {
        Collection<String> keys = new HashSet<String>();
        Collection<Keyed> objects = new HashSet<Keyed>();

        makeCollection(keys, objects);

        Set<String> orderedKeys = new TreeSet<String>();
        orderedKeys.addAll(keys);

        Map<String, Keyed> map = KeyedUtils.asSortedMap(objects, new KeyedComparator());

        assert new ArrayList<String>(orderedKeys).equals(new ArrayList<String>(map.keySet()));
        assert new HashSet<Keyed>(objects).equals(new HashSet<Keyed>(map.values()));
    }

    @Test
    public void arrayToSortedMapConversion() throws Exception {
        Collection<String> keys = new HashSet<String>();
        Keyed[] objects = makeCollection(keys);

        Set<String> orderedKeys = new TreeSet<String>();
        orderedKeys.addAll(keys);

        Map<String, Keyed> map = KeyedUtils.asSortedMap(objects, new KeyedComparator());

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

    private Keyed[] makeCollection(Collection<String> keys) {
        final Collection<Keyed> collection = makeCollection(keys, new ArrayList<Keyed>());
        return collection.toArray(new Keyed[collection.size()]);
    }
}
