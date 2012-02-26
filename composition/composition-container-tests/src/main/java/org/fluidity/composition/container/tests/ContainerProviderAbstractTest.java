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

package org.fluidity.composition.container.tests;

import java.util.HashMap;
import java.util.Map;

import org.fluidity.composition.container.ComponentCache;
import org.fluidity.composition.container.ContainerServices;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.container.DependencyInjector;
import org.fluidity.composition.container.spi.ContainerProvider;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.container.spi.PlatformContainer;
import org.fluidity.foundation.ClassDiscovery;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.foundation.spi.LogFactory;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Abstract test case for {@link ContainerProvider} implementations.
 *
 * @author Tibor Varga
 */
public abstract class ContainerProviderAbstractTest extends MockGroupAbstractTest {

    private final LogFactory logs = new NoLogFactory();

    private final ContainerServices services = mock(ContainerServices.class);
    private final ClassDiscovery classDiscovery = mock(ClassDiscovery.class);
    private final DependencyGraph.Traversal traversal = mock(DependencyGraph.Traversal.class);
    private final DependencyGraph.Node node = mock(DependencyGraph.Node.class);
    private final DependencyInjector injector = mock(DependencyInjector.class);
    private final ComponentCache componentCache = mock(ComponentCache.class);
    private final ContextDefinition context = mock(ContextDefinition.class);
    private final ContextDefinition copy = mock(ContextDefinition.class);

    private final Map<String, String> map = new HashMap<String, String>();

    private final ContainerProvider provider;      // to be provided by subclasses

    public ContainerProviderAbstractTest(final ContainerProvider provider) {
        this.provider = provider;

        map.clear();
        map.put("key", "value");
    }

    @BeforeMethod
    public void dependencies() {
        EasyMock.expect(services.logs()).andReturn(logs).anyTimes();
        EasyMock.expect(services.classDiscovery()).andReturn(classDiscovery).anyTimes();
        EasyMock.expect(services.dependencyInjector()).andReturn(injector).anyTimes();
        EasyMock.expect(services.newCache(EasyMock.anyBoolean())).andReturn(componentCache).anyTimes();
        EasyMock.expect(services.emptyContext()).andReturn(context).anyTimes();
        EasyMock.expect(services.graphTraversal()).andReturn(traversal).anyTimes();
    }

    @Test
    public void createsContainer() throws Exception {
        EasyMock.expect(context.copy()).andReturn(copy);

        replay();
        assert provider.newContainer(services, null) != null;
        verify();
    }

    @Test
    public void createsPlatformContainer() throws Exception {
        final PlatformContainer platform = localMock(PlatformContainer.class);

        EasyMock.expect(context.copy()).andReturn(copy);

        replay();
        assert provider.newContainer(services, platform) != null;
        verify();
    }
}
