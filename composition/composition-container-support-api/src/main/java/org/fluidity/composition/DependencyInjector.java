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
     * Resolves all parameters of the given constructor.
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

    /**
     * Find the injectable constructor of the given class.
     * <p/>
     * There is no check for any constructor parameter being satisfiable. Synthetic constructors are ignored. If there is any constructor annotated with
     * @{@link Inject}, all other constructors are ignored. If there's only one constructor, it is returned. If there is a default constructor and another, and
     * neither is annotated with @Component, the other one is returned. If there are more constructors, the only one annotated with @{@link Inject} is returned.
     * If these checks do not yield a single constructor, the same is repeated for public constructors only. If that yields no or multiple constructors, a
     * ComponentContainer.ResolutionException is thrown.
     * <p/>
     * For synthetic constructors see http://java.sun.com/docs/books/jls/third_edition/html/binaryComp.html#13.1, "synthetic"
     *
     * @param componentClass the component class to find the injectable constructor of.
     *
     * @return the injectable constructor of the given class.
     * @throws org.fluidity.composition.ComponentContainer.ResolutionException thrown when the search yields no or multiple constructors.
     */
    Constructor<?> findConstructor(Class<?> componentClass) throws ComponentContainer.ResolutionException;
}
