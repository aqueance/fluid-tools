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

import java.util.HashMap;
import java.util.Map;

import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * @author Tibor Varga
 */
final class ComponentCacheImpl implements ComponentCache {

    private final Map<ComponentContext, Object> cache;
    private final Log log;

    public ComponentCacheImpl(final LogFactory logs, boolean singleton) {
        this.cache = singleton ? new HashMap<ComponentContext, Object>() : null;
        this.log = logs.createLog(getClass());
    }

    public Object lookup(final Object source, final ComponentContext context, final Class<?> api, final Instantiation create) {
        assert context != null : api;
        return lookup(cache == null ? new HashMap<ComponentContext, Object>() : cache, source, context, api, create, log);
    }

    private synchronized Object lookup(final Map<ComponentContext, Object> cache,
                                       final Object source,
                                       final ComponentContext context,
                                       final Class<?> api,
                                       final Instantiation delegate,
                                       final Log log) {
        if (!cache.containsKey(context)) {

            // go ahead and create the component and then see if it was actually necessary; context may change as new instantiations take place
            final Object component = delegate.instantiate();

            if (component == null) {
                throw new ComponentContainer.ResolutionException("Could not create component for %s", api);
            }

            cache.put(context, component);

            if (log.isInfoEnabled()) {
                log.info("%s: created %s@%s%s",
                         source,
                         component.getClass().getName(),
                         System.identityHashCode(component),
                         context.types().isEmpty() ? "" : String.format(" for %s", context));
            }
        } else {
            if (log.isInfoEnabled()) {
                final Object component = cache.get(context);
                log.info("%s: reusing %s@%s%s",
                         source,
                         component.getClass().getName(),
                         System.identityHashCode(component),
                         context.types().isEmpty() ? "" : String.format(" for %s", context));
            }
        }

        assert cache.containsKey(context) : String.format("Component %s not found in context %s", api, context);
        return cache.get(context);
    }
}
