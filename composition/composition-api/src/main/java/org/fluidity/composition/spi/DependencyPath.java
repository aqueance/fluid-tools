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

import java.util.List;

/**
 * A dependency path.
 *
 * @author Tibor Varga
 */
public interface DependencyPath {

    /**
     * The component interface at the head of the path.
     *
     * @param api specifies whether the interface (<code>true</code>) or the class (<code>false</code>), if available, is to be returned.
     *
     * @return the component interface at the head of the path.
     */
    Class<?> head(boolean api);

    /**
     * The list of component interfaces or classes that comprise the dependency path.
     *
     * @param api specifies whether the interface (<code>true</code>) or the class (<code>false</code>), if available, is to be returned for each path element.
     *
     * @return the list of component interfaces or classes that comprise the dependency path.
     */
    List<Class<?>> path(boolean api);

    /**
     * Returns a string representation of the path.
     *
     * @param api specifies whether the interface (<code>true</code>) or the class (<code>false</code>), if available, is to be returned for each path element.
     *
     * @return a string representation of the path.
     */
    String toString(boolean api);
}
