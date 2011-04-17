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

import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.spi.ComponentResolutionObserver;

/**
 * Wraps another container and denies access to it until {@link #enable()} is invoked.
 *
 * @author Tibor Varga
 */
public final class RestrictedContainer implements ComponentContainer {

    private final AtomicReference<ComponentContainer> reference = new AtomicReference<ComponentContainer>(new NoContainer());
    private final ComponentContainer delegate;

    RestrictedContainer(final ComponentContainer delegate) {
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

    public <T> T getComponent(final Class<T> api, final Bindings bindings) throws ResolutionException {
        return reference.get().getComponent(api, bindings);
    }

    public <T> T initialize(final T component) throws ResolutionException {
        return reference.get().initialize(component);
    }

    public <T> T instantiate(final Class<T> componentClass) throws ResolutionException {
        return reference.get().instantiate(componentClass);
    }

    public void enable() {
        reference.set(delegate);
    }

    private class NoContainer implements ComponentContainer {
        private RuntimeException denied() {
            return new ResolutionException("No dynamic dependency allowed, use a ComponentFactory if you need such functionality");
        }

        public ObservedComponentContainer observed(final ComponentResolutionObserver observer) {
            throw denied();
        }

        public <T> T getComponent(final Class<T> api) throws ResolutionException {
            throw denied();
        }

        public <T> T[] getComponentGroup(final Class<T> api) {
            throw denied();
        }

        public OpenComponentContainer makeChildContainer() {
            throw denied();
        }

        public <T> T getComponent(final Class<T> api, final Bindings bindings) throws ResolutionException {
            throw denied();
        }

        public <T> T initialize(final T component) throws ResolutionException {
            throw denied();
        }

        public <T> T instantiate(final Class<T> componentClass) throws ResolutionException {
            throw denied();
        }
    }
}
