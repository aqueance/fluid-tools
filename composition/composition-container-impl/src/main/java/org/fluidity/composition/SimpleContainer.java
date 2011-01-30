/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.composition;

import java.util.List;

import org.fluidity.composition.spi.DependencyResolver;

/**
 * A dependency injection container interface.
 *
 * @author Tibor Varga
 */
interface SimpleContainer extends DependencyResolver {

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
     * @return a new container with the receiving container as its parent.
     */
    SimpleContainer newChildContainer();

    /**
     * Finds the component mapping for the given component interface.
     *
     * @param componentInterface the component interface to find the mapping for.
     * @param ascend             tells whether the parent container, if any, should be consulted in case no mapping is found in the receiver.
     *
     * @return the component mapping for the given component interface or <code>null</code> if none found.
     */
    ComponentProducer producer(Class<?> componentInterface, boolean ascend);

    /**
     * Binds a component mapping in the container
     *
     * @param key   the component interface to bind.
     * @param entry the mapping to bind.
     *
     * @return the mapping bound.
     *
     * @throws ComponentContainer.BindingException
     *          when binding fails.
     */
    ComponentProducer bindProducer(Class<?> key, ComponentProducer entry) throws ComponentContainer.BindingException;

    /**
     * Binds a component class to an interface.
     *
     * @param key            the interface to bind to.
     * @param implementation the class to bind.
     *
     * @return the mapping bound.
     *
     * @throws ComponentContainer.BindingException
     *          when binding fails.
     */
    ComponentProducer bindComponent(Class<?> key, Class<?> implementation) throws ComponentContainer.BindingException;

    /**
     * Binds a component instance to an interface.
     *
     * @param key      the interface to bind to.
     * @param instance the component instance to bind.
     *
     * @return the mapping bound.
     *
     * @throws ComponentContainer.BindingException
     *          when binding fails.
     */
    ComponentProducer bindInstance(Class<?> key, Object instance) throws ComponentContainer.BindingException;

    /**
     * Creates a child container of the receiver and links the given interface to a mapping added to the returned child, effectively directing the component
     * resolution in the parent container to the child.
     *
     * @param key            the interface to bind in the receiving container.
     * @param implementation the implementation to bind to the interface in the returned child container.
     *
     * @return the child container returned.
     *
     * @throws ComponentContainer.BindingException
     *          when binding fails.
     */
    SimpleContainer linkComponent(Class<?> key, Class<?> implementation) throws ComponentContainer.BindingException;

    /**
     * Returns the component instance bound to the given interface.
     *
     * @param key the interface to return the bound component to.
     *
     * @return the component instance bound to the given interface.
     *
     * @throws ComponentContainer.ResolutionException
     *          when dependency resolution fails.
     */
    <T> T get(Class<? extends T> key) throws ComponentContainer.ResolutionException;

    /**
     * Returns the component instance bound to the given interface in the given context.
     *
     * @param key     the interface to return the bound component to.
     * @param context the context to resolve the component instance in.
     *
     * @return the component instance bound to the given interface.
     *
     * @throws ComponentContainer.ResolutionException
     *          when dependency resolution fails.
     */
    <T> T get(Class<? extends T> key, ComponentContext context) throws ComponentContainer.ResolutionException;

    /**
     * Injects the {@link Component} annotated fields of the given component instance.
     *
     * @param component the component instance.
     *
     * @return the component instance.
     *
     * @throws ComponentContainer.ResolutionException
     *          when dependency resolution fails.
     */
    <T> T initialize(T component) throws ComponentContainer.ResolutionException;

    /**
     * Returns all singleton component instances, created in response to this method call, in the container under the default empty context assignable to the
     * given interface.
     *
     * @param componentInterface filters the component instances returned.
     *
     * @return all singleton component instances in the container under the default empty context assignable to the given interface.
     *
     * @throws ComponentContainer.ResolutionException
     *          when dependency resolution fails.
     */
    <T> List<T> allSingletons(Class<T> componentInterface) throws ComponentContainer.ResolutionException;

    /**
     * Returns a textual identifier for the container.
     *
     * @return a textual identifier for the container.
     */
    String id();
}
