package org.fluidity.web;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
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

    private final DateFormat df = new SimpleDateFormat("yyyy-MM-dd:HH:mm:SSS");
    private final Pattern warFilePattern = Pattern.compile("^jar:file:(.*.war)\\!/.*");

    public static void main(String[] args) throws Exception {
        new WarBootstrapLoader().boot(args);
    }

    private void boot(final String[] args) throws Exception {
        final Class<? extends WarBootstrapLoader> bootstrapClass = getClass();
        final String name = bootstrapClass.getName().replace('.', '/') + ".class";
        final ClassLoader bootstrapLoader = bootstrapClass.getClassLoader();
        final String bootUrl = bootstrapLoader.getResource(name).toExternalForm();

        final List<File> otherWars = new ArrayList<File>();

        final Matcher matcher = warFilePattern.matcher(bootUrl);
        if (matcher.matches()) {
            final String warPath = matcher.group(1);
            final File bootWar = new File(warPath);
            assert bootWar.exists() : bootWar;

            final File workDirectory = createWorkDirectory(bootWar);
            final List<URL> classpath = unpackBootModules(workDirectory, bootWar);

            for (final String path : args) {
                if (path.endsWith(".war")) {
                    final File file = new File(path);
                    assert file.exists() : file;

                    otherWars.add(file);
                }
            }

            bootstrapServer(classpath, bootWar, otherWars, workDirectory);
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

    private void bootstrapServer(final List<URL> classpath, final File bootWar, final List<File> otherWars, final File workDirectory) {
        final URLClassLoader classLoader = new URLClassLoader(classpath.toArray(new URL[classpath.size()]));

        final Iterator providers = Service.providers(ServerBootstrap.class, classLoader);

        if (providers.hasNext()) {
            final Thread currentThread = Thread.currentThread();
            final ClassLoader contextLoader = currentThread.getContextClassLoader();

            currentThread.setContextClassLoader(classLoader);
            try {
                ((ServerBootstrap) providers.next()).bootstrap(bootWar, otherWars, workDirectory);
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