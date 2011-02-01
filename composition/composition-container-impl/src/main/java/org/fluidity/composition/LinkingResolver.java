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

import org.fluidity.foundation.spi.LogFactory;

/**
 * Links a component binding in the parent container to a resolver in the child. The component when looked up in the parent ends up handled by the child.
 *
 * @author Tibor Varga
 */
final class LinkingResolver extends AbstractResolver {

    private final SimpleContainer target;
    private ComponentResolver delegate;

    public LinkingResolver(final SimpleContainer container,
                           final Class<?> api,
                           final ComponentResolver delegate,
                           final ReferenceChain references,
                           final LogFactory logs) {
        super(api, delegate.isFallback(), references, null, logs);
        this.delegate = delegate;
        this.target = container;
    }

    public Class<?> componentClass() {
        return delegate.componentClass();
    }

    @Override
    public boolean isVariantMapping() {
        return delegate.isVariantMapping();
    }

    @Override
    public Class<?> factoryClass() {
        return delegate.factoryClass();
    }

    @Override
    public boolean isInstanceMapping() {
        return delegate.isInstanceMapping();
    }

    public Object create(final SimpleContainer container, final Class<?> api, boolean circular) {
        assert target.parentContainer() == container;
        return delegate.create(target, api, circular);
    }

    @Override
    public void resolverReplaced(final ComponentResolver previous, final ComponentResolver replacement) {
        if (delegate == previous) {
            delegate = replacement;
        }
    }
}
