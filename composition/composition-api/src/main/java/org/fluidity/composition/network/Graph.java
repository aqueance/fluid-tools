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

import org.fluidity.composition.ComponentContext;

/**
 * TODO: documentation
 */
public interface Graph {

    Node resolveComponent(Class<?> api, ContextDefinition context, Traversal traversal);

    Node resolveGroup(Class<?> api, ContextDefinition context, Traversal traversal);

    interface Node {

        Class<?> type();

        Object instance();

        ComponentContext context();

        class Constant implements Node {

            private final Class<?> type;
            private final Object instance;
            private final ComponentContext context;

            public Constant(final Class<?> type, final Object instance, final ComponentContext context) {
                this.type = type;
                this.instance = instance;
                this.context = context;
            }

            public final Class<?> type() {
                return type;
            }

            public final Object instance() {
                return instance;
            }

            public ComponentContext context() {
                return context;
            }
        }
    }

    interface Traversal {

        Node follow(final Graph graph, ContextDefinition context, Reference reference);

        Traversal observed(Observer observer);

        interface Strategy {

            Strategy DEFAULT = new Strategy() {
                public Node resolve(final Graph graph, final ContextDefinition context, final Traversal traversal, final boolean repeating, final Trail trail) {
                    return repeating ? null : trail.advance();
                }
            };

            Node resolve(Graph graph, ContextDefinition context, Traversal traversal, boolean repeating, Trail trail);
        }

        interface Observer {

            void resolved(Path path, Class<?> type);
        }

        interface Path {

            Class<?> head();

            List<Class<?>> path();
        }

        interface Trail extends Path {

            Node advance();
        }
    }

    interface Reference {

        Class<?> api();

        Node resolve(Traversal traversal, ContextDefinition context);
    }
}
