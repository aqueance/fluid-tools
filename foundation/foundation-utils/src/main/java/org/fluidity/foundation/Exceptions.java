/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * Utility methods on exceptions.
 * <h3>Usage</h3>
 * <pre>
 * void someMethod() throws <span class="hl2">CheckedException1</span>, <span class="hl2">CheckedException2</span>, <span class="hl2">CheckedException3</span> {
 *   try {
 *     ...
 *     <span class="hl1">{@linkplain Exceptions}.wrap(new {@linkplain Exceptions.Command}</span>&lt;Void> {
 *       ...
 *       return null;
 *     }<span class="hl1">)</span>;
 *     ...
 *   } catch (final <span class="hl1">Exceptions.Wrapper</span> wrapper) {
 *     throw wrapper
 *         .<span class="hl1">rethrow(</span><span class="hl2">CheckedException1</span>.class<span class="hl1">)</span>
 *         .<span class="hl1">rethrow(</span><span class="hl2">CheckedException2</span>.class<span class="hl1">)</span>
 *         .<span class="hl1">rethrow(</span><span class="hl2">CheckedException3</span>.class<span class="hl1">)</span>;
 *   }
 * }
 * </pre>
 * <p/>
 * The above will re-throw either <code>CheckedException1</code>, <code>CheckedException2</code>,
 * <code>CheckedException3</code> or the <code>wrapper</code>, which is an {@linkplain RuntimeException unchecked} exception.
 *
 * @author Tibor Varga
 */
public final class Exceptions extends Utilities {

    private Exceptions() { }

    /**
     * Re-trows {@link RuntimeException RuntimeExceptions} and wraps other {@link Exception Exceptions} in a {@link Wrapper}.
     *
     * @param context the action part of the "Error %s" message in the wrapper exception.
     * @param command the command to run.
     *
     * @return whatever the command returns.
     */
    public static <T> T wrap(final String context, final Command<T> command) {
        try {
            try {
                return command.run();
            } catch (final Exception e) {
                throw unwrap(e);
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Error e) {
            throw e;
        } catch (final Throwable e) {
            throw context == null ? new Wrapper(e) : new Wrapper(e, "Error %s", context);
        }
    }

    /**
     * Re-trows {@link RuntimeException RuntimeExceptions} and wraps other {@link Exception Exceptions} in a {@link Wrapper}.
     *
     * @param command the command to run.
     *
     * @return whatever the command returns.
     */
    public static <T> T wrap(final Command<T> command) {
        return wrap(null, command);
    }

    private static Throwable unwrap(final Exception error) {
        Throwable cause = error;

        for (Class type = cause.getClass();
             cause.getCause() != null && (cause instanceof UndeclaredThrowableException || cause instanceof InvocationTargetException || type == RuntimeException.class);
             cause = cause.getCause(), type = cause.getClass()) {
            // empty
        }

        return cause;
    }

    /**
     * Used by {@link Exceptions}, this is a command to run and wrap the exceptions thrown therefrom.
     */
    public interface Command<T> {

        /**
         * Code to run and wrap the exceptions therefrom.
         *
         * @return whatever the caller of the {@link Exceptions#wrap(Command)} or {@link Exceptions#wrap(String, Command)} wishes to
         *         receive.
         *
         * @throws Throwable to turn to {@link RuntimeException} if necessary.
         */
        T run() throws Throwable;
    }

    /**
     * An unchecked exception that wraps a checked exception. Thrown by {@link Exceptions#wrap(Exceptions.Command)} and {@link Exceptions#wrap(String,
     * Exceptions.Command)}.
     */
    public static final class Wrapper extends RuntimeException {

        Wrapper(final Throwable cause) {
            super(cause);
        }

        Wrapper(final Throwable cause, final String format, final Object... args) {
            super(String.format(format, args), cause);
        }

        /**
         * If the wrapped exception is of the given type, it is thrown, otherwise this instance is returned. The intended usage is:
         * <h3>Usage</h3>
         * See {@link Exceptions} for an example.
         *
         * @param accept the class of the exception to check.
         * @param <T>    the type of the exception to check.
         *
         * @return returns itself if the wrapped exception is not of the given type.
         *
         * @throws T the wrapped exception if its class is assignable to the given type.
         */
        @SuppressWarnings("unchecked")
        public <T extends Throwable> Wrapper rethrow(final Class<T> accept) throws T {
            final Throwable cause = getCause();
            assert cause != null : this;

            if (accept.isAssignableFrom(cause.getClass())) {
                throw (T) cause;
            } else {
                return this;
            }
        }
    }
}
