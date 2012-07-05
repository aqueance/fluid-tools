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

/**
 * Convenience methods on methods.
 *
 * @author Tibor Varga
 */
public final class Methods extends Utility {

    private Methods() { }

    /**
     * Finds a method object in a refactoring friendly way. Let's say you need to find the {@link java.io.Closeable#close()} method at run time. Here's how you
     * do that with this method:
     * <pre>
     * final {@linkplain Method} closeMethod = <span class="hl1">Methods.get</span>(<span class="hl2">Closeable</span>.class, new <span class="hl1">Methods.Invoker</span>&lt;<span class="hl2">Closeable</span>>() {
     *   public void invoke(final <span class="hl2">Closeable</span> dummy) {
     *     dummy.<span class="hl2">close</span>();
     *   }
     * });
     * </pre>
     * <p/>
     * This works only on interface methods.
     *
     * @param type    the class that directly or indirectly defines the method you seek.
     * @param invoker code that invokes the method on a dummy implementation of its owning interface.
     *
     * @return the method object.
     */
    public static <T> Method get(final Class<T> type, final Invoker<T> invoker) {
        try {
            invoker.invoke(Proxies.create(type, new InvocationHandler() {
                public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                    throw new Capture(method);
                }
            }));

            throw new IllegalStateException("Desired method not called or exceptions not thrown");
        } catch (final Capture e) {
            return e.method;
        } catch (final Throwable e) {
            throw new IllegalStateException(e);
        }
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

    private static class Capture extends Error {

        public final Method method;

        private Capture(final Method method) {
            this.method = method;
        }
    }
}
