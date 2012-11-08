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

package org.fluidity.foundation.jarjar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.Streams;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.fluidity.foundation.Command.Job;

/**
 * @author Tibor Varga
 */
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
    public void testHandler(final boolean caching) throws Exception {
        assertContent(caching, "level 0", "level0.txt");
        assertContent(caching, "level 1", "level1-1.jar", "level1.txt");
        assertContent(caching, "level 1", "level1-2.jar", "level1.txt");
        assertContent(caching, "level 2", "level1-1.jar", "level2.jar", "level2.txt");
        assertContent(caching, "level 2", "level1-2.jar", "level2.jar", "level2.txt");
        assertContent(caching, "level 3", "level1-1.jar", "level2.jar", "level3.jar", "level3.txt");
        assertContent(caching, "level 3", "level1-2.jar", "level2.jar", "level3.jar", "level3.txt");
    }

    @Test(dataProvider = "caching")
    public void testURL(final boolean caching) throws Exception {
        assertContent(caching, "level 0", "level0.txt");
        assertContent(caching, "level 1", "level1-1.jar", "level1.txt");
        assertContent(caching, "level 1", "level1-2.jar", "level1.txt");
        assertContent(caching, "level 2", "level1-1.jar", "level2.jar", "level2.txt");
        assertContent(caching, "level 2", "level1-2.jar", "level2.jar", "level2.txt");
        assertContent(caching, "level 3", "level1-1.jar", "level2.jar", "level3.jar", "level3.txt");
        assertContent(caching, "level 3", "level1-2.jar", "level2.jar", "level3.jar", "level3.txt");

        {
            final URL root = container;
            final URL level1 = Handler.formatURL(root, "level1-2.jar");
            final URL level2 = Handler.formatURL(level1, "level2.jar");
            final URL level3 = Handler.formatURL(level2, "level3.jar");

            final URLConnection connection = Handler.formatURL(level3, "level3.txt").openConnection();
            connection.setUseCaches(caching);

            verify("level 3", Streams.load(connection.getInputStream(), "ASCII", BUFFER, true).replaceAll("\n", ""));
        }
    }

    @Test
    public void testSingleThreadedCaching() throws Exception {
        final URL url1 = Handler.formatURL(container, "level1-1.jar", "level2.jar", "level3.jar", "level3.txt");
        final URL url2 = Handler.formatURL(samples, "META-INF/dependencies/dependency-1.jar");

        Handler.contents(url1);
        assert Handler.loaded(url1, true);
        assert Handler.loaded(url1, false);

        Archives.Nested.access(new Job<IOException>() {
            public void run() throws IOException {
                assert Handler.loaded(url1, true);
                assert Handler.loaded(url1, false);

                Archives.Nested.access(new Job<IOException>() {
                    public void run() throws IOException {
                        assert Handler.loaded(url1, true);
                        assert Handler.loaded(url1, false);

                        Handler.contents(url2);
                        assert Handler.loaded(url2, true);
                        assert !Handler.loaded(url2, false);

                        Archives.Nested.unload(url1);
                        assert !Handler.loaded(url1, true);
                        assert Handler.loaded(url1, false);

                        assert Handler.loaded(url2, true);
                        assert !Handler.loaded(url2, false);
                        Archives.Nested.unload(url2);
                        assert !Handler.loaded(url2, true);
                        assert !Handler.loaded(url2, false);
                    }
                });

                assert Handler.loaded(url1, true);
                assert Handler.loaded(url1, false);

                Archives.Nested.unload(url1);
                assert !Handler.loaded(url1, true);
                assert Handler.loaded(url1, false);
            }
        });

        assert Handler.loaded(url1, true);
        assert Handler.loaded(url1, false);

        Archives.Nested.unload(url1);
        assert !Handler.loaded(url1, true);
        assert !Handler.loaded(url1, false);
    }

    @Test
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void testMultiThreadedCaching() throws Exception {
        final URL url1 = Handler.formatURL(container, "level1-1.jar", "level2.jar", "level3.jar", "level3.txt");
        final URL url2 = Handler.formatURL(samples, "META-INF/dependencies/dependency-1.jar");

        final CyclicBarrier barrier = new CyclicBarrier(2);
        final AtomicBoolean running = new AtomicBoolean();
        final AtomicReference<Exception> error = new AtomicReference<Exception>();

        final Thread thread = new Thread() {
            @Override
            public void run() {
                running.set(true);

                try {
                    Archives.Nested.access(new Job<Exception>() {
                        public void run() throws Exception {
                            barrier.await(100, TimeUnit.MILLISECONDS);

                            assert Handler.loaded(url1, true);

                            Handler.contents(url2);
                            assert Handler.loaded(url2, true);

                            Archives.Nested.unload(url1);
                            assert !Handler.loaded(url1, true);

                            assert Handler.loaded(url2, true);
                            Archives.Nested.unload(url2);
                            assert !Handler.loaded(url2, true);
                        }
                    });
                } catch (final Exception e) {
                    error.set(e);
                }
            }
        };

        try {
            Archives.Nested.access(new Job<Exception>() {
                public void run() throws Exception {
                    Handler.contents(url1);
                    assert Handler.loaded(url1, true);

                    thread.start();
                    barrier.await(100, TimeUnit.MILLISECONDS);
                    assert running.get();

                    Archives.Nested.unload(url1);
                    assert !Handler.loaded(url1, true);
                }
            });
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

    @Test
    public void testExploration() throws Exception {
        final List<String> files = new ArrayList<String>();

        Archives.read(true, container, new Archives.Entry() {
            public boolean matches(final URL url, final JarEntry entry) throws IOException {
                final String name = entry.getName();

                if (name.endsWith(".txt")) {
                    files.add(name);
                }

                return true;
            }

            public boolean read(final URL url, final JarEntry entry, final InputStream stream) throws IOException {
                Archives.read(true, Archives.Nested.formatURL(url, entry.getName()), this);
                return true;
            }
        });

        verify(Arrays.asList("level0.txt", "level1.txt", "level2.txt", "level3.txt", "level1.txt", "level2.txt", "level3.txt"), files);
    }

    @Test
    public void testRelativeURLs() throws Exception {
        verify(Handler.formatURL(container, "level1-2.jar", "level2.jar", "level2.txt"),
               new URL(Handler.formatURL(container, "level1-2.jar", "level2.jar", "level3.jar"), "level2.txt"));
        verify(Handler.formatURL(container, "level2-2.jar"),
               new URL(Handler.formatURL(container, "level1-2.jar"), "level2-2.jar"));
    }

    private void verify(final Object expected, final Object actual) {
        assert expected.equals(actual) : String.format("%nExpected %s,%n     got %s", expected, actual);
    }

    @Test
    public void testParsing() throws Exception {
        final URL archive = Handler.formatURL(new URL("file:/root.jar"), "level1.jar", "level2.jar", "some/path");
        assert Archives.Nested.PROTOCOL.equals(archive.getProtocol()) : archive;
    }

    private void assertContent(final boolean caching, final String content, final String... path) throws IOException {
        verify(content, Streams.load(Archives.open(caching, Handler.formatURL(container, path)), "ASCII", BUFFER, true).replaceAll("\n", ""));
    }
}
