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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Convenience methods on methods.
 *
 * @author Tibor Varga
 */
public final class Methods {

    private Methods() {
        throw new UnsupportedOperationException("No instance allowed");
    }

    /**
     * Finds a method object in a refactoring friendly way. Let's say you need to find the {@link java.io.Closeable#close()} method at run-time. Here's how you
     * do that with this method:
     * <pre>
     *  final Method closeMethod = Methods.get(Closeable.class, new Methods.Invoker&lt;Closeable>() {
     *    public void invoke(final Closeable dummy) {
     *      dummy.close();
     *    }
     *  });
     * </pre>
     *
     * @param type    the class that directly or indirectly defined the method.
     * @param invoker code that invokes the method in question.
     *
     * @return the method object.
     */
    @SuppressWarnings("unchecked")
    public static <T> Method get(final Class<T> type, final Invoker<T> invoker) {
        try {
            invoker.invoke(Proxies.create(type, new InvocationHandler() {
                public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                    throw new Capture(method);
                }
            }));

            throw new IllegalStateException("Desired method not called or errors blocked");
        } catch (final Capture e) {
            return e.method;
        }
    }

    /**
     * Allows the caller of {@link Methods#get(Class, Invoker)} to find a method without referring to it by name.
     */
    public interface Invoker<T> {

        /**
         * Invokes the method that the caller of {@link Methods#get(Class, Invoker)} intends to find.
         *
         * @param capture a dummy implementation of the interface owning the method being sought. The implementation must call on the supplied
         *                object the one method it is looking for via {@link Methods#get(Class, Invoker)}.
         */
        void invoke(T capture);
    }

    private static class Capture extends Error {

        public final Method method;

        private Capture(final Method method) {
            this.method = method;
        }
    }
}
