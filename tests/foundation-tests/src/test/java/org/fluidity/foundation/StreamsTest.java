/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.foundation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import org.fluidity.testing.Simulator;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class StreamsTest extends Simulator {

    @Test
    public void testCopying() throws Exception {
        final byte[] buffer = new byte[128];
        final String original = "some rubbish to copy";

        final ByteArrayOutputStream stream = Streams.copy(new ByteArrayInputStream(original.getBytes()), new ByteArrayOutputStream(), buffer, true, true);

        final String copy = new String(stream.toByteArray());
        assert original.equals(copy) : copy;
    }

    @Test
    public void testChainedCopying() throws Exception {
        final byte[] buffer = new byte[128];
        final String[] input = "Hello World!".split("\\b");

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final StringBuilder format = new StringBuilder();

        for (final String text : input) {
            format.append("%s");

            Streams.copy(new ByteArrayInputStream(text.getBytes()), stream, buffer, true, false);
        }

        final String copy = new String(stream.toByteArray());
        assert String.format(format.toString(), (Object[]) input).equals(copy) : copy;
    }

    @Test
    public void testClosing() throws Exception {
        final Closeable closeable = arguments().normal(Closeable.class);

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

        verify((Task) () -> Streams.copy(new ByteArrayInputStream("".getBytes()), output, buffer, true, false));

        test(() -> {
            closeable.close();

            verify((Task) () -> Streams.copy(new ByteArrayInputStream("".getBytes()), output, buffer, true, true));
        });
    }
}
