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
import org.fluidity.composition.spi.ContainerServices;
import org.fluidity.composition.spi.ContextChain;
import org.fluidity.composition.spi.ContextFactory;
import org.fluidity.composition.spi.DependencyInjector;
import org.fluidity.composition.spi.ReferenceChain;
import org.fluidity.foundation.Logging;

/**
 * Production services for a container implementation.
 *
 * @author Tibor Varga
 */
public final class ProductionServices implements ContainerServices {

    private final Logging log;
    private final ClassDiscoveryImpl classDiscovery = new ClassDiscoveryImpl();
    private final ReferenceChainImpl referenceChain = new ReferenceChainImpl();
    private final ContextFactory contextFactory = new ContextFactoryImpl();
    private final ContextChain contextChain = new ContextChainImpl(contextFactory);
    private final DependencyInjector dependencyInjector = new DependencyInjectorImpl();

    public ProductionServices(final Logging log) {
        this.log = log;
    }

    public ClassDiscovery classDiscovery() {
        return classDiscovery;
    }

    public ContextFactory contextFactory() {
        return contextFactory;
    }

    public ContextChain contextChain() {
        return contextChain;
    }

    public ReferenceChain referenceChain() {
        return referenceChain;
    }

    public DependencyInjector dependencyInjector() {
        return dependencyInjector;
    }

    public Logging log() {
        return log;
    }

    public ComponentCache newCache() {
        return new ComponentCacheImpl(contextChain, referenceChain, log);
    }
}