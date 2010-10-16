/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.fluidity.composition.spi.ComponentMapping;
import org.fluidity.composition.spi.ContextChain;
import org.fluidity.composition.spi.ContextFactory;
import org.fluidity.composition.spi.ReferenceChain;

/**
 * @author Tibor Varga
 */
final class ContextChainImpl implements ContextChain {

    private final ContextFactory factory;

    private final ThreadLocal<ComponentContext> establishedContext = new ThreadLocal<ComponentContext>();
    private final ThreadLocal<ComponentContext> consumedContext = new ThreadLocal<ComponentContext>();

    public ContextChainImpl(final ContextFactory factory) {
        this.factory = factory;
        final ComponentContext context = factory.newContext(new Properties());
        establishedContext.set(context);
        consumedContext.set(context);
    }

    public ComponentContext currentContext() {
        return establishedContext.get();
    }

    public <T> T nested(final ComponentContext context, final Command<T> command) {
        final ComponentContext currentContext = establishedContext.get();

        if (context == null || context.keySet().isEmpty() || context.equals(currentContext)) {
            return command.run(currentContext);
        } else {
            establishedContext.set(factory.deriveContext(currentContext, context));
            try {
                return command.run(context);
            } finally {
                establishedContext.set(currentContext);
                consumedContext.set(factory.filteredContext(currentContext, consumedContext.get()));
            }
        }
    }

    public ComponentContext consumedContext(final Class<?> componentType, final ComponentContext context, final ReferenceChain referenceChain) {
        final String[] names = contextNames(componentType, referenceChain);
        final Map<String, String> map = new HashMap<String, String>();

        if (context != null && names != null) {
            for (final String name : names) {
                if (context.defines(name)) {
                    map.put(name, context.value(name, null));
                }
            }
        }

        final ComponentContext consumed = factory.deriveContext(consumedContext.get(), factory.newContext(map));

        consumedContext.set(consumed);

        return consumed;
    }

    public ComponentContext consumedContext() {
        return consumedContext.get();
    }

    private String[] contextNames(final Class<?> componentType, final ReferenceChain referenceChain) {
        assert componentType != null;
        final Set<String> names = new HashSet<String>();

        referenceChain.iterate(new ReferenceChain.Visitor<Void>() {
            public boolean visit(final ReferenceChain.Link item) {
                final ComponentMapping mapping = item.mapping();

                if (mapping.isVariantMapping() && mapping.componentInterface().isAssignableFrom(componentType)) {
                    final Context factoryContext = mapping.factoryClass().getAnnotation(Context.class);

                    if (factoryContext != null) {
                        names.addAll(Arrays.asList(factoryContext.accept()));
                        return false;
                    }
                }

                return true;
            }
        });

        final Context context = ComponentVariantFactory.class.isAssignableFrom(componentType) ? null : componentType.getAnnotation(Context.class);
        if (context != null) {
            names.addAll(Arrays.asList(context.accept()));
        }

        return names.size() > 0 ? names.toArray(new String[names.size()]) : null;
    }
}
