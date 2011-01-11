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

/**
 * Provides context related functionality to the container.
 * <p/>
 * Contexts are the means to have multiple instances of the same component with different configuration by components that directly support contexts or that
 * have been complemented by a {@link org.fluidity.composition.spi.ComponentVariantFactory} object to add context support.
 * <p/>
 * A context aware component or a {@link org.fluidity.composition.spi.ComponentVariantFactory} declares, using the {@link org.fluidity.composition.Context}
 * annotation, context keys that they consume. Upon instantiation a context is passed to such a component or the variant factory in the form of a {@link
 * java.util.Properties} object with keys that are a subset of the keys the component or factory declares to support.
 * <p/>
 * The values in the context object are calculated from a base context and any context encountered during the entire dependency chain starting at that base and
 * ending at the instantiation of a given component.
 * <p/>
 * The base context comes from either a {@link org.fluidity.composition.Context} annotated component, or a field or constructor parameter reference thereto, or
 * the prevalent context at the instantiation of a component that depends on and uses a {@link org.fluidity.composition.ComponentContainer} directly to get
 * other components from.
 * <p/>
 * Further contexts are added to the base at each {@link org.fluidity.composition.Context} annotated reference along a dependency chain. The context consumed by
 * a context aware component is inherited back through the dependency chain ending with the instantiation of that component up to the point where the context is
 * fully defined and may cause contextual instantiation of components in between that are not themselves context aware.
 *
 * @author Tibor Varga
 */
public interface ContextChain {

    /**
     * Returns the context currently established.
     *
     * @return the context currently established.
     */
    ComponentContext currentContext();

    /**
     * Adds the given properties to the stack and then removes it when the given command completes.
     *
     * @param context the context to add to the stack.
     * @param command the command that will receive the new context, which includes the given properties and all others from the stack.
     *
     * @return whatever the given command returns.
     */
    <T> T nested(ComponentContext context, Command<T> command);

    /**
     * Returns the actual context supported by the given component type using the possibly larger context established at this point.
     *
     * @param componentType  the interface implemented by the component.
     * @param componentClass the component type to check for supported context annotations.
     * @param context        the actual context established at this point.
     * @param resolutions    the dependency resolution chain to search for {@link org.fluidity.composition.spi.ComponentVariantFactory} objects that consume
     *                       contexts on behalf of other components.
     *
     * @return the narrowed context that can be passed to an instance of the given component class.
     */
    ComponentContext consumedContext(Class<?> componentType, Class<?> componentClass, ComponentContext context, ReferenceChain resolutions);

    /**
     * Returns the context supported ahead at this point and ahead in the resolution chain.
     *
     * @param context the context consumed at the point of calling this method.
     *
     * @return the context supported ahead at this point and ahead in the resolution chain.
     */
    ComponentContext consumedContext(ComponentContext context);

    /**
     * A command to run while establishing a new context. The established context is restored to its pre-flight value after the command completes.
     */
    interface Command<T> {

        /**
         * Runs the command in a new component context.
         *
         * @param context the context established at the point of invocation.
         *
         * @return whatever the caller of {@link ContextChain#nested(ComponentContext, ContextChain.Command)}
         *         wishes to receive back.
         */
        T run(ComponentContext context);
    }
}
