/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;

import org.fluidity.foundation.Logging;
import org.fluidity.foundation.logging.BootstrapLog;

import sun.misc.Service;
import sun.misc.ServiceConfigurationError;

/**
 * Static access to class loader specific dependency injection container. The child container - parent container hierarchy matches the child class loader -
 * parent class loader hierarchy. The root class loader to have a container is the one that can find the dependencies of this class: {@link
 * org.fluidity.composition.ContainerBootstrap} and {@link org.fluidity.composition.ClassDiscovery}.
 *
 * <p/>
 *
 * This component also bootstraps the required container if it has not yet been populated. Instances of this class all work against the same data structure,
 * thereby giving classes instantiated by third parties access to the container relevant for their level in the application's class loader hierarchy. Due to the
 * bootstrap bubbling up, it is advised to explicitly bootstrap each level container before all child containers are bootstrapped where binding properties are
 * specified for those properties to take effect at the given level.
 *
 * <p/>
 *
 * This class is a special case in the design since it has to be self-sufficient, depending on nothing but what's always available, and it also has to be
 * visible as it acts as the root object of an application's dependency graph. Thus it has to depend on concrete classes, thus somewhat breaking the Dependency
 * Inversion Principle. After due consideration, it has been decided for this class to depend on functionality provided by the Sun JDK.
 *
 * @author Tibor Varga
 */
public final class ComponentContainerAccess implements ComponentContainer {

    private static final Map<ClassLoader, OpenComponentContainer> populatedContainers = new WeakHashMap<ClassLoader, OpenComponentContainer>();
    private static final Set<ClassLoader> inflightContainers = new HashSet<ClassLoader>();
    private static final Map<ClassLoader, Map> propertiesMap = new HashMap<ClassLoader, Map>();
    private static final Set<OpenComponentContainer> committedContainers = new HashSet<OpenComponentContainer>();

    /*
     * The component that can bootstrap a container.
     */
    private ContainerBootstrap containerBootstrap;

    /*
     * The component that can provide container implementations.
     */
    private ContainerProvider containerProvider;

    /*
     * The component that can discover the above dependencies for us.
     * The point is to have one single dependency for unit tests to override.
     * This class is not dependency injected and so it is important to keep
     * its dependencies down to a minimum.
     */
    private BootstrapServices services;

    /**
     * The root class loader the parent of which will not have a container created
     */
    private ClassLoader rootClassLoader;
    private final ClassLoader classLoader;

    public ComponentContainerAccess() {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        this.classLoader = cl == null ? getClass().getClassLoader() : cl;
    }

    public ComponentContainerAccess(final ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * This is for the unit test cases to override our single dependency.
     *
     * @param services is a mock object.
     */
    /* package */ void reset(final BootstrapServices services) {
        ComponentContainerAccess.populatedContainers.clear();
        ComponentContainerAccess.inflightContainers.clear();
        ComponentContainerAccess.propertiesMap.clear();
        ComponentContainerAccess.committedContainers.clear();
        this.services = services;
        this.rootClassLoader = null;
        this.containerBootstrap = null;
        this.containerProvider = null;
    }

    /**
     * Adds a property to a collection that will be passed to the bindings if they have a constructor that receives a <code>java.utils.Map</code> object.
     *
     * @param key   is the key of the property.
     * @param value is the value of the property.
     */
    @SuppressWarnings({"unchecked"})
    public void setBindingsProperty(final Object key, final Object value) {
        Map map = propertiesMap.get(classLoader);

        if (map == null) {
            propertiesMap.put(classLoader, map = new HashMap());
        }

        map.put(key, value);
    }

    /**
     * Makes the container with the least scope available to the caller.
     *
     * @return the application wide container.
     */
    public ComponentContainer getContainer() {
        return getContainer(true);
    }

    private void makeContainer(final ClassLoader classLoader) {
        if (services == null) {
            services = new BootstrapServicesImpl();
        }

        final List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();

        for (ClassLoader loader = classLoader; loader != rootClassLoader; loader = loader.getParent()) {
            classLoaders.add(loader);
        }

        for (final ListIterator<ClassLoader> i = classLoaders.listIterator(classLoaders.size()); i.hasPrevious();) {
            final ClassLoader loader = i.previous();

            OpenComponentContainer container = populatedContainers.get(loader);

            if (container == null) {
                if (containerProvider == null) {
                    containerProvider = services.findInstance(ContainerProvider.class, loader);
                }

                if (containerBootstrap == null) {
                    containerBootstrap = services.findInstance(ContainerBootstrap.class, loader);
                }

                if (containerBootstrap != null && containerProvider != null) {                    
                    if (rootClassLoader == null) {
                        rootClassLoader = loader;
                    }

                    inflightContainers.add(loader);
                    try {
                        final Map map = propertiesMap.get(loader);
                        container = containerBootstrap.populateContainer(containerProvider, map == null ? new HashMap() : map, populatedContainers.get(loader.getParent()), loader);
                        populatedContainers.put(loader, container);
                    } finally {
                        inflightContainers.remove(loader);
                    }
                } else {
                    populatedContainers.put(loader, null);
                }
            }
        }

        assert populatedContainers.containsKey(classLoader);
    }

    private synchronized OpenComponentContainer getContainer(final boolean commit) {
        if (inflightContainers.contains(classLoader)) {
            throw new IllegalStateException("Container for " + classLoader + " cannot be externally accessed while it is being populated");
        } else if (!populatedContainers.containsKey(classLoader)) {
            makeContainer(classLoader);
        }

        ClassLoader loader = classLoader;
        OpenComponentContainer container = populatedContainers.get(loader);
        while (container == null && (loader = loader.getParent()) != null) {
            container = populatedContainers.get(loader);
        }

        if (container != null) {
            if (commit) {
                committedContainers.add(container);
            } else if (committedContainers.contains(container)) {
                throw new IllegalStateException("Component container is read-only.");
            }
        }

        return container;
    }

    /**
     * Delegates to the enclosed container.
     *
     * @see ComponentContainer#getComponent(Class)
     */
    public <T> T getComponent(final Class<T> componentClass) {
        return getContainer(true).getComponent(componentClass);
    }

    public <T> T getComponent(final Class<T> componentClass, final Bindings bindings) {
        return getContainer(true).getComponent(componentClass, bindings);
    }

    public ComponentContext makeContext(final Properties properties) {
        return getContainer(true).makeContext(properties);
    }

    public ComponentContext makeContext(final Map<String, String> map) {
        return getContainer(true).makeContext(map);
    }

    public <T> T getComponent(final Class<T> componentClass, final ComponentContext context) {
        return getContainer(true).getComponent(componentClass, context);
    }

    /**
     * Delegates to the enclosed container.
     *
     * @see OpenComponentContainer#makeNestedContainer()
     */
    public OpenComponentContainer makeNestedContainer() {
        return getContainer(false).makeNestedContainer();
    }

    public <T> T initialize(final T component) {
        return getContainer(true).initialize(component);
    }

    /**
     * Allows a bootstrap code to add components to the container. This method can only be invoked before any component is taken out of the container by any of
     * the {@link #getComponent(Class)} or {@link #getComponent(Class, org.fluidity.composition.ComponentContainer.Bindings)} methods. Once that happens, this
     * method will throw an <code>IllegalStateException</code>.
     *
     * <p/>
     *
     * This method will trigger population of the associated container and its parents.
     *
     * @param key      the key by which to register the component; preferably an interface class.
     * @param instance the component instance.
     *
     * @throws IllegalStateException if the container is made read only by getting any component out of it.
     */
    public <T> void bindBootComponent(final Class<? super T> key, final T instance) {
        getContainer(false).getRegistry().bindInstance(key, instance);
    }

    private static class BootstrapServicesImpl implements BootstrapServices {

        private final Logging log = new BootstrapLog("container");

        @SuppressWarnings({"unchecked"})
        public <T> T findInstance(final Class<? super T> interfaceClass, final ClassLoader classLoader) {

            // Java 6: use java.util.ServiceLoader.load(interfaceClass, classLoader).iterator()
            for (final Iterator i = Service.providers(interfaceClass, classLoader); i.hasNext();) {
                try {
                    return (T) i.next();
                } catch (final ServiceConfigurationError e) {
                    log.warning(getClass(), "Finding service providers for " + interfaceClass + " using " + classLoader, e);
                }
            }

            return null;
        }
    }
}
