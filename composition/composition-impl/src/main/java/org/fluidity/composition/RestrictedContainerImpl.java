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

package org.fluidity.composition;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.spi.ComponentResolutionObserver;

/**
 * @author Tibor Varga
 */
final class RestrictedContainerImpl implements  RestrictedContainer {

    private final AtomicReference<ComponentContainer> reference
            = new AtomicReference<ComponentContainer>((ComponentContainer) Proxy.newProxyInstance(getClass().getClassLoader(),
                                                                                                  new Class<?>[] { ComponentContainer.class },
                                                                                                  new AccessDenied()));
    private final ComponentContainer delegate;

    RestrictedContainerImpl(final ComponentContainer delegate) {
        this.delegate = delegate;
    }

    public ObservedComponentContainer observed(final ComponentResolutionObserver observer) {
        return reference.get().observed(observer);
    }

    public <T> T getComponent(final Class<T> api) throws ResolutionException {
        return reference.get().getComponent(api);
    }

    public <T> T[] getComponentGroup(final Class<T> api) {
        return reference.get().getComponentGroup(api);
    }

    public OpenComponentContainer makeChildContainer() {
        return reference.get().makeChildContainer();
    }

    public OpenComponentContainer makeDomainContainer() {
        return reference.get().makeDomainContainer();
    }

    public <T> T getComponent(final Class<T> api, final Bindings bindings) throws ResolutionException {
        return reference.get().getComponent(api, bindings);
    }

    public <T> T initialize(final T component) throws ResolutionException {
        return reference.get().initialize(component);
    }

    public <T> T instantiate(final Class<T> componentClass) throws ResolutionException {
        return reference.get().instantiate(componentClass);
    }

    public ComponentContainer inheritContext(final ComponentContainer container) {
        return reference.get().inheritContext(container);
    }

    public ComponentContext context() {
        return reference.get().context();
    }

    public void enable() {
        reference.set(delegate);
    }

    private static class AccessDenied implements InvocationHandler {
        private RuntimeException denied() {
            return new ResolutionException("No dynamic dependencies allowed, use a CustomComponentFactory if you need such functionality");
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            } else {
                throw denied();
            }
        }
    }
}
