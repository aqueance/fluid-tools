/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package org.fluidity.composition.pico;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Map;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.fluidity.composition.ClassDiscovery;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.EmptyPackageBindings;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.composition.PackageBindings;
import org.fluidity.composition.ShutdownHook;
import org.fluidity.tests.MockGroupAbstractTest;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ContainerBootstrapImplTest extends MockGroupAbstractTest {

    private final ClassDiscovery discovery = addControl(ClassDiscovery.class);

    private final ShutdownHook shutdown = addControl(ShutdownHook.class);

    private final OpenComponentContainer container = addControl(OpenComponentContainer.class);

    private final ComponentContainer.Registry registry = addControl(ComponentContainer.Registry.class);

    private final PackageBindings bindings = addControl(PackageBindings.class);

    public ContainerBootstrapImplTest() {
        ShutdownHookPackageBindingsImpl.hook = shutdown;
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void connectedComponentAssembly() throws Exception {
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, true)).andReturn(new Class[] {
            PackageBindingsImpl.class,
            DependentPackageBindingsImpl.class,
            ResponsiblePackageBindingsImpl.class,
        });

        final List<PackageBindings> list = new ArrayList<PackageBindings>();

        PackageBindingsImpl.bindings = bindings;
        PackageBindingsImpl.list = list;
        DependentPackageBindingsImpl.bindings = bindings;
        DependentPackageBindingsImpl.list = list;

        EasyMock.expect(container.getRegistry()).andReturn(registry);

        bindings.registerComponents(registry);
        EasyMock.expectLastCall().times(2);

        bindings.initialiseComponents(container);
        EasyMock.expectLastCall().times(2);

        bindings.shutdownComponents(container);
        EasyMock.expectLastCall().times(2);

        registry.requireDependency(ShutdownHook.class, PicoContainerBootstrap.class);

        EasyMock.expect(container.makeNestedContainer()).andReturn(container);
        EasyMock.expect(container.getUnresolvedDependencies()).andReturn(new HashMap<Class, List<Class>>());
        EasyMock.expect(container.getComponent(ShutdownHook.class)).andReturn(shutdown);

        shutdown.addTask((String) EasyMock.notNull(), (Runnable) EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                Object[] args = EasyMock.getCurrentArguments();

                // check that all components have been initialised
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
        assert container == new PicoContainerBootstrap().populateContainer(discovery, null, container, null);
        verify();
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void standaloneComponentAssembly() throws Exception {
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, false)).andReturn(new Class[] {
            ShutdownHookPackageBindingsImpl.class,
        });

        shutdown.addTask((String) EasyMock.notNull(), (Runnable) EasyMock.anyObject());

        replay();
        final OpenComponentContainer picoContainer =
            new PicoContainerBootstrap().populateContainer(discovery, null, null, null);
        assert picoContainer != null;
        assert picoContainer.getComponent(ShutdownHook.class) == shutdown;
        verify();
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void missingDependencyException() throws Exception {
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, false)).andReturn(new Class[0]);

        replay();
        try {
            new PicoContainerBootstrap().populateContainer(discovery, null, null, null);
        } catch (final MissingDependenciesException e) {
            String message = e.getMessage();
            assert message.indexOf(ShutdownHook.class.getName()) > -1;
            assert message.indexOf(PicoContainerBootstrap.class.getName()) > -1;
        } finally {
            verify();
        }
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void bindingProperties() throws Exception {
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, true)).andReturn(new Class[] {
            ConfigurablePackageBindingsImpl.class,
        });

        EasyMock.expect(container.getRegistry()).andReturn(registry);

        registry.requireDependency(ShutdownHook.class, PicoContainerBootstrap.class);

        EasyMock.expect(container.makeNestedContainer()).andReturn(container);
        EasyMock.expect(container.getUnresolvedDependencies()).andReturn(new HashMap<Class, List<Class>>());
        EasyMock.expect(container.getComponent(ShutdownHook.class)).andReturn(shutdown);

        final String value = "value";

        final Properties properties = new Properties();
        properties.setProperty(ConfigurablePackageBindingsImpl.KEY, value);

        shutdown.addTask((String) EasyMock.notNull(), (Runnable) EasyMock.anyObject());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                Object[] args = EasyMock.getCurrentArguments();

                // invoke the shutdown command to test deregistration
                ((Runnable) args[1]).run();
                return null;
            }
        });

        ConfigurablePackageBindingsImpl.value = null;
        replay();
        assert container == new PicoContainerBootstrap().populateContainer(discovery, properties, container, null);
        verify();

        assert value.equals(ConfigurablePackageBindingsImpl.value);
    }

    @SuppressWarnings({ "unchecked" })
    @Test(expectedExceptions = MissingDependenciesException.class)
    public void missingDependency() throws Exception {
        EasyMock.expect(discovery.findComponentClasses(PackageBindings.class, null, true)).andReturn(new Class[] {
            ResponsiblePackageBindingsImpl.class,
        });

        EasyMock.expect(container.makeNestedContainer()).andReturn(container);
        EasyMock.expect(container.getRegistry()).andReturn(registry);

        registry.requireDependency(ShutdownHook.class, PicoContainerBootstrap.class);

        final Map<Class, List<Class>> missing = new HashMap<Class, List<Class>>();
        missing.put(getClass(), Arrays.asList((Class) getClass()));
        EasyMock.expect(container.getUnresolvedDependencies()).andReturn(missing);

        replay();

        try {
            assert container == new PicoContainerBootstrap().populateContainer(discovery, null, container, null);
        } finally {
            verify();
        }
    }

    public static class ResponsiblePackageBindingsImpl extends EmptyPackageBindings {
        // empty
    }

    public static class ShutdownHookPackageBindingsImpl extends EmptyPackageBindings {

        public static ShutdownHook hook;

        public void registerComponents(ComponentContainer.Registry registry) {
            registry.bind(ShutdownHook.class, hook);
        }
    }

    public static class PackageBindingsImpl implements PackageBindings {

        public static PackageBindings bindings;

        public static List<PackageBindings> list;

        @SuppressWarnings({ "UnusedDeclaration" })
        public PackageBindingsImpl(ResponsiblePackageBindingsImpl dependent) {
            // empty
        }

        public void registerComponents(ComponentContainer.Registry registry) {
            bindings.registerComponents(registry);
        }

        public void initialiseComponents(ComponentContainer container) {
            bindings.initialiseComponents(container);
            list.add(this);
        }

        public void shutdownComponents(ComponentContainer container) {
            bindings.shutdownComponents(container);
            list.remove(this);
        }
    }

    public static class DependentPackageBindingsImpl implements PackageBindings {

        public static PackageBindings bindings;

        public static List<PackageBindings> list;

        @SuppressWarnings({ "UnusedDeclaration" })
        public DependentPackageBindingsImpl(PackageBindingsImpl dependent) {
            // empty
        }

        public void registerComponents(ComponentContainer.Registry registry) {
            bindings.registerComponents(registry);
        }

        public void initialiseComponents(ComponentContainer container) {
            bindings.initialiseComponents(container);
            list.add(this);
        }

        public void shutdownComponents(ComponentContainer container) {
            bindings.shutdownComponents(container);
            list.remove(this);
        }
    }

    public static class ConfigurablePackageBindingsImpl extends EmptyPackageBindings {
        public static final String KEY = ConfigurablePackageBindingsImpl.class.getName() + ".key";
        public static String value;

        public ConfigurablePackageBindingsImpl(Properties properties) {
            assert properties != null;
            value = properties.getProperty(KEY);
        }
    }
}
