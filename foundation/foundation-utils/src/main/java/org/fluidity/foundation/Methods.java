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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Convenience methods on methods.
 *
 * @author Tibor Varga
 */
public final class Methods extends Utility {

    private Methods() { }

    /**
     * Finds method objects in a refactoring friendly way.
     * <p/>
     * <b>Note</b>: only works on interface methods.
     * <h3>Usage</h3>
     * Let's say you need to find the {@link java.io.Closeable#close()} method at run time:
     * <pre>
     * final {@linkplain Method} closeMethod = <span class="hl1">Methods.get</span>(<span class="hl2">Closeable</span>.class, new <span class="hl1">Methods.Invoker</span>&lt;<span class="hl2">Closeable</span>>() {
     *   public void <span class="hl1">invoke</span>(final <span class="hl2">Closeable</span> capture) {
     *     capture.<span class="hl2">close</span>();
     *   }
     * })[0];
     * </pre>
     *
     * @param type    the interface that directly or indirectly defines the method you seek.
     * @param invoker code that invokes the method on the supplied implementation of the specified interface <code>type</code>.
     *
     * @return the method object.
     */
    public static <T> Method[] get(final Class<T> type, final Invoker<T> invoker) {
        final Collection<Method> methods = new ArrayList<Method>();

        Exceptions.wrap(new Command.Job<Throwable>() {
            public void run() throws Throwable {
                invoker.invoke(Proxies.create(type, new InvocationHandler() {
                    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                        methods.add(method);

                        final Class<?> type = method.getReturnType();

                        if (type == boolean.class) {
                            return Boolean.FALSE;
                        } else if (type == byte.class) {
                            return (byte) 0;
                        } else if (type == short.class) {
                            return (short) 0;
                        } else if (type == int.class) {
                            return 0;
                        } else if (type == long.class) {
                            return 0L;
                        } else if (type == float.class) {
                            return 0.0F;
                        } else if (type == double.class) {
                            return 0.0;
                        } else if (type == char.class) {
                            return '\0';
                        } else {
                            return null;
                        }
                    }
                }));
            }
        });

        return Lists.asArray(Method.class, methods);
    }

    /**
     * Allows the caller of {@link Methods#get(Class, Methods.Invoker) Methods.get()} to find a method without referring to it by name.
     * <h3>Usage</h3>
     * See {@link Methods#get(Class, Methods.Invoker) Methods.get()}.
     *
     * @author Tibor Varga
     */
    public interface Invoker<T> {

        /**
         * Invokes the method that the caller of {@link Methods#get(Class, Methods.Invoker) Methods.get()} intends to find.
         *
         * @param capture a dummy implementation of the interface owning the method being sought. The implementation must call, on the supplied
         *                object, the method it is looking for.
         * @throws Throwable listed for semantic purposes; should never actually be thrown.
         */
        void invoke(T capture) throws Throwable;
    }
}
