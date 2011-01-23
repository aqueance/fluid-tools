/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.foundation.jarjar;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.fluidity.foundation.ClassLoaders;

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
        final int delimiter = string.indexOf('!');

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

    public static URL formatURL(final URL root, final String... paths) throws MalformedURLException {
        final StringBuilder specification = new StringBuilder(256);

        specification.append(PROTOCOL).append(':').append(root.toExternalForm());

        for (final String path : paths) {
            specification.append(DELIMITER).append(ClassLoaders.absoluteResourceName(path));
        }

        return new URL(specification.toString());
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
}
