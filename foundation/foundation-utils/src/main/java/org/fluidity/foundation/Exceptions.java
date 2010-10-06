/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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

/**
 * Provides exception related common functionality.
 *
 * @author Tibor Varga
 */
public abstract class Exceptions {

    /**
     * Retrows {@link RuntimeException}s and wraps other {@link Exception}s in a {@link RuntimeException}.
     *
     * @param context the action part of the "Error %s" message in the wrapping exception.
     * @param command the command to run.
     *
     * @return whatever the command returns.
     */
    public static <T> T wrap(final String context, final Command<T> command) {
        try {
            return command.run();
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw context == null ? new RuntimeException(e) : new RuntimeException(String.format("Error %s", context), e);
        }
    }

    /**
     * Retrows {@link RuntimeException}s and wraps other {@link Exception}s in a {@link RuntimeException}.
     *
     * @param command the command to run.
     *
     * @return whatever the command returns.
     */
    public static <T> T wrap(final Command<T> command) {
        return wrap(null, command);
    }

    /**
     * The command to run and wrap the exceptions thrown therefrom.
     */
    public static interface Command<T> {

        /**
         * Code to run and wrap the exceptions therefrom.
         *
         * @return whatever the caller of the {@link org.fluidity.foundation.Exceptions#wrap(org.fluidity.foundation.Exceptions.Command)} or {@link
         *         org.fluidity.foundation.Exceptions#wrap(String,org.fluidity.foundation.Exceptions.Command)} wishes to receive.
         *
         * @throws Exception to turn to {@link RuntimeException} if necessary.
         */
        T run() throws Exception;
    }
}
