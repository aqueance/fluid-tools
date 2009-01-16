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
package org.fluidity.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.testng.annotations.BeforeMethod;

/**
 * Abstract test cases that facilitates the use of a <code>IMocksControl</code>.
 *
 * @author Tibor Varga
 */
public abstract class MockGroupAbstractTest {

    private final IMocksControl group = EasyMock.createControl();
    private final IMocksControl niceGroup = EasyMock.createNiceControl();
    private final IMocksControl strictGroup = EasyMock.createStrictControl();

    private final List<IMocksControl> groups =
            new ArrayList<IMocksControl>(Arrays.asList(strictGroup, group, niceGroup));

    @BeforeMethod
    public void setup() throws Exception {
        reset();
    }

    protected final IMocksControl group() {
        return group;
    }

    protected final IMocksControl niceGroup() {
        return niceGroup;
    }

    protected final IMocksControl strictGroup() {
        return niceGroup;
    }

    protected <T> T addControl(final Class<T> interfaceClass) {
        return group.createMock(interfaceClass);
    }

    protected <T> T addNiceControl(final Class<T> interfaceClass) {
        return niceGroup.createMock(interfaceClass);
    }

    protected <T> T addStrictControl(final Class<T> interfaceClass) {
        return strictGroup.createMock(interfaceClass);
    }

    protected final void replay() {
        for (final IMocksControl group : groups) {
            group.replay();
        }
    }

    protected final void verify() {
        boolean failed = false;

        for (final IMocksControl group : groups) {
            try {
                group.verify();
            } catch (final AssertionError e) {
                if (!failed) {
                    throw e;
                } else {
                    failed = true;
                }
            } finally {
                try {
                    group.reset();
                } catch (final Throwable e) {
                    assert false : e;
                }
            }
        }
    }

    protected final void reset() {
        for (final IMocksControl group : groups) {
            group.reset();
        }
    }
}
