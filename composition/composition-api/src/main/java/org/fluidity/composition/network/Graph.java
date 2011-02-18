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

package org.fluidity.composition.network;

import java.util.List;

/**
 * TODO: documentation
 */
public interface Graph {

    Node traverse(Class<?> api, Traversal traversal);

    interface Node {

        Class<?> type();

        Object instance();

        class Constant implements Node {

            private final Object instance;

            public Constant(final Object instance) {
                this.instance = instance;
            }

            public final Class<?> type() {
                return instance.getClass();
            }

            public final Object instance() {
                return instance;
            }
        }
    }

    public interface Path {

        Class<?> head();

        List<Class<?>> path();
    }

    interface Traversal {

        Node follow(final Container container, ContextDefinition context, final boolean explore, Reference reference);

        interface Strategy {

            Strategy DEFAULT = new Strategy() {
                public Node resolve(final boolean circular, final Container container, final Traversal traversal, final Trail trail) {
                    return trail.advance();
                }
            };

            Node resolve(boolean circular, Container container, Traversal traversal, Trail trail);
        }

        interface Observer {

            Observer DEFAULT = null;

            void resolved(Path path, Object instance);
        }

        interface Trail extends Path {

            Node advance();
        }
    }

    interface Reference {

        Class<?> api();

        Node resolve(Traversal traversal, ContextDefinition context, final boolean explore);
    }

    /**
     * TODO: documentation
     */
    interface Container extends Graph {

        Node resolveComponentNode(Class<?> api, ContextDefinition context, Traversal traversal);

        Node resolveGroupNode(Class<?> api, ContextDefinition context, Traversal traversal);
    }
}
