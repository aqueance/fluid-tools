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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import static org.fluidity.foundation.Command.Job;
import static org.fluidity.foundation.Command.Process;

/**
 * A utility that allows propagation of checked exceptions through method calls that don't allow those checked exceptions thrown from inside.
 * <h3>Usage</h3>
 * Let's say we need to execute, using <code>someObject</code>, some code from <code>someMethod</code> below within a {@link Runnable} command, and let's say
 * that code may throw some checked exceptions. This is how you could do that using this utility class.
 * <pre>
 * public void someMethod() throws <span class="hl2">CheckedException1</span>, <span class="hl2">CheckedException2</span>, <span class="hl2">CheckedException3</span> {
 *   final command = new Runnable() {
 *     public void run() {
 *
 *       // wrap any checked exception thrown from our code enclosed in the process
 *       <span class="hl1">Exceptions.wrap</span>(new {@linkplain Command.Process}&lt;Void, Exception>() {
 *         public Void <span class="hl1">run</span>() throws Exception {
 *           &hellip;
 *           throw new <span class="hl2">CheckedException2</span>();
 *         }
 *       });
 *     }
 *   };
 *
 *   try {
 *     someObject.run(command);
 *   } catch (final <span class="hl1">{@linkplain Exceptions.Wrapper}</span> wrapper) {
 *
 *     // unwrap the checked exceptions that we can throw
 *     throw wrapper
 *         .<span class="hl1">rethrow</span><span class="hl2">(CheckedException1</span>.class)
 *         .<span class="hl1">rethrow</span><span class="hl2">(CheckedException2</span>.class)
 *         .<span class="hl1">rethrow</span><span class="hl2">(CheckedException3</span>.class);
 *   }
 * }
 * </pre>
 * The above allows re-throwing either <code>CheckedException1</code>, <code>CheckedException2</code>, <code>CheckedException3</code>, or <code>wrapper</code>,
 * which is an {@linkplain RuntimeException unchecked} exception.
 *
 * @author Tibor Varga
 */
public final class Exceptions extends Utility {

    private Exceptions() { }

    /**
     * Executes the given command and {@linkplain Exceptions.Wrapper wraps} any exception other than {@link RuntimeException} and {@link Error} thrown
     * therefrom.
     *
     * @param action  the action part of the "Error %s" message in the wrapped exception.
     * @param command the command to run.
     * @param <T>     the generic return type of the command.
     * @param <E>     the generic exception type of the command.
     *
     * @return whatever the command returns.
     */
    public static <T, E extends Throwable> T wrap(final String action, final Process<T, E> command) {
        try {
            try {
                return command.run();
            } catch (final Exception e) {
                throw Exceptions.unwrap(e);
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Error e) {
            throw e;
        } catch (final Throwable e) {
            throw action == null ? new Wrapper(e) : new Wrapper(e, "Error %s", action);
        }
    }

    /**
     * Executes the given command and {@linkplain Exceptions.Wrapper wraps} any exception thrown therefrom.
     *
     * @param command  the command to execute.
     * @param <T>      the generic return type of the command.
     * @param <E>     the generic exception type of the command.
     *
     * @return whatever the given command returns.
     */
    public static <T, E extends Throwable> T wrap(final Process<T, E> command) {
        return Exceptions.wrap(null, command);
    }

    /**
     * Executes the given command and {@linkplain Exceptions.Wrapper wraps} any exception other than {@link RuntimeException} and {@link Error} thrown
     * therefrom.
     *
     * @param action  the action part of the "Error %s" message in the wrapped exception.
     * @param command the command to run.
     * @param <E>     the generic exception type of the command.
     */
    public static <E extends Throwable> void wrap(final String action, final Job<E> command) {
        Exceptions.wrap(action, new Process<Void, Throwable>() {
            public Void run() throws Throwable {
                command.run();
                return null;
            }
        });
    }

    /**
     * Executes the given command and {@linkplain Exceptions.Wrapper wraps} any exception thrown therefrom.
     *
     * @param command  the command to execute.
     * @param <E>     the generic exception type of the command.
     */
    public static <E extends Throwable> void wrap(final Job<E> command) {
        Exceptions.wrap(null, command);
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
     * An unchecked exception that wraps a checked exception. Thrown by {@link Exceptions#wrap(Command.Process)} and {@link Exceptions#wrap(String,
     * Command.Process)}.
     * <h3>Usage</h3>
     * See {@link Exceptions} for an example.
     */
    public static final class Wrapper extends RuntimeException {

        Wrapper(final Throwable cause) {
            super(cause);
        }

        Wrapper(final Throwable cause, final String format, final Object... args) {
            super(String.format(format, args), cause);
        }

        /**
         * If the wrapped exception is of the given type, it is thrown, otherwise this instance is returned.
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
