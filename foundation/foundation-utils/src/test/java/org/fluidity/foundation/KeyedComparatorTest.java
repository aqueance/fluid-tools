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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class KeyedComparatorTest extends MockGroupAbstractTest {

    @Test
    public void comparison() throws Exception {
        final Keyed object1 = new MyKeyed("aaaa");
        final Keyed object2 = new MyKeyed("bbbb");
        final Keyed object3 = new MyKeyed("cccc");
        final Keyed object4 = new MyKeyed("dddd");
        final Keyed object5 = new MyKeyed("eeee");

        final Set<Keyed> sorted = new TreeSet<Keyed>(new KeyedComparator());
        sorted.add(object2);
        sorted.add(object4);
        sorted.add(object1);
        sorted.add(object3);
        sorted.add(object5);

        final Iterator iterator = sorted.iterator();
        assert object1 == iterator.next();
        assert object2 == iterator.next();
        assert object3 == iterator.next();
        assert object4 == iterator.next();
        assert object5 == iterator.next();
        assert !iterator.hasNext();
    }

    private static class MyKeyed implements Keyed {

        private String key;

        public MyKeyed(final String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
