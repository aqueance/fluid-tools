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

import java.lang.annotation.Annotation;

import org.fluidity.composition.spi.ComponentMapping;

/**
 * Extends the {@link ComponentMapping} interface with methods required by the actual container implementation.
 *
 * @author Tibor Varga
 */
interface ComponentResolver extends ComponentMapping {

    /**
     * Returns the list of annotations that may comprise the context of some other component. Factories do not provide context annotations.
     *
     * @return the list of annotations that may comprise the context of some other component or <code>null</code> if none present.
     */
    Annotation[] providedContext();

    /**
     * Tells if the component is a fallback or primary. When both a primary and fallback component is mapped to the same interface, the fallback is ignored and
     * the primary is used.
     *
     * @return <code>true</code> if the component is a fallback component, <code>false</code> if it is primary.
     */
    boolean isFallback();

    /**
     * Returns the component class in case of a {@link org.fluidity.composition.spi.ComponentVariantFactory} or a {@link sun.awt.ComponentFactory} mapping.
     *
     * @return the component class in case of a {@link org.fluidity.composition.spi.ComponentVariantFactory} or a {@link sun.awt.ComponentFactory} mapping.
     * @deprecated and will be removed
     */
    Class<?> factoryClass();

    /**
     * Returns the class of the component instance.
     *
     * @return the class of the component instance.
     * @deprecated and will be removed
     */
    Class<?> componentClass();

    /**
     * Creates the actual component.
     *
     * @param container the container to use to resolve dependencies.
     * @param api       the API the component is requested for.
     * @param circular  a flag telling if this resolver has been invoked the second time during one invocation chain, e.g., to signify circular reference in the
     *                  component dependency graph.
     *
     * @return the component instance, never <code>null</code>.
     */
    Object create(SimpleContainer container, Class<?> api, boolean circular);

    /**
     * Tells whether this mapping has been created for an already instantiated component.
     *
     * @return <code>true</code> if this mapping represents an already instantiated component, <code>false</code> otherwise.
     */
    boolean isInstanceMapping();

    /**
     * Notifies the receiver that a previously bound resolver has been replaced by another one.
     *
     * @param api         the api for which the resolver is being replaced
     * @param previous    the old resolver.
     * @param replacement the new resolver.
     */
    void resolverReplaced(Class<?> api, ComponentResolver previous, ComponentResolver replacement);
}
