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

package org.fluidity.composition.container;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.Components;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.container.spi.DependencyResolver;

/**
 * A dependency injection container interface.
 *
 * @author Tibor Varga
 */
interface SimpleContainer extends DependencyGraph {

    /**
     * Returns the container services provided by Fluid Tools.
     *
     * @return the container services provided by Fluid Tools.
     */
    ContainerServices services();

    /**
     * Returns the parent container, if any.
     *
     * @return the parent container, if any, <code>null</code> otherwise.
     */
    SimpleContainer parentContainer();

    /**
     * Creates and returns a new container with the receiving container as its parent.
     *
     * @param domain <code>true</code> if the new child will be a domain container, <code>false</code> otherwise.
     *
     * @return a new container with the receiving container as its parent.
     */
    SimpleContainer newChildContainer(boolean domain);

    /**
     * Finds the component mapping for the given component interface.
     *
     * @param api    the component interface to find the mapping for.
     * @param ascend tells whether the parent container, if any, should be consulted in case no mapping is found in the receiver.
     *
     * @return the component mapping for the given component interface or <code>null</code> if none found.
     */
    ComponentResolver resolver(Class<?> api, boolean ascend);

    /**
     * Resolves a component using the given traversal in the given context.
     *
     * @param domain    the domain container to resolve missing dependencies in.
     * @param ascend    tells whether to consult the parent on a missing resolver (<code>true</code>) or not (<code>false</code>).
     * @param api       the component interface.
     * @param context   the component context at the point of resolution.
     * @param traversal the graph traversal to use.
     * @param reference the parameterized type of the dependency reference.
     *
     * @return the resolved component or <code>null</code> if none could be resolved.
     */
    Node resolveComponent(ParentContainer domain, boolean ascend, Class<?> api, ContextDefinition context, Traversal traversal, Type reference);

    /**
     * Binds a component mapping in the container
     *
     * @param key   the component interface to bind.
     * @param entry the mapping to bind.
     *
     * @return the bound resolver.
     *
     * @throws org.fluidity.composition.ComponentContainer.BindingException
     *          when binding fails.
     */
    ComponentResolver bindResolver(Class<?> key, ComponentResolver entry) throws ComponentContainer.BindingException;

    /**
     * Binds a component class to an interface.
     *
     * @param interfaces the component and group interfaces to bind to.
     *
     * @throws ComponentContainer.BindingException
     *          when binding fails.
     */
    void bindComponent(Components.Interfaces interfaces) throws ComponentContainer.BindingException;

    /**
     * Binds a component instance to an interface.
     *
     * @param instance   the component instance to bind.
     * @param interfaces the component and group interfaces to bind to.
     *
     * @throws ComponentContainer.BindingException
     *          when binding fails.
     */
    void bindInstance(Object instance, Components.Interfaces interfaces) throws ComponentContainer.BindingException;

    /**
     * Creates a child container and links the given interfaces to mappings added to the returned child, effectively directing the component
     * resolution for these interfaces in this container to the child.
     *
     * @param interfaces the component and group interfaces to bind to.
     *
     * @return the child container returned.
     *
     * @throws ComponentContainer.BindingException
     *          when binding fails.
     */
    SimpleContainer linkComponent(Components.Interfaces interfaces) throws ComponentContainer.BindingException;

    /**
     * Injects the {@link org.fluidity.composition.Inject @Inject} annotated fields of the given component instance.
     *
     * @param component the component instance.
     * @param context   the base context to resolve the component's dependencies in.
     * @param observer  the resolution observer to use.
     *
     * @return the component instance.
     *
     * @throws ComponentContainer.ResolutionException
     *          when dependency resolution fails.
     */
    Object initialize(Object component, ContextDefinition context, ComponentContainer.Observer observer) throws ComponentContainer.ResolutionException;

    /**
     * Injects all applicable parameters of the given method that the provided argument list contains no value for, and invokes it on the given interface.
     *
     * @param component the component instance.
     * @param method    the method to inject and invoke.
     * @param context   the base context to resolve the component's dependencies in.
     * @param arguments the method parameters already resolved.
     * @param explicit  tells if all parameters of the method are subject to injection (<code>true</code>) or only those annotated with {@link org.fluidity.composition.Inject @Inject}.
     *
     * @return the component instance.
     *
     * @throws ComponentContainer.ResolutionException
     *                                   when dependency resolution fails.
     * @throws InvocationTargetException when the method throws an exception.
     */
    Object invoke(Object component, Method method, ContextDefinition context, Object[] arguments, boolean explicit) throws InvocationTargetException;

    /**
     * Returns a textual identifier for the container.
     *
     * @return a textual identifier for the container.
     */
    String id();

    /**
     * Notifies each bound resolver that a previously bound resolver has been supplanted by another.
     *
     * @param key         the key for which the resolver is being replaced.
     * @param previous    the old resolver.
     * @param replacement the new resolver.
     */
    void replaceResolver(Class<?> key, ComponentResolver previous, ComponentResolver replacement);

    /**
     * Establishes the given resolution observer for the duration of the given command's execution in any container that may be involved.
     *
     * @param observer the resolution observer to establish; may be <code>null</code>.
     * @param command  the command to execute while the observer is active.
     * @param <T>      the return value type of the command.
     *
     * @return whatever the command returns.
     */
    <T> T observe(ComponentContainer.Observer observer, Observed<T> command);

    /**
     * Returns a {@link DependencyResolver} using the given domain container.
     *
     * @param domain the domain container.
     *
     * @return a {@link DependencyResolver} using the given domain container.
     */
    DependencyResolver dependencyResolver(ParentContainer domain);

    /**
     * A command that is executed while some resolution observer is active.
     *
     * @param <T> the return value type of the command.
     *
     * @author Tibor Varga
     */
    interface Observed<T> {

        /**
         * Executes the business logic while some resolution observer is active.
         *
         * @param traversal the strategy to use to traverse the dependency graph.
         *
         * @return whatever the caller of {@link SimpleContainer#observe(org.fluidity.composition.ComponentContainer.Observer, Observed)} expects.
         */
        T run(Traversal traversal);
    }
}
