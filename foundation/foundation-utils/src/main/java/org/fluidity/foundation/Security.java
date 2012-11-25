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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Security related convenience functions.
 * <h3>Usage</h3>
 * <h4>Deciding whether to check access to some resource</h4>
 * <pre>
 * if (Security.CONTROLLED) {
 *     AccessController.doPrivileged(&hellip;);
 * } else {
 *     &hellip;
 * }
 * </pre>
 * <h4>Object Accessibility</h4>
 * <pre>
 * final Method method = &hellip;;
 * final PrivilegedAction&lt;Method> access = Security.setAccessible(method);
 * if (access != null) {
 *     AccessController.doPrivileged(access);
 * }
 *
 * method.invoke(&hellip;);
 * </pre>
 *
 * @author Tibor Varga
 */
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
            controlled = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    return System.getSecurityManager() != null || Boolean.getBoolean(PROPERTY);
                }
            });
        } catch (final AccessControlException e) {
            controlled = true;
        }

        CONTROLLED = controlled;
    }

    private Security() { }

    /**
     * Convenience method to safely set some object {@linkplain AccessibleObject#setAccessible(boolean) accessible}.
     * <p/>
     * If the given <code>object</code> is already {@linkplain AccessibleObject#isAccessible() accessible}, this method returns <code>null</code>.
     * <p/>
     * If the <code>object</code> is not {@linkplain AccessibleObject#isAccessible() accessible} and access control is {@link #CONTROLLED enabled}, the method
     * returns a {@link PrivilegedAction} that can be fed to {@link AccessController#doPrivileged(PrivilegedAction)} to make the object
     * {@linkplain AccessibleObject#setAccessible(boolean) accessible}.
     * <p/>
     * If the <code>object</code> is not {@linkplain AccessibleObject#isAccessible() accessible} and access control is not {@link #CONTROLLED enabled}, the
     * method simply makes the object {@linkplain AccessibleObject#setAccessible(boolean) accessible}.
     *
     * @param object the object to process.
     * @param <T>    the object's type.
     *
     * @return a {@link PrivilegedAction} or <code>null</code>, as described above.
     */
    public static <T extends AccessibleObject> PrivilegedAction<T> setAccessible(final T object) {
        if (object.isAccessible()) {
            return null;
        } else if (CONTROLLED) {
            return new PrivilegedAction<T>() {
                public T run() {
                    object.setAccessible(true);
                    return object;
                }
            };
        } else {
            object.setAccessible(true);
            return null;
        }
    }

    /**
     * Convenience method to safely set {@linkplain AccessibleObject#setAccessible(AccessibleObject[], boolean) accessible} the constructors of a class.
     * <p/>
     * The method returns a {@link PrivilegedAction} that can either be fed to {@link AccessController#doPrivileged(PrivilegedAction)} to make the constructors
     * {@linkplain AccessibleObject#setAccessible(AccessibleObject[], boolean) accessible} if access control is {@link #CONTROLLED enabled}, or can be directly
     * {@linkplain PrivilegedAction#run() run} when not.
     *
     * @param type     is the class the constructors of which are to be made accessible.
     * @param declared if <code>true</code>, the {@linkplain Class#getDeclaredConstructors() declared} constructors are made accessible, if <code>false</code>,
     *                 only the {@linkplain Class#getConstructors() public} ones are.
     *
     * @return a {@link PrivilegedAction}; never <code>null</code>.
     */
    public static PrivilegedAction<Constructor[]> setConstructorsAccessible(final Class<?> type, final boolean declared) {
        return new PrivilegedAction<Constructor[]>() {
            public Constructor[] run() {
                final Constructor<?>[] constructors = declared ? type.getDeclaredConstructors() : type.getConstructors();
                AccessibleObject.setAccessible(constructors, true);
                return constructors;
            }
        };
    }

    /**
     * Convenience method to safely set {@linkplain AccessibleObject#setAccessible(AccessibleObject[], boolean) accessible} the methods of a class.
     * <p/>
     * The method returns a {@link PrivilegedAction} that can either be fed to {@link AccessController#doPrivileged(PrivilegedAction)} to make the methods
     * {@linkplain AccessibleObject#setAccessible(AccessibleObject[], boolean) accessible} if access control is {@link #CONTROLLED enabled}, or can be directly
     * {@linkplain PrivilegedAction#run() run} when not.
     *
     * @param type     is the class the methods of which are to be made accessible.
     * @param declared if <code>true</code>, the {@linkplain Class#getDeclaredConstructors() declared} methods are made accessible, if <code>false</code>,
     *                 only the {@linkplain Class#getConstructors() public} ones are.
     *
     * @return a {@link PrivilegedAction}; never <code>null</code>.
     */
    public static PrivilegedAction<Method[]> setMethodsAccessible(final Class<?> type, final boolean declared) {
        return new PrivilegedAction<Method[]>() {
            public Method[] run() {
                final Method[] methods = declared ? type.getDeclaredMethods() : type.getMethods();
                AccessibleObject.setAccessible(methods, true);
                return methods;
            }
        };
    }

    /**
     * Convenience method to safely set {@linkplain AccessibleObject#setAccessible(AccessibleObject[], boolean) accessible} the fields of a class.
     * <p/>
     * The method returns a {@link PrivilegedAction} that can either be fed to {@link AccessController#doPrivileged(PrivilegedAction)} to make the fields
     * {@linkplain AccessibleObject#setAccessible(AccessibleObject[], boolean) accessible} if access control is {@link #CONTROLLED enabled}, or can be directly
     * {@linkplain PrivilegedAction#run() run} when not.
     *
     * @param type     is the class the fields of which are to be made accessible.
     * @param declared if <code>true</code>, the {@linkplain Class#getDeclaredConstructors() declared} fields are made accessible, if <code>false</code>,
     *                 only the {@linkplain Class#getConstructors() public} ones are.
     *
     * @return a {@link PrivilegedAction}; never <code>null</code>.
     */
    public static PrivilegedAction<Field[]> setFieldsAccessible(final Class<?> type, final boolean declared) {
        return new PrivilegedAction<Field[]>() {
            public Field[] run() {
                final Field[] fields = declared ? type.getDeclaredFields() : type.getFields();
                AccessibleObject.setAccessible(fields, true);
                return fields;
            }
        };
    }
}
