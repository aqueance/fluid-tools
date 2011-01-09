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

package org.fluidity.foundation.spi;

import org.testng.annotations.Test;

/**
 * Tests the id in relation to the implementation's {@link Object#hashCode()} and {@link Object#equals(Object)} method.
 *
 * @author Tibor Varga
 */
public abstract class IdentifiedAbstractTest {

    protected abstract Identified newIdentified(int id);

    @SuppressWarnings("StringEquality")
    @Test
    public void testKey() throws Exception {
        final int id = 1;
        assert newIdentified(id).id().equals(String.valueOf(id)) : "Did not retain id";
    }

    @Test
    public void testEquality() throws Exception {
        final Identified identified1 = newIdentified(1);
        final Identified identified2 = newIdentified(2);
        final Identified identified3 = newIdentified(1);
        final Identified identified4 = new Identified() {
            public String id() {
                return String.valueOf(4);
            }
        };

        assert identified1.equals(identified1) : "Instance is not equal to itself";
        assert !identified1.equals(identified2) : "Two instances with different ids are equal";
        assert identified1.equals(identified3) : "Two instances with the same ids are not equal";
        assert !identified1.equals(identified4) : "Two instances of incompatible classes are equal";
    }

    @Test
    public void testHashCode() throws Exception {
        final Identified identified1 = newIdentified(1);
        final Identified identified2 = newIdentified(2);
        final Identified identified3 = newIdentified(1);

        assert identified1.hashCode() != identified2.hashCode() : "Two instances with different ids have the same hash code";
        assert identified1.hashCode() == identified3.hashCode() : "Two instances with the same ids have different hash code";
    }
}
