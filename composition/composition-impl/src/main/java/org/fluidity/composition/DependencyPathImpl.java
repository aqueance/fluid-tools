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

package org.fluidity.composition;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.fluidity.composition.network.ContextDefinition;
import org.fluidity.composition.spi.ComponentMapping;

/**
 * @author Tibor Varga
 */
final class DependencyPathImpl implements DependencyPath {

    /*
     * When component instantiation is deferred in order to break circular dependence, the deferred resolver must be able to use the reference path prevalent
     * at its invocation. Without a thread local variable to hold that prevalent path, the deferred resolver could either use the path prevalent at its
     * creation or a new empty path, both of which are wrong in one case or the other.
     *
     * Thus, we need a thread local variable to hold the prevalent reference path.
     */
    private final ThreadLocal<Path> path = new InheritableThreadLocal<Path>() {
        @Override
        protected Path initialValue() {
            return new Path();
        }
    };

    public Object follow(final Class<?> api, final ContextDefinition context, final ComponentMapping mapping, final Command command) {
        final Path lastPath = path.get();
        final Path newPath = lastPath.descend(mapping);
        final ContextDefinition newContext = context == null ? new ContextDefinitionImpl() : context;

        path.set(newPath);
        try {
            if (newPath.isCircular()) {
                return deferredCreate(api, newContext, newPath, mapping, command, null);
            } else {
                try {
                    return command.run(newContext);
                } catch (final ComponentContainer.CircularReferencesException error) {
                    return deferredCreate(api, newContext, newPath, mapping, command, error);
                }
            }
        } finally {
            path.set(lastPath);
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private Object deferredCreate(final Class<?> api,
                                  final ContextDefinition context,
                                  final Path path,
                                  final ComponentMapping mapping,
                                  final Command command,
                                  final ComponentContainer.CircularReferencesException error) {
        if (command instanceof DeferredCommand) {
            throw circularity(api, path, ((DeferredCommand) command).error());
        } else if (api.isInterface()) {
            return Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] { api }, new InvocationHandler() {
                private volatile Object delegate;

                public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
                    Object cache = delegate;

                    if (delegate == null) {
                        synchronized (this) {
                            cache = delegate;

                            if (cache == null) {
                                delegate = cache = follow(api, context, mapping, new DeferredCommand(command, error));
                            }
                        }
                    }

                    return method.invoke(cache, arguments);
                }
            });
        } else {
            throw circularity(api, path, error);
        }
    }

    public static ComponentContainer.CircularReferencesException circularity(final Class<?> api,
                                                                             final Path path,
                                                                             final ComponentContainer.CircularReferencesException error) {
        return error == null ? new ComponentContainer.CircularReferencesException(api, path.toString()) : error;
    }

    private static class DeferredCommand implements Command {

        private final Command command;
        private final ComponentContainer.CircularReferencesException error;

        public DeferredCommand(final Command command, final ComponentContainer.CircularReferencesException error) {
            this.error = error;
            this.command = command;
        }

        public Object run(final ContextDefinition context) {
            return command.run(context);
        }

        public ComponentContainer.CircularReferencesException error() {
            return error;
        }
    }

    /**
     * Represents the path of references to the particular dependency where the object appears.
     */
    private class Path {

        private final Set<ComponentMapping> loop = new LinkedHashSet<ComponentMapping>();
        private final ComponentMapping mapping;
        private final boolean circular;

        private Path() {
            this.circular = false;
            this.mapping = null;
        }

        public Path(final Set<ComponentMapping> loop, final ComponentMapping mapping) {
            this.loop.addAll(loop);
            this.circular = !this.loop.add(mapping);
            this.mapping = mapping;
        }

        public Path descend(final ComponentMapping mapping) {
            return new Path(loop, mapping);
        }

        /**
         * Tells if the path of reference up to and including the dependency resolver receiving this object that it is being invoked the second time during one
         * dependency resolution cycle; i.e., there is circular reference in the dependency path.
         *
         * @return <code>true</code> if the dependency resolver adds circular dependency to the dependency path, <code>false</code> otherwise.
         */
        public boolean isCircular() {
            return circular;
        }

        @Override
        public String toString() {
            if (circular) {
                final List<ComponentMapping> list = new ArrayList<ComponentMapping>(loop.size() + 1);
                list.addAll(loop);
                list.add(mapping);
                return list.toString();
            } else {
                return loop.toString();
            }
        }
    }
}
