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
 * Provides exception related common functionality.
 *
 * @author Tibor Varga
 */
public abstract class Exceptions {

    /**
     * Re-trows {@link RuntimeException}s and wraps other {@link Exception}s in a {@link RuntimeException}.
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
     * Re-trows {@link RuntimeException}s and wraps other {@link Exception}s in a {@link RuntimeException}.
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
     * The command to run and wrap the exceptions thrown therefrom.
     */
    public interface Command<T> {

        /**
         * Code to run and wrap the exceptions therefrom.
         *
         * @return whatever the caller of the {@link Exceptions#wrap(Exceptions.Command)} or {@link Exceptions#wrap(String, Exceptions.Command)} wishes to
         *         receive.
         *
         * @throws Throwable to turn to {@link RuntimeException} if necessary.
         */
        T run() throws Throwable;
    }

    /**
     * An unchecked exception that wraps a checked exception.
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
         * <pre>
         * try {
         *   ...
         *   Exceptions.wrap(new Exceptions.Command&lt;Void> {
         *     ...
         *      return null;
         *   });
         *   ...
         * } catch (final Exceptions.Wrapper wrapper) {
         *   throw wrapper
         *       .rethrow(ExpectedCheckedException1.class)
         *       .rethrow(ExpectedCheckedException2.class)
         *       .rethrow(ExpectedCheckedException3.class);
         * }
         * </pre>
         * <p/>
         * The above will throw re-throw either <code>ExpectedCheckedException1</code>, <code>ExpectedCheckedException2</code>,
         * <code>ExpectedCheckedException3</code> or <code>wrapper</code>, which is an unchecked exception.
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
