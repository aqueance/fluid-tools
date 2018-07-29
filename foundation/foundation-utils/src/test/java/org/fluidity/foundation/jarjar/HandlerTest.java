/*
 * Copyright (c) 2006-2018 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.IOStreams;
import org.fluidity.foundation.Strings;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("ThrowFromFinallyBlock")
public class HandlerTest {

    private final ClassLoader loader = getClass().getClassLoader();

    private final URL container = loader.getResource("level0.jar");
    private final URL samples = loader.getResource("samples.jar");

    private static final byte[] BUFFER = new byte[128];

    @DataProvider(name = "caching")
    public Object[][] caching() {
        return new Object[][] {
            new Object[] { true },
            new Object[] { false },
        };
    }

    @Test(dataProvider = "caching")
    public void testHandler(final boolean cached) throws Exception {
        assertContent(cached, "level 0", "level0.txt");
        assertContent(cached, "level 1", "level1-1.jar", "level1.txt");
        assertContent(cached, "level 1", "level1-2.jar", "level1.txt");
        assertContent(cached, "level 2", "level1-1.jar", "level2.jar", "level2.txt");
        assertContent(cached, "level 2", "level1-2.jar", "level2.jar", "level2.txt");
        assertContent(cached, "level 3", "level1-1.jar", "level2.jar", "level3.jar", "level3.txt");
        assertContent(cached, "level 3", "level1-2.jar", "level2.jar", "level3.jar", "level3.txt");
    }

    @Test(dataProvider = "caching")
    public void testURL(final boolean cached) throws Exception {
        assertContent(cached, "level 0", "level0.txt");
        assertContent(cached, "level 1", "level1-1.jar", "level1.txt");
        assertContent(cached, "level 1", "level1-2.jar", "level1.txt");
        assertContent(cached, "level 2", "level1-1.jar", "level2.jar", "level2.txt");
        assertContent(cached, "level 2", "level1-2.jar", "level2.jar", "level2.txt");
        assertContent(cached, "level 3", "level1-1.jar", "level2.jar", "level3.jar", "level3.txt");
        assertContent(cached, "level 3", "level1-2.jar", "level2.jar", "level3.jar", "level3.txt");

        {
            final URL root = container;
            final URL level1 = Handler.formatURL(root, "level1-2.jar");
            final URL level2 = Handler.formatURL(level1, "level2.jar");
            final URL level3 = Handler.formatURL(level2, "level3.jar");

            final URLConnection connection = Handler.formatURL(level3, "level3.txt").openConnection();
            connection.setUseCaches(cached);

            try (final InputStream stream = connection.getInputStream()) {
                verify("level 3", IOStreams.load(stream, Strings.ASCII, BUFFER).replaceAll("\n", ""));
            }
        }
    }

    @Test
    public void testSingleThreadedCaching() throws Exception {
        final URL url1 = Handler.formatURL(container, "level1-1.jar", "level2.jar", "level3.jar", "level3.txt");
        final URL url2 = Handler.formatURL(samples, "META-INF/dependencies/dependency-1.jar");

        Handler.Cache.contents(url1);
        assert Handler.Cache.loaded(url1, true);
        assert Handler.Cache.loaded(url1, false);

        Archives.Cache.access(() -> {
            assert Handler.Cache.loaded(url1, true);
            assert Handler.Cache.loaded(url1, false);

            Archives.Cache.access(() -> {
                assert Handler.Cache.loaded(url1, true);
                assert Handler.Cache.loaded(url1, false);

                Handler.Cache.contents(url2);
                assert Handler.Cache.loaded(url2, true);
                assert !Handler.Cache.loaded(url2, false);

                Handler.Cache.unload(url1);
                assert !Handler.Cache.loaded(url1, true);
                assert Handler.Cache.loaded(url1, false);

                assert Handler.Cache.loaded(url2, true);
                assert !Handler.Cache.loaded(url2, false);
                Handler.Cache.unload(url2);
                assert !Handler.Cache.loaded(url2, true);
                assert !Handler.Cache.loaded(url2, false);

                return null;
            });

            assert Handler.Cache.loaded(url1, true);
            assert Handler.Cache.loaded(url1, false);

            Handler.Cache.unload(url1);
            assert !Handler.Cache.loaded(url1, true);
            assert Handler.Cache.loaded(url1, false);

            return null;
        });

        assert Handler.Cache.loaded(url1, true);
        assert Handler.Cache.loaded(url1, false);

        Handler.Cache.unload(url1);
        assert !Handler.Cache.loaded(url1, true);
        assert !Handler.Cache.loaded(url1, false);
    }

    @Test
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void testMultiThreadedCaching() throws Exception {
        final URL url1 = Handler.formatURL(container, "level1-1.jar", "level2.jar", "level3.jar", "level3.txt");
        final URL url2 = Handler.formatURL(samples, "META-INF/dependencies/dependency-1.jar");

        final CyclicBarrier barrier = new CyclicBarrier(2);
        final AtomicBoolean running = new AtomicBoolean();
        final AtomicReference<Exception> error = new AtomicReference<>();

        Handler.Cache.unload(url1);
        Handler.Cache.unload(url2);

        final Thread thread = new Thread(() -> {
            running.set(true);

            try {
                Archives.Cache.access(() -> {
                    barrier.await(100, TimeUnit.MILLISECONDS);

                    assert !Handler.Cache.loaded(url1, true);
                    Handler.Cache.contents(url1);
                    assert Handler.Cache.loaded(url1, true);

                    Handler.Cache.contents(url2);
                    assert Handler.Cache.loaded(url2, true);

                    Handler.Cache.unload(url1);
                    assert !Handler.Cache.loaded(url1, true);

                    assert Handler.Cache.loaded(url2, true);
                    Handler.Cache.unload(url2);
                    assert !Handler.Cache.loaded(url2, true);

                    return null;
                });

                barrier.await(100, TimeUnit.MILLISECONDS);

                Archives.Cache.access(() -> {
                    assert !Handler.Cache.loaded(url1, true);

                    Handler.Cache.contents(url2);
                    assert Handler.Cache.loaded(url2, true);

                    Handler.Cache.unload(url2);
                    assert !Handler.Cache.loaded(url2, true);

                    return null;
                });
            } catch (final Exception e) {
                error.set(e);
            }
        });

        try {
            Archives.Cache.access(() -> {
                Handler.Cache.contents(url1);
                assert Handler.Cache.loaded(url1, true);

                thread.start();
                barrier.await(100, TimeUnit.MILLISECONDS);
                assert running.get();

                Handler.Cache.unload(url1);
                assert !Handler.Cache.loaded(url1, true);

                return null;
            });

            barrier.await(100, TimeUnit.MILLISECONDS);
            assert running.get();
        } finally {
            thread.join(200);
        }

        assert !thread.isAlive();
        assert error.get() == null : error.get();
    }

    @Test
    public void testFormatting() throws Exception {
        final URL expected = Handler.formatURL(container, "level1-2.jar", "level2.jar", "level3.jar");
        verify(expected, Handler.formatURL(Handler.rootURL(expected), "level1-2.jar", "level2.jar", "level3.jar"));
        verify(expected, Handler.formatURL(Handler.formatURL(Handler.rootURL(expected), "level1-2.jar", "level2.jar"), "level3.jar"));
        verify(expected, Handler.formatURL(Handler.formatURL(Handler.formatURL(Handler.rootURL(expected), "level1-2.jar"), "level2.jar"), "level3.jar"));

        verify(expected, Handler.formatURL(Handler.formatURL(Handler.formatURL(Handler.formatURL(Handler.rootURL(expected), "level1-2.jar"), "level2.jar"), "level3.jar")));

        final URL insane = Handler.formatURL(new URL("http://xxx:yyy@insane.org/whatever.jar?query=param"), "level1.jar", "level2.jar", "level3.jar");
        verify(insane, Handler.formatURL(Handler.formatURL(Handler.formatURL(Handler.rootURL(insane), "level1.jar"), "level2.jar"), "level3.jar"));
    }

    @Test(dataProvider = "caching")
    public void testExploration(final boolean cached) throws Exception {
        final List<String> files = new ArrayList<>();

        Archives.read(container, cached, new Archives.Entry() {
            public Reader matches(final URL url, final JarEntry entry) throws IOException {
                final String name = entry.getName();

                if (name.endsWith(".txt")) {
                    files.add(name);
                }

                return (_url, _entry, stream) -> {
                    Archives.read(Archives.Nested.formatURL(_url, _entry.getName()), cached, this);
                    return true;
                };
            }
        });

        verify(Arrays.asList("level0.txt", "level1.txt", "level2.txt", "level3.txt", "level1.txt", "level2.txt", "level3.txt"), files);
    }

    @Test(dataProvider = "caching")
    public void testMetadata(final boolean cached) throws Exception {
        {
            final URL url = this.container;
            final URLConnection connection = Archives.connect(url, cached);

            assert connection.getContentLength() != 0 : String.format("Content length of %s", url);
            assert connection.getContentType() != null : String.format("Content type of %s", url);
            assert connection.getLastModified() > 0 : String.format("Last modification date of %s", url);
        }

        {
            final URL url = Handler.formatURL(container, "level0.txt");
            final URLConnection connection = Archives.connect(url, cached);

            assert connection.getContentLength() != 0 : String.format("Content length of %s", url);
            assert Objects.equals(connection.getContentType(), "text/plain") : String.format("Content type of %s", url);
            assert connection.getLastModified() > 0 : String.format("Last modification date of %s", url);
        }

        {
            final URL url = Handler.formatURL(container, "level1-2.jar", "level2.jar", "level3.jar", "level3.txt");
            final URLConnection connection = Archives.connect(url, cached);

            assert connection.getContentLength() != 0 : String.format("Content length of %s", url);
            assert Objects.equals(connection.getContentType(), "text/plain") : String.format("Content type of %s", url);
            assert connection.getLastModified() > 0 : String.format("Last modification date of %s", url);
        }
    }

    @Test(dataProvider = "caching")
    public void testHeaders(final boolean cached) throws Exception {
        final URL url = Handler.formatURL(container, "level1-2.jar", "level2.jar", "level3.jar", "level3.txt");
        final URLConnection connection = Archives.connect(url, cached);

        {
            assert Objects.equals(connection.getHeaderField(0), "text/plain") : String.format("Content type of %s", url);
            assert Integer.valueOf(connection.getHeaderField(1)) != 0 : String.format("Content length of %s", url);
            assert Long.valueOf(connection.getHeaderField(2)) > 0 : String.format("Last modification date of %s", url);
            assert connection.getHeaderField(3) == null;
        }

        {
            assert Objects.equals(connection.getHeaderFieldKey(0), "content-type") : connection.getHeaderFieldKey(0);
            assert Objects.equals(connection.getHeaderFieldKey(1), "content-length") : connection.getHeaderFieldKey(1);
            assert Objects.equals(connection.getHeaderFieldKey(2), "last-modified") : connection.getHeaderFieldKey(2);
            assert connection.getHeaderFieldKey(3) == null;
        }

        {
            final Map<String, List<String>> fields = connection.getHeaderFields();

            assert fields.size() == 3 : fields.size();
            assert Objects.equals(fields.get("content-type").get(0), "text/plain") : String.format("Content type of %s", url);
            assert Integer.valueOf(fields.get("content-length").get(0)) != 0 : String.format("Content length of %s", url);
            assert Long.valueOf(fields.get("last-modified").get(0)) > 0 : String.format("Last modification date of %s", url);
        }
    }

    @Test(dataProvider = "caching")
    public void testContent(final boolean cached) throws Exception {
        final URL url = Handler.formatURL(container, "level1-2.jar", "level2.jar", "level3.jar", "level3.txt");
        final URLConnection connection = Archives.connect(url, cached);

        final Object content = connection.getContent();

        assert content != null : url;
        assert content instanceof InputStream : content.getClass();

        try (final InputStream input = (InputStream) content) {
            final String value = IOStreams.load(input, Strings.UTF_8, new byte[128]).replaceAll("\n", "");

            assert Objects.equals(value, "level 3") : value;
        }
    }

    @Test
    public void testRelativeURLs() throws Exception {
        verify(Handler.formatURL(container, "level1-2.jar", "level2.jar", "level2.txt"),
               new URL(Handler.formatURL(container, "level1-2.jar", "level2.jar", "level3.jar"), "level2.txt"));
        verify(Handler.formatURL(container, "level2-2.jar"),
               new URL(Handler.formatURL(container, "level1-2.jar"), "level2-2.jar"));
    }

    private void verify(final Object expected, final Object actual) {
        assert Objects.equals(expected, actual) : String.format("%nExpected %s,%n     got %s", expected, actual);
    }

    @Test
    public void testParsing() throws Exception {
        final URL archive = Handler.formatURL(new URL(Archives.FILE.concat(":/root.jar")), "level1.jar", "level2.jar", "some/path");
        assert Objects.equals(Archives.Nested.PROTOCOL, archive.getProtocol()) : archive;
    }

    private void assertContent(final boolean cached, final String content, final String... path) throws IOException {
        try (final InputStream input = Archives.open(Handler.formatURL(container, path), cached)) {
            verify(content, IOStreams.load(input, Strings.ASCII, BUFFER).replaceAll("\n", ""));
        }
    }
}
