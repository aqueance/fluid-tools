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

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Component producer for a {@link ComponentFactory} component.
 *
 * @author Tibor Varga
 */
abstract class FactoryProducer extends AbstractProducer {

    private final Class<? extends ComponentFactory> factoryClass;
    private final Class<?> componentInterface;

    private Class<?> componentClass;

    protected abstract ComponentFactory factory(final SimpleContainer container);

    public FactoryProducer(final Class<? extends ComponentFactory> factoryClass,
                           final ReferenceChain references,
                           final ComponentCache cache,
                           final LogFactory logs) {
        super(references, cache, logs);
        this.factoryClass = factoryClass;

        final Component annotation = factoryClass.getAnnotation(Component.class);

        if (annotation == null) {
            throw new ComponentContainer.BindingException("Factory %s must have a @%s annotation", factoryClass, Component.class);
        }

        this.componentInterface = annotation.api();
        this.componentClass = annotation.type();

        assert componentInterface != null;
        assert componentClass != null;

        if (componentInterface == Object.class || componentClass == Object.class) {
            throw new ComponentContainer.BindingException("Factory %s must have a @%s(api = ..., type = ...) annotation", factoryClass, Component.class);
        }
    }

    public final Class<?> componentInterface() {
        return componentInterface;
    }

    public final Class<?> componentClass() {
        return componentClass;
    }

    @Override
    protected ComponentCache.Command createCommand(final SimpleContainer container) {
        return new ComponentCache.Command() {
            public Object run(final ComponentContext context) {
                return factory(container).newComponent(new ComponentContainerShell(container, true), context);
            }
        };
    }

    public final Class<? extends ComponentFactory> factoryClass() {
        return factoryClass;
    }
}
