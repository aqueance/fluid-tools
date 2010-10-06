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

import org.fluidity.composition.spi.ComponentCache;

/**
 * Component producer for a {@link org.fluidity.composition.ComponentFactory} component.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
abstract class FactoryProducer extends AbstractProducer {

    private final Class<? extends ComponentFactory> factoryClass;
    private final Class<?> componentInterface;

    private Class<?> componentClass;

    protected abstract ComponentFactory factory(final SimpleContainer container);

    public FactoryProducer(final Class<? extends ComponentFactory> factoryClass, final ComponentCache cache) {
        super(cache);
        this.factoryClass = factoryClass;

        final ComponentContainer.BindingException error = new ComponentContainer.BindingException(
                "Factory %s must have a @Component(api = ..., type = ...) annotation",
                factoryClass);

        final Component annotation = factoryClass.getAnnotation(Component.class);

        if (annotation == null) {
            throw error;
        }

        this.componentInterface = annotation.api();
        this.componentClass = annotation.type();

        assert componentInterface != null;
        assert componentClass != null;
        if (componentInterface == Object.class || componentClass == Object.class) {
            throw error;
        }
    }

    public final Class<Object> componentInterface() {
        return (Class<Object>) componentInterface;
    }

    public final Class<?> componentClass() {
        return componentClass;
    }

    @Override
    protected ComponentCache.Command createCommand(final SimpleContainer container) {
        return new ComponentCache.Command() {
            public Object run(final ComponentContext context) {
                return factory(container).newComponent(new FluidComponentContainer(container, true), context);
            }
        };
    }

    public final Class<? extends ComponentFactory> factoryClass() {
        return factoryClass;
    }
}
