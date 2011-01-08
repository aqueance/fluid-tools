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

import org.fluidity.composition.spi.ContainerServices;
import org.fluidity.foundation.NullLogFactory;
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

    private final LogFactory logs = new NullLogFactory();

    private final ContainerProvider provider = addControl(ContainerProvider.class);
    private final ContainerServices services = addControl(ContainerServices.class);
    private final ClassDiscovery discovery = addControl(ClassDiscovery.class);
    private final ShutdownHook shutdown = addControl(ShutdownHook.class);
    private final OpenComponentContainer container = addControl(OpenComponentContainer.class);
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
    @Test
    public void connectedComponentAssembly() throws Exception {
        final Class[] assemblies = {
                PackageBindingsImpl.class, DependentPackageBindingsImpl.class, ResponsiblePackageBindingsImpl.class,
        };

        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, true)).andReturn(assemblies);

        EasyMock.expect(provider.instantiateBindings(EasyMock.same(services), EasyMock.<Map>isNull(), EasyMock.<Collection<Class<PackageBindings>>>notNull()))
                .andAnswer(new IAnswer() {
                    public Object answer() throws Throwable {
                        final ResponsiblePackageBindingsImpl bindings1 = new ResponsiblePackageBindingsImpl();
                        final PackageBindingsImpl bindings2 = new PackageBindingsImpl(bindings1);
                        final DependentPackageBindingsImpl bindings3 = new DependentPackageBindingsImpl(bindings2);

                        return Arrays.asList(bindings1, bindings2, bindings3);
                    }
                });

        final List<PackageBindings> list = new ArrayList<PackageBindings>();

        PackageBindingsImpl.bindings = bindings;
        PackageBindingsImpl.list = list;
        DependentPackageBindingsImpl.bindings = bindings;
        DependentPackageBindingsImpl.list = list;

        EasyMock.expect(container.getRegistry()).andReturn(registry);

        bindings.bindComponents(registry);
        EasyMock.expectLastCall().times(2);

        bindings.initializeComponents(container);
        EasyMock.expectLastCall().times(2);

        bindings.shutdownComponents(container);
        EasyMock.expectLastCall().times(2);

        EasyMock.expect(container.makeNestedContainer()).andReturn(container);
        EasyMock.expect(container.getComponent(ShutdownHook.class)).andReturn(shutdown);

        shutdown.addTask(EasyMock.<String>notNull(), EasyMock.<Runnable>anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                Object[] args = EasyMock.getCurrentArguments();

                // check that both components have been initialised
                assert list.size() == 2;
                assert list.get(0) instanceof PackageBindingsImpl;
                assert list.get(1) instanceof DependentPackageBindingsImpl;

                // invoke the shutdown command to test deregistration
                ((Runnable) args[1]).run();

                // check that all components have been shut down
                assert list.isEmpty();
                return null;
            }
        });

        replay();
        assert container == bootstrap.populateContainer(services, provider, null, container, null);
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
                .andAnswer(new IAnswer() {
                    public Object answer() throws Throwable {
                        return Collections.singletonList(new ShutdownHookPackageBindingsImpl());
                    }
                });

        registry.bindInstance(ClassDiscovery.class, discovery);

        ShutdownHookPackageBindingsImpl.bindings = bindings;
        bindings.bindComponents(registry);
        bindings.initializeComponents(container);

        EasyMock.expect(container.getComponent(ShutdownHook.class)).andReturn(shutdown);

        shutdown.addTask(EasyMock.<String>notNull(), EasyMock.<Runnable>anyObject());

        replay();
        assert bootstrap.populateContainer(services, provider, null, null, null) != null;
        verify();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bindingProperties() throws Exception {
        final Class<?>[] assemblies = new Class<?>[0];
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, true)).andReturn((Class<PackageBindings>[]) assemblies);

        final Properties properties = new Properties();

        final String value = "value";
        properties.setProperty(ConfigurablePackageBindingsImpl.KEY, value);
        EasyMock.expect(provider.instantiateBindings(EasyMock.same(services), EasyMock.eq(properties), EasyMock.<Collection<Class<PackageBindings>>>notNull()))
                .andReturn(new ArrayList());

        EasyMock.expect(container.getRegistry()).andReturn(registry);

        EasyMock.expect(container.makeNestedContainer()).andReturn(container);
        EasyMock.expect(container.getComponent(ShutdownHook.class)).andReturn(shutdown);

        shutdown.addTask(EasyMock.<String>notNull(), EasyMock.<Runnable>anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                Object[] args = EasyMock.getCurrentArguments();

                // invoke the shutdown command to test deregistration
                ((Runnable) args[1]).run();
                return null;
            }
        });

        replay();
        assert container == bootstrap.populateContainer(services, provider, properties, container, null);
        verify();
    }

    @SuppressWarnings("unchecked")
    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = ".*require.*ShutdownHook.*")
    public void missingDependency() throws Exception {
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, true)).andReturn(new Class[0]);

        EasyMock.expect(provider.instantiateBindings(EasyMock.same(services), EasyMock.<Map>isNull(), EasyMock.<Collection<Class<PackageBindings>>>notNull()))
                .andReturn(new ArrayList());
        EasyMock.expect(container.getComponent(ShutdownHook.class)).andReturn(null);

        EasyMock.expect(container.makeNestedContainer()).andReturn(container);
        EasyMock.expect(container.getRegistry()).andReturn(registry);

        replay();

        try {
            assert container == bootstrap.populateContainer(services, provider, null, container, null);
        } finally {
            verify();
        }
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

        public static final String KEY = ConfigurablePackageBindingsImpl.class.getName() + ".key";
        public static String value;

        public ConfigurablePackageBindingsImpl(final Map<String, String> properties) {
            assert properties != null;
            value = properties.get(KEY);
        }
    }
}
