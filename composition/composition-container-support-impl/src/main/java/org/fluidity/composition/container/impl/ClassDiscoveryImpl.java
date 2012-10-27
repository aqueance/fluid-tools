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

package org.fluidity.composition.container.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.fluidity.composition.Component;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassDiscovery;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.ServiceProviders;

import static org.fluidity.foundation.Command.Process;

/**
 * The component is instantiated by {@link ProductionServices} and picked up at container bootstrap to be made available as a component. This implementation
 * assumes class loaders adhere to the contract of delegating to parent first and only if that fails trying to resolve classes.
 *
 * @author Tibor Varga
 */
@Component(automatic = false)
final class ClassDiscoveryImpl implements ClassDiscovery {

    private final Log log;

    ClassDiscoveryImpl(final Log<ClassDiscoveryImpl> log) {
        this.log = log;
    }

    public <T> Class<T>[] findComponentClasses(final Class<T> api, final ClassLoader classLoader, final boolean strict) {
        return Exceptions.wrap(new Process<Class<T>[], Throwable>() {
            public Class<T>[] run() throws Throwable {
                final ServiceProvider annotation = api.getAnnotation(ServiceProvider.class);
                return findComponentClasses(annotation == null ? ServiceProviders.TYPE : annotation.type(), api, classLoader, strict);
            }
        });
    }

    private <T> Class<T>[] findComponentClasses(final String type, final Class<T> api, final ClassLoader cl, final boolean strict) throws Exception {
        final ClassLoader classLoader = cl == null ? ClassLoaders.findClassLoader(api, true) : cl;
        log.debug("Loading '%s' type service provider files for %s using class loader %s", type, api, classLoader);

        final Collection<Class<T>> componentList = new LinkedHashSet<Class<T>>();

        for (final URL url : ClassLoaders.findResources(classLoader, "%s/%s", ServiceProviders.location(type), api.getName())) {
            log.debug("Loading %s", url);

            final Collection<Class<T>> localList = new LinkedHashSet<Class<T>>();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(Archives.open(true, url), "UTF-8"));
            String content;

            try {
                while ((content = reader.readLine()) != null) {
                    final int hash = content.indexOf('#');
                    final String line = (hash < 0 ? content : content.substring(0, hash)).trim();

                    if (!line.isEmpty()) {
                        try {
                            final Class<?> rawClass = classLoader.loadClass(line);

                            if (!strict || rawClass.getClassLoader() == classLoader) {
                                if (api.isAssignableFrom(rawClass)) {
                                    @SuppressWarnings("unchecked")
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
                            log.error(e, "Invalid class name: %s", line);
                        }
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

        return Lists.asArray(Class.class, componentList);
    }
}
