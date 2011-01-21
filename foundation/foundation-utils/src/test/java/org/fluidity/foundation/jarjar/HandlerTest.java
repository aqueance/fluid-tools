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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class HandlerTest {
    private final Handler handler = new Handler();

    @Test
    public void testHandler() throws Exception {
        final String container = "level0.jar";

        assertContent(container, "/level0.txt", "level 0");
        assertContent(container, "/level1-1.jar!/level1.txt", "level 1");
        assertContent(container, "/level1-2.jar!/level1.txt", "level 1");
        assertContent(container, "/level1-1.jar!/level2.jar!/level2.txt", "level 2");
        assertContent(container, "/level1-2.jar!/level2.jar!/level2.txt", "level 2");
    }

    private void assertContent(final String container, final String file, final String content) throws IOException {
        final URL root = getClass().getClassLoader().getResource(container);
        final URLConnection connection = handler.openConnection(Handler.formatURL(root, file));
        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

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
