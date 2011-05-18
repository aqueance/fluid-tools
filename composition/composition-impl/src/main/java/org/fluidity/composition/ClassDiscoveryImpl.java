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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Methods;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * The component is instantiated by {@link org.fluidity.composition.ProductionServices} and picked up at container bootstrap to be made available as a
 * component. This implementation assumes class loaders adhere to the contract of delegating to parent first and only if that fails trying to resolve classes.
 *
 * @author Tibor Varga
 */
@Component(automatic = false)
final class ClassDiscoveryImpl implements ClassDiscovery {

    private final Log log;
    private final String defaultType;

    public ClassDiscoveryImpl(final LogFactory logs) {
        this.log = logs.createLog(getClass());
        this.defaultType = (String) Methods.get(ServiceProvider.class, new Methods.Invoker<ServiceProvider>() {
            public void invoke(final ServiceProvider dummy) {
                dummy.type();
            }
        }).getDefaultValue();
    }

    public <T> Class<T>[] findComponentClasses(final Class<T> api, final ClassLoader classLoader, final boolean strict) {
        final ServiceProvider annotation = api.getAnnotation(ServiceProvider.class);
        return findComponentClasses(annotation == null ? defaultType : annotation.type(), api, classLoader, strict);
    }

    @SuppressWarnings({ "unchecked", "SuspiciousToArrayCall" })
    private <T> Class<T>[] findComponentClasses(final String type, final Class<T> api, final ClassLoader cl, final boolean strict) {
        final ClassLoader classLoader = cl == null ? ClassLoaders.findClassLoader(api) : cl;
        log.debug("Loading '%s' type service provider files for %s using class loader %s", type, api, classLoader);

        final Collection<Class<T>> componentList = Exceptions.wrap(new Exceptions.Command<Collection<Class<T>>>() {
            public Collection<Class<T>> run() throws Exception {
                final Collection<Class<T>> componentList = new LinkedHashSet<Class<T>>();

                final Set<URL> loaded = new HashSet<URL>();

                for (final URL url : Collections.list(classLoader.getResources(ClassLoaders.absoluteResourceName("META-INF/%s/%s", type, api.getName())))) {

                    /* Some dumb class loaders load JAR files more than once */
                    if (!loaded.contains(url)) {
                        loaded.add(url);

                        log.debug("Loading %s", url);

                        final Collection<Class<T>> localList = new LinkedHashSet<Class<T>>();
                        final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                        String line;

                        try {
                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith("#")) {
                                    continue;
                                }

                                try {
                                    final Class<?> rawClass = classLoader.loadClass(line.trim());

                                    if (!strict || rawClass.getClassLoader() == classLoader) {
                                        if (api.isAssignableFrom(rawClass)) {
                                            final Class<T> componentClass = (Class<T>) rawClass;

                                            if (Modifier.isAbstract(componentClass.getModifiers())) {
                                                log.debug("Ignoring abstract service provider %s", componentClass);
                                            } else {
                                                if (componentList.contains(componentClass)) {
                                                    log.error("Multiple export of %s", componentClass);
                                                } else {
                                                    if (localList.contains(componentClass)) {
                                                        log.error("Duplicate %s", componentClass);
                                                    } else {
                                                        log.debug("Found %s", componentClass);
                                                        localList.add(componentClass);
                                                    }
                                                }
                                            }
                                        } else {
                                            log.error("%s does not implement %s", rawClass, api);
                                        }
                                    }
                                } catch (final ClassNotFoundException e) {
                                    log.error("Invalid class name: %s", line);
                                }
                            }
                        } finally {
                            try {
                                reader.close();
                            } catch (final IOException e) {
                                // ignore
                            }
                        }

                        componentList.addAll(localList);
                    }
                }

                return componentList;
            }
        });

        return (Class<T>[]) componentList.toArray(new Class[componentList.size()]);
    }
}
