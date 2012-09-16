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

package org.fluidity.deployment.osgi;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;

import org.fluidity.composition.Component;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Proxies;

import static org.fluidity.foundation.Command.Function;
import static org.fluidity.foundation.Command.Process;

/**
 * <b>NOTE</b>: This class <em>must be</em> loaded by the class loader of the bundle it is expected to import services into.
 *
 * @author Tibor Varga
 */
@Component
final class BundleBoundaryImpl implements BundleBoundary {

    public <T> T imported(final Class<T> type, final T remote) {

        // the use of "this" here requires this class to be loaded by the class loader of the bundle importing the remote object into
        return tunnel(remote, this, new Tunnel<T, RuntimeException>() {
            public T run(final boolean internal, final DelegatingClassLoader loader) {
                return internal ? remote : Proxies.create(type, new ServiceInvocation(remote, loader));
            }
        });
    }

    public <T> T exported(final Class<T> type, final Object remote, final T local) {
        return tunnel(local, remote, new Tunnel<T, RuntimeException>() {
            public T run(final boolean internal, final DelegatingClassLoader loader) {
                return internal ? local : Proxies.create(type, new ServiceInvocation(local, loader));
            }
        });
    }

    public <T, E extends Exception> T invoke(final Object remote, final Object local, final Process<T, E> command) throws E {
        return tunnel(remote, local, new Tunnel<T, E>() {
            public T run(final boolean internal, final DelegatingClassLoader loader) throws E {
                return internal ? command.run() : ClassLoaders.context(loader, new Function<T, ClassLoader, E>() {
                    public T run(final ClassLoader loader) throws E {
                        return command.run();
                    }
                });
            }
        });
    }

    private <T, E extends Exception> T tunnel(final Object remote, final Object local, final Tunnel<T, E> command) throws E {
        final ClassLoader remoteCL = loader(remote);
        final ClassLoader localCL = loader(local);

        return command.run(remoteCL == localCL, new DelegatingClassLoader(remoteCL, localCL));
    }

    private ClassLoader loader(final Object remote) {
        return (remote instanceof Class ? (Class) remote : remote.getClass()).getClassLoader();
    }

    /**
     * Internal interface to invoke with a tunneling class loader.
     *
     * @param <T> the return type of the command.
     * @param <E> the return type of the command.
     *
     * @author Tibor Varga
     */
    public interface Tunnel<T, E extends Throwable> {

        /**
         * Run caller logic with the given tunneling class loader.
         *
         * @param internal <code>true</code> if the local and remote classes were loaded by the same bundle,
         *                 <code>false</code> otherwise.
         * @param loader   the class loader.
         *
         * @return the result of the caller logic.
         */
        T run(boolean internal, DelegatingClassLoader loader) throws E;
    }

    /**
     * Proxy invocation handler that wraps all method calls to set and reset the calling thread's context class loader to allow the invoked method to load
     * classes both from the local bundle and from the one from which the invocation was made.
     *
     * @author Tibor Varga
     */
    private static final class ServiceInvocation implements InvocationHandler {

        private final ClassLoader tunnel;
        private final Object implementation;

        ServiceInvocation(final Object service, final ClassLoader classLoader) {
            this.implementation = service;
            this.tunnel = classLoader;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            return ClassLoaders.context(tunnel, new Function<Object, ClassLoader, Exception>() {
                public Object run(final ClassLoader loader) throws Exception {
                    return method.invoke(implementation, args);
                }
            });
        }
    }

    /**
     * Delegating class loader that allows a class in one bundle to find classes in another. If a class or resource cannot be found in the remote bundle then
     * the local bundle is consulted.
     *
     * @author Tibor Varga
     */
    private static class DelegatingClassLoader extends ClassLoader {

        private final ClassLoader caller;

        DelegatingClassLoader(final ClassLoader remote, final ClassLoader local) {
            super(remote);
            this.caller = local == null ? getSystemClassLoader() : local;
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException {
            return caller.loadClass(name);
        }

        @Override
        protected Enumeration<URL> findResources(final String name) throws IOException {
            return caller.getResources(name);
        }

        @Override
        protected URL findResource(final String name) {
            return caller.getResource(name);
        }
    }
}
