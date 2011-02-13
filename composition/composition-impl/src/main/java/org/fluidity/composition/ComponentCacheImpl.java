/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.composition;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * @author Tibor Varga
 */
final class ComponentCacheImpl implements ComponentCache {

    private final Map<Map<Class<? extends Annotation>, Annotation[]>, Object> cache;
    private final Listener listener;
    private final Log log;

    public ComponentCacheImpl(final Listener listener, final LogFactory logs, boolean cache) {
        this.cache = cache ? new ConcurrentHashMap<Map<Class<? extends Annotation>, Annotation[]>, Object>() : null;
        this.listener = listener;
        this.log = logs.createLog(getClass());
    }

    public Object lookup(final Object source, final ContextDefinition context, final Class<?> api, final Instantiation create) {
        return lookup(cache == null ? new HashMap<Map<Class<? extends Annotation>, Annotation[]>, Object>() : cache, source, context, api, create, log);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private Object lookup(final Map<Map<Class<? extends Annotation>, Annotation[]>, Object> cache,
                          final Object source,
                          final ContextDefinition context,
                          final Class<?> api,
                          final Instantiation instantiation,
                          final Log log) {

        /* Caching strategy:
         *
         * Incoming context definition is checked first. Return cache hit, if any.
         *
         * New component is created and consumed context is checked.
         *
         * Cache hit: ignore new component, map existing one to context definition.
         *
         * Cache miss: map new component to both consumed context and context definition.
         */

        final Map<Class<? extends Annotation>, Annotation[]> incoming = context.defined();
        if (!cache.containsKey(incoming)) {

            // go ahead and create the component and then see if it was actually necessary; context may change as new instantiations take place
            final Object component = instantiation.perform(context);

            if (component == null) {
                throw new ComponentContainer.ResolutionException("Could not create component for %s", api);
            }

            final Map<Class<? extends Annotation>, Annotation[]> outgoing = context.collected();

            synchronized (cache) {
                if (!cache.containsKey(outgoing)) {
                    cache.put(incoming, component);
                    cache.put(outgoing, component);

                    if (log.isInfoEnabled()) {
                        final ComponentContext consumed = context.create();

                        log.info("%s: created %s@%s%s",
                                 source,
                                 component.getClass().getName(),
                                 System.identityHashCode(component),
                                 consumed.types().isEmpty() ? "" : String.format(" for context %s", consumed));
                    }

                    if (listener != null) {
                        listener.created(api, component);
                    }
                } else if (!cache.containsKey(incoming)) {
                    cache.put(incoming, cache.get(outgoing));
                }
            }
        }

        assert cache.containsKey(incoming) : String.format("Component %s not found in context %s", api, incoming);
        return cache.get(incoming);
    }
}
