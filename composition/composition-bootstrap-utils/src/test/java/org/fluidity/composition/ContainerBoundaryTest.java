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
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;

import org.fluidity.composition.spi.ContainerProvider;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.foundation.spi.LogFactory;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IMocksControl;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ContainerBoundaryTest extends MockGroupAbstractTest {

    private final BootstrapServices providers = addControl(BootstrapServices.class);
    private final ContainerBootstrap bootstrap = addControl(ContainerBootstrap.class);
    private final ContainerProvider provider = addControl(ContainerProvider.class);
    private final OpenComponentContainer container = addControl(OpenComponentContainer.class);
    private final OpenComponentContainer.Registry registry = addControl(OpenComponentContainer.Registry.class);

    private final ContainerServicesFactory servicesFactory = addControl(ContainerServicesFactory.class);
    private final ContainerServices services = addControl(ContainerServices.class);

    private final LogFactory logs = new NoLogFactory();

    private void setupDependencies(final ClassLoader classLoader, final boolean assign) {
        EasyMock.expect(providers.<ContainerBootstrap>findInstance(ContainerBootstrap.class, classLoader)).andReturn(assign ? bootstrap : null);
        EasyMock.expect(providers.<ContainerProvider>findInstance(ContainerProvider.class, classLoader)).andReturn(assign ? provider : null);

        if (assign) {
            EasyMock.expect(providers.<ContainerServicesFactory>findInstance(ContainerServicesFactory.class, classLoader)).andReturn(servicesFactory);
            EasyMock.expect(providers.<LogFactory>findInstance(LogFactory.class, classLoader)).andReturn(logs);
            EasyMock.expect(servicesFactory.containerServices(logs)).andReturn(services);
        }
    }

    @Test
    public void populatesTopLevelContainer() throws Exception {
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put("key1", "value1");
        properties.put("key2", "value2");

        // find the top level class loader
        ClassLoader classLoader;
        for (classLoader = getClass().getClassLoader(); classLoader.getParent() != null; classLoader = classLoader.getParent()) {
            // empty
        }

        // set up the test class
        final ContainerBoundary boundary = new ContainerBoundary(classLoader);
        boundary.reset(providers);

        for (final Map.Entry<String, String> entry : properties.entrySet()) {
            boundary.setBindingProperty(entry.getKey(), entry.getValue());
        }

        // make testee receive its dependencies from the top-level class loader
        setupDependencies(classLoader, true);

        // give testee a container for that class loader
        EasyMock.expect(bootstrap.populateContainer(EasyMock.same(services),
                                                    EasyMock.same(provider),
                                                    EasyMock.<Properties>notNull(),
                                                    EasyMock.<OpenComponentContainer>same(null),
                                                    EasyMock.same(classLoader))).andAnswer(new IAnswer<OpenComponentContainer>() {
            public OpenComponentContainer answer() throws Throwable {

                // check that the properties received by bootstrap is contains exactly what we set up above
                assert properties.equals(EasyMock.getCurrentArguments()[2]);

                return container;
            }
        });

        bootstrap.initializeContainer(container, services);

        replay();

        // first access goes through the above interaction
        assert boundary.getContainer() == container;

        verify();

        replay();

        // second access should simply return the cached container
        assert boundary.getContainer() == container;

        verify();
    }

    @Test
    public void populatesStandaloneContainer() throws Exception {
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put("key1", "value1");
        properties.put("key2", "value2");

        final ContainerBoundary boundary = new ContainerBoundary();
        boundary.reset(providers);

        for (final Map.Entry<String, String> entry : properties.entrySet()) {
            boundary.setBindingProperty(entry.getKey(), entry.getValue());
        }

        // find all class loaders on the ancestry except ours
        List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
        for (ClassLoader cl = getClass().getClassLoader().getParent(); cl != null; cl = cl.getParent()) {
            classLoaders.add(cl);
        }

        // make testee receive no dependency for all class loaders except ours
        for (final ListIterator<ClassLoader> i = classLoaders.listIterator(classLoaders.size()); i.hasPrevious();) {
            ClassLoader cl = i.previous();

            // make testee receive its dependencies from the top-level class loader
            setupDependencies(cl, false);
        }

        // make testee receive its dependencies from our class loader
        // make testee receive its dependencies from the top-level class loader
        setupDependencies(getClass().getClassLoader(), true);

        // give testee a container for our class loader
        EasyMock.expect(bootstrap.populateContainer(EasyMock.same(services),
                                                    EasyMock.same(provider),
                                                    EasyMock.<Properties>notNull(),
                                                    EasyMock.<OpenComponentContainer>same(null),
                                                    EasyMock.same(getClass().getClassLoader()))).andAnswer(new IAnswer<OpenComponentContainer>() {
            public OpenComponentContainer answer() throws Throwable {

                // check that the properties received by bootstrap is contains exactly what we set up above
                assert properties.equals(EasyMock.getCurrentArguments()[2]);

                return container;
            }
        });

        bootstrap.initializeContainer(container, services);

        replay();

        // first access goes through the above interaction
        assert boundary.getContainer() == container;

        verify();

        replay();

        // second access should simply return the cached container
        assert boundary.getContainer() == container;

        verify();
    }

    @Test
    public void populatesConnectedContainer() throws Exception {
        final ContainerBoundary boundary = new ContainerBoundary();
        boundary.reset(providers);

        final IMocksControl containersControl = EasyMock.createControl();
        final Map<ClassLoader, OpenComponentContainer> containers = new HashMap<ClassLoader, OpenComponentContainer>();

        // find all class loaders on the ancestry
        final List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
        final ClassLoader ourClassLoader = getClass().getClassLoader();
        assert ourClassLoader != null;

        for (ClassLoader cl = ourClassLoader; cl != null; cl = cl.getParent()) {
            classLoaders.add(cl);
            containers.put(cl, containersControl.createMock(OpenComponentContainer.class));
        }

        // find the top level class loader
        final ClassLoader classLoader = classLoaders.get(classLoaders.size() - 1);

        // make testee receive its dependencies from the top-level class loader
        setupDependencies(classLoader, true);

        // go through the whole class loader ancestry
        for (final ListIterator<ClassLoader> i = classLoaders.listIterator(classLoaders.size()); i.hasPrevious();) {
            final ClassLoader cl = i.previous();
            final OpenComponentContainer container = containers.get(cl);

            // make testee receive a container (the same) at each level
            EasyMock.expect(bootstrap.populateContainer(EasyMock.same(services),
                                                        EasyMock.same(provider),
                                                        EasyMock.<Properties>notNull(),
                                                        EasyMock.same(containers.get(cl.getParent())),
                                                        EasyMock.same(cl))).andReturn(container);

            // the container must also be initialized at some point
            bootstrap.initializeContainer(container, services);
        }

        final OpenComponentContainer ourContainer = containers.get(ourClassLoader);
        assert ourContainer != null;

        replay();

        // first access goes through the above interaction
        assert boundary.getContainer() == ourContainer;

        verify();

        replay();

        // second access should simply return the cached container
        assert boundary.getContainer() == ourContainer;

        verify();

        if (ourClassLoader.getParent() != null) {
            replay();

            // access to higher level container should simply return the cached container
            assert new ContainerBoundary(ourClassLoader.getParent()).getContainer() == containers.get(ourClassLoader.getParent());

            verify();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void bindsBootComponentBeforePopulationButRefusesAfter() throws Exception {

        // find the top level class loader
        ClassLoader classLoader;
        for (classLoader = getClass().getClassLoader(); classLoader.getParent() != null; classLoader = classLoader.getParent()) {
            // empty
        }

        // set up the test class
        final ContainerBoundary boundary = new ContainerBoundary(classLoader);
        boundary.reset(providers);

        // make testee receive its dependencies from the top-level class loader
        setupDependencies(classLoader, true);

        // give testee a container for that class loader
        EasyMock.expect(bootstrap.populateContainer(EasyMock.same(services),
                                                    EasyMock.same(provider),
                                                    EasyMock.<Properties>notNull(),
                                                    EasyMock.<OpenComponentContainer>same(null),
                                                    EasyMock.same(classLoader))).andReturn(container);

        final BootComponent1 component1 = new BootComponent1();
        final BootComponent2 component2 = new BootComponent2();

        EasyMock.expect(container.getRegistry()).andReturn(registry);
        registry.bindInstance(component1, BootComponent1.class);

        EasyMock.expect(container.getRegistry()).andReturn(registry);
        registry.bindInstance(component2, BootComponent2.class);

        // container is initialized
        bootstrap.initializeContainer(container, services);

        replay();

        boundary.bindBootComponent(BootComponent1.class, component1);
        boundary.bindBootComponent(BootComponent2.class, component2);

        // first access goes through the above interaction
        assert boundary.getContainer() == container;

        verify();

        replay();

        try {
            boundary.bindBootComponent(BootComponent3.class, new BootComponent3());
        } catch (final IllegalStateException e) {
            // ignore
        }

        verify();
    }

    private static class BootComponent1 {

    }

    private static class BootComponent2 {

    }

    private static class BootComponent3 {

    }
}
