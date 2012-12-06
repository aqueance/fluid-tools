/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.container.spi;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.DependencyPath;
import org.fluidity.composition.container.impl.CompositeObserver;
import org.fluidity.testing.Simulator;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class CompositeObserverTest extends Simulator {

    private final MockObjects dependencies = dependencies();

    private final ComponentContainer.Observer observer1 = dependencies.normal(ComponentContainer.Observer.class);
    private final ComponentContainer.Observer observer2 = dependencies.normal(ComponentContainer.Observer.class);
    private final ComponentContainer.Observer observer3 = dependencies.normal(ComponentContainer.Observer.class);
    private final DependencyPath path = dependencies.normal(DependencyPath.class);
    private final Object component = new Object();

    @Test
    public void testNoObserver() throws Exception {
        assert CompositeObserver.combine() == null;
        assert CompositeObserver.combine((ComponentContainer.Observer) null) == null;
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
    }

    private void allThree(final ComponentContainer.Observer observer) throws Exception {
        test(new Task() {
            public void run() throws Exception {
                observer1.resolved(path, Serializable.class);
                observer2.resolved(path, Serializable.class);
                observer3.resolved(path, Serializable.class);

                verify(new Task() {
                    public void run() throws Exception {
                        observer.resolved(path, Serializable.class);
                    }
                });
            }
        });

        test(new Task() {
            public void run() throws Exception {
                final AtomicReference<Object> reference = new AtomicReference<Object>(component);

                observer1.instantiated(path, reference);
                observer2.instantiated(path, reference);
                observer3.instantiated(path, reference);

                verify(new Task() {
                    public void run() throws Exception {
                        observer.instantiated(path, reference);
                    }
                });
            }
        });
    }
}
