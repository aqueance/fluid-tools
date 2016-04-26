/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import org.fluidity.composition.container.ClassDiscovery;
import org.fluidity.composition.container.ComponentCache;
import org.fluidity.composition.container.ContainerServices;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.container.DependencyInjector;
import org.fluidity.composition.container.spi.ContainerProvider;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Abstract test case for {@link ContainerProvider} implementations.
 *
 * @author Tibor Varga
 */
public abstract class ContainerProviderAbstractTest extends Simulator {

    private final MockObjects dependencies = dependencies();
    private final Log log = NoLogFactory.consume(getClass());

    private final ContainerServices services = dependencies.normal(ContainerServices.class);
    private final ClassDiscovery classDiscovery = dependencies.normal(ClassDiscovery.class);
    private final DependencyGraph.Traversal traversal = dependencies.normal(DependencyGraph.Traversal.class);
    private final DependencyInjector injector = dependencies.normal(DependencyInjector.class);
    private final ComponentCache componentCache = dependencies.normal(ComponentCache.class);
    private final ContextDefinition context = dependencies.normal(ContextDefinition.class);
    private final ContextDefinition copy = dependencies.normal(ContextDefinition.class);

    private final ContainerProvider provider;      // to be provided by subclasses

    public ContainerProviderAbstractTest(final ContainerProvider provider) {
        this.provider = provider;
    }

    @BeforeMethod
    public final void setup() {
        EasyMock.expect(services.createLog(EasyMock.<Log>anyObject(), EasyMock.<Class<?>>anyObject())).andReturn(log).anyTimes();
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
                return provider.newContainer(services, false);
            }
        });

        assert container != null;
    }
}
