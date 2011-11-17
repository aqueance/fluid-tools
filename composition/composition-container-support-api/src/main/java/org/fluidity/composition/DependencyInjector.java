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
import java.lang.reflect.Method;

import org.fluidity.composition.spi.ContextNode;
import org.fluidity.composition.spi.DependencyResolver;

/**
 * Performs dependency injection. This is an internal interface to be used by dependency injection container implementations.
 *
 * @author Tibor Varga
 */
public interface DependencyInjector {

    /**
     * Resolves all parameters of the given constructor and returns a dependency graph node representing the component instance that would be produced by that
     * constructor. The returned node's {@link DependencyGraph.Node#instance(DependencyGraph.Traversal)} method will instantiate the component and also inject
     * its {@link Inject @Inject} annotated fields.
     *
     * @param traversal   the current graph traversal.
     * @param container   the container to resolve dependencies of the component.
     * @param contexts    context node corresponding to the constructor's owning component.
     * @param context     the component context of the component.
     * @param constructor the constructor to resolve the arguments of.
     *
     * @return a dependency graph node.
     */
    DependencyGraph.Node constructor(DependencyGraph.Traversal traversal,
                                     DependencyResolver container,
                                     ContextNode contexts,
                                     ContextDefinition context,
                                     Constructor<?> constructor);

    /**
     * Injects all {@link Inject @Inject} annotated fields of the received component. Useful for components instantiated by means other than calling {@link
     * #constructor(DependencyGraph.Traversal, DependencyResolver, ContextNode, ContextDefinition, Constructor)}.
     *
     * @param instance   the object to inject the fields of.
     * @param traversal  the current graph traversal.
     * @param container  the container to resolve dependencies of the component.
     * @param contexts   context node corresponding to the component.
     * @param context    the component context of the provided instance.
     *
     * @return the received component.
     */
    <T> T fields(T instance, DependencyGraph.Traversal traversal, DependencyResolver container, ContextNode contexts, ContextDefinition context);

    /**
     * Injects all parameters of the given method and invokes the method on the given component.
     *
     * @param component  the object to call the method on.
     * @param method     the method to invoke.
     * @param traversal  the graph traversal to use.
     * @param container  the container to resolve method parameters.
     * @param contexts   context node corresponding to the component.
     * @param context    the component context of the given component.
     *
     * @return the received instance.
     *
     * @throws ComponentContainer.ResolutionException
     *          thrown when the search yields no or multiple constructors.
     */
    Object invoke(Object component,
                  Method method,
                  DependencyGraph.Traversal traversal,
                  DependencyResolver container,
                  ContextNode contexts,
                  ContextDefinition context) throws ComponentContainer.ResolutionException;

    /**
     * Injects all {@link Inject @Inject} annotated parameters of the given method with no value in the provided argument list and invokes the method on the
     * given component.
     *
     * @param component  the object to call the method on.
     * @param method     the method to invoke.
     * @param arguments  the argument list containing parameters that need no dependency injection.
     * @param traversal  the graph traversal to use.
     * @param container  the container to resolve method parameters.
     * @param contexts   context node corresponding to the component.
     * @param context    the component context of the given component.
     *
     * @return the received instance.
     *
     * @throws ComponentContainer.ResolutionException
     *          thrown when the search yields no or multiple constructors.
     */
    Object invoke(Object component,
                  Method method,
                  Object[] arguments,
                  DependencyGraph.Traversal traversal,
                  DependencyResolver container,
                  ContextNode contexts,
                  ContextDefinition context) throws ComponentContainer.ResolutionException;

    /**
     * Find the injectable constructor of the given class.
     * <p/>
     * There is no check for any constructor parameter being satisfiable. Synthetic constructors are ignored. If there is any constructor annotated with
     * {@link Inject @Inject}, that one is returned. If there's only one constructor, it is returned. If there is a default constructor and another, and
     * neither is annotated with <code>@Inject</code>, the other one is returned. If these checks do not yield a single constructor, the same checks are
     * made on public constructors only. If that yields no or multiple constructors, a {@link ComponentContainer.ResolutionException} is thrown.
     * <p/>
     * For synthetic constructors see <a href="http://java.sun.com/docs/books/jls/third_edition/html/binaryComp.html#13.1">The Form of a Binary</a> and search
     * for the term <em>synthetic</em>.
     *
     * @param componentClass the component class to find the injectable constructor of.
     *
     * @return the injectable constructor of the given class.
     *
     * @throws ComponentContainer.ResolutionException
     *          thrown when the search yields no or multiple constructors.
     */
    Constructor<?> findConstructor(Class<?> componentClass) throws ComponentContainer.ResolutionException;

    /**
     * Invokes the appropriate method of provided resolution object based on the type of the given dependency. Implements the special handling of non-regular
     * dependencies of a component. A dependency to a {@link ComponentContainer} will be <em>restricted</em>, meaning that it will only accept method calls
     * after it has been {@link RestrictedContainer#enable() enabled}.
     *
     * @param api        the dependency type.
     * @param resolution resolves the various dependency types.
     *
     * @return a resolved node or <code>null</code> if the given dependency could not be resolved.
     */
    DependencyGraph.Node resolve(Class<?> api, Resolution resolution);

    /**
     * Callback methods to resolve the various types of special and regular dependencies.
     */
    public interface Resolution {

        /**
         * A {@link ComponentContext} is being depended on.
         *
         * @return the resolved component context.
         */
        ComponentContext context();

        /**
         * A {@link ComponentContainer} is being depended on.
         *
         * @return the resolved component container.
         */
        ComponentContainer container();

        /**
         * A regular dependency is being depended on.
         *
         * @return the resolved dependency.
         */
        DependencyGraph.Node regular();

        /**
         * A restricted container has been resolved. The receiver can stash it somewhere and enable it later.
         *
         * @param container the restricted container that has been resolved.
         */
        void handle(RestrictedContainer container);
    }
}
