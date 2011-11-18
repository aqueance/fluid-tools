/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.composition;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.DependencyPath;
import org.fluidity.tests.MockGroupAbstractTest;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class CompositeObserverTest extends MockGroupAbstractTest {

    private final ComponentResolutionObserver observer1 = mock(ComponentResolutionObserver.class);
    private final ComponentResolutionObserver observer2 = mock(ComponentResolutionObserver.class);
    private final ComponentResolutionObserver observer3 = mock(ComponentResolutionObserver.class);
    private final DependencyPath path = mock(DependencyPath.class);
    private final Object component = new Object();

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

        final AtomicReference<Object> reference = new AtomicReference<Object>(component);

        observer1.instantiated(path, reference);
        observer2.instantiated(path, reference);
        observer3.instantiated(path, reference);

        replay();
        observer.instantiated(path, reference);
        verify();
    }
}
