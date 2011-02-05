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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fluidity.composition.spi.ComponentMapping;

/**
 * @author Tibor Varga
 */
final class ReferenceChainImpl implements ReferenceChain {

    // TODO: pass along in method invocations
    private static final ThreadLocal<List<Link>> reference = new ThreadLocal<List<Link>>() {

        @Override
        protected List<Link> initialValue() {
            return new ArrayList<Link>();
        }
    };

    // TODO: get rid of
    private static final ThreadLocal<Set<ComponentMapping>> mappings = new ThreadLocal<Set<ComponentMapping>>() {

        @Override
        protected Set<ComponentMapping> initialValue() {
            return new HashSet<ComponentMapping>();
        }
    };

    public Link lastLink() {
        final List<Link> stack = reference.get();
        assert stack.size() > 0 : "Empty reference chain";
        return stack.get(stack.size() - 1);
    }

    public <T> T track(final ComponentContext context, final ComponentMapping mapping, final Class<?> dependency, final Command<T> command) {
        final List<Link> stack = reference.get();
        final Set<ComponentMapping> loop = mappings.get();

        final boolean circular = loop.contains(mapping);

        stack.add(new LinkImpl(mapping, dependency));
        loop.add(mapping);
        try {
            return command.run(context == null ? new ComponentContextImpl() : context.copy(), circular);
        } finally {
            stack.remove(stack.size() - 1);

            if (!circular) {
                loop.remove(mapping);
            }
        }
    }

    public String toString() {
        return reference.get().toString();
    }

    private static final class LinkImpl implements Link {

        private final ComponentMapping mapping;
        private final Class<?> reference;

        public LinkImpl(final ComponentMapping mapping, final Class<?> reference) {
            this.mapping = mapping;
            this.reference = reference;
        }

        public ComponentMapping mapping() {
            return mapping;
        }

        public Class<?> reference() {
            return reference;
        }

        @Override
        public String toString() {
            return String.format("%s - %s", reference, mapping);
        }
    }
}
