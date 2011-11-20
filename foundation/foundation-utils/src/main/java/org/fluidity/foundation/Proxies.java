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
import java.util.concurrent.atomic.AtomicReference;

/**
 * Convenience utilities concerning proxies.
 */
public final class Proxies extends Utilities {

    private static ObjectIdentity DEFAULT_IDENTITY = new ObjectIdentity<Object>() {
        public int hashCode(final Object instance) {
            return System.identityHashCode(instance);
        }

        public boolean equals(final Object other, final Object instance) {
            return instance == other;
        }

        public String toString(final Object instance) {
            return String.format("%s@%x", Proxies.api(instance.getClass()).getSimpleName(), instance.hashCode());
        }
    };


    /**
     * Creates a new proxy for the given interface using the given invocation handler. The class loader of the interface will be used to load the proxy class.
     *
     * @param type    the interface to implement with a proxy.
     * @param handler the handler that implements the interface's methods.
     *
     * @return a proxy implementing the given interface.
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(final Class<T> type, final InvocationHandler handler) {
        return create(type, (ObjectIdentity<T>) DEFAULT_IDENTITY, handler);
    }

    /**
     * Creates a new proxy for the given interface using the given invocation handler. The class loader of the interface will be used to load the proxy class.
     *
     * @param type     the interface to implement with a proxy.
     * @param identity the object that computes object identity for the returned proxy.
     * @param handler  the handler that implements the interface's methods.
     *
     * @return a proxy implementing the given interface.
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(final Class<T> type, final ObjectIdentity<T> identity, final InvocationHandler handler) {
        final AtomicReference<T> proxy = new AtomicReference<T>();
        proxy.set((T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, new MethodInvocations(handler, proxy, identity)));
        return proxy.get();
    }

    /**
     * If the supplied class is a proxy created by {@link #create(Class, InvocationHandler)} then this method returns the interface passed to that method;
     * otherwise the supplied class is returned.
     *
     * @param type the class to find the original interface for.
     *
     * @return the first interface implemented by the supplied class if it is a {@link Proxy}, otherwise the supplied class itself.
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> api(final Class<T> type) {
        return Proxy.isProxyClass(type) ? (Class<T>) type.getInterfaces()[0] : type;
    }

    /**
     * Returns the original <code>InvocationHandler</code> passed to {@link #create(Class, InvocationHandler)}.
     *
     * @param proxy the proxy returned by {@link #create(Class, InvocationHandler)}.
     *
     * @return the original <code>InvocationHandler</code> passed to {@link #create(Class, InvocationHandler)}.
     */
    public static InvocationHandler invocationHandler(final Object proxy) {
        final InvocationHandler invocations = Proxy.getInvocationHandler(proxy);
        return invocations instanceof MethodInvocations ? ((MethodInvocations) invocations).handler : invocations;
    }

    /**
     * Provides object identity to some proxy.
     *
     * @param <T> the interface the proxy stands for.
     */
    public interface ObjectIdentity<T> {

        /**
         * Computes the hash code for the given instance.
         *
         * @param instance the instance.
         *
         * @return the hash code for the given instance.
         */
        int hashCode(T instance);

        /**
         * Returns whether the given objects are equal. The proxy invocation handler calling this method assumes that the instance is the last parameter.
         *
         * @param other    the other object
         * @param instance the implementation object.
         *
         * @return <code>true</code> if the two objects are equal.
         */
        boolean equals(Object other, T instance);

        /**
         * Returns the string representation of the given instance.
         *
         * @param instance the instance.
         *
         * @return the string representation of the given instance.
         */
        String toString(T instance);
    }

    /**
     * Handles Object methods, i.e., {@link Object#hashCode()}, {@link Object#equals(Object)}, and {@link Object#toString()}. Exceptions wrapped in
     * <code>InvocationTargetException</code>, <code>UndeclaredThrowableException</code> and <code>RuntimeException</code> are unwrapped and re-thrown.
     */
    private static final class MethodInvocations<T> implements InvocationHandler {

        private final ObjectIdentity<T> identity;
        private final InvocationHandler handler;
        private final AtomicReference<T> proxy;

        protected MethodInvocations(final InvocationHandler handler, final AtomicReference<T> proxy, final ObjectIdentity<T> identity) {
            this.handler = handler;
            this.proxy = proxy;
            this.identity = identity;
        }

        @SuppressWarnings("unchecked")
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            try {
                return Exceptions.wrap(new Exceptions.Command<Object>() {
                    public Object run() throws Throwable {
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(MethodInvocations.this, args);
                        } else {
                            return handler.invoke(proxy, method, args);
                        }
                    }
                });
            } catch (final Exceptions.Wrapper e) {
                for (final Class<Throwable> type : (Class<Throwable>[]) method.getExceptionTypes()) {
                    e.rethrow(type);
                }

                throw e.rethrow(Throwable.class);
            }
        }

        @Override
        public int hashCode() {
            return identity.hashCode(proxy.get());
        }

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(final Object obj) {
            return identity.equals(obj, proxy.get());
        }

        @Override
        public String toString() {
            return identity.toString(proxy.get());
        }
    }
}
