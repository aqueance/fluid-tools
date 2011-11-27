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

package org.fluidity.composition.tests;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.ClassDiscovery;
import org.fluidity.composition.spi.ComponentCache;
import org.fluidity.composition.spi.ContainerProvider;
import org.fluidity.composition.spi.ContainerServices;
import org.fluidity.composition.spi.ContextDefinition;
import org.fluidity.composition.spi.DependencyGraph;
import org.fluidity.composition.spi.DependencyInjector;
import org.fluidity.composition.spi.EmptyPackageBindings;
import org.fluidity.composition.spi.PackageBindings;
import org.fluidity.composition.spi.PlatformContainer;
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

    @Test
    @SuppressWarnings("unchecked")
    public void standaloneBindings() throws Exception {
        final Class<?>[] assemblies = { StandalonePackageBindingsImpl.class };

        final PackageBindings bindings = new StandalonePackageBindingsImpl();

        final PackageBindings[] instance = { bindings };
        EasyMock.expect(traversal.follow(EasyMock.<DependencyGraph>notNull(), EasyMock.same(context), EasyMock.<DependencyGraph.Node.Reference>notNull())).andReturn(node);

        EasyMock.expect(node.instance(traversal)).andReturn(instance);
        EasyMock.expect(injector.findConstructor(EasyMock.<Class<?>>notNull())).andReturn(null).anyTimes();

        replay();
        final List<PackageBindings> list = provider.instantiateBindings(services, map, (Class<PackageBindings>[]) assemblies);
        verify();

        assert list.equals(Arrays.asList(instance)) : list;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void connectedBindings() throws Exception {
        final Class<?>[] assemblies = { PackageBindingsImpl.class, DependentPackageBindingsImpl.class, ResponsiblePackageBindingsImpl.class };

        final ResponsiblePackageBindingsImpl bindings1 = new ResponsiblePackageBindingsImpl();
        final PackageBindingsImpl bindings2 = new PackageBindingsImpl(bindings1);
        final DependentPackageBindingsImpl bindings3 = new DependentPackageBindingsImpl(bindings2);

        final PackageBindings[] bindings = { bindings1, bindings2, bindings3 };
        EasyMock.expect(traversal.follow(EasyMock.<DependencyGraph>notNull(), EasyMock.same(context), EasyMock.<DependencyGraph.Node.Reference>notNull())).andReturn(node);
        EasyMock.expect(injector.findConstructor(EasyMock.<Class<?>>notNull())).andReturn(null).anyTimes();

        EasyMock.expect(node.instance(traversal)).andReturn(bindings);

        replay();
        final List<PackageBindings> list = provider.instantiateBindings(services, map, (Class<PackageBindings>[]) assemblies);
        verify();

        assert list.equals(Arrays.asList(bindings)) : list;
    }

    public static class ResponsiblePackageBindingsImpl extends EmptyPackageBindings {
        // empty
    }

    public static class StandalonePackageBindingsImpl extends EmptyPackageBindings {

    }

    public static class PackageBindingsImpl extends EmptyPackageBindings {

        @SuppressWarnings("UnusedDeclaration")
        public PackageBindingsImpl(final ResponsiblePackageBindingsImpl dependent) {
            // empty
        }
    }

    public static class DependentPackageBindingsImpl extends EmptyPackageBindings {

        @SuppressWarnings("UnusedDeclaration")
        public DependentPackageBindingsImpl(final PackageBindingsImpl dependent) {
            // empty
        }
    }
}