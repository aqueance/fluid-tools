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

import org.testng.annotations.Test;

/**
 * Tests the key in relation to the implementation's {@link Object#hashCode()} and {@link Object#equals(Object)} method.
 *
 * @author Tibor Varga
 */
public abstract class KeyedAbstractTest {

    protected abstract Keyed newKeyed(int id);

    @SuppressWarnings("StringEquality")
    @Test
    public void testKey() throws Exception {
        final int key = 1;
        assert newKeyed(key).key().equals(String.valueOf(key)) : "Did not retain key";
    }

    @Test
    public void testEquality() throws Exception {
        final Keyed keyed1 = newKeyed(1);
        final Keyed keyed2 = newKeyed(2);
        final Keyed keyed3 = newKeyed(1);
        final Keyed keyed4 = new Keyed() {
            public String key() {
                return String.valueOf(4);
            }
        };

        assert keyed1.equals(keyed1) : "Instance is not equal to itself";
        assert !keyed1.equals(keyed2) : "Two instances with different keys are equal";
        assert keyed1.equals(keyed3) : "Two instances with the same keys are not equal";
        assert !keyed1.equals(keyed4) : "Two instances of incompatible classes are equal";
    }

    @Test
    public void testHashCode() throws Exception {
        final Keyed keyed1 = newKeyed(1);
        final Keyed keyed2 = newKeyed(2);
        final Keyed keyed3 = newKeyed(1);

        assert keyed1.hashCode() != keyed2.hashCode() : "Two instances with different keys have the same hash code";
        assert keyed1.hashCode() == keyed3.hashCode() : "Two instances with the same keys have different hash code";
    }
}
