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

package org.fluidity.composition;

import org.fluidity.composition.spi.ComponentResolutionObserver;

/**
 * A <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Concept">dependency injection</a>
 * <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Containers">container</a> that offers traversal of static dependencies
 * without instantiating components and dynamic dependencies while resolving and instantiating components. An <code>ObservedComponentContainer</code> instance
 * works together with a client supplied {@link ComponentResolutionObserver} object that it sends component resolution events to.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain Component @Component}
 * final class MyComponent {
 *
 *   private final <span class="hl3">{@linkplain ComponentContainer}</span> container;
 *
 *   public MyComponent(final <span class="hl3">{@linkplain ComponentContainer}</span> container) {
 *     this.container = container;
 *     ...
 *   }
 *
 *   private void exampleMethod() {
 *     final <span class="hl1">ObservedComponentContainer</span> observed = container.<span class="hl3">observed(</span>new <span class="hl2">{@linkplain ComponentResolutionObserver}</span>() {
 *       public void <span class="hl2">resolving</span>(final Class<?> declaringType,
 *                             final Class<?> dependencyType,
 *                             final Annotation[] typeAnnotations,
 *                             final Annotation[] referenceAnnotations) {
 *         ...
 *       }
 *
 *       public void <span class="hl2">resolved</span>(final {@linkplain org.fluidity.composition.spi.DependencyPath} path, final Class&lt;?> type) {
 *         ...
 *       }
 *
 *       public void <span class="hl2">instantiated</span>(final {@linkplain org.fluidity.composition.spi.DependencyPath} path, final AtomicReference&lt;?> object) {
 *         assert object.get() == null; // will return the just instantiated object only <b>after</b> this method completes
 *         ...
 *       }
 *     }<span class="hl3">)</span>;
 *
 *     &#47;* ... static dependencies only; will invoke <span class="hl2">resolving()</span> and <span class="hl2">resolved()</span>: *&#47;
 *     observed.<span class="hl1">resolveComponent</span>(SomeComponent.class);
 *
 *     &#47;* ... static and dynamic dependencies; will invoke all methods of <span class="hl2">{@linkplain ComponentResolutionObserver}</span>: *&#47;
 *     observed.<span class="hl1">getComponent</span>(SomeComponent.class);
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
public interface ObservedComponentContainer extends ComponentContainer {

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
