/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import org.fluidity.composition.container.ContextDefinition;

/**
 * A dependency graph of components and component groups. This is an internal interface implemented by dependency injection containers.
 * <p>
 * The graph is backed by static and dynamic dependencies between components. Static dependencies are those that can be discovered without instantiating
 * components while dynamic dependencies are those that require component instantiation to manifest.
 * <h3>Usage</h3>
 * You don't interact with an internal interface.
 *
 * @author Tibor Varga
 */
public interface DependencyGraph {

    /**
     * Resolves, without instantiating, a component using the given traversal in the given context, for the given dependency reference.
     *
     * @param api       the component interface.
     * @param context   the component context at the node at which the resolution starts.
     * @param traversal the graph traversal state.
     * @param reference the parameterized type of the dependency reference.
     *
     * @return the resolved component or <code>null</code> if none could be resolved.
     */
    Node resolveComponent(Class<?> api, ContextDefinition context, Traversal traversal, Type reference);

    /**
     * Resolves, without instantiating its members, a component group with the given traversal in the given context, for the given dependency reference.
     *
     * @param api       the group interface.
     * @param context   the component context at the node at which the resolution starts.
     * @param traversal the graph traversal state.
     * @param reference the parameterized type of the dependency reference.
     *
     * @return the resolved component group or <code>null</code> if none could be resolved.
     */
    Node resolveGroup(Class<?> api, ContextDefinition context, Traversal traversal, Type reference);

    /**
     * A node in a {@linkplain DependencyGraph dependency graph}. Nodes are created during traversal of a {@link DependencyGraph} and hold information about
     * the node for component instantiation at that node.
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
         * Creates and returns the component or component group instance at this node. This may result in further graph traversal, as in case of {@linkplain
         * org.fluidity.composition.spi.ComponentFactory component factories}, as well as changes to the traversal up to this point in case of a circular
         * dependency that cannot be replaced with a proxy due to depending on a class rather than an interface.
         *
         * @param traversal the graph traversal state.
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
         * A resolvable reference to a {@linkplain DependencyGraph.Node node} in a {@linkplain DependencyGraph dependency graph}.
         * <h3>Usage</h3>
         * You don't interact with an internal interface.
         *
         * @author Tibor Varga
         */
        interface Reference {

            /**
             * Resolves the node.
             *
             * @return the resolved node.
             */
            Node resolve();
        }
    }

    /**
     * Maintains, during {@linkplain DependencyGraph dependency graph} traversal, state such as nodes and component contexts along a dependency path, and
     * handles circular references.
     * <h3>Usage</h3>
     * You don't interact with an internal interface.
     *
     * @author Tibor Varga
     */
    interface Traversal {

        /**
         * Follows a dependency reference. This is an indirectly recursive method as during resolution of a node further invocations of this method are
         * expected and thus one single traversal instance is able to maintain a path of invocations.
         *
         * @param identity  identifies the reference; used to detect circular references.
         * @param api       the component interface the dependency refers to.
         * @param type      the component API to list in instantiation paths.
         * @param context   the component context at the dependency reference.
         * @param reference the node reference that can resolve the graph node that the dependency leads to.
         *
         * @return the resolved node.
         */
        Node follow(Object identity, Class<?> api, Class<?> type, ContextDefinition context, Node.Reference reference);

        /**
         * Returns a new instance, one that invokes the given observers in addition to invoking the observer this instance already has. The returned traversal
         * will keep maintaining the same path as the one handling this method call.
         *
         * @param observers the observers to invoke.
         *
         * @return the new traversal.
         */
        Traversal observed(ComponentContainer.Observer... observers);

        /**
         * Notifies the traversal's observer, if any, about the actual class of the object being instantiated.
         *
         * @param type the class of the object being instantiated.
         */
        void instantiating(Class<?> type);

        /**
         * Notifies the traversal's observer, if any, about the instantiation of the given type. See {@link
         * org.fluidity.composition.ComponentContainer.Observer#instantiated(DependencyPath,
         * java.util.concurrent.atomic.AtomicReference) ComponentContainer.Observer.instantiated()} for details.
         * <p>
         * <b>Note:</b> the receiver will <i>not</i> call any method on the
         * instantiated object while executing this method.
         *
         * @param type      the type just instantiated.
         * @param component the component that has just been instantiated.
         *
         * @return the component.
         */
        Object instantiated(Class<?> type, Object component);

        /**
         * Returns the current resolution or instantiation path.
         *
         * @return the current resolution or instantiation path.
         */
        DependencyPath path();

        /**
         * Notifies the traversal's observer, if any, that a dependency is being resolved. See {@link org.fluidity.composition.ComponentContainer.Observer}
         * for details.
         *
         * @param declaringType        see {@link org.fluidity.composition.ComponentContainer.Observer#descending(Class, Class, Annotation[], Annotation[]) ComponentResolutionObserver}.
         * @param dependencyType       see {@link org.fluidity.composition.ComponentContainer.Observer#descending(Class, Class, Annotation[], Annotation[]) ComponentResolutionObserver}.
         * @param typeAnnotations      see {@link org.fluidity.composition.ComponentContainer.Observer#descending(Class, Class, Annotation[], Annotation[]) ComponentResolutionObserver}.
         * @param referenceAnnotations see {@link org.fluidity.composition.ComponentContainer.Observer#descending(Class, Class, Annotation[], Annotation[]) ComponentResolutionObserver}.
         */
        void descend(Class<?> declaringType, Class<?> dependencyType, Annotation[] typeAnnotations, Annotation[] referenceAnnotations);

        /**
         * Notifies the traversal's observer, if any, that a dependency has been resolved. See {@link org.fluidity.composition.ComponentContainer.Observer}
         * for details.
         *
         * @param declaringType        see {@link org.fluidity.composition.ComponentContainer.Observer#descending(Class, Class, Annotation[], Annotation[]) ComponentResolutionObserver}.
         * @param dependencyType       see {@link org.fluidity.composition.ComponentContainer.Observer#descending(Class, Class, Annotation[], Annotation[]) ComponentResolutionObserver}.
         */
        void ascend(Class<?> declaringType, Class<?> dependencyType);
    }
}
