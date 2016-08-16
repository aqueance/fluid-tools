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
import java.io.InputStream;
import java.util.stream.Stream;

import org.fluidity.testing.Simulator;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class StreamsTest extends Simulator {

    @Test
    public void testCopying() throws Exception {
        final String original = "some text to copy";

        try (final InputStream input = new ByteArrayInputStream(original.getBytes());
             final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            final String copy = new String(Streams.pipe(input, output, new byte[128]).toByteArray());

            assert original.equals(copy) : copy;
        }
    }

    @Test
    public void testChainedCopying() throws Exception {
        final String[] content = "This a sentence with several words in it".split("\\b");

        final byte[] bytes = Stream
                .of(content)
                .reduce(new ByteArrayOutputStream(),
                        (stream, text) -> Exceptions.wrap(() -> Streams.pipe(new ByteArrayInputStream(text.getBytes()), stream, new byte[128])),
                        (stream, ignored) -> stream)
                .toByteArray();

        final String copy = new String(bytes);

        assert String.join("", (CharSequence[]) content).equals(copy) : copy;
    }
}
