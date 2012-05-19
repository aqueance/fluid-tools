/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.composition.container.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.DependencyPath;
import org.fluidity.composition.container.ContainerServices;
import org.fluidity.composition.container.ContextDefinition;

/**
 * A dependency graph of components and component groups. This is an internal interface used by dependency injection container implementations.
 * <p/>
 * The graph is backed by static and dynamic dependencies between components. Static dependencies are those that can be discovered without instantiating
 * components while dynamic dependencies are those that require component instantiation.
 * <p/>
 * The graph enables the resolution of a component or component group interface to a component class or component classes, respectively, and, while doing so,
 * the graph also enables the traversal of the dependency graph under that component or component group to collect information about that dependency graph.
 * <p/>
 * This interface is implemented together with the {@link DependencyGraph.Traversal} interface, the implementation of which holds all state concerning the
 * particular graph traversal, and these together allow the implementation of a third interface, {@link org.fluidity.composition.ComponentContainer.Observer}, to act like a visitor by
 * getting its callback methods invoked at certain events during graph traversal.
 * <h3>Usage</h3>
 * You don't interact with an internal interface.
 *
 * @author Tibor Varga
 */
public interface DependencyGraph {

    /**
     * Resolves, without instantiating, a component using the given traversal in the given context.
     *
     * @param api       the component interface.
     * @param context   the component context at the node at which the resolution starts.
     * @param traversal the graph traversal to use.
     *
     * @return the resolved component or <code>null</code> if none could be resolved.
     */
    Node resolveComponent(Class<?> api, ContextDefinition context, Traversal traversal);

    /**
     * Resolves, without instantiating its members, a component group with the given traversal in the given context.
     *
     * @param api       the group interface.
     * @param context   the component context at the node at which the resolution starts.
     * @param traversal the graph traversal to use.
     *
     * @return the resolved component group or <code>null</code> if none could be resolved.
     */
    Node resolveGroup(Class<?> api, ContextDefinition context, Traversal traversal);

    /**
     * Resolves, without instantiating, a component using the given traversal in the given context.
     *
     * @param api       the component interface.
     * @param context   the component context at the node at which the resolution starts.
     * @param traversal the graph traversal to use.
     * @param reference the parameterized type of the dependency reference.
     *
     * @return the resolved component or <code>null</code> if none could be resolved.
     */
    Node resolveComponent(Class<?> api, ContextDefinition context, Traversal traversal, Type reference);

    /**
     * Resolves, without instantiating its members, a component group with the given traversal in the given context.
     *
     * @param api       the group interface.
     * @param context   the component context at the node at which the resolution starts.
     * @param traversal the graph traversal to use.
     * @param reference the parameterized type of the dependency reference.
     *
     * @return the resolved component group or <code>null</code> if none could be resolved.
     */
    Node resolveGroup(Class<?> api, ContextDefinition context, Traversal traversal, Type reference);

    /**
     * A node in a dependency graph. Nodes are created during traversal of a {@link DependencyGraph} and hold information about the node that enables proper
     * component instantiation at that node.
     * <h3>Usage</h3>
     * You don't interact with an internal interface.
     *
     * @author Tibor Varga
     */
    interface Node {

        /**
         * The actual component class at this node, or in case of circular dependency, the class or interface being depended on.
         *
         * @return the actual component class at this node, or in case of circular dependency, the class or interface being depended on.
         */
        Class<?> type();

        /**
         * Creates and returns the component or component group instance at this node. This may result in further graph traversal, as in case a constructor
         * dynamically resolves a component, as well as changes to the traversal up to this point in case of a circular dependency that cannot be replaced with
         * a proxy due to depending on a class rather than an interface.
         *
         * @param traversal the current graph traversal.
         *
         * @return the instance at this node.
         */
        Object instance(Traversal traversal);

        /**
         * The component context at this node. Calculating this value requires that the path leading up to this node has already been resolved.
         *
         * @return the component context at this node.
         */
        ComponentContext context();

        /**
         * A resolvable reference to a node in a dependency graph.
         * <h3>Usage</h3>
         * You don't interact with an internal interface.
         *
         * @author Tibor Varga
         */
        interface Reference {

            /**
             * Identifies the reference. This is used to detect circular references.
             *
             * @return an object to identify the reference by.
             */
            Object identity();

            /**
             * The component interface this node refers to.
             *
             * @return the component interface this node refers to.
             */
            Class<?> api();

            /**
             * The component api to list in instantiation paths.
             *
             * @return the component api to list in instantiation paths.
             */
            Class<?> type();

            /**
             * Returns the annotations at this reference.
             *
             * @return the annotations at this reference.
             */
            Annotation[] annotations();

            /**
             * Resolves the node using the given traversal and context.
             *
             * @param traversal the current graph traversal.
             * @param context   the component context at this node.
             *
             * @return the resolved node.
             */
            Node resolve(Traversal traversal, ContextDefinition context);
        }

        /**
         * A node whose instance is known without further resolution, or as a result of such resolution.
         * <h3>Usage</h3>
         * You don't interact with an internal interface.
         *
         * @author Tibor Varga
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

            public final Object instance(final Traversal traversal) {
                return instance;
            }

            public ComponentContext context() {
                return context;
            }
        }
    }

    /**
     * A graph traversal that maintains state during traversal such as current path, nodes and component contexts along the path, and handles circular
     * references.
     * <h3>Usage</h3>
     * You don't interact with an internal interface.
     *
     * @author Tibor Varga
     */
    interface Traversal {

        /**
         * Follows a dependency. This is an indirectly recursive method as during resolution of a node further invocations of this method are expected and thus
         * one single instance is able to maintain a path of invocations.
         *
         * @param graph     the graph in which this dependency is to be followed.
         * @param context   the component context at the point of dependency.
         * @param reference the node reference that can resolve the graph node that the dependency leads to.
         *
         * @return the resolved node.
         */
        Node follow(DependencyGraph graph, ContextDefinition context, Node.Reference reference);

        /**
         * Returns a new instance, one that invokes the given observer in addition to invoking the observer this object already has. The returned traversal
         * maintains the same path as the one handling this method call.
         *
         * @param services the container services to aggregate observers with.
         * @param observers the observers to invoke.
         *
         * @return the new traversal.
         */
        Traversal observed(ContainerServices services, ComponentContainer.Observer... observers);

        /**
         * Notifies the traversal about the actual class of the object being instantiated.
         *
         * @param type the class of the object being instantiated.
         */
        void instantiating(Class<?> type);

        /**
         * Notifies the traversal's observer, if any, about the instantiation of the given type. <b>Note:</b> the receiver must not call any method on the
         * instantiated object.
         *
         * @param type      the type just instantiated.
         * @param component the component that has just been instantiated.
         */
        void instantiated(Class<?> type, Object component);

        /**
         * Returns the current resolution or instantiation path.
         *
         * @return the current resolution or instantiation path.
         */
        DependencyPath path();

        /**
         * Notifies the observer registered with the receiver that a dependency is being resolved. See {@link org.fluidity.composition.ComponentContainer.Observer} for details.
         *
         * @param declaringType        see {@link org.fluidity.composition.ComponentContainer.Observer#descend(Class, Class, Annotation[], Annotation[]) ComponentResolutionObserver}.
         * @param dependencyType       see {@link org.fluidity.composition.ComponentContainer.Observer#descend(Class, Class, Annotation[], Annotation[]) ComponentResolutionObserver}.
         * @param typeAnnotations      see {@link org.fluidity.composition.ComponentContainer.Observer#descend(Class, Class, Annotation[], Annotation[]) ComponentResolutionObserver}.
         * @param referenceAnnotations see {@link org.fluidity.composition.ComponentContainer.Observer#descend(Class, Class, Annotation[], Annotation[]) ComponentResolutionObserver}.
         */
        void descend(Class<?> declaringType, Class<?> dependencyType, Annotation[] typeAnnotations, Annotation[] referenceAnnotations);

        /**
         * Notifies the observer registered with the receiver that a dependency has been resolved. See {@link org.fluidity.composition.ComponentContainer.Observer} for details.
         *
         * @param declaringType        see {@link org.fluidity.composition.ComponentContainer.Observer#descend(Class, Class, java.lang.annotation.Annotation[], java.lang.annotation.Annotation[]) ComponentResolutionObserver}.
         * @param dependencyType       see {@link org.fluidity.composition.ComponentContainer.Observer#descend(Class, Class, java.lang.annotation.Annotation[], java.lang.annotation.Annotation[]) ComponentResolutionObserver}.
         */
        void ascend(Class<?> declaringType, Class<?> dependencyType);
    }
}
