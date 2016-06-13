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

package org.fluidity.foundation.security;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * Security related convenience functions.
 * <h3>Usage</h3>
 * <h4>Privileged access</h4>
 * <pre>
 * final PrivilegedAction&lt;T&gt; action = &hellip;
 * final T result = <span class="hl1">Security</span>.{@linkplain #invoke(PrivilegedAction) invoke}(action);
 *
 * try {
 *     return <span class="hl1">Security</span>.{@linkplain #invoke(Class, PrivilegedExceptionAction) invoke}(<span class="hl2">SomeCheckedException</span>.class, &hellip;);
 * } catch (final <span class="hl2">SomeCheckedException</span> e) {
 *     &hellip;
 * }
 * </pre>
 * <h4>Object Accessibility</h4>
 * <pre>
 * final {@linkplain java.lang.reflect.Method Method} method = <span class="hl1">Security</span>.{@linkplain Security#access(AccessibleObject) access}(&hellip;);
 *
 * method.{@linkplain java.lang.reflect.Method#invoke(Object, Object...) invoke}(&hellip;);
 * </pre>
 *
 * @author Tibor Varga
 */
@SuppressWarnings("WeakerAccess")
public final class Security {

    private Security() {
        throw new UnsupportedOperationException(String.format("No instance allowed of %s", getClass()));
    }

    /**
     * Convenience method to safely make some object {@linkplain AccessibleObject#setAccessible(boolean) accessible}.
     * <p>
     * If the given <code>object</code> is already {@linkplain AccessibleObject#isAccessible() accessible}, this method simply returns it.
     * <p>
     * If the <code>object</code> is not {@linkplain AccessibleObject#isAccessible() accessible}, this method {@linkplain
     * AccessibleObject#setAccessible(boolean) makes it so} in a {@link PrivilegedAction}.
     *
     * @param object the object to process.
     * @param <T>    the object's type.
     *
     * @return a {@link PrivilegedAction} or <code>null</code>, as described above.
     */
    public static <T extends AccessibleObject> T access(final T object) {
        if (object.isAccessible()) {
            return object;
        } else {
            return AccessController.doPrivileged((PrivilegedAction<T>) () -> {
                object.setAccessible(true);
                return object;
            });
        }
    }

    /**
     * Simply invokes {@link AccessController#doPrivileged(PrivilegedAction)} with <code>action</code>.
     *
     * @param action the privileged action to invoke.
     * @param <T>    the return type of <code>action</code>.
     *
     * @return the result of invoking <code>action</code>.
     */
    public static <T> T invoke(final PrivilegedAction<T> action) {
        return AccessController.doPrivileged(action);
    }

    /**
     * Invokes either {@link AccessController#doPrivileged(PrivilegedAction, AccessControlContext)} or {@link
     * AccessController#doPrivileged(PrivilegedAction, AccessControlContext, Permission...)} with <code>action</code>.
     *
     * @param <T>         the return type of <code>action</code>.
     * @param context     see {@link AccessController#doPrivileged(PrivilegedAction, AccessControlContext)}.
     * @param action      the privileged action to invoke.
     * @param permissions see {@link AccessController#doPrivileged(PrivilegedAction, AccessControlContext, Permission...)}.
     *
     * @return the result of invoking <code>action</code>.
     */
    public static <T> T invoke(final AccessControlContext context, final PrivilegedAction<T> action, final Permission... permissions) {
        return permissions.length == 0
               ? AccessController.doPrivileged(action, context)
               : AccessController.doPrivileged(action, context, permissions);
    }

    /**
     * Simply invokes {@link AccessController#doPrivileged(PrivilegedExceptionAction)} with <code>action</code>.
     *
     * @param error  the highest level {@link Exception} class that <code>action</code> can throw.
     * @param action the privileged action to invoke.
     * @param <T>    the return type of <code>action</code>.
     * @param <E>    the highest level {@link Exception} type that <code>action</code> can throw.
     *
     * @return the result of invoking <code>action</code>.
     *
     * @throws E whatever <code>action</code>.
     */
    public static <T, E extends Exception> T invoke(final Class<E> error, final PrivilegedExceptionAction<T> action) throws E {
        return _invoke(error, () -> AccessController.doPrivileged(action));
    }

    /**
     * Invokes either {@link AccessController#doPrivileged(PrivilegedExceptionAction, AccessControlContext)} or {@link
     * AccessController#doPrivileged(PrivilegedExceptionAction, AccessControlContext, Permission...)} with <code>action</code>.
     *
     * @param error  the highest level {@link Exception} class that <code>action</code> can throw.
     * @param context     see {@link AccessController#doPrivileged(PrivilegedExceptionAction, AccessControlContext)}.
     * @param action      the privileged action to invoke.
     * @param permissions see {@link AccessController#doPrivileged(PrivilegedExceptionAction, AccessControlContext, Permission...)}.
     * @param <T>         the return type of <code>action</code>.
     * @param <E>         the highest level {@link Exception} type <code>action</code> can throw.
     *
     * @return the result of invoking <code>action</code>.
     *
     * @throws E whatever <code>action</code>.
     */
    public static <T, E extends Exception> T invoke(final Class<E> error,
                                                    final AccessControlContext context,
                                                    final PrivilegedExceptionAction<T> action,
                                                    final Permission... permissions) throws E {
        return _invoke(error,
                       () -> permissions.length == 0
                          ? AccessController.doPrivileged(action, context)
                          : AccessController.doPrivileged(action, context, permissions));
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Exception> T _invoke(final Class<E> error, final Action<T> action) throws E {
        try {
            try {
                return action.invoke();
            } catch (final PrivilegedActionException e) {
                throw e.getException();
            }
        } catch (final Exception e) {
            if (error.isAssignableFrom(e.getClass())) {
                throw (E) e;
            } else {
                throw new UndeclaredThrowableException(e);
            }
        }
    }

    /**
     * Internal interface to generalize invocable actions.
     *
     * @param <T> the return type of {@link #invoke()}.
     *
     * @author Tibor Varga
     */
    interface Action<T> {

        /**
         * Invokes the action.
         *
         * @return what needs to be returned.
         *
         * @throws Exception when a problem occurs.
         */
        T invoke() throws Exception;
    }
}
