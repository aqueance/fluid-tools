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
import java.util.Collection;
import java.util.Set;

/**
 * Provides a runtime context for components. A context represents configuration at the point of reference to a component, which it elects to receive using the
 * {@link Context} annotation. Contexts are provided by components that depend, directly or indirectly, on context consuming components using custom, user
 * defined, annotations. The context consuming components list these annotation types in their respective {@link Context} annotations.
 * <p/>
 * Context support can be added to a component that itself does not support contexts using {@link org.fluidity.composition.spi.ComponentVariantFactory}
 * components as long as the component does supports some other configuration mechanism that can be manipulated by the variant factory class.
 *
 * @author Tibor Varga
 */
public interface ComponentContext {

    /**
     * Returns all annotations in the context of the specified type. Annotations may be defined at multiple points along a reference chain hence multiple
     * annotations may be present for any given type. This method returns all of them in the order of decreasing distance from the reference being queried.
     *
     * @param type the annotation type to return instances of.
     *
     * @return all annotations in the context for the specified type, or an empty array or null if none present.
     *
     * @see #defines(Class)
     */
    <T extends Annotation> T[] annotations(Class<T> type);

    /**
     * Returns the annotation in the context for the specified type that was defined nearest to the component reference being queried.
     *
     * @param type      the annotation type to return instances of.
     * @param reference the reference whose annotation is being queried. The method throws a {@link ComponentContainer.ResolutionException} exception if this
     *                  parameter is not <code>null</code> and no annotation is found for the given <code>type</code>.
     *
     * @return the nearest annotation in the context for the specified type or null if none present.
     *
     * @throws org.fluidity.composition.ComponentContainer.ResolutionException
     *          when the <code>reference</code> parameter is not <code>null</code> and no annotation of the given <code>type</code> is found.
     * @see #defines(Class)
     */
    <T extends Annotation> T annotation(Class<T> type, Class<?> reference) throws ComponentContainer.ResolutionException;

    /**
     * Tells whether the context contains an annotation of the given type.
     *
     * @param type the annotation type to check the existence of instances thereof.
     *
     * @return <code>true</code> if there is at least one annotation of the given type, <code>false</code> otherwise.
     */
    boolean defines(Class<? extends Annotation> type);

    /**
     * Returns the set of annotation types the context contains instances of.
     *
     * @return the set of annotation types the context contains instances of.
     */
    Set<Class<? extends Annotation>> types();

    /**
     * Adds the context definitions to the context.
     *
     * @param definition the annotations potentially defining new context.
     *
     * @return the receiver.
     */
    ComponentContext expand(Annotation[] definition);

    /**
     * Reduces the returned context annotations to those specified by the parameter.
     *
     * @param accepted the {@link Context} annotation listing the accepted annotations.
     *
     * @return the receiver.
     */
    ComponentContext reduce(Context accepted);

    /**
     * Adds the accepted context annotations from all supplied contexts to the accepted annotations of the receiver.
     *
     * @param contexts a list of context accepted during component instantiations downstream.
     *
     * @return the receiver.
     */
    ComponentContext collect(Collection<ComponentContext> contexts);

    /**
     * Returns a new copy of the receiver.
     *
     * @return a new copy of the receiver.
     */
    ComponentContext copy();

    /**
     * Returns a component context that can be used to cache component instances for this context.
     *
     * @return a component context that can be used to cache component instances for this context.
     */
    ComponentContext key();
}
