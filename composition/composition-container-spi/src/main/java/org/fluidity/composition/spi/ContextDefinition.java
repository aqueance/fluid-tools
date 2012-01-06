/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContext;

/**
 * Maintains context state during dependency resolution. This is an internal interface to be used by dependency injection container implementations.
 * <p/>
 * For any context aware component, the context state consists of two sets of annotations: defined set and active set.
 * <p/>
 * The defined set contains all the annotations that have been defined along
 * an instantiation path and is computed as we move downstream in the path by adding new context annotations to it.
 * <p/>
 * The active set is computed as we go backward on the instantiation path by adding context annotations from the defined set that are accepted by components
 * on the path and removing them as we pass their definition.
 * <p/>
 * The active set is then used as a cache key for stateless (i.e., cacheable) components regardless of whether they themselves are context aware or not.
 * <p/>
 * The above is implemented by<ol>
 * <li>{@link ContainerServices#emptyContext() creating an empty context} definition object at the head of some dependency path</li>
 * <li>{@link #expand(java.lang.annotation.Annotation[], Type) expanding} that context with the context annotations at each node of that path as we move
 * downstream</li>
 * <li>passing downstream a {@link #copy() copy} of the current definition for each dependency of the current component</li>
 * <li>{@link #accept(Class) narrowing} another copy of the definition down to the contexts accepted by the current context that can then be used to<ul>
 * <li>{@link #create() create} the actual context to be injected to the current component upon instantiation</li>
 * <li>as a cache key to map the instantiated stateless component to</li>
 * </ul></li>
 * <li>{@link #collect(Collection) collecting} the copies of the definition carrying the active set for the resolved dependencies of the current component</li>
 * <li>passing the resulting definition upstream.</li>
 * </ol>
 * <p/>
 * This context tracking algorithm ensures that between the component that defines some context and the ones that consume it, all intermediate components will
 * also have a dedicated instance for the active context. This may not appear intuitive at first but this rule guarantees that context aware components will
 * indeed have a unique instance for each actual context in the application.
 *
 * @author Tibor Varga
 */
public interface ContextDefinition {

    /**
     * Expands the defined context.
     *
     * @param definition the annotations potentially defining new context.
     * @param reference  the parameterized type of the reference to the current component.
     *
     * @return the receiver.
     */
    ContextDefinition expand(Annotation[] definition, Type reference);

    /**
     * Narrows down the defined context set to the annotation classes accepted by the current component in the instantiation path.
     *
     * @param consumer the class that may accept some context annotations; may be <code>null</code>.
     *
     * @return the receiver.
     */
    ContextDefinition accept(Class<?> consumer);

    /**
     * Expands the active context set with annotations consumed by dependencies of the component at the current node in the instantiation path.
     *
     * @param contexts the context definitions carrying the active component set from the dependencies of the current component.
     *
     * @return the receiver.
     */
    ContextDefinition collect(Collection<ContextDefinition> contexts);

    /**
     * Returns the active context set.
     *
     * @return the active context set.
     */
    Map<Class<? extends Annotation>, Annotation[]> active();

    /**
     * Returns a new copy of the receiver.
     *
     * @return a new copy of the receiver.
     */
    ContextDefinition copy();

    /**
     * Returns a component context containing the active context set.
     *
     * @return a component context containing the active context set.
     */
    ComponentContext create();

    /**
     * Returns a component context containing the given context set.
     *
     * @param map a context set.
     *
     * @return a component context containing the given context set.
     */
    ComponentContext create(Map<Class<? extends Annotation>, Annotation[]> map);

    /**
     * Returns the component reference.
     *
     * @return the component reference.
     */
    Component.Reference reference();
}
