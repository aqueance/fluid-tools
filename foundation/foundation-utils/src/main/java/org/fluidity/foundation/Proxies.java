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
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Convenience utilities concerning proxies.
 * <h3>Usage Example</h3>
 * <pre>
 * final class MyComponent {
 *
 *   private final <span class="hl1">Proxies.Identity</span>&lt;<span class="hl2">MyDelegate</span>> <span class="hl3">identity</span> = new <span class="hl1">Proxies.Identity</span>&lt;<span class="hl2">MyDelegate</span>>() {
 *       &hellip;
 *   }
 *
 *   private final {@linkplain Class}[] types = { <span class="hl2">MyDelegate</span>.class };
 *
 *   private final <span class="hl2">MyDelegate</span> delegate = <span class="hl1">Proxies.create</span>(getClass().getClassLoader(), <span class="hl3">identity</span>, types, new {@linkplain InvocationHandler}() {
 *     &hellip;
 *   }
 *
 *   interface <span class="hl2">MyDelegate</span> {
 *     &hellip;
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
public final class Proxies extends Utility {

    private Proxies() { }

    private static Identity DEFAULT_IDENTITY = new Identity<Object>() {
        public int hashCode(final Object instance) {
            return System.identityHashCode(instance);
        }

        public boolean equals(final Object other, final Object instance) {
            return instance == other;
        }

        public String toString(final Object instance) {
            assert Proxy.isProxyClass(instance.getClass()) : instance.getClass();
            return String.format("%s@%x", instance.getClass().getInterfaces()[0].getSimpleName(), instance.hashCode());
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
        return create(type, (Identity<T>) DEFAULT_IDENTITY, handler);
    }

    /**
     * Creates a new proxy for the given interface using the given invocation handler and object identity. The class loader of the interface will be used to
     * load the proxy class.
     *
     * @param type     the interface to implement with a proxy.
     * @param identity the object that computes object identity for the returned proxy.
     * @param handler  the handler that implements the interface's methods.
     *
     * @return a proxy implementing the given interface.
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(final Class<T> type, final Identity<T> identity, final InvocationHandler handler) {
        final AtomicReference<T> proxy = new AtomicReference<T>();
        proxy.set((T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, new MethodInvocations(handler, proxy, identity)));
        return proxy.get();
    }

    /**
     * Creates a new proxy for the given interfaces using the given invocation handler.
     *
     * @param loader  the class loader to create the proxy.
     * @param types   the interfaces to implement with a proxy.
     * @param handler the handler that implements the interface's methods.
     *
     * @return a proxy implementing the given interface.
     */
    @SuppressWarnings("unchecked")
    public static Object create(final ClassLoader loader, final Class<?>[] types, final InvocationHandler handler) {
        return create(loader, (Identity<?>) DEFAULT_IDENTITY, types, handler);
    }

    /**
     * Creates a new proxy for the given interfaces using the given invocation handler and object identity.
     *
     * @param loader   the class loader to create the proxy.
     * @param identity the object that computes object identity for the returned proxy.
     * @param types    the interfaces to implement with a proxy.
     * @param handler  the handler that implements the interface's methods.
     *
     * @return a proxy implementing the given interface.
     */
    @SuppressWarnings("unchecked")
    public static Object create(final ClassLoader loader, final Identity<?> identity, final Class<?>[] types, final InvocationHandler handler) {
        final AtomicReference proxy = new AtomicReference();
        proxy.set(Proxy.newProxyInstance(loader, types, new MethodInvocations(handler, proxy, identity)));
        return proxy.get();
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
     * <h3>Usage</h3>
     * See {@link Proxies}.
     *
     * @param <T> the interface the proxy stands for.
     *
     * @author Tibor Varga
     */
    /*
     *  The proxy invocation handler calling methods on this interface assumes that the instance is the last parameter of the method.
     */
    public interface Identity<T> {

        /**
         * Computes the hash code for the given instance.
         *
         * @param instance the instance.
         *
         * @return the hash code for the given instance.
         */
        int hashCode(T instance);

        /**
         * Returns whether the given objects are equal.
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

        private final Identity<T> identity;
        private final InvocationHandler handler;
        private final AtomicReference<T> proxy;

        protected MethodInvocations(final InvocationHandler handler, final AtomicReference<T> proxy, final Identity<T> identity) {
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
                            method.setAccessible(true);
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
