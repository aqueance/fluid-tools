/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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
package org.fluidity.deployment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sun.misc.Service;

/**
 * Prepares the web container bootstrap process, e.g. creating a work directory, setting up the boot classpath and loading and invoking the bootstrap
 * component.
 */
public final class WarBootstrapLoader {

    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH.mm.SSS");
    private final Pattern warFilePattern = Pattern.compile("^jar:file:(.*.war)\\!/.*");

    public static void main(String[] args) throws Exception {
        new WarBootstrapLoader().boot(args);
    }

    private void boot(final String[] args) throws Exception {
        final Class<? extends WarBootstrapLoader> bootstrapClass = getClass();
        final String name = bootstrapClass.getName().replace('.', '/') + ".class";
        final ClassLoader bootstrapLoader = bootstrapClass.getClassLoader();
        final String bootUrl = URLDecoder.decode(bootstrapLoader.getResource(name).toExternalForm(), "UTF-8");

        final List<File> managedApps = new ArrayList<File>();

        final Matcher matcher = warFilePattern.matcher(bootUrl);
        if (matcher.matches()) {
            final String warPath = matcher.group(1);
            final File bootWar = new File(warPath);
            assert bootWar.exists() : bootWar;

            final File workDirectory = createWorkDirectory(bootWar);
            final List<URL> classpath = unpackBootModules(workDirectory, bootWar);

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
                                throw new RuntimeException("Parameter " + args[i + 1] + " is not a port number");
                            }
                        }
                    } else {
                        httpPort = 80;
                    }
                } else {
                    params.add(param);
                }
            }

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
            throw new RuntimeException("Cannot create direcory " + classpathRoot);
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
                        final InputStream inputStream = warInput.getInputStream(entry);
                        final OutputStream outputStream = new FileOutputStream(file);
                        int bytesRead;

                        try {
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        } finally {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                // ignore
                            }

                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    }

                    classpath.add(new URL("file:" + file.getAbsolutePath()));
                }
            }
        } finally {
            try {
                warInput.close();
            } catch (IOException e) {
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

        final Iterator providers = Service.providers(ServerBootstrap.class, classLoader);

        if (providers.hasNext()) {
            final Thread currentThread = Thread.currentThread();
            final ClassLoader contextLoader = currentThread.getContextClassLoader();

            currentThread.setContextClassLoader(classLoader);
            try {
                ((ServerBootstrap) providers.next()).bootstrap(httpPort, bootApp, managedApps, workDirectory, args);
            } finally {
                currentThread.setContextClassLoader(contextLoader);
            }
        } else {
            throw new RuntimeException("No server bootstrap found (service provider for " + ServerBootstrap.class + ")");
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