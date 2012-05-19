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

package org.fluidity.foundation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * General stream related convenience methods.
 *
 * @author Tibor Varga
 */
public final class Streams extends Utility {

    private Streams() { }

    /**
     * Fully copies the contents of one stream into another. The input stream is closed afterwards.
     *
     * @param input  the input stream to copy data from.
     * @param output the output stream to copy data to.
     * @param buffer the buffer to use.
     * @param close  closes the output stream if <code>true</code>.
     *
     * @return the output stream.
     *
     * @throws IOException thrown when reading or writing fails.
     */
    public static <T extends OutputStream> T copy(final InputStream input, final T output, final byte[] buffer, final boolean close) throws IOException {
        try {
            int len;
            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }

            return output;
        } finally {
            try {
                input.close();
            } catch (final IOException e) {
                // ignore
            }

            if (close) {
                try {
                    output.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Loads into a byte array all content from the given <code>stream</code>.
     *
     * @param stream the stream to load the contents of.
     * @param buffer the buffer to use.
     *
     * @return the contents of the given <code>stream</code>.
     *
     * @throws IOException thrown when reading fails.
     */
    public static byte[] load(final InputStream stream, final byte[] buffer) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        Streams.copy(stream, output, buffer, true);
        return output.toByteArray();
    }

    /**
     * Loads into a text string all content from the given <code>stream</code>.
     *
     * @param stream  the stream to load the contents of.
     * @param charset the character set of the stream.
     * @param buffer  the buffer to use.
     *
     * @return the contents of the given <code>stream</code>.
     *
     * @throws IOException thrown when reading fails.
     */
    public static String load(final InputStream stream, final String charset, final byte[] buffer) throws IOException {
        return new String(Streams.load(stream, buffer), charset);
    }

    /**
     * Saves the given <code>data</code> to the given <code>stream</code>.
     *
     * @param stream the stream to save to.
     * @param data   the contents to save.
     * @param buffer the buffer to use.
     * @param close  closes the output stream if <code>true</code>.
     *
     * @throws IOException thrown when writing fails.
     */
    public static void store(final OutputStream stream, final byte[] data, final byte[] buffer, final boolean close) throws IOException {
        Streams.copy(new ByteArrayInputStream(data), stream, buffer, close);
    }

    /**
     * Saves the given <code>text</code> to the given <code>stream</code>.
     *
     * @param stream  the stream to save to.
     * @param text    the contents to save.
     * @param charset the character set of the <code>text</code>.
     * @param buffer  the buffer to use.
     * @param close   closes the output stream if <code>true</code>.
     *
     * @throws IOException thrown when writing fails.
     */
    public static void store(final OutputStream stream, final String text, final String charset, final byte[] buffer, final boolean close) throws IOException {
        Streams.store(stream, text.getBytes(charset), buffer, close);
    }
}
