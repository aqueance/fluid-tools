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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.ComponentMapping;
import org.fluidity.composition.spi.ComponentVariantFactory;

/**
 * @author Tibor Varga
 */
final class ContextChainImpl implements ContextChain {

    private final ContextFactory factory;

    private final ThreadLocal<ComponentContext> establishedContext = new ThreadLocal<ComponentContext>();
    private final ThreadLocal<ComponentContext> consumedContext = new ThreadLocal<ComponentContext>();

    public ContextChainImpl(final ContextFactory factory) {
        this.factory = factory;

        final ComponentContext context = new ComponentContextImpl(new HashMap<Class<? extends Annotation>, Annotation[]>());
        establishedContext.set(context);
        consumedContext.set(context);
    }

    public ComponentContext currentContext() {
        return establishedContext.get();
    }

    public <T> T track(final ComponentContext context, final Command<T> command) {
        final ComponentContext currentContext = establishedContext.get();

        if (context == null || context.types().isEmpty() || context.equals(currentContext)) {
            return command.run(currentContext);
        } else {
            final ComponentContext derived = factory.deriveContext(currentContext, context);
            establishedContext.set(derived);
            try {
                return command.run(context);
            } finally {
                establishedContext.set(currentContext);

                // collect context on the reference chain for components earlier in the chain
                consumedContext.set(factory.filteredContext(currentContext, consumedContext.get()));
            }
        }
    }

    public ComponentContext consumedContext(final Class<?> componentType,
                                            final Class<?> componentClass,
                                            final ComponentContext context,
                                            final ReferenceChain referenceChain) {
        final Class<? extends Annotation>[] types = contextTypes(componentType, componentClass, referenceChain);
        final Map<Class<? extends Annotation>, Annotation[]> map = new HashMap<Class<? extends Annotation>, Annotation[]>();

        if (context != null && types != null) {
            for (final Class<? extends Annotation> type : types) {
                if (context.defines(type)) {
                    map.put(type, context.annotations(type));
                }
            }
        }

        return factory.deriveContext(consumedContext.get(), factory.newContext(map));
    }

    public ComponentContext consumedContext(final ComponentContext parent) {
        final ComponentContext child = consumedContext.get();
        final ComponentContext context = child == null ? parent : factory.deriveContext(parent, child);
        consumedContext.set(context);
        return context;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation>[] contextTypes(final Class<?> componentType, final Class<?> componentClass, final ReferenceChain referenceChain) {
        assert componentClass != null;
        final Set<Class<? extends Annotation>> types = new HashSet<Class<? extends Annotation>>();

        // find the first factory for the given component type and take its @Context details to apply to the component at hand
        referenceChain.iterate(new ReferenceChain.Visitor() {
            public boolean visit(final ReferenceChain.Link item) {
                final ComponentMapping mapping = item.mapping();

                final Class<?> factoryClass = mapping.factoryClass();
                if (factoryClass != null && factoryClass != componentClass && mapping.componentInterface() == componentType) {
                    final Context factoryContext = factoryClass.getAnnotation(Context.class);

                    if (factoryContext != null) {
                        types.addAll(Arrays.asList(factoryContext.value()));
                        return false;
                    }
                }

                return true;
            }
        });

        final boolean isFactory = ComponentVariantFactory.class.isAssignableFrom(componentClass) || ComponentFactory.class.isAssignableFrom(componentClass);

        // @Context on a factory does not apply to the factory itself
        final Context context = isFactory ? null : componentClass.getAnnotation(Context.class);
        if (context != null) {
            types.addAll(Arrays.asList(context.value()));
        }

        return (Class<Annotation>[]) (types.size() > 0 ? types.toArray(new Class<?>[types.size()]) : null);
    }
}
