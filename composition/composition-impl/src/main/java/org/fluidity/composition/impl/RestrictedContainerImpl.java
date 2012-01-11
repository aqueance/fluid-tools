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

package org.fluidity.composition.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ObservedComponentContainer;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.RestrictedContainer;
import org.fluidity.foundation.Proxies;

/**
 * @author Tibor Varga
 */
final class RestrictedContainerImpl implements RestrictedContainer {

    private final AtomicReference<ComponentContainer> reference
            = new AtomicReference<ComponentContainer>((ComponentContainer) Proxies.create(ComponentContainer.class, new AccessDenied()));
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

    public <T> T getComponent(final Class<T> api, final Bindings... bindings) throws ResolutionException {
        return reference.get().getComponent(api, bindings);
    }

    public OpenComponentContainer makeChildContainer(final Bindings... bindings) {
        return reference.get().makeChildContainer(bindings);
    }

    public OpenComponentContainer makeDomainContainer() {
        return reference.get().makeDomainContainer();
    }

    public <T> T initialize(final T component) throws ResolutionException {
        return reference.get().initialize(component);
    }

    public Object invoke(final Object component, final Method method, final Object... arguments) throws ResolutionException, InvocationTargetException {
        return reference.get().invoke(component, method, arguments);
    }

    public <T> T complete(final T component, final Class<? super T>... api) throws ResolutionException {
        return reference.get().complete(component, api);
    }

    public <T> T instantiate(final Class<T> componentClass) throws ResolutionException {
        return reference.get().instantiate(componentClass);
    }

    public <T> T instantiate(final Class<T> componentClass, final Bindings bindings) throws ResolutionException {
        return reference.get().instantiate(componentClass, bindings);
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
