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
import org.fluidity.composition.ObservedComponentContainer;
import org.fluidity.composition.container.RestrictedContainer;
import org.fluidity.testing.MockGroup;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class RestrictedContainerImplTest extends MockGroup {

    private final ObservedComponentContainer level1 = mock(ObservedComponentContainer.class);
    private final ObservedComponentContainer level2 = mock(ObservedComponentContainer.class);

    private RestrictedContainer container() {
        return new RestrictedContainerImpl(level1);
    }

    @Test
    public void testComponentAccess() throws Exception {
        final RestrictedContainer container = container();

        replay();
        try {
            container.getComponent(Serializable.class);
            assert false : "Operation should have been prevented";
        } catch (final ComponentContainer.ResolutionException e) {
            // this is expected
        }
        verify();

        container.enable();

        final String value = "value";
        EasyMock.expect(level1.getComponent(Serializable.class)).andReturn(value);

        replay();
        assert value.equals(container.getComponent(Serializable.class));
        verify();
    }

    @Test
    public void testHierarchyAccess() throws Exception {
        final RestrictedContainer container = container();

        EasyMock.expect(level1.makeChildContainer()).andReturn(level2);

        replay();
        final ComponentContainer child = container.makeChildContainer();
        verify();

        replay();
        try {
            child.getComponent(Serializable.class);
            assert false : "Operation should have been prevented";
        } catch (final ComponentContainer.ResolutionException e) {
            // this is expected
        }
        verify();

        container.enable();

        final String value = "value";
        EasyMock.expect(level2.getComponent(Serializable.class)).andReturn(value);

        replay();
        assert value.equals(child.getComponent(Serializable.class));
        verify();
    }
}
