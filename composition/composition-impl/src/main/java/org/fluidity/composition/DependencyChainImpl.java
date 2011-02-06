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

import java.util.LinkedHashSet;
import java.util.Set;

import org.fluidity.composition.spi.ComponentMapping;

/**
 * @author Tibor Varga
 */
final class DependencyChainImpl implements DependencyChain {

    /*
     * When component instantiation is deferred in order to break circular dependence, the deferred resolver must be able to use the reference chain prevalent
     * at its invocation. Without a thread local variable to hold that prevalent chain, the deferred resolver could either use the chain prevalent at its
     * creation or a new empty chain, both of which are wrong in one case or the other.
     *
     * Thus, we need a thread local variable to hold the prevalent reference chain.
     */
    private final ThreadLocal<Chain> prevalent = new InheritableThreadLocal<Chain>() {
        @Override
        protected Chain initialValue() {
            return new Chain();
        }
    };

    public <T> T follow(final ContextDefinition context, final ComponentMapping mapping, final Command<T> command) {
        final Chain lastChain = prevalent.get();
        final Chain newChain = lastChain.descend(mapping);

        prevalent.set(newChain);
        try {
            return command.run(newChain, context == null ? new ContextDefinitionImpl() : context);
        } finally {
            prevalent.set(lastChain);
        }
    }

    private class Chain implements Lineage {

        private final Set<ComponentMapping> loop = new LinkedHashSet<ComponentMapping>();
        private final boolean circular;

        private Chain() {
            this.circular = false;
        }

        public Chain(final Set<ComponentMapping> loop, final ComponentMapping mapping) {
            this.loop.addAll(loop);
            this.circular = this.loop.contains(mapping);
            this.loop.add(mapping);
        }

        public Chain descend(final ComponentMapping mapping) {
            return new Chain(loop, mapping);
        }

        public boolean isCircular() {
            return circular;
        }

        @Override
        public String toString() {
            return loop.toString();
        }
    }
}
