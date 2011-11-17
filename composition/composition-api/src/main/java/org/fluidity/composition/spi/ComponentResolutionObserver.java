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

package org.fluidity.composition.spi;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Observes component dependency resolutions. An object implementing this interface can be passed to the {@link
 * org.fluidity.composition.ComponentContainer#observed(ComponentResolutionObserver) observed()} method of a <code>ComponentContainer</code> to get the
 * observer's methods invoked as components get resolved and instantiated by that container.
 *
 * @author Tibor Varga
 */
public interface ComponentResolutionObserver {

    /**
     * Notifies the receiver that a dependency is being resolved.
     *
     * @param declaringType        the class having the dependency to the <code>api</code>.
     * @param dependencyType       the class with which the <code>declaringType</code> references the dependency.
     * @param typeAnnotations      the annotations of the declaring class.
     * @param referenceAnnotations the annotations of the dependency reference.
     */
    void resolving(Class<?> declaringType, Class<?> dependencyType, Annotation[] typeAnnotations, Annotation[] referenceAnnotations);

    /**
     * Notifies the receiver that a dependency has been resolved. The path and type are not final, they may change as circular references are handled. Elements
     * of the path are reference declarations and may not be the actual classes that will be instantiated for those references.
     *
     * @param path the dependency path at which the given type has been resolved.
     * @param type the type that has been resolved at the given dependency path.
     */
    void resolved(DependencyPath path, Class<?> type);

    /**
     * Notifies the receiver that a dependency has been instantiated. The path and type are final. Elements of the path are actual classes that will be or have
     * been instantiated.
     * <p/>
     * The {@link DependencyPath#head()} returns details about the class just instantiated.
     *
     * @param path      the dependency path at which the given type has been instantiated.
     * @param reference a reference to the component that has just been instantiated. The reference will be set <em>after</em> this method returns to prevent
     *                  the receiver from wreaking havoc by accessing the just instantiated component.
     */
    void instantiated(DependencyPath path, AtomicReference<?> reference);
}
