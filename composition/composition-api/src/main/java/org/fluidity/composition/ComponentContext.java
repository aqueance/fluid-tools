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

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * The runtime context for a component. A context represents configuration at the point of reference to a component, which it elects to receive using the
 * {@link Context} annotation. Contexts are provided by components that depend, directly or indirectly, on context consuming components using custom, user
 * defined, annotations. The context consuming components list these annotation types in their respective {@link Context} annotations.
 * <p/>
 * TODO: example
 * <p/>
 * Context support can be added to a component that itself does not support contexts using a {@link org.fluidity.composition.spi.ComponentVariantFactory}
 * as long as the component does supports some other configuration mechanism that can be manipulated by the variant factory.
 *
 * @author Tibor Varga
 */
public interface ComponentContext {

    /**
     * Returns all annotations in the context of the specified type. Annotations may be defined at multiple points along a reference path hence multiple
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
}
