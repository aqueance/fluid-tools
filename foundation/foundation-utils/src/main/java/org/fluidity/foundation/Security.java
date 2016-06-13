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

import java.lang.reflect.AccessibleObject;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

/**
 * Security related convenience functions.
 * <h3>Usage</h3>
 * <h4>Deciding whether to check access to some resource</h4>
 * <pre>
 * if (Security.{@linkplain #CONTROLLED CONTROLLED}) {
 *     {@linkplain AccessController}.{@linkplain AccessController#doPrivileged(PrivilegedAction) doPrivileged}(&hellip;);
 * } else {
 *     &hellip;
 * }
 * </pre>
 * <h4>Object Accessibility</h4>
 * <pre>
 * final {@linkplain java.lang.reflect.Method Method} method = Security.{@linkplain Security#access(AccessibleObject) access}(&hellip;);
 *
 * method.{@linkplain java.lang.reflect.Method#invoke(Object, Object...) invoke}(&hellip;);
 * </pre>
 *
 * @author Tibor Varga
 */
@SuppressWarnings("WeakerAccess")
public final class Security extends Utility {

    /**
     * The system property with which access control checks can be enabled.
     */
    public static final String PROPERTY = "org.fluidity.access-controlled";

    /**
     * Tells if access control is enabled (<code>true</code>) or not (<code>false</code>). Access control is enabled if there is a {@link SecurityManager}, or
     * the {@link #PROPERTY} is specified and its value is <code>true</code>, or any of these checks results in an {@link AccessControlException}.
     */
    public static final boolean CONTROLLED;

    static {
        boolean controlled;

        try {
            controlled = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> System.getSecurityManager() != null || Boolean.getBoolean(PROPERTY));
        } catch (final AccessControlException e) {
            controlled = true;
        }

        CONTROLLED = controlled;
    }

    // TODO: remove methods without AccessControlContext as they lend our privileges to the caller
    private Security() { }

    /**
     * Convenience method to safely set some object {@linkplain AccessibleObject#setAccessible(boolean) accessible}.
     * <p>
     * If the given <code>object</code> is already {@linkplain AccessibleObject#isAccessible() accessible}, this method returns <code>null</code>.
     * <p>
     * If the <code>object</code> is not {@linkplain AccessibleObject#isAccessible() accessible} and access control is {@link #CONTROLLED enabled}, the method
     * returns a {@link PrivilegedAction} that can be fed to {@link AccessController#doPrivileged(PrivilegedAction)} to make the object
     * {@linkplain AccessibleObject#setAccessible(boolean) accessible}.
     * <p>
     * If the <code>object</code> is not {@linkplain AccessibleObject#isAccessible() accessible} and access control is not {@link #CONTROLLED enabled}, the
     * method simply makes the object {@linkplain AccessibleObject#setAccessible(boolean) accessible}.
     *
     * @param object the object to process.
     * @param <T>    the object's type.
     *
     * @return a {@link PrivilegedAction} or <code>null</code>, as described above.
     */
    public static <T extends AccessibleObject> T access(final T object) {
        if (object.isAccessible()) {
            return object;
        } else if (CONTROLLED) {
            return AccessController.doPrivileged((PrivilegedAction<T>) () -> {
                object.setAccessible(true);
                return object;
            });
        } else {
            object.setAccessible(true);
            return object;
        }
    }

    /**
     * Depending on the value of {@link Security#CONTROLLED}, either invokes {@link AccessController#doPrivileged(PrivilegedAction)} with <code>action</code>,
     * or invokes the <code>action</code> directory.
     *
     * @param action the privileged action to invoke.
     * @param <T>    the return type of <code>action</code>.
     *
     * @return the result of invoking <code>action</code>.
     */
    public static <T> T invoke(final PrivilegedAction<T> action) {
        return Security.CONTROLLED ? AccessController.doPrivileged(action) : action.run();
    }

    /**
     * Depending on the value of {@link Security#CONTROLLED}, either invokes {@link AccessController#doPrivileged(PrivilegedAction)} with <code>action</code>,
     * or invokes the <code>action</code> directory.
     *
     * @param <T>         the return type of <code>action</code>.
     * @param context     see {@link AccessController#doPrivileged(PrivilegedAction, AccessControlContext)}.
     * @param action      the privileged action to invoke.
     * @param permissions see {@link AccessController#doPrivileged(PrivilegedAction, AccessControlContext, Permission...)}.
     *
     * @return the result of invoking <code>action</code>.
     */
    public static <T> T invoke(final AccessControlContext context, final PrivilegedAction<T> action, final Permission... permissions) {
        return Security.CONTROLLED && context != null
               ? permissions.length == 0
                 ? AccessController.doPrivileged(action, context)
                 : AccessController.doPrivileged(action, context, permissions)
               : action.run();
    }

    /**
     * Depending on the value of {@link Security#CONTROLLED}, either invokes {@link AccessController#doPrivileged(PrivilegedExceptionAction)} with
     * <code>action</code>, or invokes the <code>action</code> directory.
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
    @SuppressWarnings({ "unchecked", "UnusedParameters" })
    public static <T, E extends Exception> T invoke(final Class<E> error, final PrivilegedExceptionAction<T> action) throws E {
        return Exceptions.wrap(error, () -> Security.CONTROLLED ? AccessController.doPrivileged(action) : action.run());
    }

    /**
     * Depending on the value of {@link Security#CONTROLLED}, either invokes {@link AccessController#doPrivileged(PrivilegedExceptionAction)} with
     * <code>action</code>, or invokes the <code>action</code> directory.
     *
     * @param error  the highest level {@link Exception} class that <code>action</code> can throw.
     * @param context     see {@link AccessController#doPrivileged(PrivilegedAction, AccessControlContext)}.
     * @param action      the privileged action to invoke.
     * @param permissions see {@link AccessController#doPrivileged(PrivilegedAction, AccessControlContext, Permission...)}.
     * @param <T>         the return type of <code>action</code>.
     * @param <E>         the highest level {@link Exception} type <code>action</code> can throw.
     *
     * @return the result of invoking <code>action</code>.
     *
     * @throws E whatever <code>action</code>.
     */
    @SuppressWarnings({ "unchecked", "UnusedParameters" })
    public static <T, E extends Exception> T invoke(final Class<E> error,
                                                    final AccessControlContext context,
                                                    final PrivilegedExceptionAction<T> action,
                                                    final Permission... permissions) throws E {
        return Exceptions.wrap(error, () -> Security.CONTROLLED && context != null
                                            ? permissions.length == 0
                                              ? AccessController.doPrivileged(action, context)
                                              : AccessController.doPrivileged(action, context, permissions)
                                            : action.run());
    }
}
