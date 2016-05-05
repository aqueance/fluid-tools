/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.util.List;
import java.util.Set;

/**
 * A dependency path used when {@linkplain ObservedContainer observing} dependency resolutions. Objects implementing this interface are created and
 * provided to {@linkplain ComponentContainer.Observer component resolution observers} and to {@linkplain ComponentContainer.InstantiationException
 * instantiation exception} handlers. A dependency path is a list of components that depend on one another, each on the next in the path, up to the {@linkplain
 * #tail() tail} of the path, which represents the "current" component.
 * <h3>Usage</h3>
 * See {@link ObservedContainer}.
 *
 * @author Tibor Varga
 */
public interface DependencyPath {

    /**
     * The component interface at the tail of the dependency path.
     *
     * @return the component interface at the tail of the dependency path.
     */
    Element tail();

    /**
     * The list of component interfaces or classes that comprise the dependency path.
     *
     * @return the list of component interfaces or classes that comprise the dependency path.
     */
    List<? extends Element> path();

    /**
     * Returns a string representation of the path.
     *
     * @param api specifies whether the interface (<code>true</code>) or the class (<code>false</code>), if available, is to be returned for each path element.
     *
     * @return a string representation of the path.
     */
    String toString(boolean api);

    /**
     * Details about one element in a dependency path. A sequence of objects implementing this interface comprise a {@link DependencyPath}.
     * <h3>Usage</h3>
     * <pre>
     * final {@linkplain DependencyPath} dependencies = &hellip;; // acquired from somewhere
     *
     * for (final <span class="hl1">DependencyPath.Element</span> element : dependencies.path()) {
     *   &hellip;
     * }
     * </pre>
     *
     * @author Tibor Varga
     */
    interface Element {

        /**
         * Returns the referenced interface at this point in the dependency path.
         *
         * @return the referenced interface at this point in the dependency path; never <code>null</code>.
         */
        Class<?> api();

        /**
         * Returns the resolved type at this point in the dependency path, or the referenced interface if not yet known.
         *
         * @return the resolved type at this point in the dependency path, or the referenced interface if not yet known; never <code>null</code>.
         */
        Class<?> type();
    }
}
