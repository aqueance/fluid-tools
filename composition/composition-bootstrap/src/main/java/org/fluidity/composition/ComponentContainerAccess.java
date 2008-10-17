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
package org.fluidity.composition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.WeakHashMap;

import org.fluidity.foundation.Logging;
import org.fluidity.foundation.logging.StandardOutLogging;
import sun.misc.Service;
import sun.misc.ServiceConfigurationError;

/**
 * Static access to class loader specific dependency injection container. The child container - parent container
 * hierarchy matches the child class loader - parent class loader hierarchy. The root class loader to have a container
 * is the one that can find the dependencies of this class: {@link org.fluidity.composition.ContainerBootstrap} and
 * {@link org.fluidity.composition.ClassDiscovery}.
 *
 * <p/>
 *
 * This component also bootstraps the required container if it has not yet been populated. Instances of this class all
 * work against the same data structure, thereby giving classes instantiated by third parties access to the container
 * relevant for their level in the application's class loader hierarchy. Due to the bootstrap bubbling up, it is advised
 * to explicitly bootstrap each level container before all child containers are bootstrapped where binding properties
 * are specified for those properties to take effect at the given level.
 *
 * <p/>
 *
 * This class is a special case in the design since it has to be self-sufficient, depending on nothing but what's always
 * available, and it also has to be visible as it acts as the root object of an application's dependency graph. Thus it
 * has to depend on concrete classes, thus somewhat breaking the Dependency Inversion Principle. After due
 * consideration, it has been decided for this class to depend on functionality provided by the Sun JDK.
 *
 * @author Tibor Varga
 */
public final class ComponentContainerAccess implements ComponentContainer {

    private static final Map<ClassLoader, OpenComponentContainer> containerMap =
        new WeakHashMap<ClassLoader, OpenComponentContainer>();

    private static final Map<ClassLoader, Map> propertiesMap = new HashMap<ClassLoader, Map>();

    /*
     * The component that can bootstrap the container for us.
     */
    private ContainerBootstrap containerBootstrap;

    /*
     * The component that can discover classes for us.
     */
    private ClassDiscovery classDiscovery;

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
     * @param services is mock object.
     */
    /* package */ void reset(final BootstrapServices services) {
        ComponentContainerAccess.containerMap.clear();
        ComponentContainerAccess.propertiesMap.clear();
        this.services = services;
        this.rootClassLoader = null;
        this.containerBootstrap = null;
        this.classDiscovery = null;
    }

    /**
     * Adds a property to a collection that will be passed to the bindings if they have a constructor that receives a
     * <code>java.utils.Map</code> object.
     *
     * @param key   is the key of the property.
     * @param value is the value of the property.
     */
    @SuppressWarnings({ "unchecked" })
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
    public synchronized ComponentContainer getContainer() {
        if (!containerMap.containsKey(classLoader)) {
            makeContainer(classLoader);
        }

        ClassLoader cl = classLoader;
        OpenComponentContainer container = containerMap.get(cl);
        while (container == null && (cl = cl.getParent()) != null) {
            container = containerMap.get(cl);
        }

        return container;
    }

    private void makeContainer(final ClassLoader classLoader) {
        if (services == null) {
            services = new BootstrapServicesImpl();
        }

        final List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();

        for (ClassLoader cl = classLoader; cl != rootClassLoader; cl = cl.getParent()) {
            classLoaders.add(cl);
        }

        for (ListIterator<ClassLoader> i = classLoaders.listIterator(classLoaders.size()); i.hasPrevious();) {
            final ClassLoader cl = i.previous();

            OpenComponentContainer ct = containerMap.get(cl);

            if (ct == null) {
                if (containerBootstrap == null) {
                    containerBootstrap = services.findInstance(ContainerBootstrap.class, cl);
                }

                if (classDiscovery == null) {
                    classDiscovery = services.findInstance(ClassDiscovery.class, cl);
                }

                if (containerBootstrap != null && classDiscovery != null) {
                    if (rootClassLoader == null) {
                        rootClassLoader = cl;
                    }

                    final Map map = propertiesMap.get(cl);
                    ct = containerBootstrap
                        .populateContainer(classDiscovery, map == null ? new HashMap() : map,
                            containerMap.get(cl.getParent()), cl);
                    containerMap.put(cl, ct);
                } else {
                    containerMap.put(cl, null);
                }
            }
        }

        assert containerMap.containsKey(classLoader);
    }

    /**
     * Delegates to the enclosed container.
     *
     * @see ComponentContainer#getComponent(Class)
     */
    public <T> T getComponent(final Class<T> componentClass) {
        return getContainer().getComponent(componentClass);
    }

    public <T> T getComponent(final Class<T> componentClass, final Bindings bindings) {
        return getContainer().getComponent(componentClass, bindings);
    }

    /**
     * Delegates to the enclosed container.
     *
     * @see OpenComponentContainer#makeNestedContainer()
     */
    public OpenComponentContainer makeNestedContainer() {
        return getContainer().makeNestedContainer();
    }

    private static class BootstrapServicesImpl implements BootstrapServices {

        private final Logging log = new StandardOutLogging(null);

        @SuppressWarnings({ "unchecked" })
        public <T> T findInstance(final Class<? super T> interfaceClass, final ClassLoader classLoader) {
            for (final Iterator i = Service.providers(interfaceClass, classLoader); i.hasNext(); ) {
                try {
                    return (T) i.next();
                } catch (final ServiceConfigurationError e) {
                    log.warning(getClass(),
                        "Finding service providers for " + interfaceClass + " using " + classLoader, e);
                }
            }

            return null;
        }
    }
}
