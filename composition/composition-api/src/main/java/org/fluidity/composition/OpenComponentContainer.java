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
 * container's registry. Except in {@link ContainerBoundary rare circumstances}, you do not need to directly interact with a registry of the root container as
 * the <code>org.fluidity.maven:maven-composition-plugin</code> Maven plugin does that for you.
 * <p/>
 * The registry offers several ways to map an implementation to an interface in the host container. Which one you need depends on your requirements. These
 * methods are mostly invoked from the {@link OpenComponentContainer.Bindings#bindComponents(OpenComponentContainer.Registry) bindComponents()}
 * method of your {@link org.fluidity.composition.spi.PackageBindings binding} implementation.
 * <ul>
 * <li>To simply register a component implementation for its component interfaces, use {@link OpenComponentContainer.Registry#bindComponent(Class, Class[])
 * bindComponent()}. This is exactly what the Maven plugin does for a {@link Component @Component} annotated class with no {@link Component#automatic()
 * &#64;Component&#40;automatic = false&#41;} setting so if this method is all you need then you should simply use the plugin instead of creating your own
 * binding class.</li>
 * <li>To register an already instantiated component implementation for a component interface, use {@link OpenComponentContainer.Registry#bindInstance(Object,
 * Class[]) bindInstance()}. If the implementation is annotated with <code>@Component</code> then its <code>@Component(automatic = ...)</code> parameter must
 * be
 * set to <code>false</code>.</li>
 * <li>To register a component implementation when some or all of its dependencies are - by design - not accessible in the same container, use {@link
 * org.fluidity.composition.OpenComponentContainer.Registry#makeChildContainer(Class, Class[]) makeChildContainer()} method and use the returned container's
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
     * @return a {@link OpenComponentContainer.Registry} instance.
     */
    Registry getRegistry();

    /**
     * <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Composition">Component</a> bindings. Implementations of this interface populate a
     * dependency injection container when asked to. A component binding is a mapping from an interface or class to a class that implements the interface or
     * extends the class, respectively, or to an instance of such a class.
     */
    interface Bindings {

        /**
         * Add the component bindings to the given registry.
         *
         * @param registry is an object to register component bindings with.
         */
        void bindComponents(Registry registry);
    }

    /**
     * Allows registration of <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Composition">components</a> into a
     * <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Concept">dependency injection</a>
     * <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Containers">container</a>.
     * <p/>
     * This interface is mainly used by {@link org.fluidity.composition.spi.PackageBindings} objects that are invoked when the host application populates its
     * dependency injection containers.
     * <p/>
     * Outside of <code>PackageBindings</code>, an object implementing this interface for an {@link OpenComponentContainer uninitialized container} can be
     * acquired by calling {@link OpenComponentContainer#getRegistry() getRegistry()} on the container.
     *
     * @author Tibor Varga
     */
    interface Registry {

        /**
         * Binds a component class to a list of component interfaces in the container behind this registry. The component class will be instantiated on demand
         * by the container when it resolves any of the provided component interfaces.
         * <p/>
         * The supplied component class may implement {@link org.fluidity.composition.spi.ComponentFactory}, in which case the receiver must support
         * the factory functionality as described in the <code>ComponentFactory</code> documentation.
         * <p/>
         * When no class is provided in the <code>interfaces</code> parameter, the list of interfaces that resolve to the component at run-time will be
         * determined by the algorithm documented at {@link Components}.
         *
         * @param implementation the component class.
         * @param interfaces     optional list of interfaces that should resolve to the supplied component class.
         *
         * @throws OpenComponentContainer.BindingException
         *          when component registration fails
         */
        @SuppressWarnings("JavadocReference")
        <T> void bindComponent(Class<T> implementation, Class<? super T>... interfaces) throws BindingException;

        /**
         * Binds a component instance to a list of component interfaces in the container behind this registry. Use this method when you have no control over
         * the instantiation of a class as in case of third party tools handing you the component, but you still want to make the instances available for
         * components in the container to depend on.
         * <p/>
         * The supplied component instance may be a {@link org.fluidity.composition.spi.ComponentFactory} instance, in which case the receiver must support
         * the factory functionality as described in the <code>ComponentFactory</code> documentation.
         * <p/>
         * When no class is provided in the <code>interfaces</code> parameter, the list of interfaces that resolve to the component at run-time will be
         * determined by the algorithm documented at {@link Components}.
         *
         * @param instance   the component instance.
         * @param interfaces optional list of interfaces that should resolve to the supplied component class.
         *
         * @throws OpenComponentContainer.BindingException
         *          when component registration fails
         */
        @SuppressWarnings("JavadocReference")
        <T> void bindInstance(T instance, Class<? super T>... interfaces) throws BindingException;

        /**
         * Returns a new child container that will use the container behind this registry as its parent. See {@link ComponentContainer#makeChildContainer()}.
         *
         * @return an open container, never <code>null</code>.
         */
        OpenComponentContainer makeChildContainer();

        /**
         * Returns a new child container that will use the container behind this registry as its parent to resolve only the given list of interfaces. This
         * variant is intended to be used when the list of component interfaces resolvable in the returned container through the container behind this registry
         * is restricted to a fixed set.
         *
         * @param implementation the component whose dependencies the child container will resolve.
         * @param interfaces     the interfaces that the returned container should be able resolve through this container.
         *
         * @return an open container, never <code>null</code>.
         *
         * @throws OpenComponentContainer.BindingException
         *          when component registration fails
         */
        <T> OpenComponentContainer makeChildContainer(Class<T> implementation, Class<? super T>... interfaces) throws BindingException;
    }

    /**
     * Reports an error that occurred when binding a component class or instance to its component interfaces.
     */
    class BindingException extends ContainerException {

        /**
         * Creates a new instance using the given formatted text.
         *
         * @param format the Java format specification.
         * @param data   the details to format.
         */
        public BindingException(final String format, final Object... data) {
            super(format, data);
        }
    }
}
