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

import java.lang.reflect.Constructor;

import org.fluidity.composition.spi.ComponentMapping;
import org.fluidity.composition.spi.DependencyResolver;

/**
 * Performs dependency injection.
 *
 * @author Tibor Varga
 */
public interface DependencyInjector {

    /**
     * Resolves all parameters of the given constructor and invokes it to construct a new object with it.
     *
     * @param traversal   the graph traversal to use.
     * @param container   the container that can resolve dependencies
     * @param mapping     the mapping that triggered the dependency resolution.
     * @param context     the instantiation context of the object being constructed.
     * @param constructor the constructor to find the arguments for.
     *
     * @return the argument array for the given constructor.
     */
    DependencyGraph.Node constructor(DependencyGraph.Traversal traversal,
                                     DependencyResolver container,
                                     ComponentMapping mapping,
                                     ContextDefinition context,
                                     Constructor<?> constructor);

    /**
     * Sets all {@link Inject} annotated fields of the receiver.
     *
     * @param traversal the graph traversal to use.
     * @param container the container that can resolve dependencies
     * @param mapping   the mapping that triggered the dependency resolution.
     * @param context   the instantiation context of the object being constructed.
     * @param instance  the object to set the fields of.     @return the received instances.
     *
     * @return the received instance.
     */
    <T> T fields(DependencyGraph.Traversal traversal, DependencyResolver container, ComponentMapping mapping, ContextDefinition context, T instance);
}
