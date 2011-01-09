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

/**
 * Wraps a container to be used as a dependency of a component on its enclosing container. This implementation retains the component's original context and uses
 * that as the base context for any further component resolution.
 *
 * @author Tibor Varga
 */
final class EmbeddedContainer extends AbstractComponentContainer {

    private final ComponentContext base;
    private final ContextChain contexts;

    public EmbeddedContainer(final SimpleContainer container, final ComponentContext base) {
        super(container);
        assert base != null;
        this.base = base;
        this.contexts = container.services().contextChain();
    }

    public <T> T getComponent(final Class<T> componentClass) throws ResolutionException {
        return container.get(componentClass, base);
    }

    public OpenComponentContainer makeNestedContainer() {
        return new FluidComponentContainer(container, true);
    }

    public <T> T initialize(final T component) throws ResolutionException {
        return contexts.nested(base, new ContextChain.Command<T>() {
            public T run(final ComponentContext ignore) {
                return container.initialize(component);
            }
        });
    }
}
