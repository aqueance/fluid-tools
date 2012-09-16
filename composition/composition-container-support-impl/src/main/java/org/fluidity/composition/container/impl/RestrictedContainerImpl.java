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

package org.fluidity.composition.container.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ObservedComponentContainer;
import org.fluidity.composition.container.RestrictedContainer;
import org.fluidity.composition.spi.ComponentInterceptor;

/**
 * @author Tibor Varga
 */
final class RestrictedContainerImpl implements RestrictedContainer {

    private final AccessGuard guard;
    private final ComponentContainer delegate;

    RestrictedContainerImpl(final ComponentContainer delegate) {
        this(new AccessGuard(), delegate);
    }

    private RestrictedContainerImpl(final AccessGuard guard, final ComponentContainer delegate) {
        this.guard = guard;
        this.delegate = delegate;
    }

    public ObservedComponentContainer observed(final Observer observer) {
        return new RestrictedContainerImpl(guard, delegate.observed(observer));
    }

    public <T> T getComponent(final Class<T> api) throws ResolutionException {
        return guard.access(delegate).getComponent(api);
    }

    public <T> T[] getComponentGroup(final Class<T> api) {
        return guard.access(delegate).getComponentGroup(api);
    }

    public <T> T getComponent(final Class<T> api, final Bindings... bindings) throws ResolutionException {
        return guard.access(delegate).getComponent(api, bindings);
    }

    public ComponentContainer makeChildContainer(final Bindings... bindings) {
        return new RestrictedContainerImpl(guard, delegate.makeChildContainer(bindings));
    }

    public ComponentContainer makeDomainContainer(final Bindings... bindings) {
        return new RestrictedContainerImpl(guard, delegate.makeDomainContainer(bindings));
    }

    public ComponentContainer intercepting(final ComponentInterceptor... interceptors) {
        return new RestrictedContainerImpl(guard, delegate.intercepting(interceptors));
    }

    public <T> T initialize(final T component) throws ResolutionException {
        return guard.access(delegate).initialize(component);
    }

    public Object invoke(final Object component, final Method method, final Object... arguments) throws ResolutionException, InvocationTargetException {
        return guard.access(delegate).invoke(component, method, arguments);
    }

    public <T> T complete(final T component, final Class<? super T>... api) throws ResolutionException {
        return guard.access(delegate).complete(component, api);
    }

    public <T> T instantiate(final Class<T> componentClass) throws ResolutionException {
        return guard.access(delegate).instantiate(componentClass);
    }

    public <T> T instantiate(final Class<T> componentClass, final Bindings bindings) throws ResolutionException {
        return guard.access(delegate).instantiate(componentClass, bindings);
    }

    public void resolveComponent(final Class<?> api) {
        ((ObservedComponentContainer) guard.access(delegate)).resolveComponent(api);
    }

    public void resolveGroup(final Class<?> api) {
        ((ObservedComponentContainer) guard.access(delegate)).resolveGroup(api);
    }

    public void enable() {
        guard.enable();
    }

    private static class AccessGuard {

        private final AtomicBoolean enabled;

        private AccessGuard() {
            this(new AtomicBoolean(false));
        }

        private AccessGuard(final AtomicBoolean enabled) {
            this.enabled = enabled;
        }

        public ComponentContainer access(final ComponentContainer valid) {
            if (enabled.get()) {
                return valid;
            } else {
                throw new ResolutionException("No dynamic dependencies allowed, use a ComponentFactory if you need such functionality");
            }
        }

        public void enable() {
            enabled.set(true);
        }
    }
}
