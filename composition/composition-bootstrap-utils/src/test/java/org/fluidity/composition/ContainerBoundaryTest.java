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
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;

import org.fluidity.composition.spi.ContainerProvider;
import org.fluidity.composition.spi.PlatformContainer;
import org.fluidity.foundation.logging.NoLogFactory;
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

    private final PlatformContainer platform = mock(PlatformContainer.class);
    private final BootstrapServices providers = mock(BootstrapServices.class);
    private final ContainerBootstrap bootstrap = mock(ContainerBootstrap.class);
    private final ContainerProvider provider = mock(ContainerProvider.class);
    private final OpenComponentContainer container = mock(OpenComponentContainer.class);
    private final OpenComponentContainer.Registry registry = mock(OpenComponentContainer.Registry.class);

    private final ContainerServicesFactory servicesFactory = mock(ContainerServicesFactory.class);
    private final ContainerServices services = mock(ContainerServices.class);

    private final DependencyGraph.Traversal.Strategy strategy = mock(DependencyGraph.Traversal.Strategy.class);
    private final LogFactory logs = new NoLogFactory();

    private void setupDependencies(final ClassLoader classLoader, final boolean assign) {
        EasyMock.expect(providers.<ContainerBootstrap>findInstance(ContainerBootstrap.class, classLoader)).andReturn(assign ? bootstrap : null);
        EasyMock.expect(providers.<ContainerProvider>findInstance(ContainerProvider.class, classLoader)).andReturn(assign ? provider : null);

        if (assign) {
            EasyMock.expect(providers.<ContainerServicesFactory>findInstance(ContainerServicesFactory.class, classLoader)).andReturn(servicesFactory);
            EasyMock.expect(providers.<LogFactory>findInstance(LogFactory.class, classLoader)).andReturn(logs);
            EasyMock.expect(providers.<DependencyGraph.Traversal.Strategy>findInstance(DependencyGraph.Traversal.Strategy.class, classLoader)).andReturn(strategy);
            EasyMock.expect(servicesFactory.containerServices(logs, strategy)).andReturn(services);
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

        boundary.setPlatformContainer(platform);

        for (final Map.Entry<String, String> entry : properties.entrySet()) {
            boundary.setBindingProperty(entry.getKey(), entry.getValue());
        }

        // make subject receive its dependencies from the top-level class loader
        setupDependencies(classLoader, true);

        final ContainerBootstrap.Callback callback[] = new ContainerBootstrap.Callback[1];

        // give subject a container for that class loader
        EasyMock.expect(bootstrap.populateContainer(EasyMock.same(services),
                                                    EasyMock.same(provider),
                                                    EasyMock.<Properties>notNull(),
                                                    EasyMock.<OpenComponentContainer>same(null),
                                                    EasyMock.same(classLoader),
                                                    EasyMock.same(platform),
                                                    EasyMock.<ContainerBootstrap.Callback>notNull())).andAnswer(new IAnswer<OpenComponentContainer>() {
            public OpenComponentContainer answer() throws Throwable {

                // check that the properties received by bootstrap is contains exactly what we set up above
                assert properties.equals(EasyMock.getCurrentArguments()[2]);
                callback[0] = ((ContainerBootstrap.Callback) EasyMock.getCurrentArguments()[6]);

                return container;
            }
        });

        bootstrap.initializeContainer(container, services);
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                callback[0].containerInitialized(container);
                return null;
            }
        });

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

        // make subject receive no dependency for all class loaders except ours
        for (final ListIterator<ClassLoader> i = classLoaders.listIterator(classLoaders.size()); i.hasPrevious();) {
            ClassLoader cl = i.previous();

            // make subject receive its dependencies from the top-level class loader
            setupDependencies(cl, false);
        }

        // make subject receive its dependencies from our class loader
        // make subject receive its dependencies from the top-level class loader
        setupDependencies(getClass().getClassLoader(), true);

        final ContainerBootstrap.Callback callback[] = new ContainerBootstrap.Callback[1];

        // give subject a container for our class loader
        EasyMock.expect(bootstrap.populateContainer(EasyMock.same(services),
                                                    EasyMock.same(provider),
                                                    EasyMock.<Properties>notNull(),
                                                    EasyMock.<OpenComponentContainer>same(null),
                                                    EasyMock.same(getClass().getClassLoader()),
                                                    EasyMock.<PlatformContainer>isNull(),
                                                    EasyMock.<ContainerBootstrap.Callback>notNull())).andAnswer(new IAnswer<OpenComponentContainer>() {
            public OpenComponentContainer answer() throws Throwable {

                // check that the properties received by bootstrap is contains exactly what we set up above
                assert properties.equals(EasyMock.getCurrentArguments()[2]);
                callback[0] = ((ContainerBootstrap.Callback) EasyMock.getCurrentArguments()[6]);

                return container;
            }
        });

        bootstrap.initializeContainer(container, services);
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                callback[0].containerInitialized(container);
                return null;
            }
        });

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

        // make subject receive its dependencies from the top-level class loader
        setupDependencies(classLoader, true);

        // go through the whole class loader ancestry
        for (final ListIterator<ClassLoader> i = classLoaders.listIterator(classLoaders.size()); i.hasPrevious();) {
            final ClassLoader cl = i.previous();
            final OpenComponentContainer container = containers.get(cl);

            final ContainerBootstrap.Callback callback[] = new ContainerBootstrap.Callback[1];

            // make subject receive a container (the same) at each level
            EasyMock.expect(bootstrap.populateContainer(EasyMock.same(services),
                                                        EasyMock.same(provider),
                                                        EasyMock.<Properties>notNull(),
                                                        EasyMock.same(containers.get(cl.getParent())),
                                                        EasyMock.same(cl),
                                                        EasyMock.<PlatformContainer>isNull(),
                                                        EasyMock.<ContainerBootstrap.Callback>notNull())).andAnswer(new IAnswer<OpenComponentContainer>() {
                public OpenComponentContainer answer() throws Throwable {
                    callback[0] = ((ContainerBootstrap.Callback) EasyMock.getCurrentArguments()[6]);
                    return container;
                }
            });

            // the container must also be initialized at some point
            bootstrap.initializeContainer(container, services);
            EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
                public Object answer() throws Throwable {
                    callback[0].containerInitialized(container);
                    return null;
                }
            });
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

        // make subject receive its dependencies from the top-level class loader
        setupDependencies(classLoader, true);

        final ContainerBootstrap.Callback callback[] = new ContainerBootstrap.Callback[1];

        // give subject a container for that class loader
        EasyMock.expect(bootstrap.populateContainer(EasyMock.same(services),
                                                    EasyMock.same(provider),
                                                    EasyMock.<Properties>notNull(),
                                                    EasyMock.<OpenComponentContainer>same(null),
                                                    EasyMock.same(classLoader),
                                                    EasyMock.<PlatformContainer>isNull(),
                                                    EasyMock.<ContainerBootstrap.Callback>notNull())).andAnswer(new IAnswer<OpenComponentContainer>() {
            public OpenComponentContainer answer() throws Throwable {
                callback[0] = (ContainerBootstrap.Callback) EasyMock.getCurrentArguments()[6];
                return container;
            }
        });

        final BootComponent1 component1 = new BootComponent1();
        final BootComponent2 component2 = new BootComponent2();

        EasyMock.expect(container.getRegistry()).andReturn(registry);
        registry.bindInstance(component1);

        EasyMock.expect(container.getRegistry()).andReturn(registry);
        registry.bindInstance(component2);

        // container is initialized
        bootstrap.initializeContainer(container, services);
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                callback[0].containerInitialized(container);
                return null;
            }
        });

        replay();

        boundary.bindBootComponent(component1);
        boundary.bindBootComponent(component2);

        // first access goes through the above interaction
        assert boundary.getContainer() == container;

        verify();

        replay();

        try {
            boundary.bindBootComponent(new BootComponent3());
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
