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

package org.fluidity.composition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.container.ContainerServices;
import org.fluidity.composition.container.spi.ContainerProvider;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.composition.spi.EmptyPackageBindings;
import org.fluidity.composition.spi.PackageBindings;
import org.fluidity.foundation.Command;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public final class ContainerBootstrapImplTest extends Simulator {

    private final MockObjects dependencies = dependencies();
    private final Log log = NoLogFactory.consume(getClass());

    private final ContainerBootstrap.Callback callback = dependencies.normal(ContainerBootstrap.Callback.class);
    private final ContainerProvider provider = dependencies.normal(ContainerProvider.class);
    private final ContainerServices services = dependencies.normal(ContainerServices.class);
    private final ComponentDiscovery discovery = dependencies.normal(ComponentDiscovery.class);
    private final ContainerTermination termination = dependencies.normal(ContainerTermination.class);
    private final MutableContainer parent = dependencies.normal(MutableContainer.class);
    private final MutableContainer container = dependencies.normal(MutableContainer.class);
    private final ComponentContainer.Registry registry = dependencies.normal(ComponentContainer.Registry.class);
    private final MutableContainer bindingsContainer = dependencies.normal(MutableContainer.class);
    private final ComponentContainer.Registry bindingsRegistry = dependencies.normal(ComponentContainer.Registry.class);
    private final PackageBindings bindings = dependencies.normal(PackageBindings.class);

    private ContainerBootstrap bootstrap;

    @BeforeMethod
    public void setup() {
        bootstrap = new ContainerBootstrapImpl();

        EasyMock.expect(services.createLog(EasyMock.<Log>anyObject(), EasyMock.<Class<?>>anyObject())).andReturn(log).anyTimes();
        EasyMock.expect(services.componentDiscovery()).andReturn(discovery).anyTimes();
    }

    @SuppressWarnings("unchecked")
    private Object populateContainer(final Class[] classes, final PackageBindings[] instances, final PackageBindings... bindings) throws Exception {
        EasyMock.expect(provider.newContainer(services, false)).andReturn(container);
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, false)).andReturn(classes);

        EasyMock.expect(provider.newContainer(services, true)).andReturn(bindingsContainer);
        EasyMock.expect(bindingsContainer.getRegistry()).andReturn(bindingsRegistry);
        bindingsRegistry.bindComponentGroup(PackageBindings.class, classes);
        EasyMock.expect(bindingsContainer.getComponentGroup(PackageBindings.class)).andReturn(instances);

        EasyMock.expect(container.getRegistry()).andReturn(registry);

        for (final PackageBindings instance : bindings) {
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

        verify(new Task() {
            public void run() throws Exception {
                bootstrap.populateContainer(services, provider, null, null, null, callback);
            }
        });

        setup();

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

        final Command.Job<Exception> job = test(new Work<Command.Job<Exception>>() {
            public Command.Job<Exception> run() throws Exception {
                final Class[] classes = { ResponsiblePackageBindingsImpl.class, PackageBindingsImpl.class, DependentPackageBindingsImpl.class };
                final PackageBindings[] instances = { bindings1, bindings2, bindings3 };

                final Object object = populateContainer(classes, instances, bindings2, bindings3);

                EasyMock.expect(container.getComponent(ContainerTermination.class)).andReturn(termination);
                EasyMock.expect(container.getComponent(EasyMock.<Class<?>>anyObject())).andReturn(object);

                final AtomicReference<Command.Job<Exception>> job = new AtomicReference<Command.Job<Exception>>();

                termination.add(EasyMock.<Command.Job<Exception>>anyObject());
                EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
                    @SuppressWarnings("unchecked")
                    public Object answer() throws Throwable {
                        job.set(((Command.Job<Exception>) EasyMock.getCurrentArguments()[0]));
                        return null;
                    }
                });

                callback.containerInitialized();

                bindings.initialize(container, termination);
                EasyMock.expectLastCall().times(2);

                verify(new Task() {
                    public void run() throws Exception {
                        bootstrap.initializeContainer(container, services);
                    }
                });

                return job.get();
            }
        });

        assert job != null;
        assert list.equals(watched);

        test(new Task() {
            public void run() throws Exception {
                callback.containerShutdown();

                verify(new Task() {
                    public void run() throws Exception {
                        job.run();
                    }
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*require.*ContainerTermination.*")
    public void missingShutdownHook() throws Exception {
        final Object object = populateContainer(new Class[0], new PackageBindings[0]);

        EasyMock.expect(container.getComponent(EasyMock.<Class>anyObject())).andReturn(object); // type unknown outside but component expected by recipient
        EasyMock.expect(container.getComponent(ContainerTermination.class)).andReturn(null);

        // must always be invoked
        callback.containerInitialized();

        guarantee(new Task() {
            public void run() throws Exception {
                bootstrap.initializeContainer(container, services);
            }
        });
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

        EasyMock.expect(provider.newContainer(services, true)).andReturn(bindingsContainer);
        EasyMock.expect(bindingsContainer.getRegistry()).andReturn(bindingsRegistry);
        bindingsRegistry.bindComponentGroup(PackageBindings.class, assemblies);
        EasyMock.expect(bindingsContainer.getComponentGroup(PackageBindings.class)).andReturn(new PackageBindings[] { bindings1, bindings2, bindings3 });

        EasyMock.expect(container.getRegistry()).andReturn(registry);

        bindings.bindComponents(registry);
        EasyMock.expectLastCall().times(watched);

        registry.bindInstance(EasyMock.anyObject());

        assert container == verify(new Work<MutableContainer>() {
            public MutableContainer run() throws Exception {
                return bootstrap.populateContainer(services, provider, null, parent, null, callback);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void standaloneComponentAssembly() throws Exception {

        // we're not passing a container so subject is expected to create one
        EasyMock.expect(provider.newContainer(services, false)).andReturn(container);
        EasyMock.expect(container.getRegistry()).andReturn(registry);

        final Class[] assemblies = {
                ShutdownHookPackageBindingsImpl.class,
        };

        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, false)).andReturn(assemblies);

        EasyMock.expect(provider.newContainer(services, true)).andReturn(bindingsContainer);
        EasyMock.expect(bindingsContainer.getRegistry()).andReturn(bindingsRegistry);
        bindingsRegistry.bindComponentGroup(PackageBindings.class, assemblies);
        EasyMock.expect(bindingsContainer.getComponentGroup(PackageBindings.class)).andReturn(new PackageBindings[] { new ShutdownHookPackageBindingsImpl() });

        registry.bindInstance(discovery);

        ShutdownHookPackageBindingsImpl.bindings = bindings;
        bindings.bindComponents(registry);

        registry.bindInstance(EasyMock.anyObject());

        final MutableContainer populated = verify(new Work<MutableContainer>() {
            public MutableContainer run() throws Exception {
                return bootstrap.populateContainer(services, provider, null, null, null, callback);
            }
        });

        assert populated != null;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bindingProperties() throws Exception {

        // we're passing a parent to subject is expected to as for a child container
        EasyMock.expect(parent.makeChildContainer()).andReturn(container);
        EasyMock.expect(parent.getComponent(EasyMock.<Class<?>>anyObject())).andReturn(null);

        final Class[] classes = new Class[0];
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, true)).andReturn(classes);

        final Properties properties = new Properties();

        final String value = "value";
        properties.setProperty(ConfigurablePackageBindingsImpl.KEY, value);

        EasyMock.expect(container.getRegistry()).andReturn(registry);

        EasyMock.expect(provider.newContainer(services, true)).andReturn(bindingsContainer);
        EasyMock.expect(bindingsContainer.getRegistry()).andReturn(bindingsRegistry);
        bindingsRegistry.bindInstance(properties, Map.class);
        bindingsRegistry.bindComponentGroup(PackageBindings.class, classes);
        EasyMock.expect(bindingsContainer.getComponentGroup(PackageBindings.class)).andReturn(new PackageBindings[0]);

        registry.bindInstance(EasyMock.anyObject());

        assert container == verify(new Work<MutableContainer>() {
            public MutableContainer run() throws Exception {
                return bootstrap.populateContainer(services, provider, properties, parent, null, callback);
            }
        });
    }

    public static class ResponsiblePackageBindingsImpl extends EmptyPackageBindings {
        // empty
    }

    public static class ShutdownHookPackageBindingsImpl extends EmptyPackageBindings {

        public static PackageBindings bindings;

        public void bindComponents(final ComponentContainer.Registry registry) {
            bindings.bindComponents(registry);
        }

        public void initialize(final OpenContainer container, final ContainerTermination shutdown) throws Exception {
            bindings.initialize(container, shutdown);
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

        public void initialize(final OpenContainer container, final ContainerTermination shutdown) throws Exception {
            bindings.initialize(container, shutdown);
            list.add(this);
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

        public void initialize(final OpenContainer container, final ContainerTermination shutdown) throws Exception {
            bindings.initialize(container, shutdown);
            list.add(this);
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
