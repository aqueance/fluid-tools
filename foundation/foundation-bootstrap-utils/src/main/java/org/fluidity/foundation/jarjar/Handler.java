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

import org.fluidity.foundation.ClassLoaders;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Handles resources inside JAR files inside JAR files, ad infinitum.
 *
 * @author Tibor Varga
 */
public class Handler extends URLStreamHandler {

    public static final String PROTOCOL;

    private static final String PROTOCOL_HANDLERS_PROPERTY = "java.protocol.handler.pkgs";
    private static final String DELIMITER = "!/";

    static {
        final String canonicalName = Handler.class.getName();
        final int dot = canonicalName.lastIndexOf('.', canonicalName.length() - Handler.class.getSimpleName().length() - 2);

        if (dot < 0) {
            throw new IllegalStateException(String.format("Class %s must not be in a package at least two levels deep", Handler.class.getName()));
        }

        final String packageName = canonicalName.substring(0, dot);

        final String property = System.getProperty(PROTOCOL_HANDLERS_PROPERTY);
        if (property == null || !Arrays.asList(property.split("|")).contains(packageName)) {
            System.setProperty(PROTOCOL_HANDLERS_PROPERTY, property == null ? packageName : String.format("%s|%s", packageName, property));
        }

        PROTOCOL = canonicalName.substring(dot + 1, canonicalName.lastIndexOf('.'));
    }

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

    public static URL formatURL(final URL root, final String... paths) throws IOException {
        final StringBuilder specification = new StringBuilder(256);
        final URLConnection connection = root.openConnection();

        if (connection instanceof JarURLConnection) {

            // we have a JAR URL but we need the enclosed URL and the JAR file path relative to the URL separately
            final URL url = ((JarURLConnection) connection).getJarFileURL();
            final String path = root.getPath().split(DELIMITER)[1];

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

    public static class EmbeddedConnection extends URLConnection {

        public EmbeddedConnection(final URL url) {
            super(url);
        }

        @Override
        public void connect() throws IOException {
            // empty
        }

        @Override
        public InputStream getInputStream() throws IOException {

            // the host part of our URL itself is an URL
            final JarInputStream container = new JarInputStream(new URL(URLDecoder.decode(getURL().getHost(), "UTF-8")).openStream());

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

    private static class Singleton {

    // must come after the static block that registers the this class as an URL stream handler
        public static final Handler INSTANCE = new Handler();
    }
}
