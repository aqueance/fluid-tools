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

package org.fluidity.composition;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * The <a href="https://github.com/aqueance/fluid-tools/wiki/User-Guide---Composition#consuming-context">run-time context</a> for a component instance. This is
 * the object that components may receive as a dependency to encapsulate the context in which they were instantiated.
 * <p>
 * A context represents configuration at the point of reference to a component, which it elects to receive using the
 * {@link Component.Context @Component.Context} annotation. The context is defined by the referring components using custom, user defined, annotations, and are
 * consumed by referred to components that list the custom annotations' type in their <code>@Component.Context</code> annotation.
 * <p>
 * Since contexts offer a static configuration mechanism, components with other means of configuration can be adapted to context based configuration
 * using a {@link org.fluidity.composition.spi.ComponentFactory ComponentFactory}, which essentially translates the configuration embedded in the instantiation
 * context to configuration understood by the component being adapted.
 * <h3>Usage</h3>
 * <pre>
 * &#64;Retention(RetentionPolicy.RUNTIME)
 * public @interface <span class="hl2">MyContext1</span> { &hellip; }
 * </pre>
 * <pre>
 * &#64;Retention(RetentionPolicy.RUNTIME)
 * public @interface <span class="hl2">MyContext2</span> { &hellip; }
 * </pre>
 * <pre>
 * <span class="hl2">&#64;MyContext1</span>
 * {@linkplain Component @Component}
 * public final class ContextProvider {
 *
 *     ContextProvider(final <span class="hl2">&#64;MyContext2</span> dependency) {
 *         &hellip;
 *     }
 *
 *     &hellip;
 * }
 * </pre>
 * <pre>
 * {@linkplain Component @Component}
 * <span class="hl1">{@linkplain Component.Context @Component.Context}</span>({ <span class="hl2">MyContext1</span>.class,  <span class="hl2">MyContext2</span>.class })
 * final class <span class="hl3">Dependency</span> {
 *
 *     <span class="hl3">Dependency</span>(final <span class="hl1">ComponentContext</span> context) {
 *
 *         // all MyContext1 annotations in the instantiation path of this object
 *         <span class="hl2">MyContext1</span>[] context1s = context.<span class="hl1">annotations</span>(<span class="hl2">MyContext1</span>.class);
 *
 *         // the last MyContext2 annotation in the instantiation path of this object
 *         <span class="hl2">MyContext2</span> context2 = context.<span class="hl1">annotation</span>(<span class="hl2">MyContext2</span>.class, <span class="hl3">Dependency</span>.class);
 *     }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
public interface ComponentContext {

    /**
     * Returns all instances of the given context annotation type in the instantiation path of the component that received this object in its constructor.
     * Annotations may be defined at multiple points along a reference path and so multiple annotations may be present for any given type. This method returns
     * all of the instances according to the annotation's {@linkplain Component.Context#collect() accumulation} setting in the
     * order they were encountered in the reference path.
     *
     * @param type the annotation type to return instances of.
     * @param <T>  the annotation type.
     *
     * @return all context annotations of the given type, or an empty array or <code>null</code> if none present.
     *
     * @see #defines(Class)
     */
    <T extends Annotation> T[] annotations(Class<T> type);

    /**
     * Returns the last instance of the given context annotation type in the instantiation path of the component that received this object in its constructor.
     * If an optional <code>caller</code> argument is supplied and no annotation of the given type is found, an exception mentioning the given
     * <code>caller</code> is thrown.
     *
     * @param type   the annotation type to return instances of.
     * @param caller the optional caller to mention in the exception thrown when no annotation of the given type is present; may be <code>null</code>.
     * @param <T>    the annotation type.
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
     * @return <code>true</code> if least one instance of the annotation of the given type is found, <code>false</code> otherwise.
     */
    boolean defines(Class<? extends Annotation> type);

    /**
     * Returns the set of annotation types the context contains instances of.
     *
     * @return the set of annotation types the context contains instances of.
     */
    Set<Class<? extends Annotation>> types();

    /**
     * Returns a textual representation of this context that uniquely identifies it. Used internally by Fluid Tools.
     *
     * @return a textual representation of this context that uniquely identifies it.
     */
    String key();
}
