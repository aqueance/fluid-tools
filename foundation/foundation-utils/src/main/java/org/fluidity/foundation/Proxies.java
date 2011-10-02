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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Convenience utilities concerning proxies.
 */
@SuppressWarnings("unchecked")
public final class Proxies {

    private Proxies() {
        throw new UnsupportedOperationException("No instance allowed");
    }

    /**
     * Creates a new proxy for the given interface using the given invocation handler. The class loader of the interface will be used to load the proxy class.
     *
     * @param type    the interface to implement with a proxy.
     * @param handler the handler that implements the interface's methods.
     *
     * @return a proxy implementing the given interface.
     */
    public static <T> T create(final Class<T> type, final InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    /**
     * If the supplied class is a proxy created by {@link #create(Class, InvocationHandler)} then this method returns the interface passed to that method;
     * otherwise the supplied class is returned.
     *
     * @param type the class to find the original interface for.
     *
     * @return the first interface implemented by the supplied class if it is a {@link Proxy}, otherwise the supplied class itself.
     */
    public static <T> Class<T> api(final Class<T> type) {
        return Proxy.isProxyClass(type) ? (Class<T>) type.getInterfaces()[0] : type;
    }

    /**
     * Invokes the methods inherited from {@link Object}. This method replaces any proxy instance in the argument list with its invocation handler.
     *
     * @param method the method to invoke.
     * @param args   the arguments to the method.
     * @param caller the object to invoke the method on.
     *
     * @return the result of the method invocation.
     *
     * @throws IllegalAccessException    thrown by the nested method invocation.
     * @throws InvocationTargetException thrown by the nested method invocation.
     */
    public static Object inherited(final Method method, final Object[] args, final InvocationHandler caller) throws IllegalAccessException, InvocationTargetException {
        assert method.getDeclaringClass() == Object.class : method;
        assert caller instanceof MethodInvocations : caller;
        return method.invoke(caller, args);
    }

    public static abstract class MethodInvocations implements InvocationHandler {

        private final Class<?> api;

        protected MethodInvocations(final Class<?> api) {
            this.api = api;
        }

        protected final Class<?> api() {
            return api;
        }

        @Override
        public boolean equals(final Object obj) {
            return super.equals(obj != null && Proxy.isProxyClass(obj.getClass()) ? Proxy.getInvocationHandler(obj) : obj);
        }

        @Override
        public String toString() {
            return String.format("%s@%d", api.getSimpleName(), hashCode());
        }
    }
}
