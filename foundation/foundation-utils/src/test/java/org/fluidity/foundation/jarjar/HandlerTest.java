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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.fluidity.foundation.Strings;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class HandlerTest {
    private final Handler handler = new Handler();
    private final String container = "level0.jar";

    private String path(final String... elements) {
        return Strings.delimited(Handler.DELIMITER, elements);
    }

    @Test
    public void testHandler() throws Exception {
        assertContent(container, path("/level0.txt"), "level 0");
        assertContent(container, path("/level1-1.jar", "level1.txt"), "level 1");
        assertContent(container, path("/level1-2.jar", "level1.txt"), "level 1");
        assertContent(container, path("/level1-1.jar", "level2.jar", "level2.txt"), "level 2");
        assertContent(container, path("/level1-2.jar", "level2.jar", "level2.txt"), "level 2");
    }

    @Test
    public void testURL() throws Exception {
        final URL root = getClass().getClassLoader().getResource(container);
        assertContent(root, path("/level0.txt"), "level 0");
        assertContent(root, path("/level1-1.jar", "level1.txt"), "level 1");
        assertContent(root, path("/level1-2.jar", "level1.txt"), "level 1");
        assertContent(root, path("/level1-1.jar", "level2.jar", "level2.txt"), "level 2");
        assertContent(root, path("/level1-2.jar", "level2.jar", "level2.txt"), "level 2");
    }

    private void assertContent(final URL root, final String file, final String content) throws IOException {
        final URL url = Handler.formatURL(root, file);
        final InputStream stream = url.openStream();
        assertContent(stream, content);
    }

    private void assertContent(final String container, final String file, final String content) throws IOException {
        final URL url = getClass().getClassLoader().getResource(container);
        final InputStream stream = handler.openConnection(Handler.formatURL(url, file)).getInputStream();
        assertContent(stream, content);
    }

    private void assertContent(final InputStream stream, final String content) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

        try {
            final String line = reader.readLine();
            assert content.equals(line) : line;
        } finally {
            try {
                reader.close();
            } catch (final IOException e) {
                // ignore
            }
        }
    }
}
