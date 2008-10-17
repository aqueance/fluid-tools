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
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class NamedComparatorTest extends MockGroupAbstractTest {

    @Test
    public void comparison() throws Exception {
        Named object1 = new MyNamed("aaaa");
        Named object2 = new MyNamed("bbbb");
        Named object3 = new MyNamed("cccc");
        Named object4 = new MyNamed("dddd");
        Named object5 = new MyNamed("eeee");

        Set<Named> sorted = new TreeSet<Named>(new NamedComparator());
        sorted.add(object2);
        sorted.add(object4);
        sorted.add(object1);
        sorted.add(object3);
        sorted.add(object5);

        Iterator iterator = sorted.iterator();
        assert object1 == iterator.next();
        assert object2 == iterator.next();
        assert object3 == iterator.next();
        assert object4 == iterator.next();
        assert object5 == iterator.next();
        assert !iterator.hasNext();
    }

    @Test
    public void caseInsensitiveComparison() throws Exception {
        Named object1 = new MyNamed("AAAA");
        Named object2 = new MyNamed("bBbB");
        Named object3 = new MyNamed("CcCc");
        Named object4 = new MyNamed("dDdD");
        Named object5 = new MyNamed("eeee");
        Named object6 = new MyNamed("����");            // this is at the end of the alphabet in Finnish

        Set<Named> sorted = new TreeSet<Named>(new NamedComparator(new Locale("FI"), true));
        sorted.add(object2);
        sorted.add(object6);
        sorted.add(object4);
        sorted.add(object1);
        sorted.add(object3);
        sorted.add(object5);

        Iterator iterator = sorted.iterator();
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

        public MyNamed(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }
    }
}
