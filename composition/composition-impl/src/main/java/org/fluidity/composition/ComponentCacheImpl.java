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
    private final Listener listener;
    private final ContextChain contextChain;
    private final ReferenceChain referenceChain;
    private final Log log;

    public ComponentCacheImpl(final Listener listener,
                              final ContextChain contextChain,
                              final ReferenceChain referenceChain,
                              final LogFactory logs,
                              boolean cache) {
        this.cache = cache ? new HashMap<ComponentContext, Object>() : null;
        this.listener = listener;
        this.referenceChain = referenceChain;
        this.contextChain = contextChain;
        this.log = logs.createLog(getClass());
    }

    public Object lookup(final Object source, final Class<?> componentInterface, final Class<?> componentClass, final Command create) {
        if (cache == null) {
            final Object component = createComponent(componentInterface, componentClass, create);

            recordComponentCreation(source, component, componentInterface, contextChain.prevalentContext());

            return component;
        } else {
            final ComponentContext key = contextChain.currentContext();

            if (!cache.containsKey(key)) {

                // go ahead and create the component and then see if it was actually necessary
                final Object component = createComponent(componentInterface, componentClass, create);

                // get the context consumed further in the chain and pass the one consumed here
                final ComponentContext consumedContext = contextChain.prevalentContext();

                if (!cache.containsKey(consumedContext)) {
                    cache.put(key, component);
                    cache.put(consumedContext, component);

                    recordComponentCreation(source, component, componentInterface, consumedContext);
                } else {
                    cache.put(key, cache.get(consumedContext));
                }
            }

            assert cache.containsKey(key) : String.format("Component %s not found in context %s", componentInterface, key);
            return cache.get(key);
        }
    }

    private Object createComponent(Class<?> componentInterface, Class<?> componentClass, Command create) {
        final ComponentContext context = contextChain.consumedContext(componentInterface, componentClass, contextChain.currentContext(), referenceChain);
        final Object component = create.run(context);

        contextChain.contextConsumed(context);

        return component;
    }

    private void recordComponentCreation(final Object source, final Object component, final Class<?> componentInterface, final ComponentContext context) {
        if (component == null) {
            log.info("%s: not created component for %s%s",
                     source,
                     componentInterface,
                     context.types().isEmpty() ? "" : String.format(" for context %s", context));
        } else {
            log.info("%s: created %s@%s%s",
                     source,
                     component.getClass().getName(),
                     System.identityHashCode(component),
                     context.types().isEmpty() ? "" : String.format(" for context %s", context));
        }

        if (listener != null) {
            listener.created(componentInterface, component);
        }
    }
}
