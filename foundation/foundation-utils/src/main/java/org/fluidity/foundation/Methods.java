/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
