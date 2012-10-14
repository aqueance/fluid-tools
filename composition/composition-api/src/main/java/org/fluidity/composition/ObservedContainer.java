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

package org.fluidity.composition;

/**
 * A dependency injection container that exposes to {@linkplain ComponentContainer.Observer application components} the static dependencies in a dependency
 * graph without instantiating components, and the dynamic dependencies while resolving and instantiating components. An
 * <code>ObservedComponentContainer</code> instance works together with a client supplied {@link ComponentContainer.Observer} object that it sends component
 * resolution events to.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain Component @Component}
 * public final class MyComponent {
 *
 *   private final <span class="hl3">{@linkplain ComponentContainer}</span> container;
 *   &hellip;
 *
 *   MyComponent(final <span class="hl3">{@linkplain ComponentContainer}</span> container, &hellip;) {
 *     this.container = container;
 *     &hellip;
 *   }
 *
 *   public void exampleMethod() {
 *     final <span class="hl1">ObservedComponentContainer</span> observed = container.<span class="hl3">observed</span>(new <span class="hl2">{@linkplain ComponentContainer.Observer}</span>() {
 *       public void <span class="hl2">descending</span>(final {@linkplain Class}&lt;?> declaringType,
 *                              final {@linkplain Class}&lt;?> dependencyType,
 *                              final {@linkplain java.lang.annotation.Annotation}[] typeAnnotations,
 *                              final {@linkplain java.lang.annotation.Annotation}[] referenceAnnotations) {
 *         &hellip;
 *       }
 *
 *       public void <span class="hl2">ascending</span>(final {@linkplain Class}&lt;?> declaringType,
 *                             final {@linkplain Class}&lt;?> dependencyType) {
 *         &hellip;
 *       }
 *
 *       public void <span class="hl2">circular</span>(final {@linkplain DependencyPath} path) {
 *         &hellip;
 *       }
 *
 *       public void <span class="hl2">resolved</span>(final {@linkplain DependencyPath} path, final {@linkplain Class}&lt;?> type) {
 *         &hellip;
 *       }
 *
 *       public void <span class="hl2">instantiated</span>(final {@linkplain DependencyPath} path, final {@linkplain java.util.concurrent.atomic.AtomicReference}&lt;?> object) {
 *
 *         // will return the just instantiated object only <b>after</b> this method completes
 *         assert object.get() == null;
 *         &hellip;
 *       }
 *     });
 *
 *     // static dependencies only; will invoke all methods of <span class="hl2">{@linkplain ComponentContainer.Observer}</span> except <span class="hl2">instantiated()</span>:
 *     observed.<span class="hl1">resolveComponent</span>(SomeComponent.class);
 *
 *     // static and dynamic dependencies; will invoke all methods of <span class="hl2">{@linkplain ComponentContainer.Observer}</span>:
 *     observed.<span class="hl1">getComponent</span>(SomeComponent.class);
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
public interface ObservedContainer extends OpenContainer {

    /**
     * Resolves the component bound to the given interface and all dependent components without instantiating them. Dynamic dependencies, e.g., those resolved
     * in component constructors will not be picked up by this method.
     *
     * @param api the component interface.
     */
    void resolveComponent(Class<?> api);

    /**
     * Resolves the component group bound to the given interface and all dependent components without instantiating them. Dynamic dependencies, e.g., those
     * resolved in component constructors will not be picked up by this method.
     *
     * @param api the group interface.
     */
    void resolveGroup(Class<?> api);
}
