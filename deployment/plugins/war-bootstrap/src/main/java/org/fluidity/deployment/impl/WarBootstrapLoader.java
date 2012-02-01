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

package org.fluidity.deployment.impl;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fluidity.deployment.plugin.spi.ServerBootstrap;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.ServiceProviders;
import org.fluidity.foundation.jarjar.Handler;

/**
 * Prepares the web container bootstrap process, e.g. creating a work directory, setting up the boot classpath and loading and invoking the bootstrap
 * component.
 *
 * @author Tibor Varga
 */
public final class WarBootstrapLoader {

    private final Pattern warFilePattern = Pattern.compile("^(jar:)?file:(.+?.war)(\\!/.*)?");

    public static void main(final String[] args) throws Exception {
        new WarBootstrapLoader().boot(args);
    }

    private void boot(final String[] args) throws Exception {
        final Class<? extends WarBootstrapLoader> bootstrapClass = getClass();
        final String name = ClassLoaders.classResourceName(bootstrapClass);
        final ClassLoader bootstrapLoader = bootstrapClass.getClassLoader();
        final String bootUrl = URLDecoder.decode(bootstrapLoader.getResource(name).toExternalForm(), "UTF-8");

        final List<File> managedApps = new ArrayList<File>();

        final Matcher matcher = warFilePattern.matcher(bootUrl);
        if (matcher.matches()) {
            final String warPath = matcher.group(2);
            final File bootWar = new File(warPath);
            assert bootWar.exists() : bootWar;

            int httpPort = 0;
            final List<String> params = new ArrayList<String>();

            for (int i = 0; i < args.length; i++) {
                String param = args[i];

                if (param.endsWith(".war")) {
                    final File file = new File(param);
                    assert file.exists() : file;

                    managedApps.add(file);
                } else if (args.length > i && param.equals("-http")) {
                    if (args.length > i + 1) {
                        try {
                            httpPort = Integer.parseInt(args[i + 1]);
                            ++i;
                        } catch (final NumberFormatException e) {
                            if (!args[i + 1].endsWith(".war")) {
                                throw new RuntimeException(String.format("Parameter %s is not a port number", args[i + 1]));
                            }
                        }
                    } else {
                        httpPort = 80;
                    }
                } else {
                    params.add(param);
                }
            }
            final URL url = ClassLoaders.findClassResource(WarBootstrapLoader.class);
            final JarURLConnection jar = Archives.jarFile(url);

            if (jar != null) {
                final URL warURL = jar.getJarFileURL();
                final List<URL> classpath = new ArrayList<URL>();

                classpath.add(warURL);

                Archives.readEntries(warURL, new Archives.EntryReader() {
                    private final String bootEntry = "WEB-INF/boot/";

                    public boolean matches(final JarEntry entry) throws IOException {
                        final String entryName = entry.getName();
                        final boolean matches = entryName.startsWith(bootEntry) && !entryName.equals(bootEntry);

                        if (matches) {
                            classpath.add(Handler.formatURL(warURL, entryName));
                        }

                        return false;
                    }

                    public boolean read(final JarEntry entry, final JarInputStream stream) throws IOException {
                        return false;
                    }
                });

                bootstrapServer(httpPort, classpath, bootWar, managedApps, params.toArray(new String[params.size()]));
            } else {
                throw new IllegalStateException(String.format("%s does not point to a WAR file", url));
            }
        } else {
            throw new RuntimeException("Not a local WAR file: " + bootUrl);
        }
    }

    private void bootstrapServer(final int httpPort,
                                 final List<URL> classpath,
                                 final File bootApp,
                                 final List<File> managedApps,
                                 final String args[]) throws Exception {
        final URLClassLoader classLoader = new URLClassLoader(classpath.toArray(new URL[classpath.size()]));
        final ServerBootstrap server = ServiceProviders.findInstance(ServerBootstrap.class, classLoader);

        if (server != null) {
            ClassLoaders.context(classLoader, new ClassLoaders.ContextCommand<Void, Exception>() {
                public Void run(final ClassLoader loader) throws Exception {
                    server.bootstrap(httpPort, bootApp, managedApps, args);
                    return null;
                }
            });
        } else {
            throw new RuntimeException(String.format("No server bootstrap found (service provider for %s)", ServerBootstrap.class));
        }
    }
}
