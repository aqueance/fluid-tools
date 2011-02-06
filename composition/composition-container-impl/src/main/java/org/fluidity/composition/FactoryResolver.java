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
     *
     * @param container  the container in which to resolve dependencies of the factory.
     * @param references the object that keeps track of dependency reference chains.
     *
     * @return the {@link ComponentFactory} instance this is a mapping for.
     */
    protected abstract ComponentFactory factory(final SimpleContainer container, final ReferenceChain.Reference references);

    public FactoryResolver(final int priority,
                           final Class<?> api,
                           final Class<? extends ComponentFactory> factoryClass,
                           final ComponentCache cache,
                           final LogFactory logs) {
        super(priority, api, cache, logs);
        this.factoryClass = factoryClass;

        final Component annotation = factoryClass.getAnnotation(Component.class);

        if (annotation == null) {
            throw new ComponentContainer.BindingException("Factory %s must have a @%s annotation", factoryClass, Component.class);
        }

        if (annotation.stateful()) {
            throw new ComponentContainer.BindingException("Factory %s cannot be stateful (@%s(stateful = true)", factoryClass, Component.class);
        }
    }

    @Override
    public boolean isFactoryMapping() {
        return true;
    }

    public Annotation[] providedContext() {
        return null;
    }

    public <T extends Annotation> T contextSpecification(final Class<T> type) {
        return factoryClass.getAnnotation(type);
    }

    @Override
    protected ComponentCache.Instantiation createCommand(final ReferenceChain.Reference references, final SimpleContainer container, Class<?> api) {
        return new ComponentCache.Instantiation() {
            public Object perform(final ContextDefinition context) {
                return factory(container, references).newComponent(new ComponentContainerShell(container, references, context, true), context.create());
            }
        };
    }

    public final Class<? extends ComponentFactory> factoryClass() {
        return factoryClass;
    }

    @Override
    public String toString() {
        return String.format("%s (via %s)", api.getName(), factoryClass.getName());
    }
}
