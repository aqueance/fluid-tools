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

import org.fluidity.composition.ObservedComponentContainer;
import org.fluidity.composition.OpenComponentContainer;

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
        final OpenComponentContainer child = makeChildContainer();
        bindings.bindComponents(child.getRegistry());
        return child.getComponent(api);
    }

    /**
     * Implements this convenience method using the primary methods of the container.
     * <p/>
     * {@inheritDoc}
     */
    public final <T> T instantiate(final Class<T> componentClass) throws ResolutionException {
        final OpenComponentContainer container = makeChildContainer();
        container.getRegistry().bindComponent(componentClass, componentClass);
        return container.getComponent(componentClass);
    }

    public final Registry getRegistry() {
        return registry;
    }
}
