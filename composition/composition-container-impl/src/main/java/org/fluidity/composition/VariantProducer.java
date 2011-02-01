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
import org.fluidity.foundation.spi.LogFactory;

/**
 * Component mapping for a {@link ComponentVariantFactory} component.
 *
 * @author Tibor Varga
 */
abstract class VariantProducer extends AbstractProducer {

    private final Class<? extends ComponentVariantFactory> factoryClass;
    private final SimpleContainer parent;

    private ComponentProducer delegate;     // the one creating instances
    private Class<?> componentClass;        // caches what the delegate returns

    /**
     * Returns the {@link ComponentVariantFactory} instance this is a mapping for.
     *
     * @param container the container in which to resolve dependencies of the factory.
     *
     * @return the {@link ComponentVariantFactory} instance this is a mapping for.
     */
    protected abstract ComponentVariantFactory factory(final SimpleContainer container);

    public VariantProducer(final SimpleContainer container,
                           final Class<?> api,
                           final Class<? extends ComponentVariantFactory> factoryClass,
                           final boolean fallback,
                           final ReferenceChain references,
                           final ComponentCache cache,
                           final LogFactory logs) {
        super(api, fallback, references, cache, logs);
        this.parent = container.parentContainer();
        this.factoryClass = factoryClass;
    }

    @Override
    public boolean isFallback() {
        return super.isFallback() || (delegate != null && delegate.isFallback());
    }

    public final Class<?> componentClass() {
        if (delegate == null) {
            delegate = parent == null ? null : parent.producer(api, true);

            if (delegate == null) {
                throw new ComponentContainer.BindingException("Variant factory %s requires separate binding for %s", factoryClass, api);
            }
        }

        return componentClass == null ? componentClass = delegate.componentClass() : componentClass;
    }

    @Override
    public Object create(final SimpleContainer container, final Class<?> api, final boolean circular) {
        return cache.lookup(container, api, componentClass(), new ComponentCache.Command() {
            public Object run(final ComponentContext context) {
                final ComponentVariantFactory factory = factory(container);

                final SimpleContainer child = container.newChildContainer();
                child.bindProducer(api, delegate);
                final OpenComponentContainer returned = factory.newComponent(new ComponentContainerShell(child, false), context);

                if (returned == null) {
                    return null;
                } else {
                    return returned.getComponent(references.lastReference());
                }
            }
        });
    }

    @Override
    public void producerReplaced(final ComponentProducer previous, final ComponentProducer replacement) {

        // TODO: this is way too complex
        if ((delegate == null && replacement.componentApi() == api) || (previous != null && delegate == previous)) {
            if (replacement.isInstanceMapping()) {
                throw new ComponentContainer.BindingException("Component %s cannot be hijacked by %s", api, replacement.factoryClass());
            } else if (replacement.componentApi() != api) {
                throw new ComponentContainer.BindingException("Component %s should be %s (%s)", replacement.componentApi(), factoryClass, api);
            }

            delegate = replacement;
        }
    }

    public ComponentProducer delegate() {
        return delegate;
    }

    public boolean isVariantMapping() {
        return true;
    }

    public final Class<? extends ComponentVariantFactory> factoryClass() {
        return factoryClass;
    }
}
