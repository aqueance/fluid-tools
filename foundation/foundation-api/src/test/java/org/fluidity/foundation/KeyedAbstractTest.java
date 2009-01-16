/*
 * Copyright (c) 2006-2009 Tibor Adam Varga (tibor.adam.varga on gmail)
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
 */
package org.fluidity.foundation;

import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public abstract class KeyedAbstractTest extends MockGroupAbstractTest {

    protected abstract Keyed newKeyed(String key);

    protected String newKey(int id) {
        return "key" + id;
    }

    @SuppressWarnings({ "StringEquality" })
    @Test
    public void testKey() throws Exception {
        final String key = newKey(1);
        assert newKeyed(key).key() == key : "Did not retain key";
    }

    @Test
    public void testEquality() throws Exception {
        final Keyed keyed1 = newKeyed(newKey(1));
        final Keyed keyed2 = newKeyed(newKey(2));
        final Keyed keyed3 = newKeyed(newKey(1));
        final Keyed keyed4 = new Keyed() {
            public String key() {
                return newKey(4);
            }
        };

        assert keyed1.equals(keyed1) : "Instance is not equal to itself";
        assert !keyed1.equals(keyed2) : "Two instances with different keys are equal";
        assert keyed1.equals(keyed3) : "Two instances with the same keys are not equal";
        assert !keyed1.equals(keyed4) : "Two instances of incompatible classes are equal";
    }

    @Test
    public void testHashCode() throws Exception {
        final Keyed keyed1 = newKeyed(newKey(1));
        final Keyed keyed2 = newKeyed(newKey(2));
        final Keyed keyed3 = newKeyed(newKey(1));

        assert keyed1.hashCode() != keyed2.hashCode() : "Two instances with different keys have the same hash code";
        assert keyed1.hashCode() == keyed3.hashCode() : "Two instances with the same keys have different hash code";
    }
}
