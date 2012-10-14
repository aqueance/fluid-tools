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
 * The external API of a fully populated dependency injection container that that allows dynamic component lookup.
 * <h3>Usage</h3>
 * The examples below assume the following enclosing boilerplate:
 * <pre>
 * {@linkplain Component @Component}
 * public final class MyComponent {
 *
 *   private final <span class="hl1">ComponentContainer</span> container;
 *
 *   MyComponent(final <span class="hl1">ComponentContainer</span> container) {
 *     this.container = container;
 *     &hellip;
 *   }
 *
 *   private void myMethod() {
 *     <i>&hellip; example code snippet from below &hellip;</i>
 *   }
 * }
 * </pre>
 * <h4>Component Lookup</h4>
 * <pre>
 * final <span class="hl2">SomeComponent</span> component = container.<span class="hl1">getComponent</span>(<span class="hl2">SomeComponent</span>.class);
 * &hellip;
 * </pre>
 * <h4>Component Group Lookup</h4>
 * <pre>
 * final <span class="hl2">SomeGroup</span>[] group = container.<span class="hl1">getComponentGroup</span>(<span class="hl2">SomeGroup</span>.class);
 * &hellip;
 * </pre>
 *
 * @author Tibor Varga
 */
public interface OpenContainer extends ComponentContainer {

    /**
     * Looks up by interface or (super)class and returns a component. This method is provided for boundary objects (objects created outside the container by
     * third party tools) to acquire their dependencies. If there was no explicit binding to the provided class, no component is returned.
     * <p/>
     * <b>Note</b>: This method does not allow modification of the component context at the point of invocation so rather than reaching into the container with
     * this method, a better way is to call {@link ComponentContainer#instantiate(Class, ComponentContainer.Bindings...)} to gain access to
     * components in this container.
     *
     * @param api a class object that was used to bind a component to; never <code>null</code>.
     *
     * @return the component bound to the give class or <code>null</code> when none was found.
     *
     * @throws ResolutionException when dependency resolution fails.
     */
    <T> T getComponent(Class<T> api) throws ResolutionException;

    /**
     * Looks up by interface or (super)class and returns the list of components implementing the given interface, provided that they each, or the given
     * interface itself, has been marked with the {@link ComponentGroup @ComponentGroup} annotation. This method is provided for boundary objects (objects
     * created outside the container by third party tools) to acquire their dependencies. If there was no explicit binding to the provided class, no component
     * is returned.
     * <p/>
     * <b>Note</b>: This method does not allow modification of the component context at the point of invocation so rather than reaching into the container with
     * this method, a better way is to call {@link ComponentContainer#instantiate(Class, ComponentContainer.Bindings...)} to gain access to
     * components in this container.
     *
     * @param api the group interface class.
     *
     * @return an array of components that belong to the given group; may be <code>null</code>.
     */
    <T> T[] getComponentGroup(Class<T> api);
}
