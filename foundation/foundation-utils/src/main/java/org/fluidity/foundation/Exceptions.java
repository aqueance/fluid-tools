/*
 * Copyright (c) 2006-2018 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedActionException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import org.fluidity.foundation.security.Security;

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
 *       <span class="hl1">Exceptions.wrap</span>(new {@linkplain Command.Process}&lt;Void, Exception&gt;() {
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
@SuppressWarnings("WeakerAccess")
public final class Exceptions extends Utility {

    @SuppressWarnings("unchecked")
    private static final Tunnel tunnel = new Tunnel();

    private Exceptions() { }

    /**
     * Executes {@link Tunnel#wrap(Command.Process)} with no custom wrappers {@linkplain Exceptions#checked(Class[]) specified}.
     *
     * @param command the command to execute.
     * @param <R>     the return type of the command.
     * @param <E>     the type of the exception thrown by the command.
     *
     * @return whatever the given command returns.
     */
    public static <R, E extends Exception> R wrap(final Process<R, E> command) {
        return Exceptions.tunnel.wrap((Object) null, command);
    }

    /**
     * Executes {@link Tunnel#wrap(Class, Command.Process)} with no custom wrappers {@linkplain Exceptions#checked(Class[]) specified}.
     *
     * @param wrapper the exception class to use to wrap the exception thrown by the command; may be <code>null</code>, in which case {@link Wrapper} will
     *                be used.
     * @param command the command to execute.
     * @param <R>     the return type of the command.
     * @param <E>     the type of the exception thrown by the command.
     * @param <T>     the type of the exception to wrap with.
     *
     * @return whatever the given command returns.
     *
     * @throws T as thrown by the command.
     */
    public static <R, E extends Exception, T extends Exception> R wrap(final Class<T> wrapper, final Process<R, E> command) throws T {
        return Exceptions.tunnel.wrap(wrapper, command);
    }

    /**
     * See {@link Tunnel#wrap(Object, Class, Command.Process)} with no custom wrappers {@linkplain Exceptions#checked(Class[]) specified}.
     *
     * @param label   the error message in the wrapped exception; use {@link Deferred.Label}s to defer computing the label.
     * @param wrapper the exception class to use to wrap the exception thrown by the command; may be <code>null</code>, in which case {@link Wrapper} will
     *                be used.
     * @param command the command to run.
     * @param <R>     the return type of the command.
     * @param <E>     the type of the exception thrown by the command.
     * @param <T>     the type of the exception to wrap with.
     *
     * @return whatever the command returns.
     *
     * @throws T as thrown by the command.
     */
    public static <R, E extends Exception, T extends Exception> R wrap(final Object label, final Class<T> wrapper, final Process<R, E> command) throws T {
        return Exceptions.tunnel.wrap(label, wrapper, command);
    }

    /**
     * See {@link Tunnel#unwrap(Exception)} with no custom wrappers {@linkplain Exceptions#checked(Class[]) specified}.
     *
     * @param error the head of the exception chain to unwrap.
     *
     * @return the first non-wrapper exception in the chain.
     */
    public static Throwable unwrap(final Exception error) {
        return Exceptions.tunnel.unwrap(error);
    }

    /**
     * Returns an exceptions tunnel that also unwraps the given list of wrapper exceptions and their subclasses.
     *
     * @param wrappers a list of exception types to unwrap in exception chains.
     *
     * @return an {@link Tunnel} that can unwrap the given exceptions in addition to those it already {@linkplain Tunnel#unwrap(Exception) knows} about.
     */
    @SafeVarargs
    public static Tunnel checked(final Class<? extends Exception>... wrappers) {
        return wrappers == null || wrappers.length == 0 ? tunnel : new Tunnel(wrappers);
    }

    /**
     * Implements the actual exception wrapping / unwrapping functionality described at {@link Exceptions}.
     * <h3>Usage</h3>
     * See {@link Exceptions}, substituting an instance of this class acquired through {@link Exceptions#checked(Class[]) Exceptions.checked()} in place of
     * <code>Exceptions</code> in the static method call <code>Exceptions.wrap(…)</code>.
     *
     * @author Tibor Varga
     * @see Exceptions#checked(Class[])
     */
    public static final class Tunnel {

        private final Class<Exception>[] wrappers;

        @SuppressWarnings("unchecked")
        Tunnel(final Class<? extends Exception>... wrappers) {
            final Collection<Class<? extends Exception>> list = new HashSet<>();

            list.add(UndeclaredThrowableException.class);
            list.add(InvocationTargetException.class);
            list.add(PrivilegedActionException.class);

            for (final Class<? extends Exception> type : wrappers) {
                if (type != null && !isChecked(list, type)) {
                    list.add(type);
                }
            }

            this.wrappers = (Class<Exception>[]) Lists.asArray(Class.class, list);
        }

        private boolean isChecked(final Collection<Class<? extends Exception>> list, final Class<? extends Exception> error) {
            for (final Class<? extends Exception> type : list) {
                if (type.isAssignableFrom(error)) {
                    return true;
                }
            }

            return false;
        }

        private boolean isChecked(final Class<? extends Throwable> error) {
            for (final Class<Exception> type : wrappers) {
                if (type.isAssignableFrom(error)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Executes the given command and wraps any exception thrown therefrom.
         *
         * @param wrapper the exception class to use to wrap the exception thrown by the command; may be <code>null</code>, in which case {@link Wrapper} will
         *                be used.
         * @param command the command to execute.
         * @param <R>     the return type of the command.
         * @param <E>     the type of the exception thrown by the command.
         * @param <T>     the type of the exception to wrap with.
         *
         * @return whatever the given command returns.
         *
         * @throws T as thrown by the command.
         */
        public <R, E extends Exception, T extends Exception> R wrap(final Class<T> wrapper, final Process<R, E> command) throws T {
            return wrap(null, wrapper, command);
        }

        /**
         * Executes the given command and wraps any exception thrown therefrom.
         *
         * @param command the command to execute.
         * @param <R>     the return type of the command.
         * @param <E>     the type of the exception thrown by the command.
         *
         * @return whatever the given command returns.
         */
        public <R, E extends Exception> R wrap(final Process<R, E> command) {
            try {
                return wrap(null, null, command);
            } catch (final RuntimeException error) {
                throw error;
            } catch (final Exception error) {
                throw new AssertionError(error);
            }
        }

        /**
         * Executes the given command and {@linkplain Exceptions.Wrapper wraps} any exception other than {@link RuntimeException} and {@link Error} thrown
         * therefrom.
         *
         * @param label   the error message in the wrapped exception; use {@link Deferred.Label}s to defer computing the label.
         * @param command the command to run.
         * @param <R>     the return type of the command.
         * @param <E>     the type of the exception thrown by the command.
         *
         * @return whatever the command returns.
         */
        public <R, E extends Exception> R wrap(final Object label, final Process<R, E> command) {
            try {
                return wrap(label, null, command);
            } catch (final RuntimeException error) {
                throw error;
            } catch (final Exception error) {
                throw new AssertionError(error);
            }
        }

        /**
         * Executes the given command and wraps any exception other than {@link RuntimeException} and {@link Error} thrown
         * therefrom.
         *
         * @param label   the error message in the wrapped exception; use {@link Deferred.Label}s to defer computing the label.
         * @param wrapper the exception class to use to wrap the exception thrown by the command; may be <code>null</code>, in which case {@link Wrapper} will
         *                be used.
         * @param command the command to run.
         * @param <R>     the return type of the command.
         * @param <E>     the type of the exception thrown by the command.
         * @param <T>     the type of the exception to wrap with.
         *
         * @return whatever the command returns.
         *
         * @throws T as thrown by the command.
         */
        @SuppressWarnings({ "unchecked", "ConstantConditions" })
        public <R, E extends Exception, T extends Exception> R wrap(final Object label, final Class<T> wrapper, final Process<R, E> command) throws T {
            try {
                try {
                    return command.run();
                } catch (final Exception error) {
                    throw unwrap(error);
                }
            } catch (final Error error) {
                throw error;
            } catch (final RuntimeException error) {
                if (wrapper == null || RuntimeException.class.isAssignableFrom(wrapper)) {
                    throw error;
                } else {
                    throw wrapped(label, wrapper, error);
                }
            } catch (final Throwable error) {
                final T wrapped = wrapped(label, wrapper, error);

                if (wrapper == error.getClass()) {
                    final String message = error.getMessage();
                    throw !Objects.equals(message, wrapped.getMessage()) ? wrapped : (T) error;
                } else {
                    throw wrapped;
                }
            }
        }

        @SuppressWarnings({ "ThrowableResultOfMethodCallIgnored", "unchecked" })
        private <T extends Exception> T wrapped(final Object label, final Class<T> wrapper, final Throwable error) {
            if (wrapper == null) {
                throw label == null ? new Wrapper(error) : new Wrapper(label.toString(), error);
            } else if (label == null && wrapper.isAssignableFrom(error.getClass())) {
                return (T) error;
            } else {
                try {
                    return label == null
                           ? constructor(wrapper, Throwable.class).newInstance(error)
                           : constructor(wrapper, String.class, Throwable.class).newInstance(label.toString(), error);
                } catch (final Exception _error) {
                    throw new AssertionError(_error);
                }
            }
        }

        private <T extends Exception> Constructor<T> constructor(final Class<T> type, final Class<?>... parameters) throws Exception {
            return Security.invoke(Exception.class, () -> {
                final Constructor<T> constructor = type.getDeclaredConstructor(parameters);

                if (!constructor.isAccessible()) {
                    constructor.setAccessible(true);
                }

                return constructor;
            });
        }

        /**
         * Unwraps a chain of exception wrappers from the <i>head</i> of the exception chain starting at the specified <code>error</code>. Exceptions
         * recognized as wrappers are:<ul>
         * <li>any subclass of {@link UndeclaredThrowableException},</li>
         * <li>any subclass of {@link InvocationTargetException},</li>
         * <li>any subclass of {@link PrivilegedActionException},</li>
         * <li>any subclass of any exception type passed to {@link Exceptions#checked(Class[])},</li>
         * <li><code>{@link RuntimeException}</code>,</li>
         * <li><code>{@link Exceptions.Wrapper}</code>.</li>
         * </ul>
         *
         * @param error the head of the exception chain to unwrap.
         *
         * @return the first non-wrapper exception in the chain.
         */
        @SuppressWarnings("StatementWithEmptyBody")
        public Throwable unwrap(final Exception error) {
            Throwable cause = error;

            for (Class type = cause.getClass();
                 cause.getCause() != null
                 && (isChecked(cause.getClass()) || type == RuntimeException.class || (type == Wrapper.class && !((Wrapper) cause).informative()));
                 cause = cause.getCause(), type = cause.getClass()) {
                // empty
            }

            return cause;
        }
    }

    /**
     * An unchecked exception that wraps a checked exception. Thrown by {@link Exceptions#wrap(Command.Process)} and {@link Exceptions#wrap(Object, Class,
     * Command.Process)}.
     * <h3>Usage</h3>
     * See {@link Exceptions} for an example.
     */
    public static final class Wrapper extends RuntimeException {

        private final boolean informative;

        Wrapper(final Throwable cause) {
            super(cause);
            this.informative = false;
        }

        @SuppressWarnings("UnusedDeclaration")
        Wrapper(final String message, final Throwable cause) {
            super(message, cause);
            this.informative = true;
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

        boolean informative() {
            return informative;
        }
    }
}
