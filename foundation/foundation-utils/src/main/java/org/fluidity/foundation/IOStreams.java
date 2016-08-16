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
import java.nio.charset.Charset;
import java.util.function.Consumer;

/**
 * General I/O stream related convenience methods.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("WeakerAccess")
public final class IOStreams extends Utility {

    private IOStreams() { }

    /**
     * Fully copies the contents of one stream into another. Neither stream will be closed; use the try-with-resource construct to close them automatically.
     *
     * @param input  the input stream to copy data from.
     * @param output the output stream to copy data to.
     * @param buffer the buffer to use.
     * @param <T>    type of the received and returned <code>output</code> stream.
     *
     * @return the output stream.
     *
     * @throws IOException thrown when reading or writing fails.
     */
    public static <T extends OutputStream> T pipe(final InputStream input, final T output, final byte[] buffer) throws IOException {
        int len;

        while ((len = input.read(buffer)) != -1) {
            output.write(buffer, 0, len);
        }

        return output;
    }

    /**
     * Sends the contents of input stream to the consumer. Neither stream will be closed; use the try-with-resource construct to close them automatically.
     *
     * @param input    the input stream to copy data from.
     * @param buffer   the buffer to use.
     * @param consumer the consumer to send data to; the data is sent in the specified buffer, and the consumer receives the number of content bytes in that
     *                 buffer.
     *
     * @throws IOException thrown when reading fails.
     */
    public static void send(final InputStream input, final byte[] buffer, final Consumer<Integer> consumer) throws IOException {
        int len;

        while ((len = input.read(buffer)) != -1) {
            consumer.accept(len);
        }
    }

    /**
     * Reads into a byte array all content from the given <code>stream</code>. The stream will <b>not</b> be closed; use the try-with-resource construct to
     * close it automatically.
     *
     * @param stream the stream to load the contents of; may not be <code>null</code>.
     * @param buffer the buffer to use; may not be <code>null</code>.
     *
     * @return the contents of the given <code>stream</code>.
     *
     * @throws IOException thrown when reading fails.
     */
    public static byte[] load(final InputStream stream, final byte[] buffer) throws IOException {
        return IOStreams.pipe(stream, new ByteArrayOutputStream(), buffer).toByteArray();
    }

    /**
     * Reads into a text string all content from the given <code>stream</code>. The stream will <b>not</b> be closed; use the try-with-resource construct to
     * close it automatically.
     *
     * @param stream  the stream to load the contents of; may not be <code>null</code>.
     * @param charset the character set of the stream; may not be <code>null</code>.
     * @param buffer  the buffer to use; may not be <code>null</code>.
     *
     * @return the contents of the given <code>stream</code>.
     *
     * @throws IOException thrown when reading fails.
     */
    public static String load(final InputStream stream, final Charset charset, final byte[] buffer) throws IOException {
        return new String(IOStreams.load(stream, buffer), charset);
    }

    /**
     * Writes the given <code>data</code> to the given <code>stream</code>. The stream will <b>not</b> be closed; use the try-with-resource construct to close
     * it automatically.
     *
     * @param stream the stream to save to; may not be <code>null</code>.
     * @param data   the contents to save; may not be <code>null</code>.
     * @param buffer the buffer to use; may not be <code>null</code>.
     *
     * @throws IOException thrown when writing fails.
     */
    public static void store(final OutputStream stream, final byte[] data, final byte[] buffer) throws IOException {
        IOStreams.pipe(new ByteArrayInputStream(data), stream, buffer);
    }

    /**
     * Writes the given <code>text</code> to the given <code>stream</code>. The stream will <b>not</b> be closed; use the try-with-resource construct to close
     * it automatically.
     *
     * @param stream  the stream to save to; may not be <code>null</code>.
     * @param text    the contents to save; may not be <code>null</code>.
     * @param charset the character set of the <code>text</code>; may not be <code>null</code>.
     * @param buffer  the buffer to use; may not be <code>null</code>.
     *
     * @throws IOException thrown when writing fails.
     */
    public static void store(final OutputStream stream, final String text, final Charset charset, final byte[] buffer) throws IOException {
        IOStreams.store(stream, text.getBytes(charset), buffer);
    }
}
