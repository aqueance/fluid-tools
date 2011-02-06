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

import org.fluidity.composition.spi.ComponentMapping;

/**
 * Extends the {@link ComponentMapping} interface with methods required by the actual container implementation.
 *
 * @author Tibor Varga
 */
interface ComponentResolver extends ComponentMapping {

    /**
     * Returns the relative priority of this mapping compared to another.
     *
     * @return the relative priority of this mapping compared to another.
     */
    int priority();

    /**
     * Tells whether the mapping is that of a {@link org.fluidity.composition.spi.ComponentVariantFactory}.
     *
     * @return <code>true</code> if the mapping is that of a {@link org.fluidity.composition.spi.ComponentVariantFactory}, <code>false</code> otherwise.
     */
    boolean isVariantMapping();

    /**
     * Tells whether this mapping has been created for an already instantiated component.
     *
     * @return <code>true</code> if this mapping represents an already instantiated component, <code>false</code> otherwise.
     */
    boolean isInstanceMapping();

    /**
     * Tells if the receiver replaces the supplied <code>resolver</code>.
     *
     * @param resolver the resolver that the receiver may need to replace.
     *
     * @return <code>true</code> if the supplied <code>resolver</code> is replaced by the receiver.
     */
    boolean replaces(ComponentResolver resolver);

    /**
     * Creates and/or returns a component.
     *
     * @param lineage   the object that keeps track of dependency reference chains.
     * @param context   the context for the component.
     * @param container the container to use to resolve dependencies.
     * @param api       the API the component is requested for.
     *
     * @return the component instance, never <code>null</code>.
     */
    Object getComponent(DependencyChain.Lineage lineage, ContextDefinition context, SimpleContainer container, Class<?> api);

    /**
     * Notifies the receiver that a previously bound resolver has been replaced by another one.
     *
     * @param api         the api for which the resolver is being replaced
     * @param previous    the old resolver.
     * @param replacement the new resolver.
     */
    void resolverReplaced(Class<?> api, ComponentResolver previous, ComponentResolver replacement);
}
