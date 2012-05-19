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

package org.fluidity.composition.container.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.Components;
import org.fluidity.composition.Inject;
import org.fluidity.composition.ObservedComponentContainer;
import org.fluidity.foundation.Proxies;

/**
 * Implements basic method relationships and functionality useful for container and registry implementations.
 * <h3>Usage</h3>
 * You don't interact with an internal interface.
 *
 * @author Tibor Varga
 */
public abstract class EmptyComponentContainer implements OpenComponentContainer, ObservedComponentContainer, ComponentRegistry {

    private final Registry registry = new EmptyRegistry(this);

    /**
     * Invokes the given method of the given object after resolving and injecting its applicable parameters that the given argument list contains no
     * (or <code>null</code>) value for.
     * <p/>
     *
     * @param component the method to invoke on the provided object.
     * @param explicit  tells if all parameters are subject to injection (<code>true</code>) or only those annotated with {@link Inject @Inject}
     *                  (<code>false</code>).
     * @param method    the method that needs its parameters injected.
     * @param arguments the method parameters matching the method's signature with <code>null</code> values where injection is needed.
     *
     * @return the result of the method invocation.
     *
     * @throws ResolutionException       when dependency resolution fails.
     * @throws InvocationTargetException when the method throws an exception.
     */
    protected abstract Object invoke(final Object component, boolean explicit, final Method method, final Object... arguments)
            throws ResolutionException, InvocationTargetException;

    /**
     * Adds the given list of component bindings to this container.
     *
     * @param list the list of bindings to add.
     *
     * @return this container.
     */
    protected final OpenComponentContainer addBindings(final Bindings... list) {
        for (final Bindings bindings : list) {
            bindings.bindComponents(registry);
        }

        return this;
    }

    /**
     * Calls {@link #invoke(Object, boolean, Method, Object...) invoke}<code>(component, <b>true</b>, method, arguments)</code>.
     * <p/>
     * {@inheritDoc}
     */
    public final Object invoke(final Object component, final Method method, final Object... arguments) throws ResolutionException, InvocationTargetException {
        return invoke(component, true, method, arguments);
    }

    /**
     * Implements this convenience method using the primary methods of the container.
     * <p/>
     * {@inheritDoc}
     */
    public final <T> T getComponent(final Class<T> api, final Bindings... bindings) throws ResolutionException {
        return makeChildContainer(bindings).getComponent(api);
    }

    /**
     * Implements this convenience method using the primary methods of the container.
     * <p/>
     * {@inheritDoc}
     */
    public final <T> T instantiate(final Class<T> componentClass) throws ResolutionException {
        return instantiate(componentClass, null);
    }

    /**
     * Implements this convenience method using the primary methods of the container.
     * <p/>
     * {@inheritDoc}
     */
    public final <T> T instantiate(final Class<T> componentClass, final Bindings bindings) throws ResolutionException {
        final ComponentContainer container = makeChildContainer(new Bindings() {
            @SuppressWarnings("unchecked")
            public void bindComponents(final Registry registry) {
                registry.bindComponent(componentClass, componentClass);

                if (bindings != null) {
                    bindings.bindComponents(registry);
                }
            }
        });

        return container.getComponent(componentClass);
    }

    @SuppressWarnings("unchecked")
    public final <T> T complete(final T component, final Class<? super T>... api) throws ResolutionException {
        final Class<T> type = (Class<T>) component.getClass();
        final List<Class<?>> interfaces = new ArrayList<Class<?>>();

        for (final Components.Specification specification : Components.inspect(type, api).api) {
            if (specification.api.isInterface()) {
                interfaces.add(specification.api);
            } else {
                throw new IllegalArgumentException(String.format("Component %s may only have interfaces as its component interfaces", type.getName()));
            }
        }

        return (T) Proxies.create(type.getClassLoader(), interfaces.toArray(new Class<?>[interfaces.size()]), new InvocationHandler() {

            private final Map<Method, Boolean> injectMap = new ConcurrentHashMap<Method, Boolean>();

            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                Boolean inject = injectMap.get(method);

                if (inject == null) {
                    LOOP:
                    for (final Annotation[] annotations : method.getParameterAnnotations()) {
                        for (final Annotation annotation : annotations) {
                            if (annotation instanceof Inject) {
                                injectMap.put(method, inject = true);
                                break LOOP;
                            }
                        }
                    }

                    if (inject == null) {
                        injectMap.put(method, inject = false);
                    }

                    method.setAccessible(true);
                }

                return inject ? EmptyComponentContainer.this.invoke(component, false, method, args) : method.invoke(component, args);
            }
        });
    }

    public final Registry getRegistry() {
        return registry;
    }
}
