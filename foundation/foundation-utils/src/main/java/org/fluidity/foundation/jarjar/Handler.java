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

package org.fluidity.foundation.jarjar;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.fluidity.foundation.ClassLoaders;

/**
 * {@linkplain URLStreamHandler Stream protocol handler} to work with resources inside JAR archives embedded in other JAR archives, ad infinitum.
 * <p/>
 * When loaded, it adds its enclosing package to the protocol handler package list to make itself known. This stream protocol handler makes it possible for an
 * ordinary {@link java.net.URLClassLoader} to work with JAR archives embedded in other JAR archives, enabling packaged Java applications without loss of
 * functionality such as signed JAR files, etc.
 * <p/>
 * To use this this stream handler, all you need to do is {@linkplain #formatURL(URL, String...) create} URLs that map to this stream protocol handler. For
 * example, if you have a JAR archive named <code>my-archive.jar</code> that embeds another JAR archive, <code>my-dependency.jar</code>, the following will
 * create an URL that can then be fed to an URL class loader:
 * <pre>
 * final URL embedded = Handler.formatURL(new File("my-archive.jar").toURI().toURL(), "my-dependency.jar");
 * </pre>
 *
 * @author Tibor Varga
 */
public final class Handler extends URLStreamHandler {

    /**
     * The URL protocol understood by this handler. The value is computed to be the last component of the containing package.
     */
    public static final String PROTOCOL;

    static final String DELIMITER = "!:/";      // must not be the same used by the JAR handler

    private static final String PROTOCOL_HANDLERS_PROPERTY = "java.protocol.handler.pkgs";

    static {
        final String canonicalName = Handler.class.getName();
        final int dot = canonicalName.lastIndexOf('.', canonicalName.length() - Handler.class.getSimpleName().length() - 2);

        if (dot < 0) {
            throw new IllegalStateException(String.format("Class %s must be in a package at least two levels deep", Handler.class.getName()));
        }

        final String packageName = canonicalName.substring(0, dot);

        try {
            final String property = System.getProperty(PROTOCOL_HANDLERS_PROPERTY);

            if (property == null || !Arrays.asList(property.split("|")).contains(packageName)) {
                System.setProperty(PROTOCOL_HANDLERS_PROPERTY, property == null ? packageName : String.format("%s|%s", packageName, property));
            }
        } catch (final AccessControlException e) {
            // fine, we'll just assume our parent package has been registered already
        }

        PROTOCOL = canonicalName.substring(dot + 1, canonicalName.lastIndexOf('.'));
    }

    /**
     * Used by the URL stream handler framework. Do not call this constructor directly.
     */
    public Handler() { }

    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        return new EmbeddedConnection(url);
    }

    @Override
    protected void parseURL(final URL url, final String spec, final int start, final int limit) {
        final String string = spec.substring(start, limit);
        final int delimiter = string.indexOf(DELIMITER);

        if (delimiter < 0) {
            throw new IllegalStateException(new MalformedURLException(String.format("%s is not an embedded jar file.", spec)));
        }

        setURL(url, url.getProtocol(), string.substring(0, delimiter), -1, null, null, string.substring(delimiter), null, null);
    }

    @Override
    protected String toExternalForm(final URL url) {
        try {
            return String.format("%s:%s%s", url.getProtocol(), new URL(url.getHost()).toExternalForm(), url.getPath());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates a URL that will have the protocol handled, and the path format recognized, by this handler.
     *
     * @param root  the URL of the outermost JAR archive.
     * @param paths the paths of the JAR archives embedded the last archive, or in case of the first item, the root archive.
     *
     * @return a URL properly formatted for this handler.
     *
     * @throws IOException if URL handling fails.
     */
    public static URL formatURL(final URL root, final String... paths) throws IOException {
        final StringBuilder specification = new StringBuilder(256);
        final URLConnection connection = root.openConnection();

        assert !root.getProtocol().equals(PROTOCOL) : root;

        if (connection instanceof JarURLConnection) {

            // we have a JAR URL but we need the enclosed URL and the JAR file path relative to the URL separately
            final URL url = ((JarURLConnection) connection).getJarFileURL();
            final String path = root.getPath().split("!/")[1];      // parsing a JAR URL, not our own URL

            specification.append(PROTOCOL).append(':').append(url.toExternalForm());
            specification.append(DELIMITER).append(path);
        } else {
            specification.append(PROTOCOL).append(':').append(root.toExternalForm());
        }

        for (final String path : paths) {
            specification.append(DELIMITER).append(ClassLoaders.absoluteResourceName(path));
        }

        try {
            return new URL(specification.toString());
        } catch (final MalformedURLException e) {
            return new URL(null, specification.toString(), Singleton.INSTANCE);
        }
    }

    /**
     * An URL connection for the represented URL protocol as required by the URL stream protocol handler framework.
     */
    private static class EmbeddedConnection extends URLConnection {

        private final URLConnection root;

        public EmbeddedConnection(final URL url) throws IOException {
            super(url);

            // the host part of our root URL itself is an URL
            this.root = new URL(URLDecoder.decode(getURL().getHost(), "UTF-8")).openConnection();
        }

        @Override
        public void connect() throws IOException {
            root.connect();
        }

        @Override
        public Permission getPermission() throws IOException {
            return root.getPermission();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            final JarInputStream container = new JarInputStream(root.getInputStream());

            // each successive path is nested in the stream at the previous index
            final String[] paths = getURL().getPath().split(DELIMITER);

            // first stream is the container
            JarInputStream stream = container;

            // the first path is an empty string since spec starts with a ! and we split around !s
            for (int i = 1, pathCount = paths.length; i < pathCount; i++) {
                final String path = paths[i];

                JarEntry next;
                while ((next = stream.getNextJarEntry()) != null) {
                    if (!next.isDirectory()) {
                        if (path.equals(next.getName())) {
                            if (i == pathCount - 1) {

                                // caller must close the stream, which closes its input, closing all streams we just opened here
                                return stream;
                            } else {

                                // descend to entry
                                stream = new JarInputStream(stream);
                                break;
                            }
                        }
                    }

                    stream.closeEntry();
                }
            }

            throw new FileNotFoundException(getURL().toExternalForm());
        }
    }

    /**
     * Lazy instantiation of a {@link Handler} object.
     */
    private static class Singleton {
        public static final Handler INSTANCE = new Handler();
    }
}
