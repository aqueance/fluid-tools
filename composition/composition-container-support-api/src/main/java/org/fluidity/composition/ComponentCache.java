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
 * Caches components by context. Implementations must be thread safe.
 *
 * @author Tibor Varga
 */
public interface ComponentCache {

    /**
     * Looks up and instantiates if necessary using the supplied command, the component whose class is also specified to find its annotations.
     *
     * @param domain  the object against component instances are stored.
     * @param source  something to identify who is creating instances through this cache.
     * @param context the context for the component.
     * @param api     the interface the component implements.
     * @param create  the command that performs instantiation of the component; if <code>null</code>, only a lookup is made otherwise instantiation is
     *                attempted if the no cached instance is found.
     *
     * @return the component instance.
     */
    Object lookup(final Object domain, Object source, ComponentContext context, Class<?> api, Instantiation create);

    /**
     * A command to create a component instance in some context.
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
