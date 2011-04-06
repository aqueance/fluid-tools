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

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.OpenComponentContainer;

/**
 * A variant factory offers context dependent instance variants of an otherwise singleton component that itself can in some way be configured to adapt to
 * various externally defined contexts. The variant factory lists the context annotations it understands in its {@link org.fluidity.composition.Context} class
 * annotation.
 * <p/>
 * A {@link ComponentVariantFactory} works in conjunction with an otherwise singleton component independently registered in a dependency injection container
 * accessible to the factory with a binding that allows new instances to be created, i.e., the component has not been bound by {@link
 * ComponentContainer.Registry#bindInstance(Object, Class[])} or {@link ComponentContainer.Registry#bindInstance(Object, Class[])}.
 *
 * @author Tibor Varga
 */
public interface ComponentVariantFactory {

    /**
     * Binds in the provided container the dependencies of the component that this is a factory for.
     *
     * @param container is the container to resolve dependencies of the component from.
     * @param context   is the context for the instance to create. When this is null or empty, the default instance must be returned. The key set in the context
     *                  is taken from the list of annotation classes in the {@link org.fluidity.composition.Context} annotation of this factory.
     *
     * @throws ComponentContainer.ResolutionException
     *          of a component cannot be created.
     */
    void newComponent(OpenComponentContainer container, ComponentContext context) throws ComponentContainer.ResolutionException;
}
