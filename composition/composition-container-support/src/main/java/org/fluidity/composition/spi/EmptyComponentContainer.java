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

package org.fluidity.composition.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.Components;
import org.fluidity.composition.Inject;
import org.fluidity.composition.ObservedComponentContainer;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.foundation.Proxies;

/**
 * Implements basic method relationships and functionality useful for container and registry implementations.
 *
 * @author Tibor Varga
 */
public abstract class EmptyComponentContainer implements OpenComponentContainer, ObservedComponentContainer, ComponentRegistry {

    private final Registry registry = new EmptyRegistry(this);

    /**
     * Implements this convenience method using the primary methods of the container.
     * <p/>
     * {@inheritDoc}
     */
    public final <T> T getComponent(final Class<T> api, final Bindings bindings) throws ResolutionException {
        return makeChildContainer(bindings).getComponent(api);
    }

    public final ComponentContainer makeChildContainer(final Bindings bindings) {
        final OpenComponentContainer container = makeChildContainer();
        bindings.bindComponents(container.getRegistry());
        return container;
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
