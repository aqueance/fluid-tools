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

    private final Map<ComponentContext, Object> cache = new HashMap<ComponentContext, Object>();
    private final ContextChain contextChain;
    private final ReferenceChain referenceChain;
    private final Log log;

    public ComponentCacheImpl(final ContextChain contextChain, final ReferenceChain referenceChain, final LogFactory logs) {
        this.referenceChain = referenceChain;
        this.contextChain = contextChain;
        this.log = logs.createLog(getClass());
    }

    public Object lookup(final Object source, final Class<?> componentInterface, final Class<?> componentClass, final Listener listener, final Command create) {
        final ComponentContext key = contextChain.currentContext();

        if (!cache.containsKey(key)) {
            final ComponentContext context = contextChain.consumedContext(componentInterface, componentClass, contextChain.currentContext(), referenceChain);

            // go ahead and create the component and then see if it was actually necessary
            final Object component = create.run(context);

            // get the context consumed further in the chain and pass the one consumed here
            final ComponentContext consumedContext = contextChain.consumedContext(context);

            if (!cache.containsKey(consumedContext)) {
                cache.put(key, component);
                cache.put(consumedContext, component);

                if (component == null) {
                    log.info("%s: not created component for %s%s",
                             source,
                             componentInterface,
                             consumedContext.types().isEmpty() ? "" : String.format(" for context %s", consumedContext));
                } else {
                    log.info("%s: created %s@%s%s",
                             source,
                             component.getClass().getName(),
                             System.identityHashCode(component),
                             consumedContext.types().isEmpty() ? "" : String.format(" for context %s", consumedContext));
                }

                if (listener != null) {
                    listener.created(componentInterface, component);
                }
            } else {
                cache.put(key, cache.get(consumedContext));
            }
        }

        assert cache.containsKey(key) : String.format("Component %s not found in context %s", componentInterface, key);
        return cache.get(key);
    }
}
