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

package org.fluidity.deployment.osgi;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;

import org.fluidity.composition.Component;
import org.fluidity.foundation.ClassLoaders;
import org.fluidity.foundation.Proxies;

/**
 * @author Tibor Varga
 */
@Component
final class BundleBoundaryImpl implements BundleBoundary {

    public <T> T imported(final Class<T> type, final T remote) {
        return tunnel(type, remote, this);
    }

    public <T> T exported(final Class<T> type, final Object remote, final T local) {
        return tunnel(type, local, remote);
    }

    private <T> T tunnel(final Class<T> type, final T remote, final Object local) {
        final ClassLoader remoteCL = remote.getClass().getClassLoader();
        final ClassLoader localCL = (local instanceof Class ? (Class) local : local.getClass()).getClassLoader();

        return remoteCL == localCL ? remote : Proxies.create(type, new ServiceInvocation(remote, new DelegatingClassLoader(remoteCL, localCL)));
    }

    /**
     * Proxy invocation handler that wraps all methods calls to set and reset the calling thread's context class loader to allow the invoked method to load
     * classes both from the local bundle and from the one from which the invocation was made.
     *
     * @author Tibor Varga
     */
    private static final class ServiceInvocation implements InvocationHandler {

        private final ClassLoader tunnel;
        private final Object implementation;

        public ServiceInvocation(final Object service, final ClassLoader classLoader) {
            this.implementation = service;
            this.tunnel = classLoader;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            return ClassLoaders.context(tunnel, new ClassLoaders.ContextCommand<Object, Throwable>() {
                public Object run(final ClassLoader loader) throws Throwable {
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

        public DelegatingClassLoader(final ClassLoader remote, final ClassLoader local) {
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
