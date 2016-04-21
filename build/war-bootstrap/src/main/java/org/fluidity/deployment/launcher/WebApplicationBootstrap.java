/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.deployment.launcher;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fluidity.deployment.plugin.spi.ServerBootstrap;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.ServiceProviders;

/**
 * A command line main class that prepares the web container bootstrap process, e.g., creating a work directory, setting up the boot classpath and, then loads
 * and invokes a supplied {@linkplain ServerBootstrap bootstrap} component.
 * <p>
 * <b>NOTE</b>: This class is public <em>only</em> so that its <code>main</code> method can be found by the Java launcher.
 * <h3>Usage</h3>
 * Use the <code>org.fluidity.maven:fluidity-archetype-standalone-war</code> Maven archetype to create the standalone web application wrapper project.
 *
 * @author Tibor Varga
 */
public final class WebApplicationBootstrap {

    private final Pattern warFilePattern = Pattern.compile("^(jar:)?file:(.+?.war)(\\!/.*)?");

    private WebApplicationBootstrap() { }

    /**
     * Command line application entry point.
     *
     * @param arguments the command line arguments.
     *
     * @throws Exception whatever is thrown by the web application bootstrap component.
     */
    public static void main(final String[] arguments) throws Exception {
        new WebApplicationBootstrap().boot(arguments);
    }

    private void boot(final String[] arguments) throws Exception {
        final Class<? extends WebApplicationBootstrap> bootstrapClass = getClass();
        final String name = ClassLoaders.classResourceName(bootstrapClass);
        final ClassLoader bootstrapLoader = bootstrapClass.getClassLoader();
        final String bootUrl = URLDecoder.decode(bootstrapLoader.getResource(name).toExternalForm(), "UTF-8");

        final List<File> managedApps = new ArrayList<>();

        final Matcher matcher = warFilePattern.matcher(bootUrl);
        if (matcher.matches()) {
            final String warPath = matcher.group(2);
            final File bootWar = new File(warPath);
            assert bootWar.exists() : bootWar;

            final int httpPort[] = { 0 };
            final boolean extract[] = { false };

            final List<String> params = new ArrayList<>();

            for (int i = 0; i < arguments.length; i++) {
                String param = arguments[i];

                if (param.endsWith(".war")) {
                    final File file = new File(param);
                    assert file.exists() : file;

                    managedApps.add(file);
                } else if (param.equals("-http")) {
                    final int j = i + 1;

                    if (arguments.length > j) {
                        try {
                            httpPort[0] = Integer.parseInt(arguments[j]);
                            i = j;
                        } catch (final NumberFormatException e) {
                            if (!arguments[j].startsWith("-")) {
                                throw new IllegalArgumentException(String.format("Parameter %s is not a port number", arguments[j]));
                            }
                        }
                    } else {
                        httpPort[0] = 80;
                    }
                } else if (param.equals("-extract")) {
                    extract[0] = true;
                } else {
                    params.add(param);
                }
            }

            Archives.Cache.access(() -> {
                final URL war = Archives.containing(WebApplicationBootstrap.class);
                final List<URL> classpath = new ArrayList<>();

                final String bootEntry = String.format("%s/boot/", Archives.WEB_INF);

                Archives.read(true, war, (url, entry) -> {
                    final String entryName = entry.getName();
                    final boolean matches = entryName.startsWith(bootEntry) && !entryName.equals(bootEntry);

                    if (matches) {
                        classpath.add(Archives.Nested.formatURL(url, entryName));
                    }

                    return null;
                });

                bootstrapServer(httpPort[0], extract[0], classpath, bootWar, managedApps, Lists.asArray(String.class, params));

                return null;
            });
        } else {
            throw new IllegalArgumentException(String.format("Not a local WAR file: %s", bootUrl));
        }
    }

    private void bootstrapServer(final int httpPort,
                                 final boolean extract,
                                 final List<URL> classpath,
                                 final File bootApp,
                                 final List<File> managedApps,
                                 final String arguments[]) throws Exception {
        final ClassLoader classLoader = ClassLoaders.create(classpath, null);
        final ServerBootstrap server = ServiceProviders.findInstance(ServerBootstrap.class, classLoader);

        if (server != null) {
            ClassLoaders.context(classLoader, loader -> {
                server.bootstrap(httpPort, extract, bootApp, managedApps, arguments);
                return null;
            });
        } else {
            throw new IllegalStateException(String.format("No server bootstrap found (service provider for %s)", ServerBootstrap.class));
        }
    }
}
