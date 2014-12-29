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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.container.ContainerServices;
import org.fluidity.composition.container.internal.ContainerServicesFactory;
import org.fluidity.composition.container.spi.ContainerProvider;
import org.fluidity.composition.spi.ComponentInterceptor;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Command.Function;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Security;
import org.fluidity.foundation.spi.LogFactory;

import static org.fluidity.foundation.Command.Process;

/**
 * External access to a class loader specific <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Concept">dependency
 * injection</a> <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Containers">container</a>. This utility class ensures that
 * the container hierarchy of the host application matches the class loader hierarchy. The highest level class loader to have a container is the one that can
 * find the single dependency of this class: {@link ContainerProvider}.
 * <p/>
 * When instantiated, this class bootstraps all parent containers that have not yet been populated. Instances of this class all work against the same data
 * structure, thereby giving classes instantiated by third parties access to the container relevant to their level in the application's class loader hierarchy.
 * <p/>
 * Bootstrapping a container hierarchy is performed from the requested container up. Adding bootstrap bindings to a container may fail in cases when the
 * container has already been populated as the ancestor of a lower level container. In such a case it is advised to explicitly bootstrap the higher level
 * container before the lower level container is bootstrapped.
 * <p/>
 * Access to the shared data structure through instances of this class is thread safe.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain Component @Component}(automatic = false)
 * public final class <span class="hl2">Main</span> {
 *
 *   public static void main(final String[] arguments) throws Exception {
 *     <span class="hl1">{@linkplain Containers}.{@linkplain Containers#global() global}</span>().{@linkplain ComponentContainer#instantiate(Class, ComponentContainer.Bindings...) instantiate}(<span class="hl2">Main</span>.class).<span class="hl3">run</span>(arguments);
 *   }
 *
 *   private void <span class="hl3">run</span>(final String[] parameters) throws Exception {
 *     &hellip;
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
public final class ContainerBoundary implements ComponentContainer {

    private static final Map<ClassLoader, MutableContainer> populatedContainers = new WeakHashMap<ClassLoader, MutableContainer>();
    private static final Map<ClassLoader, Map> propertiesMap = new WeakHashMap<ClassLoader, Map>();
    private static final Set<ComponentContainer> lockedContainers = new HashSet<ComponentContainer>();
    private static final Object stateLock = new Object();

    /**
     * The component that can discover the above dependencies for us. The point is to have one single dependency for unit tests to override. This class is not
     * dependency injected and so it is important to keep its dependencies down to a minimum.
     */
    private BootstrapServices services;

    /**
     * The component that can bootstrap a container.
     */
    private ContainerBootstrap containerBootstrap;

    /**
     * The component that can provide container implementations.
     */
    private ContainerProvider containerProvider;

    /**
     * Component that collects common container services.
     */
    private ContainerServices containerServices;

    /**
     * The root class loader the parent of which will not have a container created
     */
    private ClassLoader rootClassLoader;

    /**
     * The class loader associated with the container made accessible by this instance.
     */
    private final ClassLoader classLoader;

    /**
     * Creates a container boundary for the current class loader.
     */
    /* package */ ContainerBoundary() {
        final PrivilegedAction<ClassLoader> action = new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                return cl != null ? cl : ContainerBoundary.class.getClassLoader();
            }
        };

        this.classLoader = Security.CONTROLLED ? AccessController.doPrivileged(action) : action.run();
    }

    /**
     * Creates a container boundary to gain access to the container associated with the given class loader.
     *
     * @param classLoader the class loader.
     */
    /* package */ ContainerBoundary(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Adds a property to a collection that will be passed to any {@link org.fluidity.composition.spi.PackageBindings} visible by the current class loader that
     * has a constructor with a {@link Map} parameter.
     *
     * @param key   is the property key.
     * @param value is the property value.
     */
    @SuppressWarnings("unchecked")
    public void setBindingProperty(final Object key, final Object value) {

        // never intended to be modified concurrently as this method is by nature should be used by one single object in one single thread
        // but you never know and it's better to ensure data consistency than to blame the user for the corrupt results stemming from poor design
        synchronized (stateLock) {
            Map map = propertiesMap.get(classLoader);

            if (map == null) {
                propertiesMap.put(classLoader, map = new HashMap());
            }

            map.put(key, value);
        }
    }

    /**
     * Allows a bootstrap code to add component instances to the container before it is populated. This method can only be invoked before any component is
     * taken out of the container by any thread using any of the {@link #initialize(Object)}, {@link ComponentContainer#instantiate(Class,
     * ComponentContainer.Bindings...)}, {@link #makeChildContainer(Bindings...)} or {@link ComponentContainer#makeChildContainer(Bindings...)} methods. Once
     * that happens, this method will throw an {@link IllegalStateException}.
     * <p/>
     * Calling this method will trigger population of the associated container and its parents but will not prevent further invocations of this method to add
     * more bindings to the container.
     *
     * @param instance the component instance.
     * @param api      optional list of interfaces to bind the object against.
     *
     * @return the supplied component instance.
     *
     * @throws IllegalStateException if the container is made read only by getting any component out of it.
     */
    @SuppressWarnings("unchecked")
    public <T> T bindBootComponent(final T instance, final Class<? super T>... api) {
        loadContainer(false).getRegistry().bindInstance(instance, api);
        return instance;
    }

    /**
     * Delegates to the enclosed container.
     * <p/>
     * {@inheritDoc}
     */
    public OpenContainer makeChildContainer(final Bindings... bindings) {
        return loadedContainer().makeChildContainer(bindings);
    }

    /**
     * Delegates to the enclosed container.
     * <p/>
     * {@inheritDoc}
     */
    public OpenContainer makeDomainContainer(final Bindings... bindings) {
        return loadedContainer().makeDomainContainer(bindings);
    }

    /**
     * Delegates to the enclosed container.
     * <p/>
     * {@inheritDoc}
     */
    public ComponentContainer intercepting(final ComponentInterceptor... interceptors) {
        return loadedContainer().intercepting(interceptors);
    }

    /**
     * Delegates to the enclosed container.
     * <p/>
     * {@inheritDoc}
     */
    public ObservedContainer observed(final Observer observer) {
        return loadedContainer().observed(observer);
    }

    /**
     * Delegates to the enclosed container.
     * <p/>
     * {@inheritDoc}
     */
    public <T> T initialize(final T component) {
        return loadedContainer().initialize(component);
    }

    /**
     * Initializes all of the given component instances.
     *
     * @param components the list of components to initialize.
     */
    public void initialize(final Object... components) {
        final ComponentContainer container = loadedContainer();

        for (final Object component : components) {
            container.initialize(component);
        }
    }

    /**
     * Delegates to the enclosed container.
     * <p/>
     * {@inheritDoc}
     */
    public Object invoke(final Object component, final Method method, final Object... arguments) throws ResolutionException, InvocationTargetException {
        return loadedContainer().invoke(component, method, arguments);
    }

    /**
     * Delegates to the enclosed container.
     * <p/>
     * {@inheritDoc}
     */
    public <T> T complete(final T component, final Class<? super T>... api) throws ResolutionException {
        return loadedContainer().complete(component, api);
    }

    /**
     * Delegates to the enclosed container.
     * <p/>
     * {@inheritDoc}
     */
    public <T> T instantiate(final Class<T> componentClass, final Bindings... bindings) throws ResolutionException {
        return loadedContainer().instantiate(componentClass, bindings);
    }

    /**
     * This is for the unit test cases to override our single dependency.
     *
     * @param services is a mock object.
     */
    /* package */ void reset(final BootstrapServices services) {
        this.services = services;

        this.rootClassLoader = null;
        this.containerBootstrap = null;
        this.containerProvider = null;

        ContainerBoundary.populatedContainers.clear();
        ContainerBoundary.propertiesMap.clear();
        ContainerBoundary.lockedContainers.clear();
    }

    /**
     * Makes the container for the nearest class loader available to the caller.
     *
     * @return the container for the nearest class loader.
     */
    /* package */ ComponentContainer loadedContainer() {
        return loadContainer(true);
    }

    /**
     * Returns list of populated containers at and above the current class loader.
     *
     * @return list of populated containers at and above the current class loader.
     */
    private List<MutableContainer> makeContainer() {
        initFinder();


        // list of class loaders from current one up the hierarchy
        final List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();

        final PrivilegedAction<Void> ancestry = new PrivilegedAction<Void>() {
            public Void run() {
                for (ClassLoader loader = classLoader; loader != rootClassLoader; loader = loader.getParent()) {
                    classLoaders.add(loader);
                }

                return null;
            }
        };

        if (Security.CONTROLLED) {
            AccessController.doPrivileged(ancestry);
        } else {
            ancestry.run();
        }

        // top down: going in reverse order because the container for a given class loader has to use as its parent the container for the parent class loader
        for (final ListIterator<ClassLoader> i = classLoaders.listIterator(classLoaders.size()); i.hasPrevious(); ) {
            final ClassLoader loader = i.previous();

            if (!populatedContainers.containsKey(loader)) {
                findBootstrap(loader);
                findProvider(loader);

                if (containerBootstrap != null && containerProvider != null) {
                    if (rootClassLoader == null) {
                        rootClassLoader = loader;
                    }

                    final Map map = propertiesMap.get(loader);
                    final MutableContainer parent = populatedContainers.get(!Security.CONTROLLED ? loader.getParent() : AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                        public ClassLoader run() {
                            return loader.getParent();
                        }
                    }));

                    final AtomicReference<MutableContainer> container = new AtomicReference<MutableContainer>();

                    final ContainerBootstrap.Callback callback = new ContainerBootstrap.Callback() {
                        public void containerInitialized() {
                            synchronized (stateLock) {
                                lockedContainers.add(container.get());
                            }
                        }

                        public void containerShutdown() {
                            synchronized (stateLock) {
                                populatedContainers.remove(loader);
                                propertiesMap.remove(loader);
                                lockedContainers.remove(container.get());
                            }
                        }
                    };

                    final PrivilegedAction<MutableContainer> populate = new PrivilegedAction<MutableContainer>() {
                        public MutableContainer run() {
                            return ClassLoaders.context(loader, new Function<MutableContainer, ClassLoader, RuntimeException>() {
                                public MutableContainer run(final ClassLoader loader) {
                                    return containerBootstrap.populateContainer(findServices(loader),
                                                                                containerProvider,
                                                                                map == null ? new HashMap() : map,
                                                                                parent, loader,
                                                                                callback);
                                }
                            });
                        }
                    };

                    container.set(Security.CONTROLLED ? AccessController.doPrivileged(populate) : populate.run());

                    populatedContainers.put(loader, container.get());
                } else {
                    populatedContainers.put(loader, null);
                }
            }
        }

        assert populatedContainers.containsKey(classLoader) : classLoader;

        final PrivilegedAction<List<MutableContainer>> list = new PrivilegedAction<List<MutableContainer>>() {
            public List<MutableContainer> run() {

                // bottom up: list of containers at and above current class loader
                final List<MutableContainer> containers = new ArrayList<MutableContainer>();

                for (ClassLoader loader = classLoader; loader != null; loader = loader.getParent()) {
                    final MutableContainer container = populatedContainers.get(loader);

                    if (container != null) {
                        containers.add(container);
                    }
                }

                return containers;
            }
        };

        return Security.CONTROLLED ? AccessController.doPrivileged(list) : list.run();
    }

    private BootstrapServices initFinder() {
        return services == null ? services = Singleton.BOOTSTRAP_SERVICES : services;
    }

    private ContainerProvider findProvider(final ClassLoader loader) {
        return containerProvider == null ? containerProvider = services.findInstance(ContainerProvider.class, loader) : containerProvider;
    }

    private ContainerBootstrap findBootstrap(final ClassLoader loader) {
        return containerBootstrap == null ? containerBootstrap = services.findInstance(ContainerBootstrap.class, loader) : containerBootstrap;
    }

    private ContainerServices findServices(final ClassLoader loader) {
        if (containerServices == null) {
            final ContainerServicesFactory factory = services.findInstance(ContainerServicesFactory.class, loader);
            assert factory != null : ContainerServicesFactory.class;

            containerServices = factory.containerServices(services.findInstance(LogFactory.class, loader));
            assert containerServices != null : ContainerServicesFactory.class;
        }

        return containerServices;
    }

    /**
     * A container can be updated any time until it is locked once. After it has been locked, it can only be queried. When a container is loaded, all
     * containers up the class loader chain are also loaded. If one is locked, all above it are also locked.
     *
     * @param lock whether to lock the container. If it has not yet been locked and this parameter is <code>true</code>, the container is initialized and
     *             locked. If the value is <code>false</code> and the container has already been locked, an {@link IllegalStateException} is thrown.
     *
     * @return the loaded and populated container. If lock is <code>true</code>, the container is also initialized.
     */
    private MutableContainer loadContainer(final boolean lock) {
        synchronized (stateLock) {
            final List<MutableContainer> containers = makeContainer();

            boolean first = true;
            ComponentContainer initialized = null;

            for (final ComponentContainer container : containers) {
                if (lock) {
                    if (initialized == null && !lockedContainers.contains(container)) {
                        initialized = container;
                    }
                } else if (first && lockedContainers.contains(container)) {
                    throw new IllegalStateException("Component container is locked.");
                }

                first = false;
            }

            if (initialized != null) {

                // initialize containers top down
                for (final ListIterator<MutableContainer> iterator = containers.listIterator(containers.size()); iterator.hasPrevious(); ) {
                    final MutableContainer container = iterator.previous();

                    Exceptions.wrap(new Process<Void, Exception>() {
                        public Void run() throws Exception {
                            containerBootstrap.initializeContainer(container, containerServices);
                            return null;
                        }
                    });

                    if (container == initialized) {
                        break;
                    }
                }
            }

            if (containers.isEmpty()) {
                throw new IllegalStateException("No container found");
            } else {
                return containers.get(0);
            }
        }
    }

    /**
     * Creates an empty independent container.
     *
     * @param quiet tells whether the returned container should keep quite about what it is doing (<code>true</code>) or can be verbose (<code>false</code>).
     *
     * @return an empty independent container.
     */
    /* package */ MutableContainer create(final boolean quiet) {
        initFinder();

        if (findProvider(classLoader) != null) {
            return containerProvider.newContainer(findServices(classLoader), quiet);
        } else {
            throw new IllegalStateException(String.format("Container implementation not found; could not find service providers %s in class loader %s",
                                                          ContainerProvider.class.getName(),
                                                          classLoader));
        }
    }

    /**
     * Deferred instantiation of {@link BootstrapServicesImpl}.
     *
     * @author Tibor Varga
     */
    private static class Singleton {
        private static final BootstrapServices BOOTSTRAP_SERVICES = new BootstrapServicesImpl();
    }
}
