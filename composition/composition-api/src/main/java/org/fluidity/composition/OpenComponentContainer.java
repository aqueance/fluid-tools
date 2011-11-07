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
 * This is a dependency injection container that components can be added to using the container's registry. Except in {@link ContainerBoundary rare
 * circumstances}, you do not need to directly interact with a registry of the root container as the <code>org.fluidity.maven:maven-composition-plugin</code>
 * Maven plugin does that for you.
 * <p/>
 * The registry offers several ways to map an implementation to an interface in the host container. Which one you need depends on your requirements. These
 * methods are mostly invoked from the {@link org.fluidity.composition.spi.PackageBindings#bindComponents(OpenComponentContainer.Registry)} method of your binding
 * implementation.
 * <ul>
 * <li>To simply register a component implementation for its component interfaces, use {@link OpenComponentContainer.Registry#bindComponent(Class, Class[])}.
 * This is exactly what the Maven plugin uses for a {@link Component @Component} annotated class with no {@link Component#automatic()
 * @Component(automatic = ...)} setting so if this method is all you need then you should simply use the plugin instead of creating your own binding class.</li>
 * <li>To register an already instantiated component implementation for a component interface, use {@link OpenComponentContainer.Registry#bindInstance(Object,
 * Class[])}. If the implementation is annotated with <code>@Component</code> then its <code>@Component(automatic = ...)</code> parameter
 * must be set to <code>false</code>.</li>
 * <li>To register a component implementation when some or all of its dependencies are not accessible in the same container, use {@link
 * org.fluidity.composition.OpenComponentContainer.Registry#makeChildContainer(Class, Class[])} method and use the returned container's {@link
 * OpenComponentContainer#getRegistry()} method to gain access to the registry in which to bind the hidden dependencies. If the implementation is annotated with
 * <code>@Component</code> then its <code>@Component(automatic = ...)</code> parameter must be set to <code>false</code>.</li>
 * </ul>
 *
 * @see ComponentContainer
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
public interface OpenComponentContainer extends ComponentContainer {

    /**
     * Returns the interface through which component bindings can be added to this container. The returned interface cannot be used to get components out of the
     * container. Thus, a container is write-only when it is being populated and read-only after it has been populated.
     *
     * @return a {@link OpenComponentContainer.Registry} instance.
     */
    Registry getRegistry();

    /**
     * Component bindings. Implementations of this interface populate a dependency injection container. A component binding is a mapping from an interface or
     * class to a class that implements the interface or extends the class, respectively, or to an instance of such a class.
     */
    interface Bindings {

        /**
         * Add component bindings to the given registry.
         *
         * @param registry is an interface to register component bindings.
         */
        void bindComponents(Registry registry);
    }

    /**
     * Allows registration of components into a container.
     * <p/>
     * This interface is mainly used by {@link org.fluidity.composition.spi.PackageBindings} objects when the host application populates its dependency
     * injection container.
     * <p/>
     * Outside of <code>PackageBindings</code>, objects implementing this interface for an uninitialized container can be acquired by calling {@link
     * OpenComponentContainer#getRegistry()} on the container.
     *
     * @author Tibor Varga
     */
    interface Registry {

        /**
         * Adds a component class to the container backing this registry. The component class will be instantiated on demand by the container.
         * <p/>
         * The supplied component class may implement {@link org.fluidity.composition.spi.ComponentFactory}, in which case the receiver must support
         * the factory functionality as described in its documentation.
         * <p/>
         * When no class is provided in the <code>interfaces</code> parameter, the list of interfaces that resolve to the component at run-time is determined
         * by the algorithm documented at {@link Components}.
         *
         * @param implementation the component class.
         * @param interfaces     optional list of interfaces that should resolve to the supplied component class.
         *
         * @throws OpenComponentContainer.BindingException when component registration fails
         */
        @SuppressWarnings("JavadocReference")
        <T> void bindComponent(Class<T> implementation, Class<? super T>... interfaces) throws BindingException;

        /**
         * Adds a component instance to the container backing this registry.
         * Binds a component instance to its interface. Use this method when you have no control over the instantiation of a class, such as those performed by
         * third party tools, but you still want to make the instances available for components in the container to depend on.
         * <p/>
         * The supplied component instance may be a {@link org.fluidity.composition.spi.ComponentFactory} instance, in which case the receiver must support
         * the factory functionality as described in its documentation.
         * <p/>
         * When no class is provided in the <code>interfaces</code> parameter, the list of interfaces that resolve to the component at run-time is determined
         * by the algorithm documented at {@link Components}.
         *
         * @param instance   the component instance.
         * @param interfaces optional list of interfaces that should resolve to the supplied component class.
         *
         * @throws OpenComponentContainer.BindingException when component registration fails
         */
        @SuppressWarnings("JavadocReference")
        <T> void bindInstance(T instance, Class<? super T>... interfaces) throws BindingException;

        /**
         * Returns a new child container that will use the container for this registry as its parent. See {@link ComponentContainer#makeChildContainer()}.
         *
         * @return an open container, never <code>null</code>.
         */
        OpenComponentContainer makeChildContainer();

        /**
         * Returns a new child container that will use the container for this registry as its parent, for the purpose of resolving through the parent
         * dependencies on the supplied <code>interfaces</code> only.
         *
         * @param implementation the component whose dependencies the child container will help resolving.
         * @param interfaces     the interfaces to resolve to the supplied <code>implementation</code> in both this container and the one returned.
         *
         * @return an open container, never <code>null</code>.
         *
         * @throws OpenComponentContainer.BindingException when component registration fails
         */
        <T> OpenComponentContainer makeChildContainer(Class<T> implementation, Class<? super T>... interfaces) throws BindingException;
    }

    /**
     * Reports errors when binding a component class or instance to its component interfaces.
     */
    class BindingException extends ContainerException {

        public BindingException(final String format, final Object... data) {
            super(format, data);
        }
    }
}
