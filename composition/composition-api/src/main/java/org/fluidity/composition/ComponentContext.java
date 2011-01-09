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
import java.util.Set;

/**
 * Provides a runtime context for components. A context represents configuration at the point of reference to a component, which the referring component elects
 * to receive using {@link Context} annotation. Contexts are provided by components that depend, directly or indirectly, on context consuming components using
 * custom, user defined, annotations. The context consuming components list these annotation types in their respective {@link Context} annotations.
 * <p/>
 * Context support can be added to a component that itself does not support contexts using {@link org.fluidity.composition.spi.ComponentVariantFactory}
 * components as long as the component does supports some other configuration mechanism that can be manipulated by the variant factory class.
 *
 * @author Tibor Varga
 */
public interface ComponentContext {

    /**
     * Returns all annotations in the context for the specified type.
     *
     * @param type the annotation type to return instances of.
     *
     * @return all annotations in the context for the specified type or null if none present.
     *
     * @see #defines(Class)
     */
    <T extends Annotation> T[] annotations(Class<T> type);

    /**
     * Returns the last annotation in the context for the specified type.
     *
     * @param type the annotation type to return instances of.
     *
     * @return the last annotation in the context for the specified type or null if none present.
     *
     * @see #defines(Class)
     */
    <T extends Annotation> T annotation(Class<T> type);

    /**
     * Tells whether the context defines a value for the given key.
     *
     * @param type the annotation type to check the existence of instances thereof.
     *
     * @return <code>true</code> if there is at least one annotation of the given type, <code>false</code> otherwise.
     */
    boolean defines(Class<? extends Annotation> type);

    /**
     * Returns the set of keys the context defines a value for.
     *
     * @return the set of keys the context defines a value for.
     */
    Set<Class<? extends Annotation>> types();
}
