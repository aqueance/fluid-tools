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

package org.fluidity.composition;

/**
 * A dependency injection container that components can be added to, using the container's {@linkplain ComponentContainer.Registry registry}.
 * <h3>Usage</h3>
 * You don't directly interact with an internal interface.
 *
 * @author Tibor Varga
 * @see OpenContainer
 * @see org.fluidity.composition.container.spi.EmptyComponentContainer
 */
@SuppressWarnings("JavadocReference")
public interface MutableContainer extends OpenContainer {

    /**
     * {@inheritDoc}
     */
    MutableContainer makeChildContainer(Bindings... bindings);

    /**
     * Returns the object through which component bindings can be added to this container.
     *
     * @return a <code>ComponentContainer.Registry</code> instance.
     */
    Registry getRegistry();
}
