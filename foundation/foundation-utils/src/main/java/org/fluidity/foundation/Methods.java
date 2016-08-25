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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Convenience methods on methods.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("WeakerAccess")
public final class Methods extends Utility {

    private Methods() { }

    /**
     * Finds method objects in a refactoring friendly way.
     * <p>
     * <b>Note</b>: only works on interface methods.
     * <h3>Usage</h3> Let's say you need to find the {@link java.io.Closeable#close() Closeable.close()} method at run time:
     * <pre>
     * final {@linkplain Method} closeMethod = <span class="hl1">Methods.get</span>(<span class="hl2">Closeable</span>.class, <span class="hl2">Closeable</span>::<span class="hl2">close</span>)[0];
     * </pre>
     *
     * @param type     the interface that directly or indirectly defines the method you seek.
     * @param selector code that invokes the method on the supplied implementation of the specified interface <code>type</code>.
     * @param <T>      the type of the interface to capture method calls on.
     *
     * @return the method object.
     */
    public static <T> Method[] get(final Class<T> type, final Selector<T> selector) {
        return Exceptions.wrap(() -> {
            final Collection<Method> methods = new ArrayList<>();

            selector.invoke(Proxies.create(type, (proxy, method, arguments) -> {
                methods.add(method);

                final Class<?> _type = method.getReturnType();

                if (_type == boolean.class) {
                    return Boolean.FALSE;
                } else if (_type == byte.class) {
                    return (byte) 0;
                } else if (_type == short.class) {
                    return (short) 0;
                } else if (_type == int.class) {
                    return 0;
                } else if (_type == long.class) {
                    return 0L;
                } else if (_type == float.class) {
                    return 0.0F;
                } else if (_type == double.class) {
                    return 0.0;
                } else if (_type == char.class) {
                    return '\0';
                } else {
                    return null;
                }
            }));

            return Lists.asArray(Method.class, methods);
        });
    }

    /**
     * Invokes the given <code>method</code> on the given <code>target</code> with the given <code>arguments</code>. All checked exceptions thrown from the
     * method will be wrapped in {@link Exceptions.Wrapper}.
     *
     * @param method    the method to invoke; may not be <code>null</code>.
     * @param target    the object to invoke the method on; may be <code>null</code> if the method is {@linkplain java.lang.reflect.Modifier#isStatic(int)
     *                  static}.
     * @param arguments the arguments to pass to the method.
     *
     * @return whatever the method returns.
     *
     * @throws Exceptions.Wrapper wraps all checked exceptions thrown by the method.
     */
    @SuppressWarnings("unchecked")
    public static Object invoke(final Method method, final Object target, final Object... arguments) throws Exceptions.Wrapper {
        assert target != null || Modifier.isStatic(method.getModifiers()) : method;

        return Exceptions.wrap(() -> method.invoke(target, arguments));
    }

    /**
     * Allows the caller of {@link Methods#get(Class, Selector) Methods.get()} to find a method without referring to it by name.
     * <h3>Usage</h3>
     * See {@link Methods#get(Class, Selector) Methods.get()}.
     *
     * @param <T> the type of the interface to capture method invocations on.
     *
     * @author Tibor Varga
     */
    @FunctionalInterface
    public interface Selector<T> {

        /**
         * Invokes the method that the caller of {@link Methods#get(Class, Selector) Methods.get()} intends to find.
         *
         * @param capture a dummy implementation of the interface owning the method being sought. The implementation must call, on the supplied object, the
         *                method it is looking for.
         *
         * @throws Exception listed for semantic purposes; should never actually be thrown.
         */
        void invoke(T capture) throws Exception;
    }
}
