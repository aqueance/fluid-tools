/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.composition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fluidity.composition.spi.ContainerProvider;
import org.fluidity.composition.spi.EmptyPackageBindings;
import org.fluidity.composition.spi.PackageBindings;
import org.fluidity.composition.spi.ShutdownTasks;
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

    private final ContainerProvider provider = addControl(ContainerProvider.class);
    private final ContainerServices services = addControl(ContainerServices.class);
    private final ClassDiscovery discovery = addControl(ClassDiscovery.class);
    private final ShutdownTasks shutdown = addControl(ShutdownTasks.class);
    private final OpenComponentContainer container = addControl(OpenComponentContainer.class);
    private final OpenComponentContainer parent = addControl(OpenComponentContainer.class);
    private final ComponentContainer.Registry registry = addControl(ComponentContainer.Registry.class);
    private final PackageBindings bindings = addControl(PackageBindings.class);

    private ContainerBootstrap bootstrap;

    @BeforeMethod
    public void dependencies() {
        bootstrap = new ContainerBootstrapImpl();

        EasyMock.expect(services.logs()).andReturn(logs).anyTimes();
        EasyMock.expect(services.classDiscovery()).andReturn(discovery).anyTimes();
    }

    @SuppressWarnings("unchecked")
    private Object populateContainer(final Class[] classes, final List<PackageBindings> instances) {
        EasyMock.expect(provider.newContainer(services)).andReturn(container);
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, false)).andReturn(classes);

        EasyMock.expect(provider.instantiateBindings(EasyMock.same(services), EasyMock.<Map>isNull(), EasyMock.<Collection<Class<PackageBindings>>>notNull()))
                .andAnswer(new IAnswer<List<PackageBindings>>() {
                    public List<PackageBindings> answer() throws Throwable {
                        return instances;
                    }
                });

        EasyMock.expect(container.getRegistry()).andReturn(registry);

        for (final PackageBindings instance : instances) {
            instance.bindComponents(registry);
        }

        registry.bindInstance(ClassDiscovery.class, discovery);
        registry.bindInstance(EasyMock.<Class>anyObject(), EasyMock.anyObject());

        final Object[] list = new Object[1];

        EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
            public Void answer() throws Throwable {
                list[0] = EasyMock.getCurrentArguments()[1];
                return null;
            }
        });

        replay();
        bootstrap.populateContainer(services, provider, null, null, null);
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

        shutdown.add(EasyMock.<String>notNull(), EasyMock.<Runnable>anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                Object[] args = EasyMock.getCurrentArguments();
                assert list.equals(watched);

                // invoke the shutdown command to test de-registration
                ((Runnable) args[1]).run();

                // check that all components have been shut down
                assert list.isEmpty();
                return null;
            }
        });

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

        // we're passing a parent to testee is expected to as for a child container
        EasyMock.expect(parent.makeChildContainer()).andReturn(container);
        EasyMock.expect(parent.getComponent(EasyMock.<Class<?>>anyObject())).andReturn(null);
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, true)).andReturn(assemblies);

        EasyMock.expect(provider.instantiateBindings(EasyMock.same(services), EasyMock.<Map>isNull(), EasyMock.<Collection<Class<PackageBindings>>>notNull()))
                .andAnswer(new IAnswer<List<PackageBindings>>() {
                    public List<PackageBindings> answer() throws Throwable {
                        return Arrays.asList(bindings1, bindings2, bindings3);
                    }
                });

        EasyMock.expect(container.getRegistry()).andReturn(registry);

        bindings.bindComponents(registry);
        EasyMock.expectLastCall().times(watched);

        registry.bindInstance(EasyMock.<Class>anyObject(), EasyMock.anyObject());

        replay();
        assert container == bootstrap.populateContainer(services, provider, null, parent, null);
        verify();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void standaloneComponentAssembly() throws Exception {

        // we're not passing a container so testee is expected to create one
        EasyMock.expect(provider.newContainer(services)).andReturn(container);
        EasyMock.expect(container.getRegistry()).andReturn(registry);

        final Class[] assemblies = {
                ShutdownHookPackageBindingsImpl.class,
        };

        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, false)).andReturn(assemblies);

        EasyMock.expect(provider.instantiateBindings(EasyMock.same(services), EasyMock.<Map>isNull(), EasyMock.<Collection<Class<PackageBindings>>>notNull()))
                .andAnswer(new IAnswer<List<PackageBindings>>() {
                    public List<PackageBindings> answer() throws Throwable {
                        return Collections.singletonList((PackageBindings) new ShutdownHookPackageBindingsImpl());
                    }
                });

        registry.bindInstance(ClassDiscovery.class, discovery);

        ShutdownHookPackageBindingsImpl.bindings = bindings;
        bindings.bindComponents(registry);

        registry.bindInstance(EasyMock.<Class>anyObject(), EasyMock.anyObject());

        replay();
        assert bootstrap.populateContainer(services, provider, null, null, null) != null;
        verify();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bindingProperties() throws Exception {

        // we're passing a parent to testee is expected to as for a child container
        EasyMock.expect(parent.makeChildContainer()).andReturn(container);
        EasyMock.expect(parent.getComponent(EasyMock.<Class<?>>anyObject())).andReturn(null);
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, true)).andReturn(new Class[0]);

        final Properties properties = new Properties();

        final String value = "value";
        properties.setProperty(ConfigurablePackageBindingsImpl.KEY, value);
        EasyMock.expect(provider.instantiateBindings(EasyMock.same(services), EasyMock.eq(properties), EasyMock.<Collection<Class<PackageBindings>>>notNull()))
                .andReturn(new ArrayList<PackageBindings>());

        EasyMock.expect(container.getRegistry()).andReturn(registry);

        registry.bindInstance(EasyMock.<Class>anyObject(), EasyMock.anyObject());

        replay();
        assert container == bootstrap.populateContainer(services, provider, properties, parent, null);
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
