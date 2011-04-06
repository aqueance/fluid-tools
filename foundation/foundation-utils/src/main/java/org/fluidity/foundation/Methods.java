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
import java.lang.reflect.Proxy;

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
            invoker.invoke((T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, new InvocationHandler() {
                public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                    throw new Capture(method);
                }
            }));

            throw new IllegalStateException("Method not called or exceptions blocked");
        } catch (final Capture e) {
            return e.method;
        }
    }

    public static interface Invoker<T> {

        void invoke(T dummy);
    }

    private static class Capture extends UnsupportedOperationException {

        public final Method method;

        private Capture(final Method method) {
            this.method = method;
        }
    }
}
