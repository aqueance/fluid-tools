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

package org.fluidity.composition.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Qualifier;

/**
 * Maintains context state during dependency resolution. This is an internal interface to be used by dependency injection container implementations.
 * <p>
 * For any context dependent component, the context state consists of two sets of annotations: defined set and active set.
 * <p>
 * The defined set contains all the annotations that have been defined along
 * an instantiation path and is computed as we move downstream in the path by adding new qualifier annotations to it.
 * <p>
 * The active set is computed as we go backward on the instantiation path by adding qualifier annotations from the defined set that are accepted by components
 * on the path and removing them as we pass their definition.
 * <p>
 * The active set is the component context that is passed to context dependent components and used also as a {@linkplain ComponentCache cache} key for
 * stateless (i.e., cacheable) components regardless of whether they themselves are context dependent or not.
 * <p>
 * The above is implemented by<ol>
 * <li>{@linkplain ContainerServices#emptyContext() creating an empty context} definition object at the tail of some dependency path</li>
 * <li>{@linkplain #advance(Type, boolean) advancing} to the next node along that path as we move downstream to each dependency of the current component and
 * {@linkplain #expand(Annotation[]) expanding} the context with the qualifier annotations present at the new node</li>
 * <li>{@linkplain #accept(Class) narrowing} a <em>copy</em> of the definition down to the contexts accepted by the current context that can then be used
 * to<ul>
 * <li>{@linkplain #create() create} the actual context to be injected to the current component upon instantiation</li>
 * <li>as a cache key to map the instantiated stateless component to</li>
 * </ul></li>
 * <li>{@linkplain #collect(Collection) collecting} the copies of the definition carrying the active set for the resolved dependencies of the current
 * component</li>
 * <li>passing the resulting definition upstream.</li>
 * </ol>
 * <p>
 * This context tracking algorithm ensures that between the component that defines some context and the ones that consume it, all intermediate components will
 * also have a dedicated instance for the active context. This may appear counter-intuitive at first but this rule guarantees that context dependent components
 * will indeed have a unique and isolated instance for each actual context in the application.
 * <h3>Usage</h3>
 * <pre>
 * final {@linkplain ContainerServices} services = &hellip;;
 * final <span class="hl1">ContextDefinition</span> definition = services.emptyContext();
 * &hellip;
 * final {@linkplain ComponentContext} context = definition.create();
 * </pre>
 *
 * @author Tibor Varga
 */
public interface ContextDefinition {

    /**
     * Expands the <em>defined</em> context with the supplied annotations.
     *
     * @param definition the annotations potentially defining new context.
     *
     * @return the receiver.
     */
    ContextDefinition expand(Annotation[] definition);

    /**
     * Narrows down the <em>active</em> context set to the annotation classes accepted by the given component. If the <code>consumer</code> argument is
     * <code>null</code>, the active set is simply cleared.
     *
     * @param consumer the class that may accept some qualifier annotations; may be <code>null</code>.
     *
     * @return the receiver.
     */
    ContextDefinition accept(Class<?> consumer);

    /**
     * Expands the <em>active</em> context set with annotations consumed by dependencies of the component at the current node in the instantiation path.
     *
     * @param contexts the context definitions carrying the active component set from the dependencies of the current component.
     *
     * @return the receiver.
     */
    ContextDefinition collect(Collection<ContextDefinition> contexts);

    /**
     * Returns the <em>defined</em> context set in definition order.
     *
     * @return the defined context set.
     */
    Map<Class<? extends Annotation>, Annotation[]> defined();

    /**
     * Returns the <em>active</em> context set.
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
     * Makes a copy and removes from it all {@link Qualifier.Composition#IMMEDIATE} qualifier annotations.
     *
     * @param reference the parameterized type of the reference to the component being advanced to.
     * @param refine    tells whether the reference is just a more refined version of the last one (<code>true</code>) or an actual advance on the dependency
     *                  path (<code>false</code>)
     *
     * @return the new copy of the receiver.
     */
    ContextDefinition advance(Type reference, boolean refine);

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
     * Returns the current component reference.
     *
     * @return the current component reference.
     */
    Component.Reference reference();

    /**
     * Tells if the context definition is empty.
     *
     * @return <code>true</code> if there is no qualifier annotation in the receiver; <code>false</code> otherwise.
     */
    boolean isEmpty();
}
