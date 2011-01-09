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

import org.fluidity.composition.spi.ComponentVariantFactory;

/**
 * Component mapping for a {@link org.fluidity.composition.spi.ComponentVariantFactory} component.
 *
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
abstract class VariantProducer extends AbstractProducer {

    private final Class<? extends ComponentVariantFactory> factoryClass;
    private final Class<?> componentInterface;
    private final SimpleContainer parent;

    private ComponentProducer delegate;     // the one creating instances
    private Class<?> componentClass;        // caches what the delegate returns

    /**
     * Returns the {@link org.fluidity.composition.spi.ComponentVariantFactory} instance this is a mapping for.
     *
     * @param container the container in which to resolve dependencies of the factory.
     *
     * @return the {@link org.fluidity.composition.spi.ComponentVariantFactory} instance this is a mapping for.
     */
    protected abstract ComponentVariantFactory factory(final SimpleContainer container);

    public VariantProducer(final Class<? extends ComponentVariantFactory> factoryClass, final ComponentCache cache, final SimpleContainer container) {
        super(cache);
        this.parent = container.parentContainer();
        this.factoryClass = factoryClass;

        final ComponentContainer.BindingException error = new ComponentContainer.BindingException(
                "Variant factory %s must have a @Component(api = ...) annotation",
                factoryClass);

        final Component annotation = factoryClass.getAnnotation(Component.class);

        if (annotation == null) {
            throw error;
        }

        this.componentInterface = annotation.api();

        assert componentInterface != null;
        if (componentInterface == Object.class) {
            throw error;
        }
    }

    public final Class<?> componentInterface() {
        return componentInterface;
    }

    public final Class<?> componentClass() {
        if (delegate == null) {
            delegate = parent == null ? null : parent.producer(componentInterface(), true);

            if (delegate == null) {
                throw new ComponentContainer.BindingException("Variant factory %s requires separate binding for %s", factoryClass, componentInterface);
            }
        }

        return componentClass == null ? componentClass = delegate.componentClass() : componentClass;
    }

    @Override
    public Object create(final SimpleContainer container, final boolean circular) {
        return cache.lookup(container, componentInterface, componentClass(), listener(), new ComponentCache.Command() {
            public Object run(final ComponentContext context) {
                final ComponentVariantFactory factory = factory(container);

                final SimpleContainer nested = container.newChildContainer();
                nested.bindProducer(delegate.componentInterface(), delegate);
                final OpenComponentContainer returned = factory.newComponent(new FluidComponentContainer(nested, false), context);

                if (returned == null) {
                    return null;
                } else {
                    return returned.getComponent(container.referenceChain().lastReference());
                }
            }
        });
    }

    public final void setDelegate(final ComponentProducer delegate) {
        assert !delegate.isInstanceMapping();
        this.delegate = delegate;
    }

    public boolean isVariantMapping() {
        return true;
    }

    public final Class<? extends ComponentVariantFactory> factoryClass() {
        return factoryClass;
    }
}
