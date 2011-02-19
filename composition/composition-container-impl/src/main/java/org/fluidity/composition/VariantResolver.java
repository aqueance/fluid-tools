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

import org.fluidity.composition.network.ContextDefinition;
import org.fluidity.composition.network.Graph;
import org.fluidity.composition.spi.ComponentVariantFactory;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Component mapping for a {@link ComponentVariantFactory} component.
 *
 * @author Tibor Varga
 */
abstract class VariantResolver extends AbstractResolver {

    private final Class<? extends ComponentVariantFactory> factoryClass;
    private final SimpleContainer parent;

    private ComponentResolver delegate;     // the one creating instances

    /**
     * Returns the {@link ComponentVariantFactory} instance this is a mapping for.
     *
     * @param container the container in which to resolve dependencies of the factory.
     * @param traversal the current graph traversal.
     *
     * @return the {@link ComponentVariantFactory} instance this is a mapping for.
     */
    protected abstract ComponentVariantFactory factory(final SimpleContainer container, final Graph.Traversal traversal);

    public VariantResolver(final int priority,
                           final SimpleContainer container,
                           final Class<?> api,
                           final Class<? extends ComponentVariantFactory> factoryClass,
                           final ComponentCache cache,
                           final LogFactory logs) {
        super(priority, api, cache, logs);
        this.parent = container.parentContainer();
        this.factoryClass = factoryClass;
    }

    @Override
    public boolean replaces(final ComponentResolver resolver) {
        final int check = resolver.priority();

        if (resolver.isVariantMapping()) {
            final VariantResolver variants = (VariantResolver) resolver;

            if (check == priority()) {
                throw new ComponentContainer.BindingException("Component %s already hijacked by %s", api, variants.factoryClass);
            } else {
                final boolean replaces = super.replaces(variants);

                if (replaces) {
                    resolverReplaced(api, null, variants.delegate());
                }

                return replaces;
            }
        } else if (resolver.isInstanceMapping()) {
            throw new ComponentContainer.BindingException("Component instance %s cannot be hijacked by %s", api, factoryClass);
        } else {
            if (delegate == null || resolver.replaces(delegate)) {
                resolverReplaced(api, delegate, resolver);
            }

            return true;
        }
    }

    private ComponentResolver findDelegate() {
        if (delegate == null) {
            delegate = parent == null ? null : parent.resolver(api, true);

            if (delegate == null) {
                throw new ComponentContainer.BindingException("Variant factory %s requires separate binding for %s", factoryClass, api);
            }
        }

        return delegate;
    }

    public Annotation[] providedContext() {
        return null;
    }

    public <T extends Annotation> T contextSpecification(final Class<T> type) {
        return factoryClass.getAnnotation(type);
    }

    @Override
    public void resolverReplaced(final Class<?> api, final ComponentResolver previous, final ComponentResolver replacement) {
        if (api == this.api && delegate == previous) {
            if (replacement.isInstanceMapping()) {
                throw new ComponentContainer.BindingException("Component %s cannot be hijacked by %s", this.api, ((VariantResolver) replacement).factoryClass());
            }

            delegate = replacement;
        }
    }

    public ComponentResolver delegate() {
        return delegate;
    }

    public boolean isVariantMapping() {
        return true;
    }

    public final Class<? extends ComponentVariantFactory> factoryClass() {
        return factoryClass;
    }

    public Graph.Node resolve(final Graph.Traversal traversal, final SimpleContainer container, final ContextDefinition context) {
        final SimpleContainer child = container.newChildContainer();
        child.bindResolver(api, findDelegate());
        factory(container, traversal).newComponent(new ComponentContainerShell(child, context, false), context.create());
        return cachingNode(child.resolveComponent(api, context, traversal), child);
    }

    @Override
    public String toString() {
        return String.format("%s (via %s)", delegate == null ? api : delegate.toString(), factoryClass.getName());
    }
}
