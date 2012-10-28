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
import java.util.jar.JarEntry;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.Streams;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class HandlerTest {

    private final URL container = getClass().getClassLoader().getResource("level0.jar");
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
    public void testCaching() throws Exception {
        final URL url = Handler.formatURL(container, "level1-2.jar", "level2.jar", "level3.jar", "level3.txt");

        Handler.load(new URL(url.getFile()), null);
        Archives.Nested.unload(url);
    }

    @Test
    public void testFormatting() throws Exception {
        final URL expected = Handler.formatURL(container, "level1-2.jar", "level2.jar", "level3.jar", null);
        verify(expected, Handler.formatURL(Handler.rootURL(expected), "level1-2.jar", "level2.jar", "level3.jar", null));
        verify(expected, Handler.formatURL(Handler.formatURL(Handler.rootURL(expected), "level1-2.jar", "level2.jar", null), "level3.jar", null));
        verify(expected, Handler.formatURL(Handler.formatURL(Handler.formatURL(Handler.rootURL(expected), "level1-2.jar", null), "level2.jar", null), "level3.jar", null));

        final URL level3 = Handler.formatURL(Handler.formatURL(Handler.formatURL(Handler.rootURL(expected), "level1-2.jar"), "level2.jar"), "level3.jar");
        verify(expected, Handler.formatURL(level3, (String) null));
        verify(expected, Handler.formatURL(level3, (String[]) null));

        final URL insane = Handler.formatURL(new URL("http://xxx:yyy@insane.org/whatever.jar?query=param#reference"), "level1.jar", "level2.jar", "level3.jar", null);
        verify(insane, Handler.formatURL(Handler.formatURL(Handler.formatURL(Handler.rootURL(insane), "level1.jar", null), "level2.jar", null), "level3.jar", null));
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

    private void verify(final Object expected, final Object actual) {
        assert expected.equals(actual) : String.format("%nExpected %s,%n     got %s", expected, actual);
    }

    @Test
    public void testParsing() throws Exception {
        final URL archive = Handler.formatURL(new URL("file:/root.jar"), "level1.jar", "level2.jar", "some/path");
        final URL nested = Handler.formatURL(new URL("file:/root.jar"), "level1.jar", "level2.jar", "some/path", null);

        assert Archives.PROTOCOL.equals(archive.getProtocol()) : archive;
        assert Archives.Nested.PROTOCOL.equals(nested.getProtocol()) : nested;
    }

    private void assertContent(final boolean caching, final String content, final String... path) throws IOException {
        verify(content, Streams.load(Archives.open(caching, Handler.formatURL(container, path)), "ASCII", BUFFER, true).replaceAll("\n", ""));
    }
}
