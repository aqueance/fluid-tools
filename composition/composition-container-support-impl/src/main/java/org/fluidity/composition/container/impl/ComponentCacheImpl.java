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

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.ComponentCache;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.ComponentInterceptor;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.Strings;

/**
 * @author Tibor Varga
 */
final class ComponentCacheImpl implements ComponentCache {

    private final Log log;
    private final Map<Object, Map<String, Object>> caches;

    ComponentCacheImpl(final Log<ComponentCacheImpl> log, boolean stateless) {
        this.log = log;
        this.caches = stateless ? new WeakHashMap<>() : null;
    }

    public Object lookup(final Domain domain, final String source, final ComponentContext context, final Class<?> api, final Supplier factory) {
        assert context != null : api;
        final boolean stateful = caches == null;

        Map<String, Object> cache;

        if (stateful) {
            cache = factory == null ? null : new HashMap<>();
        } else {
            synchronized (caches) {
                cache = caches.get(domain);

                if (cache == null && factory != null) {
                    caches.put(domain, cache = new HashMap<>());
                }
            }
        }

        return cache == null ? null : lookup(domain, cache, source, context, api, factory, log);
    }

    private synchronized Object lookup(final Domain domain,
                                       final Map<String, Object> cache,
                                       final String source,
                                       final ComponentContext context,
                                       final Class<?> api,
                                       final Supplier factory,
                                       final Log log) {
        final String key = context.key();

        if (factory != null) {
            if (!cache.containsKey(key)) {
                final Object component = factory.get();

                if (!cache.containsKey(key)) {
                    cache.put(key, component);

                    assert domain != null;

                    if (!domain.quiet() && !ComponentFactory.class.isAssignableFrom(api) && !ComponentInterceptor.class.isAssignableFrom(api)) {
                        domain.log(log,
                                   "%s: using %s%s",
                                   source,
                                   component == null ? String.format("no %s", api.getName()) : Strings.formatId(component),
                                   context.types().isEmpty() ? "" : String.format(" for %s", key));
                    }
                }
            }

            assert cache.containsKey(key) : String.format("Component %s not found in context %s", api, context);
        }

        return cache.get(key);
    }
}
