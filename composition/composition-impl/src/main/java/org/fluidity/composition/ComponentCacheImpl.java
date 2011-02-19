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
        return lookup(cache == null ? new HashMap<ComponentContext, Object>() : cache, source, context, api, create, log);
    }

    private synchronized Object lookup(final Map<ComponentContext, Object> cache,
                                       final Object source,
                                       final ComponentContext context,
                                       final Class<?> api,
                                       final Instantiation instantiation,
                                       final Log log) {
        if (cache.containsKey(context)) {
            instantiation.replay();
        } else {

            // go ahead and create the component and then see if it was actually necessary; context may change as new instantiations take place
            final Object component = instantiation.perform();

            if (component == null) {
                throw new ComponentContainer.ResolutionException("Could not create component for %s", api);
            }

            cache.put(context, component);

            if (log.isInfoEnabled()) {
                log.info("%s: created %s@%s%s",
                         source,
                         component.getClass().getName(),
                         System.identityHashCode(component),
                         context.types().isEmpty() ? "" : String.format(" for context %s", context));
            }
        }

        assert cache.containsKey(context) : String.format("Component %s not found in context %s", api, context);
        return cache.get(context);
    }
}
