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

package org.fluidity.foundation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import org.fluidity.tests.MockGroupAbstractTest;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class StreamsTests extends MockGroupAbstractTest {

    @Test
    public void testCopying() throws Exception {
        final byte[] buffer = new byte[128];
        final String original = "some rubbish to copy";

        final ByteArrayOutputStream stream = Streams.copy(new ByteArrayInputStream(original.getBytes()), new ByteArrayOutputStream(), buffer, true);

        final String copy = new String(stream.toByteArray());
        assert original.equals(copy) : copy;
    }

    @Test
    public void testChainedCopying() throws Exception {
        final byte[] buffer = new byte[128];
        final String[] input = "Hello World!".split("\\b");

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final StringBuffer format = new StringBuffer();

        for (final String text : input) {
            format.append("%s");

            Streams.copy(new ByteArrayInputStream(text.getBytes()), stream, buffer, false);
        }

        final String copy = new String(stream.toByteArray());
        assert String.format(format.toString(), (Object[]) input).equals(copy) : copy;
    }

    @Test
    public void testClosing() throws Exception {
        final Closeable closeable = addLocalControl(Closeable.class);

        final OutputStream output = new OutputStream() {
            @Override
            public void write(final int ignored) throws IOException {
                assert false : "There was no input to copy";
            }

            @Override
            public void close() throws IOException {
                closeable.close();
            }
        };

        final byte[] buffer = new byte[0];

        replay();
        Streams.copy(new ByteArrayInputStream("".getBytes()), output, buffer, false);
        verify();

        closeable.close();

        replay();
        Streams.copy(new ByteArrayInputStream("".getBytes()), output, buffer, true);
        verify();
    }
}
