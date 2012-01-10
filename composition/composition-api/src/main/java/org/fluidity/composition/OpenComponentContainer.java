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

/**
 * A <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Concept">dependency injection</a>
 * <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Containers">container</a> that components can be added to using the
 * container's registry. Except in {@linkplain ContainerBoundary rare circumstances}, you do not need to directly interact with a registry of the root
 * container as the <code>org.fluidity.maven:maven-composition-plugin</code> Maven plugin does that for you.
 * <p/>
 * The registry offers several ways to map an implementation to an interface in the host container. Which one you need depends on your requirements. These
 * methods are mostly invoked from the {@link ComponentContainer.Bindings#bindComponents(ComponentContainer.Registry) bindComponents()}
 * method of your {@linkplain org.fluidity.composition.spi.PackageBindings binding} implementation.
 * <ul>
 * <li>To simply register a component implementation for its component interfaces, use {@link ComponentContainer.Registry#bindComponent(Class, Class[])
 * bindComponent()}. This is exactly what the Maven plugin does for a {@link Component @Component} annotated class with no {@link Component#automatic()
 * &#64;Component&#40;automatic = false&#41;} setting so if this method is all you need then you should simply use the plugin instead of creating your own
 * binding class.</li>
 * <li>To register an already instantiated component implementation for a component interface, use {@link ComponentContainer.Registry#bindInstance(Object,
 * Class[]) bindInstance()}. If the implementation is annotated with <code>@Component</code> then its <code>@Component(automatic = ...)</code> parameter must
 * be
 * set to <code>false</code>.</li>
 * <li>To register a component implementation when some or all of its dependencies are - by design - not accessible in the same container, use {@link
 * org.fluidity.composition.ComponentContainer.Registry#isolateComponent(Class, Class[]) makeChildContainer()} method and use the returned container's
 * {@link OpenComponentContainer#getRegistry() getRegistry()} method to gain access to the registry in which to bind the hidden dependencies. If the
 * implementation is annotated with <code>@Component</code> then its <code>@Component(automatic = ...)</code> parameter must be set to <code>false</code>.</li>
 * </ul>
 *
 * @author Tibor Varga
 * @see ComponentContainer
 */
@SuppressWarnings("JavadocReference")
public interface OpenComponentContainer extends ComponentContainer {

    /**
     * Returns the object through which component bindings can be added to this container.
     *
     * @return a {@link ComponentContainer.Registry} instance.
     */
    Registry getRegistry();
}
