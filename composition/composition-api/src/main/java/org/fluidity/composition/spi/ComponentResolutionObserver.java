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

/**
 * Observes graph node resolutions.
 *
 * @author Tibor Varga
 */
public interface ComponentResolutionObserver {

    /**
     * Invoked for each resolved graph node. The path and type are not final, they may change as circular references are handled. Elements of the path are
     * reference declaration and may not be the actual classes that will be instantiated for those references.
     *
     * @param path the dependency path at which the given type has been resolved.
     * @param type the type that has been resolved at the given dependency path.
     */
    void resolved(DependencyPath path, Class<?> type);

    /**
     * Invoked for each instantiated graph node. The path and type are final. Elements of the path are actual classes that will be or have been instantiated.
     * The {@link DependencyPath#head(boolean)} returns the class just instantiated.
     *
     * @param path the dependency path at which the given type has been instantiated. Does not yet include <code>type</code>.
     */
    void instantiated(DependencyPath path);
}
