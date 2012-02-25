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

package org.fluidity.deployment.web;

import org.fluidity.composition.Component;
import org.fluidity.composition.container.spi.OpenComponentContainer;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class DependencyResolverImplTest extends MockGroupAbstractTest {

    private static OpenComponentContainer container;

    private DependencyResolverImpl resolver = new DependencyResolverImpl();

    public DependencyResolverImplTest() {
        container = mock(OpenComponentContainer.class);
    }

    @Test
    public void componentAcquisitionByClass() throws Exception {
        final Dependency dependency = new Dependency();

        EasyMock.expect(container.getComponent(Dependency.class)).andReturn(dependency);

        replay();
        assert resolver.findComponent(container, Dependency.class.getName()) == dependency;
        verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void usesChildContainerWhenComponentNotFound() throws Exception {
        final Dependency dependency = new Dependency();

        EasyMock.expect(container.getComponent(Dependency.class)).andReturn(null);
        EasyMock.expect(container.instantiate(Dependency.class)).andReturn(dependency);

        replay();
        assert resolver.findComponent(container, Dependency.class.getName()) == dependency;
        verify();
    }

    @Component(primary = false, automatic = false)
    public static class Dependency { }
}
