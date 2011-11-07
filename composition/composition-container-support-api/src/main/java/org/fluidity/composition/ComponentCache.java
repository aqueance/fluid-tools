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

/**
 * Caches components by context. Implementations must be thread safe. This is an internal interface used by the dependency injection container implementation.
 *
 * @author Tibor Varga
 */
public interface ComponentCache {

    /**
     * Looks up, and instantiates if necessary using the supplied command, the cached component.
     *
     * @param domain  the object against which component instances are cached; a separate instance will be cached for different domain parameter values.
     * @param source  something to identify who is creating instances through this cache; used in log messages emitted by the cache.
     * @param context the context for the component.
     * @param api     the interface the component implements.
     * @param factory the command that performs instantiation of the component; if <code>null</code>, only a lookup is made, otherwise instantiation is
     *                attempted if the no cached instance is found.
     *
     * @return the component instance or <code>null</code> depending on whether the supplied <code>factory</code> is <code>null</code> or not.
     */
    Object lookup(Object domain, Object source, ComponentContext context, Class<?> api, Instantiation factory);

    /**
     * A command to create some component instance.
     */
    interface Instantiation {

        /**
         * Creates and returns a new instance of a component.
         *
         * @return a new instance of a component; never <code>null</code>.
         */
        Object instantiate();
    }
}
