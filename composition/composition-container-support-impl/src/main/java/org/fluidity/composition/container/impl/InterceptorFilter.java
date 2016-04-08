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

package org.fluidity.composition.container.impl;

import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.spi.ComponentInterceptor;

/**
 * Internal interface to filter and sort {@link ComponentInterceptor interceptors} based on context annotations.
 *
 * @author Tibor Varga
 */
interface InterceptorFilter {

    /**
     * Returns only those interceptors from the provided ones whose context annotations are all present in the <em>defined</em> context set. The returned
     * interceptors are sorted by the relative declaration distance of their context annotation that is closest to the current dependency reference.
     * <p>
     * For example, let contexts <code>@A, @B, @C, and @D</code> be defined in that order along the dependency path, and three interceptors be given with the
     * following annotations, respectively:<ul>
     * <li><code>A, C</code></li>
     * <li><code>B, D</code></li>
     * <li><code>D, E</code></li>
     * </ul>
     * In the above case, the first two interceptors will be returned in reverse order, because the nearest context annotation of the second descriptor,
     * <code>D</code>, is closer to the current reference than the nearest context annotation of the first descriptor, <code>C</code>.
     *
     * @param context     the current context definition.
     * @param interceptors the interceptors to filter and sort; may be <code>null</code>.
     *
     * @return the filtered and sorted list of interceptors; possible empty, <code>null</code> only if the descriptor parameter was <code>null</code>
     */
    ComponentInterceptor[] filter(ContextDefinition context, ComponentInterceptor[] interceptors);
}
