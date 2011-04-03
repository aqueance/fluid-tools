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

package org.fluidity.composition;

import java.io.Serializable;
import java.util.Arrays;

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.DependencyPath;
import org.fluidity.tests.MockGroupAbstractTest;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class CompositeObserverTest extends MockGroupAbstractTest {

    private final ComponentResolutionObserver observer1 = addControl(ComponentResolutionObserver.class);
    private final ComponentResolutionObserver observer2 = addControl(ComponentResolutionObserver.class);
    private final ComponentResolutionObserver observer3 = addControl(ComponentResolutionObserver.class);
    private final DependencyPath path = addControl(DependencyPath.class);

    @Test
    public void testNoObserver() throws Exception {
        assert CompositeObserver.combine() == null;
        assert CompositeObserver.combine((ComponentResolutionObserver) null) == null;
        assert CompositeObserver.combine(null, null) == null;
    }

    @Test
    public void testSingleObserver() throws Exception {
        assert CompositeObserver.combine(observer1) == observer1;
        assert CompositeObserver.combine(observer2, null) == observer2;
        assert CompositeObserver.combine(null, observer2) == observer2;
        assert CompositeObserver.combine(null, observer3, null) == observer3;
    }

    @Test
    public void testMultiple() throws Exception {
        allThree(CompositeObserver.combine(observer1, observer2, observer3));
        allThree(CompositeObserver.combine(Arrays.asList(observer1, observer2, observer3)));
    }

    private void allThree(final ComponentResolutionObserver observer) {
        observer1.resolved(path, Serializable.class);
        observer2.resolved(path, Serializable.class);
        observer3.resolved(path, Serializable.class);

        replay();
        observer.resolved(path, Serializable.class);
        verify();

        observer1.instantiated(path);
        observer2.instantiated(path);
        observer3.instantiated(path);

        replay();
        observer.instantiated(path);
        verify();
    }
}
