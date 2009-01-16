/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import org.fluidity.foundation.Logging;
import org.fluidity.foundation.logging.BootstrapLog;

/**
 * This class is public so that sun.misc.Services can find it.
 *
 * @author Tibor Varga
 */
@Component
@ServiceProvider
public final class ClassDiscoveryImpl implements ClassDiscovery {

    private final Logging log = new BootstrapLog("discovery");

    @SuppressWarnings({ "unchecked" })
    public <T> Class<? extends T>[] findComponentClasses(final Class<T> componentInterface,
                                                         ClassLoader classLoader,
                                                         final boolean strict) {
        classLoader = classLoader == null ? ClassLoaderUtils.findClassLoader(componentInterface) : classLoader;
        log.info(getClass(),
                "Loading service provider files for " + componentInterface + " using class loader " + classLoader);

        final Collection<Class<? extends T>> componentList = new LinkedHashSet<Class<? extends T>>();

        try {
            final Set<URL> loaded = new HashSet<URL>();

            final Enumeration<URL> resources = classLoader.getResources(
                    ClassLoaderUtils.absoluteResourceName("META-INF/services/" + componentInterface.getName()));

            for (final URL url : Collections.list(resources)) {

                /* Some dumb class loaders load JAR files more than once */
                if (!loaded.contains(url)) {
                    loaded.add(url);

                    log.info(getClass(), "Processing " + url);

                    final Collection<Class<? extends T>> localList = new LinkedHashSet<Class<? extends T>>();
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
                                    final Class<? extends T> componentClass = (Class<? extends T>) rawClass;

                                    if (Modifier.isAbstract(componentClass.getModifiers())) {
                                        log.info(getClass(), "Ignoring abstract service provider " + componentClass);
                                    } else {
                                        if (componentList.contains(componentClass)) {
                                            log.error(getClass(), "Multiple export of " + componentClass);
                                        } else {
                                            if (localList.contains(componentClass)) {
                                                log.error(getClass(), "Duplicate " + componentClass);
                                            } else {
                                                log.info(getClass(), "Found service provider " + componentClass);
                                                localList.add(componentClass);
                                            }
                                        }
                                    }
                                } else {
                                    log.error(getClass(), rawClass + " does not implemement " + componentInterface);
                                }
                            }
                        } catch (final ClassNotFoundException e) {
                            log.error(getClass(), "Invalid class name: " + line);
                        }
                    }

                    componentList.addAll(localList);
                }
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return componentList.toArray(new Class[componentList.size()]);
    }
}
