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

package org.fluidity.composition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;

import org.fluidity.composition.container.ContainerServices;
import org.fluidity.composition.container.PlatformContainer;
import org.fluidity.composition.container.internal.ContainerServicesFactory;
import org.fluidity.composition.container.spi.ContainerProvider;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.foundation.spi.LogFactory;
import org.fluidity.testing.MockGroup;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ContainerBoundaryTest extends MockGroup {

    private final PlatformContainer platform = mock(PlatformContainer.class);
    private final BootstrapServices providers = mock(BootstrapServices.class);
    private final ContainerBootstrap bootstrap = mock(ContainerBootstrap.class);
    private final ContainerProvider provider = mock(ContainerProvider.class);
    private final MutableContainer container = mock(MutableContainer.class);
    private final ComponentContainer.Registry registry = mock(ComponentContainer.Registry.class);

    private final ContainerServicesFactory servicesFactory = mock(ContainerServicesFactory.class);
    private final ContainerServices services = mock(ContainerServices.class);

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

    private ContainerBoundary boundary(final ClassLoader loader) {
        final ContainerBoundary boundary = loader == null ? new ContainerBoundary() : new ContainerBoundary(loader);
        boundary.reset(providers);
        return boundary;
    }

    @Test
    public void populatesTopLevelContainer() throws Exception {
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put("key1", "value1");
        properties.put("key2", "value2");

        // find the top level class loader
        final ClassLoader classLoader = root();

        // set up the test class
        final ContainerBoundary boundary = boundary(classLoader);

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
                                                    EasyMock.<MutableContainer>same(null),
                                                    EasyMock.same(classLoader),
                                                    EasyMock.same(platform),
                                                    EasyMock.<ContainerBootstrap.Callback>notNull())).andAnswer(new IAnswer<MutableContainer>() {
            public MutableContainer answer() throws Throwable {

                // check that the properties received by bootstrap is contains exactly what we set up above
                assert properties.equals(EasyMock.getCurrentArguments()[2]);
                callback[0] = ((ContainerBootstrap.Callback) EasyMock.getCurrentArguments()[6]);

                return container;
            }
        });

        bootstrap.initializeContainer(container, services);
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                callback[0].containerInitialized();
                return null;
            }
        });

        final Work<ComponentContainer> loading = loading(boundary);

        // first access goes through the above interaction
        assert verify(loading) == container;

        // second access should simply return the cached container
        assert verify(loading) == container;
    }

    @Test
    public void populatesStandaloneContainer() throws Exception {
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put("key1", "value1");
        properties.put("key2", "value2");

        final ContainerBoundary boundary = boundary(null);

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
                                                    EasyMock.<MutableContainer>same(null),
                                                    EasyMock.same(getClass().getClassLoader()),
                                                    EasyMock.<PlatformContainer>isNull(),
                                                    EasyMock.<ContainerBootstrap.Callback>notNull())).andAnswer(new IAnswer<MutableContainer>() {
            public MutableContainer answer() throws Throwable {

                // check that the properties received by bootstrap is contains exactly what we set up above
                assert properties.equals(EasyMock.getCurrentArguments()[2]);
                callback[0] = ((ContainerBootstrap.Callback) EasyMock.getCurrentArguments()[6]);

                return container;
            }
        });

        bootstrap.initializeContainer(container, services);
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                callback[0].containerInitialized();
                return null;
            }
        });


        final Work<ComponentContainer> loading = loading(boundary);

        // first access goes through the above interaction
        assert verify(loading) == container;

        // second access should simply return the cached container
        assert verify(loading) == container;
    }

    @Test
    public void populatesConnectedContainer() throws Exception {
        final ContainerBoundary boundary = boundary(null);

        final Map<ClassLoader, MutableContainer> containers = new HashMap<ClassLoader, MutableContainer>();

        // find all class loaders on the ancestry
        final List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
        final ClassLoader ourClassLoader = getClass().getClassLoader();
        assert ourClassLoader != null;

        for (ClassLoader cl = ourClassLoader; cl != null; cl = cl.getParent()) {
            classLoaders.add(cl);
            containers.put(cl, localMock(MutableContainer.class));    // these local mocks ensure no method is invoked on intermediate containers
        }

        // find the top level class loader
        final ClassLoader classLoader = classLoaders.get(classLoaders.size() - 1);


        // make subject receive its dependencies from the top-level class loader
        setupDependencies(classLoader, true);

        // go through the whole class loader ancestry
        for (final ListIterator<ClassLoader> i = classLoaders.listIterator(classLoaders.size()); i.hasPrevious();) {
            final ClassLoader cl = i.previous();
            final MutableContainer container = containers.get(cl);

            final ContainerBootstrap.Callback callback[] = new ContainerBootstrap.Callback[1];

            // make subject receive a container (the same) at each level
            EasyMock.expect(bootstrap.populateContainer(EasyMock.same(services),
                                                        EasyMock.same(provider),
                                                        EasyMock.<Properties>notNull(),
                                                        EasyMock.same(containers.get(cl.getParent())),
                                                        EasyMock.same(cl),
                                                        EasyMock.<PlatformContainer>isNull(),
                                                        EasyMock.<ContainerBootstrap.Callback>notNull())).andAnswer(new IAnswer<MutableContainer>() {
                public MutableContainer answer() throws Throwable {
                    callback[0] = ((ContainerBootstrap.Callback) EasyMock.getCurrentArguments()[6]);
                    return container;
                }
            });

            // the container must also be initialized at some point
            bootstrap.initializeContainer(container, services);
            EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
                public Object answer() throws Throwable {
                    callback[0].containerInitialized();
                    return null;
                }
            });
        }

        final MutableContainer local = containers.get(ourClassLoader);
        assert local != null;

        final Work<ComponentContainer> loading = loading(boundary);

        // first access goes through the above interaction
        assert verify(loading) == local;

        // second access should simply return the cached container
        assert verify(loading) == local;

        if (ourClassLoader.getParent() != null) {

            // access to higher level container should simply return the cached container
            final ComponentContainer created = verify(loading(new ContainerBoundary(ourClassLoader.getParent())));

            assert created == containers.get(ourClassLoader.getParent());
        }
    }

    @Test
    public void testCreatesEmptyContainer() throws Exception {
        final ClassLoader classLoader = getClass().getClassLoader();
        final ContainerBoundary boundary = boundary(classLoader);

        test(new Task() {
            public void run() throws Exception {
                EasyMock.expect(providers.<ContainerProvider>findInstance(ContainerProvider.class, classLoader)).andReturn(provider);
                EasyMock.expect(providers.<ContainerServicesFactory>findInstance(ContainerServicesFactory.class, classLoader)).andReturn(servicesFactory);
                EasyMock.expect(providers.<LogFactory>findInstance(LogFactory.class, classLoader)).andReturn(logs);
                EasyMock.expect(servicesFactory.containerServices(logs)).andReturn(services);

                final MutableContainer local = localMock(MutableContainer.class);

                EasyMock.expect(provider.newContainer(services, null)).andReturn(local);

                final MutableContainer created = verify(new Work<MutableContainer>() {
                    public MutableContainer run() throws Exception {
                        return boundary.create();
                    }
                });

                assert  created == local;
            }
        });

        test(new Task() {
            public void run() throws Exception {
                final MutableContainer local = localMock(MutableContainer.class);

                EasyMock.expect(provider.newContainer(services, null)).andReturn(local);

                final MutableContainer created = verify(new Work<MutableContainer>() {
                    public MutableContainer run() throws Exception {
                        return boundary.create();
                    }
                });

                assert created == local;
            }
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void bindsBootComponentBeforePopulationButRefusesAfter() throws Exception {
        final ClassLoader classLoader = root();
        final ContainerBoundary boundary = boundary(classLoader);

        test(new Task() {
            public void run() throws Exception {

                // make subject receive its dependencies from the top-level class loader
                setupDependencies(classLoader, true);

                final ContainerBootstrap.Callback callback[] = new ContainerBootstrap.Callback[1];

                // give subject a container for that class loader
                EasyMock.expect(bootstrap.populateContainer(EasyMock.same(services),
                                                            EasyMock.same(provider),
                                                            EasyMock.<Properties>notNull(),
                                                            EasyMock.<MutableContainer>same(null),
                                                            EasyMock.same(classLoader),
                                                            EasyMock.<PlatformContainer>isNull(),
                                                            EasyMock.<ContainerBootstrap.Callback>notNull())).andAnswer(new IAnswer<MutableContainer>() {
                    public MutableContainer answer() throws Throwable {
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
                        callback[0].containerInitialized();
                        return null;
                    }
                });

                // first access goes through the above interaction
                final ComponentContainer created = verify(new Work<ComponentContainer>() {
                    public ComponentContainer run() throws Exception {
                        boundary.bindBootComponent(component1);
                        boundary.bindBootComponent(component2);

                        return boundary.loadedContainer();
                    }
                });

                assert created == container;
            }
        });

        verify(new Task() {
            public void run() throws Exception {
                try {
                    boundary.bindBootComponent(new BootComponent3());
                } catch (final IllegalStateException e) {
                    // ignore
                }
            }
        });
    }

    private Work<ComponentContainer> loading(final ContainerBoundary boundary) {
        return new Work<ComponentContainer>() {
            public ComponentContainer run() throws Exception {
                return boundary.loadedContainer();
            }
        };
    }

    private ClassLoader root() {
        ClassLoader classLoader;

        for (classLoader = getClass().getClassLoader(); classLoader.getParent() != null; classLoader = classLoader.getParent()) {
            // empty
        }

        return classLoader;
    }

    private static class BootComponent1 { }

    private static class BootComponent2 { }

    private static class BootComponent3 { }
}
