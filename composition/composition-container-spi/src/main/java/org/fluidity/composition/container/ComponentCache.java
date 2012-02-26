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

package org.fluidity.composition.container;

import org.fluidity.composition.ComponentContext;

/**
 * Caches components by <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Component_Context">context</a> in a domain. Instances of this cache are
 * provided for dependency injection container implementations to cache instantiated components.
 * <p/>
 * A domain is a mirror of the dependency injection container hierarchy of the application that is isolated from other domains. Caching of component instances
 * are performed in each domain separately from other domains.
 * <p/>
 * This cache is thread safe.
 * <u3>Usage</u3>
 * <pre>
 * final {@linkplain ContainerServices} services = ...;
 *
 * final <span class="hl1">ComponentCache</span> cache = services.newCache(false);
 *
 * final <span class="hl1">ComponentCache.Instance</span> factory = new <span class="hl1">ComponentCache.Instance</span>() {
 *   public Object create() {
 *     return new <span class="hl2">Object</span>();
 *   }
 * }
 *
 * ...
 *
 * final {@linkplain ComponentContext} context1 = ...;
 * final {@linkplain ComponentContext} context2 = ...;
 *
 * final <span class="hl2">Object</span> instance1 = cache.<span class="hl1">lookup</span>(null, "sample", context1, <span class="hl2">Object</span>.class, factory);
 * final <span class="hl2">Object</span> instance2 = cache.<span class="hl1">lookup</span>(null, "sample", context1, <span class="hl2">Object</span>.class, factory);
 * final <span class="hl2">Object</span> instance3 = cache.<span class="hl1">lookup</span>(null, "sample", context2, <span class="hl2">Object</span>.class, factory);
 *
 * assert instance2 == instance1;
 * assert instance3 != instance2;
 *
 * final <span class="hl2">Object</span> instance4 = cache.<span class="hl1">lookup</span>("domain", "sample", context2, <span class="hl2">Object</span>.class, factory);
 * final <span class="hl2">Object</span> instance5 = cache.<span class="hl1">lookup</span>("domain", "sample", context2, <span class="hl2">Object</span>.class, factory);
 *
 * assert instance4 != instance3;
 * assert instance5 == instance4;
 * </pre>
 *
 * @author Tibor Varga
 */
public interface ComponentCache {

    /**
     * Looks up, and instantiates if necessary using the supplied command, the cached component. Instantiation only takes place if <code>factory</code> is not
     * <code>null</code>.
     *
     * @param domain  the object that segregates cached instances, or <code>null</code>; a separate instance will be cached for different domains.
     * @param source  identifies in log messages the entity that is creating instances through this cache.
     * @param context the context for the component to cache against.
     * @param api     the interface the component implements.
     * @param factory the command that performs instantiation of the component if it was not yet found in the cache; if <code>null</code>, only a lookup is
     *                made, otherwise instantiation is attempted if the no cached instance is found.
     *
     * @return the component instance or <code>null</code>.
     */
    Object lookup(Object domain, String source, ComponentContext context, Class<?> api, Entry factory);

    /**
     * A command to create some component instance. This interface is used to tell a {@link ComponentCache} how to instantiate the cached component if it's
     * missing.
     * <h3>Usage</h3>
     * See {@link ComponentCache}.
     *
     * @author Tibor Varga
     */
    interface Entry {

        /**
         * Creates and returns a new instance of the component.
         *
         * @return a new instance of the component, or <code>null</code>, which will also be cached.
         */
        Object create();
    }
}
