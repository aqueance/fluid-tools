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

package org.fluidity.composition.container.impl;

import java.io.Serializable;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.OpenContainer;
import org.fluidity.composition.container.RestrictedContainer;
import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class RestrictedContainerImplTest extends Simulator {

    private final MockObjects dependencies = dependencies();

    private final OpenContainer level1 = dependencies.normal(OpenContainer.class);
    private final OpenContainer level2 = dependencies.normal(OpenContainer.class);

    private RestrictedContainer container() {
        return new RestrictedContainerImpl(level1);
    }

    @Test
    public void testComponentAccessDenied() throws Exception {
        final RestrictedContainer container = container();

        guarantee(new Task() {
            public void run() throws Exception {
                try {
                    container.getComponent(Serializable.class);
                    assert false : "Operation should have been prevented";
                } catch (final ComponentContainer.ResolutionException e) {
                    // this is expected
                }
            }
        });
    }

    @Test
    public void testComponentAccessAllowed() throws Exception {
        final RestrictedContainer container = container();

        container.enable();

        final String value = "value";
        EasyMock.expect(level1.getComponent(Serializable.class)).andReturn(value);

        final Serializable component = verify(new Work<Serializable>() {
            public Serializable run() throws Exception {
                return container.getComponent(Serializable.class);
            }
        });

        assert value.equals(component);
    }

    @Test
    public void testHierarchyAccessDenied() throws Exception {
        final RestrictedContainer container = container();

        EasyMock.expect(level1.makeChildContainer()).andReturn(level2);

        final OpenContainer child = verify(new Work<OpenContainer>() {
            public OpenContainer run() throws Exception {
                return container.makeChildContainer();
            }
        });

        guarantee(new Task() {
            public void run() throws Exception {
                try {
                    child.getComponent(Serializable.class);
                    assert false : "Operation should have been prevented";
                } catch (final ComponentContainer.ResolutionException e) {
                    // this is expected
                }
            }
        });
    }

    @Test
    public void testHierarchyAccessAllowed() throws Exception {
        final RestrictedContainer container = container();

        EasyMock.expect(level1.makeChildContainer()).andReturn(level2);

        final OpenContainer child = verify(new Work<OpenContainer>() {
            public OpenContainer run() throws Exception {
                return container.makeChildContainer();
            }
        });

        container.enable();

        final String value = "value";
        EasyMock.expect(level2.getComponent(Serializable.class)).andReturn(value);

        final Serializable component = verify(new Work<Serializable>() {
            public Serializable run() throws Exception {
                return child.getComponent(Serializable.class);
            }
        });

        assert value.equals(component);
    }
}
