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
import java.util.Set;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Component resolver for a {@link ComponentFactory} component.
 *
 * @author Tibor Varga
 */
abstract class FactoryResolver extends AbstractResolver {

    private final Class<? extends ComponentFactory> factoryClass;

    /**
     * Returns the {@link ComponentFactory} instance this is a mapping for.

     * @param container the container in which to resolve dependencies of the factory.
     * @param traversal the current graph traversal.
     *
     * @return the {@link ComponentFactory} instance this is a mapping for.
     */
    protected abstract ComponentFactory factory(final SimpleContainer container, final DependencyGraph.Traversal traversal);

    public FactoryResolver(final int priority,
                           final Class<?> api,
                           final Class<? extends ComponentFactory> factoryClass,
                           final ComponentCache cache,
                           final LogFactory logs) {
        super(priority, api, cache, logs);
        this.factoryClass = factoryClass;
    }

    public Set<Class<? extends Annotation>> acceptedContext() {
        return AbstractResolver.acceptedContext(factoryClass);
    }

    public Annotation[] annotations() {
        return null;
    }

    public final Class<? extends ComponentFactory> factoryClass() {
        return factoryClass;
    }

    public DependencyGraph.Node resolve(final DependencyGraph.Traversal traversal, final SimpleContainer container, final ContextDefinition context) {
        final SimpleContainer child = container.newChildContainer();
        factory(container, traversal).newComponent(new ComponentContainerShell(child, context, false), context.create());
        return cachingNode(child.resolveComponent(api, context, traversal), child);
    }

    @Override
    public String toString() {
        return String.format("%s (via %s)", api.getName(), factoryClass.getName());
    }
}
