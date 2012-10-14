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

package org.fluidity.composition.container.tests;

import org.fluidity.composition.MutableContainer;
import org.fluidity.composition.container.ComponentCache;
import org.fluidity.composition.container.ContainerServices;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.container.DependencyInjector;
import org.fluidity.composition.container.PlatformContainer;
import org.fluidity.composition.container.spi.ContainerProvider;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.foundation.ClassDiscovery;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.foundation.spi.LogFactory;
import org.fluidity.testing.MockGroup;

import org.easymock.EasyMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Abstract test case for {@link ContainerProvider} implementations.
 *
 * @author Tibor Varga
 */
public abstract class ContainerProviderAbstractTest extends MockGroup {

    private final LogFactory logs = new NoLogFactory();

    private final ContainerServices services = mock(ContainerServices.class);
    private final ClassDiscovery classDiscovery = mock(ClassDiscovery.class);
    private final DependencyGraph.Traversal traversal = mock(DependencyGraph.Traversal.class);
    private final DependencyInjector injector = mock(DependencyInjector.class);
    private final ComponentCache componentCache = mock(ComponentCache.class);
    private final ContextDefinition context = mock(ContextDefinition.class);
    private final ContextDefinition copy = mock(ContextDefinition.class);

    private final ContainerProvider provider;      // to be provided by subclasses

    public ContainerProviderAbstractTest(final ContainerProvider provider) {
        this.provider = provider;
    }

    @BeforeMethod
    public final void dependencies() {
        EasyMock.expect(services.logs()).andReturn(logs).anyTimes();
        EasyMock.expect(services.classDiscovery()).andReturn(classDiscovery).anyTimes();
        EasyMock.expect(services.dependencyInjector()).andReturn(injector).anyTimes();
        EasyMock.expect(services.newCache(EasyMock.anyBoolean())).andReturn(componentCache).anyTimes();
        EasyMock.expect(services.emptyContext()).andReturn(context).anyTimes();
        EasyMock.expect(services.graphTraversal()).andReturn(traversal).anyTimes();
    }

    @Test
    public final void createsContainer() throws Exception {
        EasyMock.expect(context.copy()).andReturn(copy);

        final MutableContainer container = verify(new Work<MutableContainer>() {
            public MutableContainer run() throws Exception {
                return provider.newContainer(services, null);
            }
        });

        assert container != null;
    }

    @Test
    public final void createsPlatformContainer() throws Exception {
        final PlatformContainer platform = localMock(PlatformContainer.class);

        EasyMock.expect(context.copy()).andReturn(copy);

        final MutableContainer container = verify(new Work<MutableContainer>() {
            public MutableContainer run() throws Exception {
                return provider.newContainer(services, platform);
            }
        });

        assert container != null;
    }
}
