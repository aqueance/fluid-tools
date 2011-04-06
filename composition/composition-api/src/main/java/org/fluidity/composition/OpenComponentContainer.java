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

/**
 * This is a dependency injection container that components can be added to.
 *
 * @author Tibor Varga
 */
public interface OpenComponentContainer extends ComponentContainer {

    /**
     * Returns the interface through which component bindings can be added to this container. The returned interface cannot be used to get components out of the
     * container. Thus, a container is write-only when it is being populated and read-only after it has been populated.
     *
     * @return a {@link ComponentContainer.Registry} instance.
     */
    ComponentContainer.Registry getRegistry();
}
