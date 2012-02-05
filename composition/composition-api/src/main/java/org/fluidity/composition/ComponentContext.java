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
 * The <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Component_Context">run-time context</a> for a component instance. This is the object that
 * components may receive as a dependency to encapsulate the context in which they were instantiated.
 * <p/>
 * A context represents configuration at the point of reference to a component, which it elects to receive using the
 * {@link Component.Context @Component.Context} annotation. The context is defined by the referring components using custom, user defined, annotations, and are
 * consumed by referred to components that list the custom annotations' type in their <code>@Component.Context</code> annotation. For example:
 * <pre>
 * &#64;Component
 * <span class="hl2">&#64;MyContext1</span>
 * public class ContextProvider {
 *
 *     public ContextProvider(final <span class="hl2">&#64;MyContext2</span> dependency) {
 *         ...
 *     }
 *
 *     ...
 * }
 *
 * &#64;Component
 * <span class="hl3">{@linkplain Component.Context @Component.Context}</span>({ <span class="hl2">MyContext1</span>.class,  <span class="hl2">MyContext2</span>.class })
 * final class Dependency {
 *
 *     public Dependency(final <span class="hl1">ComponentContext</span> context) {
 *
 *         // all MyContext1 annotations in the instantiation path of this object
 *         <span class="hl1">MyContext1</span>[] context1s = context.annotations(<span class="hl2">MyContext1</span>.class);
 *
 *         // the last MyContext2 annotation in the instantiation path of this object
 *         <span class="hl2">MyContext2</span> context2 = context.<span class="hl1">annotation</span>(<span class="hl2">MyContext2</span>.class, Dependency.class);
 *     }
 * }
 * </pre>
 * Essentially, the contexts offers a static configuration mechanism. Components with other means of configuration can be adapted to context based
 * configuration using a {@link org.fluidity.composition.spi.ComponentVariantFactory ComponentVariantFactory}, which, when invoked by the dependency injection
 * container in place of component instantiation, translates the configuration embedded in the instantiation context to configuration understood by the
 * component being adapted.
 *
 * @author Tibor Varga
 */
public interface ComponentContext {

    /**
     * Returns all context annotation instances of the given type in the instantiation path of the caller that received this object in its constructor.
     * Annotations may be defined at multiple points along a reference path and so multiple annotations may be present for any given type. This method returns
     * all of the instances according to the annotation's {@linkplain Component.Context#collect() accumulation} setting in the
     * order they were encountered in the reference path.
     *
     * @param type the annotation type to return instances of.
     *
     * @return all context annotations of the given type, or an empty array or <code>null</code> if none present.
     *
     * @see #defines(Class)
     */
    <T extends Annotation> T[] annotations(Class<T> type);

    /**
     * Returns the last context annotation instance of the given type in the instantiation path of the caller that received this object, which may also be
     * passed as the second parameter. If given and no annotation of the given type is found, an exception is thrown.
     *
     * @param type   the annotation type to return instances of.
     * @param caller the optional caller to mention in the exception thrown when no annotation of the given type is present; may be <code>null</code>.
     *
     * @return the last context annotation of the given type or <code>null</code> if none present.
     *
     * @throws ComponentContainer.ResolutionException
     *          when the <code>caller</code> parameter is not <code>null</code> and no annotation of the given <code>type</code> is found.
     * @see #defines(Class)
     */
    <T extends Annotation> T annotation(Class<T> type, Class<?> caller) throws ComponentContainer.ResolutionException;

    /**
     * Tells whether the context contains an annotation of the given type.
     *
     * @param type the annotation type to check the existence of instances of.
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
