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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * @author Tibor Varga
 */
final class ComponentCacheImpl implements ComponentCache {

    private final Cache cache;
    private final Listener listener;
    private final Log log;

    public ComponentCacheImpl(final Listener listener, final LogFactory logs, boolean cache) {
        this.cache = cache ? new Cache() : null;
        this.listener = listener;
        this.log = logs.createLog(getClass());
    }

    public Object lookup(final Object source, final ComponentContext context, final Class<?> componentInterface, final Instantiation create) {
        return lookup(cache == null ? new Cache() : cache, source, context, componentInterface, create);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private Object lookup(final Cache cache,
                          final Object source,
                          final ComponentContext context,
                          final Class<?> api,
                          final Instantiation instantiation) {
        final Map<ComponentContext, ComponentContext> contexts = cache.contexts;
        final Map<ComponentContext, Object> components = cache.components;

        final ComponentContext incomingKey = context.key();
        if (!components.containsKey(incomingKey)) {

            // go ahead and create the component and then see if it was actually necessary; context may change as new instantiations take place
            final Object component = instantiation.perform(context);
            final ComponentContext outgoingKey = context.key();

            synchronized (cache) {
                if (!components.containsKey(outgoingKey)) {
                    contexts.put(incomingKey, outgoingKey);
                    components.put(outgoingKey, component);

                    if (component == null) {
                        log.info("%s: not created component for %s%s", source, api, context.types().isEmpty() ? "" : String.format(" for context %s", context));
                    } else {
                        log.info("%s: created %s@%s%s",
                                 source,
                                 component.getClass().getName(),
                                 System.identityHashCode(component),
                                 context.types().isEmpty() ? "" : String.format(" for context %s", context));
                    }

                    if (listener != null) {
                        listener.created(api, component);
                    }
                } else if (!contexts.containsKey(incomingKey)) {
                    contexts.put(incomingKey, outgoingKey);
                }
            }
        }

        assert contexts.containsKey(incomingKey);

        context.collect(Collections.singleton(contexts.get(incomingKey)));

        final ComponentContext key = contexts.get(incomingKey);
        assert components.containsKey(key) : String.format("Component %s not found in context %s", api, context);
        return components.get(key);
    }

    private static class Cache {
        public final Map<ComponentContext, Object> components = new ConcurrentHashMap<ComponentContext, Object>();
        public final Map<ComponentContext, ComponentContext> contexts = new ConcurrentHashMap<ComponentContext, ComponentContext>();
    }
}
