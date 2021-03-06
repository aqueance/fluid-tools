/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.container;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.spi.ContextNode;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.container.spi.DependencyResolver;
import org.fluidity.foundation.Exceptions;

/**
 * Performs dependency injection. This is an internal interface to be used by dependency injection container implementations.
 * <h3>Usage</h3>
 * <pre>
 * final {@linkplain ContainerServices} services = &hellip;;
 * final <span class="hl1">DependencyInjector</span> injector = services.dependencyInjector();
 * &hellip;
 * final Constructor&lt;<span class="hl2">MyComponent</span>&gt; injector.findConstructor(<span class="hl2">MyComponent</span>.class);
 * </pre>
 *
 * @author Tibor Varga
 */
public interface DependencyInjector {

    /**
     * Resolves all parameters of the given constructor and returns a dependency graph node representing the component instance that would be produced by that
     * constructor. The returned node's {@link
     * org.fluidity.composition.container.spi.DependencyGraph.Node#instance(DependencyGraph.Traversal) DependencyGraph.Node.instance()} method will instantiate
     * the component and also inject its {@link org.fluidity.composition.Inject @Inject} annotated fields.
     *
     * @param api         the component interface of the component to construct.
     * @param traversal   the current graph traversal.
     * @param container   the container to resolve dependencies of the component.
     * @param contexts    context node corresponding to the constructor's owning component.
     * @param context     the current context definition.
     * @param constructor the constructor to resolve the arguments of.
     *
     * @return a dependency graph node.
     */
    DependencyGraph.Node constructor(Class<?> api,
                                     DependencyGraph.Traversal traversal,
                                     DependencyResolver container,
                                     ContextNode contexts,
                                     ContextDefinition context,
                                     Constructor<?> constructor);

    /**
     * Injects all {@link org.fluidity.composition.Inject @Inject} annotated fields of the received component. Useful for components instantiated by means
     * other than calling {@link #constructor(Class, DependencyGraph.Traversal, DependencyResolver, ContextNode, ContextDefinition, Constructor)
     * DependencyInjector.constructor()}.
     *
     * @param instance  the object to inject the fields of.
     * @param traversal the current graph traversal.
     * @param container the container to resolve dependencies of the component.
     * @param contexts  context node corresponding to the component.
     * @param context   the current context definition.
     * @param <T>       the type of the given <code>instance</code> to inject the fields of.
     *
     * @return the received component.
     */
    <T> T fields(T instance, DependencyGraph.Traversal traversal, DependencyResolver container, ContextNode contexts, ContextDefinition context);

    /**
     * Injects the parameters of the given method that the provided argument list contains no value for, and invokes the method on the given component.
     *
     * @param component the object to call the method on.
     * @param method    the method to invoke.
     * @param arguments the argument list containing parameters that need no dependency injection.
     * @param traversal the graph traversal to use.
     * @param container the container to resolve method parameters.
     * @param contexts  context node corresponding to the component.
     * @param context   the current context definition.
     * @param explicit  tells whether all method arguments are subject to injection (<code>true</code>) or only those annotated with {@link
     *                  org.fluidity.composition.Inject @Inject}.
     *
     * @return the received instance.
     *
     * @throws ComponentContainer.ResolutionException
     *                                   thrown when the search yields no or multiple constructors.
     * @throws Exceptions.Wrapper when the method throws an exception.
     */
    Object invoke(Object component,
                  Method method,
                  Object[] arguments,
                  DependencyGraph.Traversal traversal,
                  DependencyResolver container,
                  ContextNode contexts,
                  ContextDefinition context,
                  boolean explicit) throws ComponentContainer.ResolutionException, Exceptions.Wrapper;

    /**
     * Find the injectable constructor of the given class.
     * <p>
     * There is no check for any constructor parameter being satisfiable. Synthetic constructors are ignored. If there is any constructor annotated with
     * {@link org.fluidity.composition.Inject @Inject}, that one is returned. If there's only one constructor,
     * it is returned. If there is a default constructor
     * and another, and
     * neither is annotated with <code>@Inject</code>, the other one is returned. If these checks do not yield a single constructor, the same checks are
     * made on public constructors only. If that yields no or multiple constructors, a {@link org.fluidity.composition.ComponentContainer.ResolutionException}
     * is thrown.
     * <p>
     * For synthetic constructors see the <a href="http://java.sun.com/docs/books/jls/third_edition/html/binaryComp.html#13.1">Java Language Specification: The
     * Form of a Binary</a> and search for the term <em>synthetic</em>.
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
     * Handles the various special and regular dependency types by invokes the appropriate method of the provided resolution object based on the type of the
     * given dependency. A dependency to a {@link ComponentContainer} will be <em>restricted</em>, meaning that it will only resolve components and component
     * groups after it has been {@link AccessGuard#enable() enabled}.
     *
     * @param api        the dependency type.
     * @param guard      the object to guard against access from constructors to dependency injected containers; never <code>null</code> and can be created by
     *                   {@link #containerGuard()}.
     * @param resolution resolves the various dependency types.
     *
     * @return a resolved node or <code>null</code> if the given dependency could not be resolved.
     */
    DependencyGraph.Node resolve(Class<?> api, AccessGuard<ComponentContainer> guard, Resolution resolution);

    /**
     * Creates an object that guards against access to a container. Used to create the <code>guard</code> parameter of {@link #resolve(Class, AccessGuard,
     * DependencyInjector.Resolution)}.
     *
     * @return an object that guards against access to a container.
     */
    AccessGuard<ComponentContainer> containerGuard();

    /**
     * Callback methods to resolve the various types of special and regular dependencies.
     * <h3>Usage</h3>
     * <pre>
     * final {@linkplain ContainerServices} services = &hellip;;
     * final {@linkplain DependencyInjector} injector = services.dependencyInjector();
     * &hellip;
     * final {@linkplain org.fluidity.composition.container.spi.DependencyGraph.Node} node = injector.resolve(&hellip;, new <span
     * class="hl1">DependencyInjector.Resolution</span>() {
     *   &hellip;
     * });
     * </pre>
     *
     * @author Tibor Varga
     */
    interface Resolution {

        /**
         * Resolves a {@link org.fluidity.composition.ComponentContext}.
         *
         * @return the resolved component context.
         */
        ComponentContext context();

        /**
         * Resolves a {@link ComponentContainer} is being depended on.
         *
         * @return the resolved component container.
         */
        ComponentContainer container();

        /**
         * Resolves a regular dependency.
         *
         * @return the resolved dependency.
         */
        DependencyGraph.Node regular();
    }
}
