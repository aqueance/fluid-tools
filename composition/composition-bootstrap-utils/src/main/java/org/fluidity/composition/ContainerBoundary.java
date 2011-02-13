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
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.fluidity.composition.spi.ContainerProvider;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Static access to class loader specific dependency injection container. This utility class ensures that the child container - parent container hierarchy
 * matches the child class loader - parent class loader hierarchy. The root class loader to have a container is the one that can find the dependencies of this
 * class: {@link ContainerBootstrap}, {@link ContainerProvider} and {@link ContainerServicesFactory}.
 * <p/>
 * This class bootstraps containers that have not yet been populated. Instances of this class all work against the same data structure, thereby giving classes
 * instantiated by third parties access to the container relevant to their level in the application's class loader hierarchy. Bootstrapping a container
 * hierarchy is performed up in the hierarchy from the requested container and if a higher level container is attempted to add bootstrap bindings or properties
 * to when it has already been bootstrapped due to earlier access to one of its child containers, the bootstrap binding or property registration operation will
 * fail. In such a case it is advised to explicitly bootstrap the higher level container before the lower level container is bootstrapped.
 * <p/>
 * This class is a special case in the design since it has to be self-sufficient, depending on nothing that's not always available, and it also has to be
 * visible as it acts as the root object of an application's dependency graph. Thus it has to depend on concrete classes.
 * <p/>
 * Access to instances of this class is thread-safe.
 *
 * @author Tibor Varga
 */
public final class ContainerBoundary implements ComponentContainer {

    private static final Map<ClassLoader, OpenComponentContainer> populatedContainers = new WeakHashMap<ClassLoader, OpenComponentContainer>();
    private static final Map<ClassLoader, Map> propertiesMap = new HashMap<ClassLoader, Map>();
    private static final Set<OpenComponentContainer> lockedContainers = new HashSet<OpenComponentContainer>();
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

    public ContainerBoundary() {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        this.classLoader = cl == null ? getClass().getClassLoader() : cl;
    }

    public ContainerBoundary(final ClassLoader classLoader) {
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
     * Allows a bootstrap code to add component instances to the container before it bootstraps. This method can only be invoked before any component is taken
     * out of the container by any thread using any of the {@link #getComponent(Class)}, {@link #getComponent(Class, ComponentContainer.Bindings)}, {@link
     * #initialize(Object)} or {@link #makeChildContainer()} methods. Once that happens, this method will throw an {@link IllegalStateException}.
     * <p/>
     * Calling this method will trigger population of the associated container and its parents.
     *
     * @param key      the key by which to register the component; preferably an interface class.
     * @param instance the component instance.
     *
     * @throws IllegalStateException if the container is made read only by getting any component out of it.
     */
    @SuppressWarnings("unchecked")
    public <T> void bindBootComponent(final Class<? super T> key, final T instance) {
        loadContainer(false).getRegistry().bindInstance(instance, key);
    }

    /**
     * Delegates to the enclosed container.
     * <p/>
     * {@inheritDoc}
     */
    public <T> T getComponent(final Class<T> api) {
        return loadContainer(true).getComponent(api);
    }

    /**
     * Delegates to the enclosed container.
     * <p/>
     * {@inheritDoc}
     */
    public <T> T getComponent(final Class<T> api, final Bindings bindings) {
        return loadContainer(true).getComponent(api, bindings);
    }

    /**
     * Delegates to the enclosed container.
     * <p/>
     * {@inheritDoc}
     */
    public OpenComponentContainer makeChildContainer() {
        return loadContainer(true).makeChildContainer();
    }

    /**
     * Delegates to the enclosed container.
     * <p/>
     * {@inheritDoc}
     */
    public <T> T initialize(final T component) {
        return loadContainer(true).initialize(component);
    }

    public <T> T[] getComponentGroup(final Class<T> api) {
        return loadContainer(true).getComponentGroup(api);
    }

    /**
     * This is for the unit test cases to override our single dependency.
     *
     * @param services is a mock object.
     */
    /* package */ void reset(final BootstrapServices services) {
        this.services = services;

        ContainerBoundary.populatedContainers.clear();
        ContainerBoundary.propertiesMap.clear();
        ContainerBoundary.lockedContainers.clear();

        this.rootClassLoader = null;
        this.containerBootstrap = null;
        this.containerProvider = null;
    }

    /**
     * Makes the container for the nearest class loader available to the caller. Used for testing.
     *
     * @return the container for the nearest class loader.
     */
    /* package */ ComponentContainer getContainer() {
        return loadContainer(true);
    }

    /**
     * Returns list of populated containers at and above the current class loader.
     *
     * @return list of populated containers at and above the current class loader.
     */
    private List<OpenComponentContainer> makeContainer() {
        if (services == null) {
            services = new BootstrapServicesImpl();
        }

        // list of class loaders from current one up the hierarchy
        final List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();

        for (ClassLoader loader = classLoader; loader != rootClassLoader; loader = loader.getParent()) {
            classLoaders.add(loader);
        }

        // going in reverse order because the container for a given class loader has to use as its parent the container for the parent class loader
        for (final ListIterator<ClassLoader> i = classLoaders.listIterator(classLoaders.size()); i.hasPrevious();) {
            final ClassLoader loader = i.previous();

            if (!populatedContainers.containsKey(loader)) {
                if (containerBootstrap == null) {
                    containerBootstrap = services.findInstance(ContainerBootstrap.class, loader);
                }

                if (containerProvider == null) {
                    containerProvider = services.findInstance(ContainerProvider.class, loader);
                }

                if (containerBootstrap != null && containerProvider != null) {
                    if (rootClassLoader == null) {
                        rootClassLoader = loader;
                    }

                    if (containerServices == null) {
                        final ContainerServicesFactory factory = services.findInstance(ContainerServicesFactory.class, loader);
                        assert factory != null : ContainerServicesFactory.class;

                        containerServices = factory.containerServices(services.findInstance(LogFactory.class, loader));
                        assert containerServices != null : ContainerServicesFactory.class;
                    }

                    final Map map = propertiesMap.get(loader);
                    final OpenComponentContainer container = containerBootstrap.populateContainer(containerServices,
                                                                                                  containerProvider,
                                                                                                  map == null ? new HashMap() : map,
                                                                                                  populatedContainers.get(loader.getParent()),
                                                                                                  loader);
                    populatedContainers.put(loader, container);
                } else {
                    populatedContainers.put(loader, null);
                }
            }
        }

        assert populatedContainers.containsKey(classLoader);

        // list of containers at and above current class loader
        final List<OpenComponentContainer> containers = new ArrayList<OpenComponentContainer>();

        for (ClassLoader loader = classLoader; loader != null; loader = loader.getParent()) {
            final OpenComponentContainer container = populatedContainers.get(loader);

            if (container != null) {
                containers.add(container);
            }
        }

        return containers;
    }

    /**
     * A container can be updated any time until it is locked once. After it has been locked, it can only be queried. When a container is loaded, all containers
     * up the class loader chain are also loaded. If one is locked, all above it are also locked.
     *
     * @param lock whether to lock the container. If it has not yet been locked and this parameter is <code>true</code>, the container is initialized and
     *             locked. If the value is <code>false</code> and the container has already been locked, an {@link IllegalStateException} is thrown.
     *
     * @return the loaded and populated container. If lock is <code>true</code>, the container is also initialized.
     */
    private OpenComponentContainer loadContainer(final boolean lock) {
        synchronized (stateLock) {
            final List<OpenComponentContainer> containers = makeContainer();

            boolean first = true;
            for (final OpenComponentContainer container : containers) {
                if (lock) {
                    if (!lockedContainers.contains(container)) {
                        lockedContainers.add(container);
                        containerBootstrap.initializeContainer(container, containerServices);
                    }
                } else if (first && lockedContainers.contains(container)) {
                    throw new IllegalStateException("Component container is locked.");
                }

                first = false;
            }

            return containers.isEmpty() ? null : containers.get(0);
        }
    }
}
