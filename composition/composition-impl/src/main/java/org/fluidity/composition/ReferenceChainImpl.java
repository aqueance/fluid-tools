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
final class ReferenceChainImpl implements ReferenceChain {

    private final ThreadLocal<Reference> local = new InheritableThreadLocal<Reference>();

    public <T> T track(final Reference references,
                       final ContextDefinition context,
                       final ComponentMapping mapping,
                       final Class<?> dependency,
                       final Command<T> command) {
        final ContextDefinition validContext = context == null ? new ContextDefinitionImpl() : context;

        final Reference saved = local.get();
        if (references != null || saved == null) {
            return nest(references == null ? new ReferenceImpl(dependency, mapping) : references, validContext, command);
        } else {
            return saved.next(dependency, mapping, validContext, command);
        }
    }

    private <T> T nest(final Reference reference, final ContextDefinition context, final Command<T> command) {
        final Reference saved = local.get();
        local.set(reference);

        try {
            return command.run(reference, context == null ? new ContextDefinitionImpl() : context);
        } finally {
            local.set(saved);
        }
    }

    private class ReferenceImpl implements Reference {

        private final Set<ComponentMapping> loop = new LinkedHashSet<ComponentMapping>();
        private final Class<?> type;
        private final boolean circular;

        public ReferenceImpl(final Class<?> type, final ComponentMapping mapping) {
            this.type = type;
            this.loop.add(mapping);
            this.circular = false;
        }

        public ReferenceImpl(final Class<?> type, final Set<ComponentMapping> loop, final ComponentMapping mapping) {
            this.type = type;
            this.loop.addAll(loop);
            this.circular = this.loop.contains(mapping);
            this.loop.add(mapping);
        }

        public Class<?> type() {
            return type;
        }

        /**
         * @deprecated use #track()
         */
        public <T> T next(final Class<?> type, final ComponentMapping mapping, final ContextDefinition context, final Command<T> command) {
            return nest(new ReferenceImpl(type, loop, mapping), context, command);
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
