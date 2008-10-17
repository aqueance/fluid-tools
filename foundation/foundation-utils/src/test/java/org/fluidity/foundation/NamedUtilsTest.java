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
public class NamedUtilsTest extends MockGroupAbstractTest {

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void noInstantation() throws Exception {
        new NamedUtils();
    }

    @Test
    public void collectionNameExtraction() throws Exception {
        final Collection<String> names = new ArrayList<String>();
        final Collection<Named> objects = new ArrayList<Named>();

        makeCollection(names, objects);

        assert names.equals(NamedUtils.asNames(objects));
    }

    @Test
    public void arrayNameExtraction() throws Exception {
        final Collection<String> names = new ArrayList<String>();
        final Named[] objects = makeCollection(names);

        assert names.equals(NamedUtils.asNames(objects));
    }

    @Test
    public void orderSensitiveCollectionToMapConversion() throws Exception {
        final Collection<String> names = new ArrayList<String>();
        final Collection<Named> objects = new ArrayList<Named>();

        makeCollection(names, objects);

        final Map<String, Named> map = NamedUtils.asOrderedMap(objects);

        assert names.equals(new ArrayList<String>(map.keySet()));
        assert objects.equals(new ArrayList<Named>(map.values()));
    }

    @Test
    public void orderInsensitiveCollectionToMapConversion() throws Exception {
        final Collection<String> names = new ArrayList<String>();
        final Collection<Named> objects = new ArrayList<Named>();

        makeCollection(names, objects);

        final Map<String, Named> map = NamedUtils.asUnorderedMap(objects);

        assert new HashSet<String>(names).equals(new HashSet<String>(map.keySet()));
        assert new HashSet<Named>(objects).equals(new HashSet<Named>(map.values()));
    }

    @Test
    public void orderSensitiveArrayToMapConversion() throws Exception {
        final Collection<String> names = new ArrayList<String>();
        final Named[] objects = makeCollection(names);

        final Map<String, Named> map = NamedUtils.asOrderedMap(objects);

        assert names.equals(new ArrayList<String>(map.keySet()));
        assert Arrays.asList(objects).equals(new ArrayList<Named>(map.values()));
    }

    @Test
    public void orderInsensitiveArrayToMapConversion() throws Exception {
        final Collection<String> names = new ArrayList<String>();
        final Named[] objects = makeCollection(names);

        final Map<String, Named> map = NamedUtils.asUnorderedMap(objects);

        assert new HashSet<String>(names).equals(new HashSet<String>(map.keySet()));
        assert new HashSet<Named>(Arrays.asList(objects)).equals(new HashSet<Named>(map.values()));
    }

    @Test
    public void sortedCollectionToMapConversion() throws Exception {
        final Collection<String> names = new HashSet<String>();
        final Collection<Named> objects = new HashSet<Named>();

        makeCollection(names, objects);

        final Set<String> orderedKeys = new TreeSet<String>();
        orderedKeys.addAll(names);

        final Map<String, Named> map = NamedUtils.asSortedMap(objects, new NamedComparator());

        assert new ArrayList<String>(orderedKeys).equals(new ArrayList<String>(map.keySet()));
        assert new HashSet<Named>(objects).equals(new HashSet<Named>(map.values()));
    }

    @Test
    public void sortedArrayToMapConversion() throws Exception {
        final Collection<String> names = new HashSet<String>();
        final Named[] objects = makeCollection(names);

        final Set<String> orderedKeys = new TreeSet<String>();
        orderedKeys.addAll(names);

        final Map<String, Named> map = NamedUtils.asSortedMap(objects, new NamedComparator());

        assert new ArrayList<String>(orderedKeys).equals(new ArrayList<String>(map.keySet()));
        assert new HashSet<Named>(Arrays.asList(objects)).equals(new HashSet<Named>(map.values()));
    }

    private Collection<Named> makeCollection(Collection<String> names, Collection<Named> objects) {

        // first create the names individually to allow the collection to determine their order
        for (int i = 0; i < 6; ++i) {
            names.add(String.valueOf(i));
        }

        // then create the objects in the order dictated by the name collection to allow reordering
        // by the underlying collection
        for (final String name : names) {
            objects.add(new Named() {
                public String name() {
                    return name;
                }
            });
        }

        return objects;
    }

    private Named[] makeCollection(final Collection<String> names) {
        final Collection<Named> collection = makeCollection(names, new ArrayList<Named>());
        return collection.toArray(new Named[collection.size()]);
    }
}
