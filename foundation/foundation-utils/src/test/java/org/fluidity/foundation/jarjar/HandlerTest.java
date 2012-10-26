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
import java.net.MalformedURLException;
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

    private final String container = "level0.jar";
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
        assertContent(caching, container, "level 0", "level0.txt");
        assertContent(caching, container, "level 1", "level1-1.jar", "level1.txt");
        assertContent(caching, container, "level 1", "level1-2.jar", "level1.txt");
        assertContent(caching, container, "level 2", "level1-1.jar", "level2.jar", "level2.txt");
        assertContent(caching, container, "level 2", "level1-2.jar", "level2.jar", "level2.txt");
        assertContent(caching, container, "level 3", "level1-1.jar", "level2.jar", "level3.jar", "level3.txt");
        assertContent(caching, container, "level 3", "level1-2.jar", "level2.jar", "level3.jar", "level3.txt");
    }

    @Test(dataProvider = "caching")
    public void testURL(final boolean caching) throws Exception {
        assertContent(caching, container, "level 0", "level0.txt");
        assertContent(caching, container, "level 1", "level1-1.jar", "level1.txt");
        assertContent(caching, container, "level 1", "level1-2.jar", "level1.txt");
        assertContent(caching, container, "level 2", "level1-1.jar", "level2.jar", "level2.txt");
        assertContent(caching, container, "level 2", "level1-2.jar", "level2.jar", "level2.txt");
        assertContent(caching, container, "level 3", "level1-1.jar", "level2.jar", "level3.jar", "level3.txt");
        assertContent(caching, container, "level 3", "level1-2.jar", "level2.jar", "level3.jar", "level3.txt");

        {
            final URL root = getClass().getClassLoader().getResource(container);
            final URL level1 = Handler.formatURL(root, null, "level1-2.jar");
            final URL level2 = Handler.formatURL(level1, null, "level2.jar");
            final URL level3 = Handler.formatURL(level2, null, "level3.jar");

            final URLConnection connection = Handler.formatURL(level3, null, "level3.txt").openConnection();
            connection.setUseCaches(caching);

            final String expected = "level 3";
            final String actual = Streams.load(connection.getInputStream(), "ASCII", BUFFER, true).replaceAll("\n", "");

            assert expected.equals(actual) : String.format("Expected '%s', got '%s'", expected, actual);
        }
    }

    @Test
    public void testCaching() throws Exception {
        final URL url = Handler.formatURL(getClass().getClassLoader().getResource(container), null, "level1-2.jar", "level2.jar", "level3.jar", "level3.txt");

        Handler.load(new URL(url.getFile()), null);
        Archives.Nested.unload(url);
    }

    @Test
    public void testFormatting() throws Exception {
        final URL expected = Handler.formatURL(getClass().getClassLoader().getResource(container), null, "level1-2.jar", "level2.jar", "level3.jar", null);
        verify(expected, Handler.formatURL(Handler.rootURL(expected, null), null, "level1-2.jar", "level2.jar", "level3.jar", null));
        verify(expected, Handler.formatURL(Handler.formatURL(Handler.rootURL(expected, null), null, "level1-2.jar", "level2.jar", null), null, "level3.jar", null));
        verify(expected, Handler.formatURL(Handler.formatURL(Handler.formatURL(Handler.rootURL(expected, null), null, "level1-2.jar", null), null, "level2.jar", null), null, "level3.jar", null));

        final URL level3 = Handler.formatURL(Handler.formatURL(Handler.formatURL(Handler.rootURL(expected, null), null, "level1-2.jar"), null, "level2.jar"), null, "level3.jar");
        verify(expected, Handler.formatURL(level3, null, (String) null));
        verify(expected, Handler.formatURL(level3, null, (String[]) null));
    }

    @Test
    public void testExploration() throws Exception {
        final List<String> files = new ArrayList<String>();

        Archives.read(getClass().getClassLoader().getResource(container), new Archives.Entry() {
            public boolean matches(final URL url, final JarEntry entry) throws IOException {
                final String name = entry.getName();

                if (name.endsWith(".txt")) {
                    files.add(name);
                }

                return true;
            }

            public boolean read(final URL url, final JarEntry entry, final InputStream stream) throws IOException {
                Archives.read(Archives.Nested.formatURL(url, entry.getName()), this);
                return true;
            }
        });

        final List expected = Arrays.asList("level0.txt", "level1.txt", "level2.txt", "level3.txt", "level1.txt", "level2.txt", "level3.txt");
        assert files.equals(expected) : String.format("Expected %s, got %s", expected, files);
    }

    private void verify(final URL expected, final URL actual) {
        assert expected.equals(actual) : String.format("%nExpected %s,%n     got %s", expected, actual);
    }

    @Test
    public void testParsing() throws Exception {
        final String root = "file:/root.jar";
        final String level1 = "level1.jar";
        final String level2 = "level2.jar";
        final String some = "some";
        final String path = some.concat("/path");
        final String folder = path.concat("/");
        final URL folderBase = Handler.formatURL(new URL(root), null, level1, level2, folder);
        final URL fileBase = Handler.formatURL(new URL(root), null, level1, level2, path);

        final String relative = "relative/path";
        checkURL(folderBase, String.format("jar:%s:%s%s%s%s%s%s%s/%s", Handler.PROTOCOL, root, Handler.DELIMITER, level1, Handler.DELIMITER, level2, "!/", path, relative), relative);
        checkURL(fileBase, String.format("jar:%s:%s%s%s%s%s%s%s/%s", Handler.PROTOCOL, root, Handler.DELIMITER, level1, Handler.DELIMITER, level2, "!/", some, relative), relative);

        final String backtrack = "../".concat(relative);
        checkURL(folderBase, String.format("jar:%s:%s%s%s%s%s%s%s/%s", Handler.PROTOCOL, root, Handler.DELIMITER, level1, Handler.DELIMITER, level2, "!/", some, relative), backtrack);
        checkURL(fileBase, String.format("jar:%s:%s%s%s%s%s%s%s", Handler.PROTOCOL, root, Handler.DELIMITER, level1, Handler.DELIMITER, level2, "!/", relative), backtrack);

        final String absolute = "/absolute/path";
        checkURL(folderBase, String.format("jar:%s:%s%s%s%s%s%s%s", Handler.PROTOCOL, root, Handler.DELIMITER, level1, Handler.DELIMITER, level2, "!/", absolute.substring(1)), absolute);
        checkURL(fileBase, String.format("jar:%s:%s%s%s%s%s%s%s", Handler.PROTOCOL, root, Handler.DELIMITER, level1, Handler.DELIMITER, level2, "!/", absolute.substring(1)), absolute);
    }

    private void checkURL(final URL base, final String expected, final String override) throws MalformedURLException {
        final String actual = new URL(base, override).toExternalForm();
        assert expected.equals(actual) : String.format("Expected '%s', got '%s'", expected, actual);
    }

    private void assertContent(final boolean caching, final String container, final String content, final String... path) throws IOException {
        final URLConnection connection = Handler.formatURL(getClass().getClassLoader().getResource(container), null, path).openConnection();
        connection.setUseCaches(caching);

        final String loaded = Streams.load(connection.getInputStream(), "ASCII", BUFFER, true).replaceAll("\n", "");
        assert content.equals(loaded) : String.format("Expected '%s', got '%s'", content, loaded);
    }
}
