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

package org.fluidity.composition.container.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.ComponentCache;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * @author Tibor Varga
 */
final class ComponentCacheImpl implements ComponentCache {

    private final Map<Object, Map<ComponentContext, Object>> caches;
    private final Log log;

    public ComponentCacheImpl(final LogFactory logs, boolean stateless) {
        this.caches = stateless ? new WeakHashMap<Object, Map<ComponentContext, Object>>() : null;
        this.log = logs.createLog(getClass());
    }

    public Object lookup(final Object domain, final String source, final ComponentContext context, final Class<?> api, final Entry factory) {
        assert context != null : api;
        final boolean stateful = caches == null;

        Map<ComponentContext, Object> cache;

        if (stateful) {
            cache = factory == null ? null : new HashMap<ComponentContext, Object>();
        } else {
            synchronized (caches) {
                cache = caches.get(domain);

                if (cache == null && factory != null) {
                    caches.put(domain, cache = new HashMap<ComponentContext, Object>());
                }
            }
        }

        return cache == null ? null : lookup(cache, source, context, api, factory, log);
    }

    private synchronized Object lookup(final Map<ComponentContext, Object> cache,
                                       final String source,
                                       final ComponentContext context,
                                       final Class<?> api,
                                       final Entry factory,
                                       final Log log) {
        final boolean report = !ComponentFactory.class.isAssignableFrom(api) && log.isInfoEnabled();

        if (factory != null) {
            if (!cache.containsKey(context)) {
                final Object component = factory.create();

                if (!cache.containsKey(context)) {
                    cache.put(context, component);

                    log(report, "created", api, component, context, log, source);
                } else {
                    log(report, "reusing", api, cache.get(context), context, log, source);
                }
            } else {
                log(report, "reusing", api, cache.get(context), context, log, source);
            }

            assert cache.containsKey(context) : String.format("Component %s not found in context %s", api, context);
        }

        return cache.get(context);
    }

    private void log(final boolean note,
                     final String action,
                     final Class<?> api,
                     final Object component,
                     final ComponentContext context,
                     final Log log,
                     final String source) {
        if (note) {
            final String instance = component == null
                             ? String.format("no %s", api.getName())
                             : String.format("%s@%x", component.getClass().getName(), System.identityHashCode(component));
            log.debug("%s: %s %s%s", source, action, instance, context.types().isEmpty() ? "" : String.format(" for %s", context));
        }
    }
}
