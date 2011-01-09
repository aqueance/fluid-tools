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

import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.fluidity.foundation.spi.Named;
import org.fluidity.tests.MockGroupAbstractTest;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class NamedComparatorTest extends MockGroupAbstractTest {

    @Test
    public void comparison() throws Exception {
        final Named object1 = new MyNamed("aaaa");
        final Named object2 = new MyNamed("bbbb");
        final Named object3 = new MyNamed("cccc");
        final Named object4 = new MyNamed("dddd");
        final Named object5 = new MyNamed("eeee");

        final Set<Named> sorted = new TreeSet<Named>(new NamedComparator());
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

    @Test
    public void caseInsensitiveComparison() throws Exception {
        final Named object1 = new MyNamed("AAAA");
        final Named object2 = new MyNamed("bBbB");
        final Named object3 = new MyNamed("CcCc");
        final Named object4 = new MyNamed("dDdD");
        final Named object5 = new MyNamed("eeee");
        final Named object6 = new MyNamed("öÖäÄ");            // this is at the end of the alphabet in Finnish

        final Set<Named> sorted = new TreeSet<Named>(new NamedComparator(new Locale("FI"), true));
        sorted.add(object2);
        sorted.add(object6);
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
        assert object6 == iterator.next();
        assert !iterator.hasNext();
    }

    private static class MyNamed implements Named {

        private String name;

        public MyNamed(final String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }
    }
}
