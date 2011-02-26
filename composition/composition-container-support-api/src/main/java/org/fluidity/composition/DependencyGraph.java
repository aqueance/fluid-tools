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

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.DependencyPath;

/**
 * A dependency graph of components and component groups. The graph is backed by static and dynamic dependencies between components. The graph can be traversed
 * to get a component or component group instance and during that traversal the actual dependency graph, starting at the sought component or component group,
 * is realized.
 *
 * @author Tibor Varga
 */
public interface DependencyGraph {

    /**
     * Resolves a component using the given traversal in the given context.
     *
     * @param api       the component interface.
     * @param context   the component context at the point of resolution.
     * @param traversal the graph traversal to use.
     *
     * @return the resolved component or <code>null</code> if none could be resolved.
     */
    Node resolveComponent(Class<?> api, ContextDefinition context, Traversal traversal);

    /**
     * Resolves a component group with the given traversal in the given context.
     *
     * @param api       the group interface.
     * @param context   the component context at the point of resolution.
     * @param traversal the graph traversal to use.
     *
     * @return the resolved component group or <code>null</code> if none could be resolved.
     */
    Node resolveGroup(Class<?> api, ContextDefinition context, Traversal traversal);

    /**
     * A dynamic node in the graph. Such nodes are created during a particular traversal of the graph from a given component or component group.
     */
    interface Node {

        /**
         * The actual component class at this node, or in case of circular dependency, the dependency type.
         *
         * @return the actual component class at this node, or in case of circular dependency, the dependency type.
         */
        Class<?> type();

        /**
         * Creates and returns the component or component group instance at this node. This may result in further graph traversal, as in case a constructor
         * dynamically resolves a component, as well as changes to the traversal up to this point in case of a circular dependency that cannot be replaced with
         * a proxy, due to the reference not being made to an interface.
         *
         * @return the instance at this node.
         */
        Object instance();

        /**
         * The component context at this node. Calculating this value requires that the path leading up to this node has already been resolved.
         *
         * @return the component context at this node.
         */
        ComponentContext context();

        /**
         * A reference to a node that can be resolved.
         */
        interface Reference {

            /**
             * The component interface this node refers to.
             *
             * @return the component interface this node refers to.
             */
            Class<?> api();

            /**
             * Resolves the node using the given traversal and context.
             *
             * @param traversal the graph traversal that maintains the dependency path.
             * @param context   the component context at this node.
             *
             * @return the resolved the node using the given traversal and context.
             */
            Node resolve(Traversal traversal, ContextDefinition context);
        }

        /**
         * A node whose instance is known without further resolution.
         */
        class Constant implements Node {

            private final Class<?> type;
            private final Object instance;
            private final ComponentContext context;

            /**
             * @param type     the typ of the instance; useful when the instance is null thus its class cannot be queried.
             * @param instance the known component instance
             * @param context  the component context for the component instance.
             */
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

    /**
     * A graph traversal that maintains a traversal path, calculated graph nodes and component contexts along the path and handles circular references.
     */
    interface Traversal {

        /**
         * Follows a dependency. This is an indirectly recursive method as during resolution of a node further invocations of this method are expected and thus
         * one single instance is able to maintain a path of invocations.
         *
         * @param graph     the graph in which this dependency is to be followed.
         * @param context   the component context at the point of dependence.
         * @param reference the node reference that can resolve the graph node that the dependency leads to.
         *
         * @return the resolved node.
         */
        Node follow(DependencyGraph graph, ContextDefinition context, Node.Reference reference);

        /**
         * Returns a new instance, one that invokes the given observer in addition to invoking the observer this object already has. The returned traversal
         * maintains the same path as the one handling this method call.
         *
         * @param observer the new observer to invoke.
         *
         * @return the new traversal.
         */
        Traversal observed(ComponentResolutionObserver observer);

        /**
         * A path traversal strategy. The strategy is consulted before advancing on a dependency path.
         */
        @ServiceProvider
        interface Strategy {

            /**
             * Advances on a dependency path. The strategy may return an unrelated Node or may invoke the given trail object to advance on the dependency path.
             *
             * @param graph     the graph in which the dependency path is currently followed.
             * @param context   the component context at the next node in the path.
             * @param traversal the traversal object that maintains the dependency path.
             * @param repeating <code>true</code> if this node is a repetition of a previous node, as defined by the dependency API and the context.
             * @param trail     the dependency trail.
             *
             * @return the next node on the dependency path.
             */
            Node advance(DependencyGraph graph, ContextDefinition context, Traversal traversal, boolean repeating, Trail trail);
        }

        /**
         * A dependency path that can be advanced on.
         */
        interface Trail extends DependencyPath {

            /**
             * Advances on the dependency path.
             *
             * @return the node resolved by advancing on the dependency path.
             */
            Node advance();
        }
    }
}
