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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.fluidity.foundation.ClassLoaderUtils;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.LogFactory;
import org.fluidity.foundation.logging.Log;

/**
 * The component is instantiated by {@link org.fluidity.composition.ProductionServices} and
 * picked up at container bootstrap to be made available as a component.
 *
 * @author Tibor Varga
 */
@Component(automatic = false)
final class ClassDiscoveryImpl implements ClassDiscovery {

    private final Log log;

    public ClassDiscoveryImpl(final LogFactory logs) {
        log = logs.createLog(getClass());
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T>[] findComponentClasses(final Class<T> componentInterface, final ClassLoader cl, final boolean strict) {
        final ClassLoader classLoader = cl == null ? ClassLoaderUtils.findClassLoader(componentInterface) : cl;
        log.info("Loading service provider files for %s using class loader %s", componentInterface, classLoader);

        final Collection<Class<T>> componentList = Exceptions.wrap(new Exceptions.Command<Collection<Class<T>>>() {
            public Collection<Class<T>> run() throws Exception {
                final Collection<Class<T>> componentList = new LinkedHashSet<Class<T>>();

                final Set<URL> loaded = new HashSet<URL>();

                final Enumeration<URL> resources = classLoader.getResources(ClassLoaderUtils.absoluteResourceName("META-INF/services/"
                                                                                                                  + componentInterface.getName()));

                for (final URL url : Collections.list(resources)) {

                    /* Some dumb class loaders load JAR files more than once */
                    if (!loaded.contains(url)) {
                        loaded.add(url);

                        log.info("Processing %s", url);

                        final Collection<Class<T>> localList = new LinkedHashSet<Class<T>>();
                        final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                        String line;

                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("#")) {
                                continue;
                            }

                            try {
                                final Class<?> rawClass = classLoader.loadClass(line.trim());

                                if (!strict || rawClass.getClassLoader() == classLoader) {
                                    if (componentInterface.isAssignableFrom(rawClass)) {
                                        final Class<T> componentClass = (Class<T>) rawClass;

                                        if (Modifier.isAbstract(componentClass.getModifiers())) {
                                            log.info("Ignoring abstract service provider %s", componentClass);
                                        } else {
                                            if (componentList.contains(componentClass)) {
                                                log.error("Multiple export of %s", componentClass);
                                            } else {
                                                if (localList.contains(componentClass)) {
                                                    log.error("Duplicate %s", componentClass);
                                                } else {
                                                    log.info("Found service provider %s", componentClass);
                                                    localList.add(componentClass);
                                                }
                                            }
                                        }
                                    } else {
                                        log.error(rawClass + " does not implemement %s", componentInterface);
                                    }
                                }
                            } catch (final ClassNotFoundException e) {
                                log.error("Invalid class name: %s", line);
                            }
                        }

                        componentList.addAll(localList);
                    }
                }

                return componentList;
            }
        });

        return componentList.toArray(new Class[componentList.size()]);
    }
}
