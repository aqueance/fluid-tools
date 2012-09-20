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

package org.fluidity.deployment.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fluidity.deployment.plugin.spi.ServerBootstrap;
import org.fluidity.foundation.Archives;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.ServiceProviders;

import static org.fluidity.foundation.Command.Function;

/**
 * A command line main class that prepares the web container bootstrap process, e.g., creating a work directory, setting up the boot classpath and, then loads
 * and invokes a supplied {@linkplain ServerBootstrap bootstrap} component.
 * <p/>
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
     * @param args the command line arguments.
     *
     * @throws Exception whatever is thrown by the web application bootstrap component.
     */
    public static void main(final String[] args) throws Exception {
        new WebApplicationBootstrap().boot(args);
    }

    private void boot(final String[] args) throws Exception {
        final Class<? extends WebApplicationBootstrap> bootstrapClass = getClass();
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

            final URL war = Archives.containing(WebApplicationBootstrap.class);
            final List<URL> classpath = new ArrayList<URL>();

            classpath.add(war);

            Archives.readEntries(war, new Archives.Entry() {
                private final String bootEntry = "WEB-INF/boot/";

                public boolean matches(final JarEntry entry) throws IOException {
                    final String entryName = entry.getName();
                    final boolean matches = entryName.startsWith(bootEntry) && !entryName.equals(bootEntry);

                    if (matches) {
                        classpath.add(Archives.Nested.formatURL(war, null, entryName));
                    }

                    return false;
                }

                public boolean read(final JarEntry entry, final InputStream stream) throws IOException {
                    return false;
                }
            });

            bootstrapServer(httpPort, classpath, bootWar, managedApps, Lists.asArray(params, String.class));
        } else {
            throw new RuntimeException("Not a local WAR file: " + bootUrl);
        }
    }

    private void bootstrapServer(final int httpPort,
                                 final List<URL> classpath,
                                 final File bootApp,
                                 final List<File> managedApps,
                                 final String args[]) throws Exception {
        final URLClassLoader classLoader = new URLClassLoader(Lists.asArray(classpath, URL.class));
        final ServerBootstrap server = ServiceProviders.findInstance(ServerBootstrap.class, classLoader);

        if (server != null) {
            ClassLoaders.context(classLoader, new Function<Void, ClassLoader, Exception>() {
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
