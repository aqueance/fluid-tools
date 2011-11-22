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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fluidity.deployment.plugin.spi.ServerBootstrap;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.ServiceProviders;
import org.fluidity.foundation.Streams;

/**
 * Prepares the web container bootstrap process, e.g. creating a work directory, setting up the boot classpath, and loading and invoking the bootstrap
 * component. This class is used as the main class for an executable WAR file.
 *
 * @author Tibor Varga
 */
public final class WarBootstrapLoader {

    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH.mm.SSS");
    private final Pattern warFilePattern = Pattern.compile("^(jar:)?file:(.+\\.war)(\\!/.*)?");

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

            final File workDirectory = createWorkDirectory(bootWar);
            final List<URL> classpath = unpackBootModules(workDirectory, bootWar);

            bootstrapServer(httpPort, classpath, bootWar, managedApps, workDirectory, params.toArray(new String[params.size()]));
        } else {
            throw new RuntimeException("Not a local .war file: " + bootUrl);
        }
    }

    private List<URL> unpackBootModules(final File workDirectory, final File warFile) throws IOException {
        final List<URL> classpath = new ArrayList<URL>();
        final JarFile warInput = new JarFile(warFile);

        final File classpathRoot = new File(workDirectory, archiveName(warFile) + '-' + df.format(new Date(warFile.lastModified())));
        if (!classpathRoot.exists() && !classpathRoot.mkdir()) {
            throw new RuntimeException("Cannot create directory " + classpathRoot);
        }

        try {
            final String bootEntry = "WEB-INF/boot/";
            final byte buffer[] = new byte[1024 * 16];

            for (final Enumeration entries = warInput.entries(); entries.hasMoreElements();) {
                final JarEntry entry = (JarEntry) entries.nextElement();

                final String entryName = entry.getName();
                if (entryName.startsWith(bootEntry) && !entryName.equals(bootEntry)) {
                    final File file = new File(classpathRoot, new File(entryName).getName());

                    if (file.exists()) {
                        final long entryTime = entry.getTime();
                        final long fileTime = file.lastModified();

                        if (entryTime > fileTime) {
                            if (!file.delete()) {
                                throw new RuntimeException("Cannot delete " + file);
                            }
                        }
                    }

                    if (!file.exists()) {
                        Streams.copy(warInput.getInputStream(entry), new FileOutputStream(file), buffer, true);
                    }

                    // new URL("file:" + file.getAbsolutePath())
                    classpath.add(file.toURI().toURL());
                }
            }
        } finally {
            try {
                warInput.close();
            } catch (final IOException e) {
                // ignore
            }
        }

        return classpath;
    }

    private void bootstrapServer(final int httpPort,
                                 final List<URL> classpath,
                                 final File bootApp,
                                 final List<File> managedApps,
                                 final File workDirectory,
                                 final String args[]) {
        final URLClassLoader classLoader = new URLClassLoader(classpath.toArray(new URL[classpath.size()]));

        final ServerBootstrap server = ServiceProviders.findInstance(ServerBootstrap.class, classLoader);

        if (server != null) {
            final Thread currentThread = Thread.currentThread();
            final ClassLoader contextLoader = currentThread.getContextClassLoader();

            currentThread.setContextClassLoader(classLoader);
            try {
                server.bootstrap(httpPort, bootApp, managedApps, workDirectory, args);
            } finally {
                currentThread.setContextClassLoader(contextLoader);
            }
        } else {
            throw new RuntimeException(String.format("No server bootstrap found (service provider for %s)", ServerBootstrap.class));
        }
    }

    private File createWorkDirectory(final File archive) {
        File bootDirectory = new File(archive.getParentFile(), "web-container");

        if (!bootDirectory.exists() && !bootDirectory.mkdirs()) {
            throw new RuntimeException("Cannot create " + bootDirectory);
        }

        return bootDirectory;
    }

    private String archiveName(final File archive) {
        final String archiveName = archive.getName();
        return archiveName.substring(0, archiveName.length() - ".war".length());
    }
}
