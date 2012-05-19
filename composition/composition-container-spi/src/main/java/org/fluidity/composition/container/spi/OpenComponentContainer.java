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

import org.fluidity.composition.ComponentContainer;

/**
 * A dependency injection container that components can be added to, using the container's {@linkplain
 * org.fluidity.composition.ComponentContainer.Registry registry}. You rarely interact with this interface except when working with {@linkplain
 * ComponentContainer#makeDomainContainer(ComponentContainer.Bindings...) domain containers}.
 * <h3>Usage</h3>
 * <pre>
 * final {@linkplain ComponentContainer} container = ...; // got this one from somewhere
 *
 * final <span class="h1">OpenComponentContainer</span> domain = container.makeDomainContainer();
 * final {@linkplain org.fluidity.composition.ComponentContainer.Registry} registry = domain.getRegistry();
 *
 * registry.bindComponent(<span class="hl2">MyComponent</span>.class);
 *
 * final <span class="hl2">MyComponent</span> component = domain.getComponent(<span class="hl2">MyComponent</span>.class);
 * </pre>
 *
 * @author Tibor Varga
 * @see ComponentContainer
 * @see EmptyComponentContainer
 */
@SuppressWarnings("JavadocReference")
public interface OpenComponentContainer extends ComponentContainer {

    /**
     * Creates another container with this one as its parent.
     *
     * @return a container that defaults to this container for satisfying component dependencies.
     */
    OpenComponentContainer makeChildContainer();

    /**
     * Returns the object through which component bindings can be added to this container.
     *
     * @return a <code>ComponentContainer.Registry</code> instance.
     */
    Registry getRegistry();
}
