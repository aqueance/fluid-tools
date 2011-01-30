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
 * Production services for a container implementation.
 *
 * @author Tibor Varga
 */
final class ProductionServices implements ContainerServices {

    private final ReferenceChainImpl referenceChain = new ReferenceChainImpl();
    private final ContextFactory contextFactory = new ContextFactoryImpl();
    private final ContextChain contextChain = new ContextChainImpl(contextFactory);

    private final LogFactory logs;
    private final ClassDiscoveryImpl classDiscovery;
    private final DependencyInjector dependencyInjector;

    public ProductionServices(final LogFactory logs) {
        this.logs = logs;
        this.classDiscovery = new ClassDiscoveryImpl(logs);
        this.dependencyInjector = new DependencyInjectorImpl(classDiscovery, referenceChain, contextChain, contextFactory);
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

    public LogFactory logs() {
        return logs;
    }

    public ComponentCache newCache(final boolean stateless) {
        return new ComponentCacheImpl(contextChain, referenceChain, logs, stateless);
    }
}
