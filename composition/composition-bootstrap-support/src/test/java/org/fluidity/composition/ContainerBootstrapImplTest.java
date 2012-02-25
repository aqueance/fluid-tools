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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fluidity.composition.container.api.ContainerServices;
import org.fluidity.composition.container.spi.ContainerProvider;
import org.fluidity.composition.container.spi.OpenComponentContainer;
import org.fluidity.composition.container.spi.PlatformContainer;
import org.fluidity.composition.spi.EmptyPackageBindings;
import org.fluidity.composition.spi.PackageBindings;
import org.fluidity.composition.spi.ShutdownTasks;
import org.fluidity.foundation.ClassDiscovery;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.foundation.spi.LogFactory;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public final class ContainerBootstrapImplTest extends MockGroupAbstractTest {

    private final LogFactory logs = new NoLogFactory();

    private final ContainerBootstrap.Callback callback = mock(ContainerBootstrap.Callback.class);
    private final PlatformContainer platform = mock(PlatformContainer.class);
    private final ContainerProvider provider = mock(ContainerProvider.class);
    private final ContainerServices services = mock(ContainerServices.class);
    private final ClassDiscovery discovery = mock(ClassDiscovery.class);
    private final ShutdownTasks shutdown = mock(ShutdownTasks.class);
    private final OpenComponentContainer container = mock(OpenComponentContainer.class);
    private final OpenComponentContainer parent = mock(OpenComponentContainer.class);
    private final ComponentContainer.Registry registry = mock(ComponentContainer.Registry.class);
    private final PackageBindings bindings = mock(PackageBindings.class);

    private ContainerBootstrap bootstrap;

    @BeforeMethod
    public void dependencies() {
        bootstrap = new ContainerBootstrapImpl();

        EasyMock.expect(services.logs()).andReturn(logs).anyTimes();
        EasyMock.expect(services.classDiscovery()).andReturn(discovery).anyTimes();
    }

    @SuppressWarnings("unchecked")
    private Object populateContainer(final Class[] classes, final List<PackageBindings> instances) {
        EasyMock.expect(provider.newContainer(services, platform)).andReturn(container);
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, false)).andReturn(classes);

        EasyMock.expect(provider.instantiateBindings(EasyMock.same(services), EasyMock.<Map>isNull(), EasyMock.<Class<PackageBindings>[]>notNull()))
                .andAnswer(new IAnswer<List<PackageBindings>>() {
                    public List<PackageBindings> answer() throws Throwable {
                        return instances;
                    }
                });

        EasyMock.expect(container.getRegistry()).andReturn(registry);

        for (final PackageBindings instance : instances) {
            instance.bindComponents(registry);
        }

        registry.bindInstance(discovery);
        registry.bindInstance(EasyMock.anyObject());

        final Object[] list = new Object[1];

        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() throws Throwable {
                list[0] = EasyMock.getCurrentArguments()[0];
                return null;
            }
        });

        replay();
        bootstrap.populateContainer(services, provider, null, null, null, platform, callback);
        verify();

        dependencies();

        return list[0];
    }

    @Test
    public void testInitialization() throws Exception {
        final ResponsiblePackageBindingsImpl bindings1 = new ResponsiblePackageBindingsImpl();
        final PackageBindingsImpl bindings2 = new PackageBindingsImpl(bindings1);
        final DependentPackageBindingsImpl bindings3 = new DependentPackageBindingsImpl(bindings2);

        final List<PackageBindings> list = new ArrayList<PackageBindings>();

        PackageBindingsImpl.bindings = bindings;
        DependentPackageBindingsImpl.bindings = bindings;

        PackageBindingsImpl.list = list;
        DependentPackageBindingsImpl.list = list;
        final List<PackageBindings> watched = Arrays.asList(bindings2, bindings3);

        final Class[] classes = { ResponsiblePackageBindingsImpl.class, PackageBindingsImpl.class, DependentPackageBindingsImpl.class };
        final List<PackageBindings> instances = Arrays.asList(bindings1, bindings2, bindings3);

        final Object object = populateContainer(classes, instances);

        EasyMock.expect(container.getComponent(ShutdownTasks.class)).andReturn(shutdown);
        EasyMock.expect(container.getComponent(EasyMock.<Class<?>>anyObject())).andReturn(object);

        shutdown.add(EasyMock.<Runnable>anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                Object[] args = EasyMock.getCurrentArguments();
                assert list.equals(watched);

                // invoke the shutdown command to test de-registration
                ((Runnable) args[0]).run();

                // check that all components have been shut down
                assert list.isEmpty();
                return null;
            }
        });

        callback.containerInitialized(container);
        callback.containerShutdown(container);

        this.bindings.initializeComponents(container);
        EasyMock.expectLastCall().times(2);

        this.bindings.shutdownComponents(container);
        EasyMock.expectLastCall().times(2);

        replay();
        bootstrap.initializeContainer(container, services);
        verify();
    }

    @SuppressWarnings("unchecked")
    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*require.*ShutdownTasks.*")
    public void missingShutdownHook() throws Exception {
        final Object object = populateContainer(new Class[0], Collections.EMPTY_LIST);

        EasyMock.expect(container.getComponent(ShutdownTasks.class)).andReturn(null);
        EasyMock.expect(container.getComponent(EasyMock.<Class>anyObject())).andReturn(object);
        callback.containerInitialized(container);

        replay();

        try {
            bootstrap.initializeContainer(container, services);
        } finally {
            verify();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void connectedComponentAssembly() throws Exception {
        final Class[] assemblies = { PackageBindingsImpl.class, DependentPackageBindingsImpl.class, ResponsiblePackageBindingsImpl.class };

        final ResponsiblePackageBindingsImpl bindings1 = new ResponsiblePackageBindingsImpl();
        final PackageBindingsImpl bindings2 = new PackageBindingsImpl(bindings1);
        final DependentPackageBindingsImpl bindings3 = new DependentPackageBindingsImpl(bindings2);

        int watched = 0;
        PackageBindingsImpl.bindings = bindings;
        ++watched;
        DependentPackageBindingsImpl.bindings = bindings;
        ++watched;

        // we're passing a parent to subject is expected to as for a child container
        EasyMock.expect(parent.makeChildContainer()).andReturn(container);
        EasyMock.expect(parent.getComponent(EasyMock.<Class<?>>anyObject())).andReturn(null);
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, true)).andReturn(assemblies);

        EasyMock.expect(provider.instantiateBindings(EasyMock.same(services), EasyMock.<Map>isNull(), EasyMock.<Class<PackageBindings>[]>notNull()))
                .andAnswer(new IAnswer<List<PackageBindings>>() {
                    public List<PackageBindings> answer() throws Throwable {
                        return Arrays.asList(bindings1, bindings2, bindings3);
                    }
                });

        EasyMock.expect(container.getRegistry()).andReturn(registry);

        bindings.bindComponents(registry);
        EasyMock.expectLastCall().times(watched);

        registry.bindInstance(EasyMock.anyObject());

        replay();
        assert container == bootstrap.populateContainer(services, provider, null, parent, null, platform, callback);
        verify();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void standaloneComponentAssembly() throws Exception {

        // we're not passing a container so subject is expected to create one
        EasyMock.expect(provider.newContainer(services, platform)).andReturn(container);
        EasyMock.expect(container.getRegistry()).andReturn(registry);

        final Class[] assemblies = {
                ShutdownHookPackageBindingsImpl.class,
        };

        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, false)).andReturn(assemblies);

        EasyMock.expect(provider.instantiateBindings(EasyMock.same(services), EasyMock.<Map>isNull(), EasyMock.<Class<PackageBindings>[]>notNull()))
                .andAnswer(new IAnswer<List<PackageBindings>>() {
                    public List<PackageBindings> answer() throws Throwable {
                        return Collections.singletonList((PackageBindings) new ShutdownHookPackageBindingsImpl());
                    }
                });

        registry.bindInstance(discovery);

        ShutdownHookPackageBindingsImpl.bindings = bindings;
        bindings.bindComponents(registry);

        registry.bindInstance(EasyMock.anyObject());

        replay();
        assert bootstrap.populateContainer(services, provider, null, null, null, platform, callback) != null;
        verify();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bindingProperties() throws Exception {

        // we're passing a parent to subject is expected to as for a child container
        EasyMock.expect(parent.makeChildContainer()).andReturn(container);
        EasyMock.expect(parent.getComponent(EasyMock.<Class<?>>anyObject())).andReturn(null);
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, true)).andReturn(new Class[0]);

        final Properties properties = new Properties();

        final String value = "value";
        properties.setProperty(ConfigurablePackageBindingsImpl.KEY, value);
        EasyMock.expect(provider.instantiateBindings(EasyMock.same(services), EasyMock.eq(properties), EasyMock.<Class<PackageBindings>[]>notNull()))
                .andReturn(new ArrayList<PackageBindings>());

        EasyMock.expect(container.getRegistry()).andReturn(registry);

        registry.bindInstance(EasyMock.anyObject());

        replay();
        assert container == bootstrap.populateContainer(services, provider, properties, parent, null, platform, callback);
        verify();
    }

    public static class ResponsiblePackageBindingsImpl extends EmptyPackageBindings {
        // empty
    }

    public static class ShutdownHookPackageBindingsImpl extends EmptyPackageBindings {

        public static PackageBindings bindings;

        public void bindComponents(final ComponentContainer.Registry registry) {
            bindings.bindComponents(registry);
        }

        public void initializeComponents(final ComponentContainer container) {
            bindings.initializeComponents(container);
        }

        public void shutdownComponents(final ComponentContainer container) {
            bindings.shutdownComponents(container);
        }
    }

    public static class PackageBindingsImpl implements PackageBindings {

        public static PackageBindings bindings;

        public static List<PackageBindings> list;

        @SuppressWarnings("UnusedDeclaration")
        public PackageBindingsImpl(final ResponsiblePackageBindingsImpl dependent) {
            // empty
        }

        public void bindComponents(final ComponentContainer.Registry registry) {
            bindings.bindComponents(registry);
        }

        public void initializeComponents(final ComponentContainer container) {
            bindings.initializeComponents(container);
            list.add(this);
        }

        public void shutdownComponents(final ComponentContainer container) {
            bindings.shutdownComponents(container);
            list.remove(this);
        }
    }

    public static class DependentPackageBindingsImpl implements PackageBindings {

        public static PackageBindings bindings;

        public static List<PackageBindings> list;

        @SuppressWarnings("UnusedDeclaration")
        public DependentPackageBindingsImpl(final PackageBindingsImpl dependent) {
            // empty
        }

        public void bindComponents(final ComponentContainer.Registry registry) {
            bindings.bindComponents(registry);
        }

        public void initializeComponents(final ComponentContainer container) {
            bindings.initializeComponents(container);
            list.add(this);
        }

        public void shutdownComponents(final ComponentContainer container) {
            bindings.shutdownComponents(container);
            list.remove(this);
        }
    }

    public static class ConfigurablePackageBindingsImpl extends EmptyPackageBindings {

        public static final String KEY = ConfigurablePackageBindingsImpl.class.getName().concat(".key");
        public static String value;

        public ConfigurablePackageBindingsImpl(final Map<String, String> properties) {
            assert properties != null;
            value = properties.get(KEY);
        }
    }
}
