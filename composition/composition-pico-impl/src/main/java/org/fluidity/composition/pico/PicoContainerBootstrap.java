/*
 * Copyright (c) 2006-2009 Tibor Adam Varga (tibor.adam.varga on gmail)
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
 */
package org.fluidity.composition.pico;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.fluidity.composition.ClassDiscovery;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContainerBootstrap;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.composition.PackageBindings;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.composition.ShutdownHook;
import org.fluidity.foundation.ClassLoaderUtils;
import org.fluidity.foundation.Logging;
import org.fluidity.foundation.logging.BootstrapLog;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.defaults.CachingComponentAdapterFactory;
import org.picocontainer.defaults.ConstructorInjectionComponentAdapterFactory;
import org.picocontainer.defaults.DefaultPicoContainer;

/**
 * Bootstraps the component container. This class is exported via the standard service provider discovery mechanism described in the JAR file specification (for
 * dummies: the fully qualitified name of this class can be found in a file whose name is the fully qualified class name of the implemented interface).
 *
 * <p/>
 *
 * This class is public so that sun.misc.Services can find it.
 *
 * @author Tibor Varga
 */
@ServiceProvider
public final class PicoContainerBootstrap implements ContainerBootstrap {

    private final Logging log = new BootstrapLog("container");

    @SuppressWarnings({ "unchecked" })
    public OpenComponentContainer populateContainer(final ClassDiscovery discovery,
                                                    final Map properties,
                                                    final OpenComponentContainer parent,
                                                    final ClassLoader classLoader) {
        final OpenComponentContainer container = parent == null ? new PicoComponentContainer() : parent.makeNestedContainer();

        log.info(getClass(), "Created new " + container + (classLoader == null ? "" : " for " + classLoader));

        final ComponentContainer.Registry registry = container.getRegistry();

        /*
         * Find instances of classes implementing the PackageBindings interface.
         */
        final Collection<Class> assemblySet = new HashSet<Class>(Arrays.asList(discovery.findComponentClasses(
                PackageBindings.class,
                classLoader,
                parent != null)));

        log.info(getClass(), "Found " + assemblySet.size() + " package(s).");

        final MutablePicoContainer pico =
                new DefaultPicoContainer(new CachingComponentAdapterFactory(new ConstructorInjectionComponentAdapterFactory(
                        true)));

        if (properties != null) {
            pico.registerComponentInstance(Map.class, properties);
        }

        /*
         * Add each to a modified PicoContainer
         */
        for (final Class componentClass : assemblySet) {
            pico.registerComponentImplementation(componentClass, componentClass);
        }

        /*
         * Get the instances in instantiation order
         */
        final List<? extends PackageBindings> assemblies =
                (List<? extends PackageBindings>) pico.getComponentInstancesOfType(PackageBindings.class);
        assert assemblies != null;

        /*
         * Process each package component set.
         */
        for (final PackageBindings bindings : assemblies) {
            try {
                log.info(getClass(), id() + ": processing " + bindings.getClass().getName());
                bindings.bindComponents(registry);
            } catch (final RuntimeException e) {
                throw e;
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        registry.requireDependency(ShutdownHook.class, PicoContainerBootstrap.class);

        /*
         * Perform post-registration initialisation.
         */
        for (final PackageBindings bindings : assemblies) {
            try {
                bindings.initializeComponents(container);
            } catch (final RuntimeException e) {
                throw e;
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        final Map<Class, List<Class>> unresolved = container.getUnresolvedDependencies();

        if (!unresolved.isEmpty()) {
            throw new MissingDependenciesException(unresolved);
        }

        final ShutdownHook shutdown = container.getComponent(ShutdownHook.class);
        assert shutdown != null;

        shutdown.addTask("container-shutdown", new Runnable() {
            public void run() {

                /*
                 * Perform pre-shutdown tasks.
                 */
                for (final ListIterator i = assemblies.listIterator(assemblies.size()); i.hasPrevious();) {
                    try {
                        ((PackageBindings) i.previous()).shutdownComponents(container);
                    } catch (final RuntimeException e) {
                        throw e;
                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        return container;
    }

    private String id() {
        final String className = getClass().getName();
        return className.substring(className.lastIndexOf(".") + 1);
    }
}

// TODO: test this

final class MissingDependenciesException extends RuntimeException {

    private final Map<Class, List<Class>> dependencies;

    public MissingDependenciesException(Map<Class, List<Class>> dependencies) {
        this.dependencies = dependencies;
    }

    public String getMessage() {
        final StringWriter message = new StringWriter();
        final PrintWriter pw = new PrintWriter(message);

        pw.println("The following dependencies could not be resolved:");

        for (final Map.Entry<Class, List<Class>> entry : dependencies.entrySet()) {
            final Class key = entry.getKey();
            final List<Class> value = entry.getValue();

            pw.println(" " + key + " required by: ");

            final Set<String> classes = new TreeSet<String>();

            for (final Class component : value) {
                final URL resource = ClassLoaderUtils.findClassResource(component);

                String moduleName = resource.getPath();
                final String className = ClassLoaderUtils.classResourceName(component);

                if (moduleName.endsWith(className)) {
                    moduleName = moduleName.substring(0, moduleName.length() - className.length());

                    if (moduleName.endsWith("!")) {
                        moduleName = moduleName.substring(0, moduleName.length() - 1);
                    }
                }

                classes.add(component + " in module " + moduleName);
            }

            pw.println("  " + classes);
        }

        return message.toString();
    }
}
