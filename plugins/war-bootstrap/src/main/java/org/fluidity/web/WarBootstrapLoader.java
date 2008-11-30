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
 * Prepares the web container bootstrap process, e.g. creating a work directory, setting up the boot classpath and loading and instantiating the bootstrap
 * component.
 */
public final class WarBootstrapLoader {

    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd:HH:mm:SSS");

    public static void main(String[] args) throws Exception {
        final Class<WarBootstrapLoader> bootstrapClass = WarBootstrapLoader.class;
        final String name = bootstrapClass.getName().replace('.', '/') + ".class";
        final URL warUrl = bootstrapClass.getClassLoader().getResource(name);
        final String url = warUrl.toExternalForm();

        final Matcher matcher = Pattern.compile("^jar:file:(.*.war)\\!/.*").matcher(url);
        if (matcher.matches()) {
            final List<URL> classpath = new ArrayList<URL>();

            final String warPath = matcher.group(1);
            final File warFile = new File(warPath);
            assert warFile.exists() : warFile;

            final JarFile warInput = new JarFile(warFile);

            final File workDirectory = createWorkDirectory(warFile);
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

            final URLClassLoader classLoader = new URLClassLoader(classpath.toArray(new URL[classpath.size()]));

            // Jetty needs this to function
            Thread.currentThread().setContextClassLoader(classLoader);
            final Iterator providers = Service.providers(ServerBootstrap.class, classLoader);

            if (providers.hasNext()) {
                final ServerBootstrap server = (ServerBootstrap) providers.next();
                server.bootstrap(warFile, workDirectory);
            } else {
                throw new RuntimeException("No server bootstrap found (service provider for " + ServerBootstrap.class + ")");
            }
        } else {
            throw new RuntimeException("Not a local .war file: " + url);
        }
    }

    private static File createWorkDirectory(final File archive) {
        File bootDirectory = new File(archive.getParentFile(), "web-container");

        if (!bootDirectory.exists() && !bootDirectory.mkdirs()) {
            throw new RuntimeException("Cannot create " + bootDirectory);
        }

        return bootDirectory;
    }

    private static String archiveName(final File archive) {
        final String archiveName = archive.getName();
        return archiveName.substring(0, archiveName.length() - ".war".length());
    }
}