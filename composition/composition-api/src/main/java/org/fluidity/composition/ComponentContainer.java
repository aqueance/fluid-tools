/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.composition;

/**
 * This is a dependency injection container. Containers may form a hierarchy. If a component is not found in a nested container, it tries to find the component
 * in its parent. Components are instantiated by the container on demand and their dependencies are resolved by means of constructor or field based dependency
 * injection. No setter injection or other means of injections are supported. Components may be context aware in that different instance is created for
 * different contexs. Entire dependency chains of dependencies may be instantiated multiple times for different contexts.
 * <p/>
 * Most of your components should never interact directly with this interface. Exceptions to this are objects created by third party tools, or components with
 * dynamic dependencies, e.g. that depend on some run-time criteria.
 * <p/>
 * The registry offers several ways to map an implementation to interface in the host container. Which one you need depends on your requirements. These methods
 * are normally invoked from the {@link org.fluidity.composition.spi.PackageBindings#bindComponents(ComponentContainer.Registry)} method.
 * <ul>
 * <li>To simply register a component implementation for a component interface, use {@link ComponentContainer.Registry#bindComponent(Class,Class)}. For
 * auto-wired components this is achieved by annotating the implementation class with {@link Component}.
 * </li>
 * <li>To register a default implementation of some component interface, use {@link ComponentContainer.Registry#bindDefault(Class)}. For auto-wired components
 * this is achieved by annotating the implementation class with {@link Component#fallback()} set to <code>true</code>.
 * </li>
 * <li>To register an already instantiated component implementation for a component interface, use
 * {@link ComponentContainer.Registry#bindInstance(Class,Object)}. This use precludes auto-wiring, which must be switched off by annotating the implementation
 * class with the {@link Component#automatic()} annotation set to <code>false</code>.
 * </li>
 * <li>To register a component implementation without having some or all of its dependencies accessible in the same container, use
 * {@link ComponentContainer.Registry#makeNestedContainer(Class,Class)} method and use the returned container's {@link OpenComponentContainer#getRegistry()}
 * method to gain access to the registry in which to bind the hidden dependencies. This use precludes auto-wiring, which must be switched off by annotating the
 * implementation class with {@link Component#automatic()} annotation set to <code>false</code>.
 * </li>
 * </ul>
 *
 * @author Tibor Varga
 */
public interface ComponentContainer {

    /**
     * Returns a component by interface or (super)class. This method is provided for boundary objects (objects created outside the container by third party
     * tools) to acquire their dependencies.
     * <p/>
     * If there is no component bound to the given class itself, an attempt is made to locate a single component that implements the given interface or is an
     * instance of the given class or any of its subclasses.
     *
     * @param componentClass is a class object that was used to register a component against; never <code>null</code>.
     *
     * @return the component registered against the give class or <code>null</code> when none was found.
     *
     * @throws ComponentContainer.ResolutionException
     *          when dependency resolution fails
     */
    <T> T getComponent(Class<T> componentClass) throws ResolutionException;

    /**
     * Returns a component by interface or (super)class. This method is provided for factory objects (objects creating transient components) as a convenient
     * shortcut to acquire a nested container, bind components to it and get the nested container instantiate the requested component.
     *
     * @param componentClass is a class object that was used to register a component against; never <code>null</code>.
     * @param bindings       is invoked to add component bindings to the nested container.
     *
     * @return the component registered against the give class or <code>null</code> when none was found.
     *
     * @throws ComponentContainer.ResolutionException
     *          when dependency resolution fails
     */
    <T> T getComponent(Class<T> componentClass, Bindings bindings) throws ResolutionException;

    /**
     * Creates another container whose components' dependencies will be satisfied from itself first and which will default to this container when it could find
     * no component to satisfy a dependency with.
     * <p/>
     * This method can be used to gain access to the dependency resolution and injection functionality of the container without polluting it with new components
     * after it has been set up. Components placed in the nested container will not be visible to clients of, or components in, this container.
     *
     * @return a container that defaults to this container for satisfying component dependencies.
     */
    OpenComponentContainer makeNestedContainer();

    /**
     * Post-initialize the @{@link Component} annotated fields of the given object.
     *
     * @param component is a component that needs field injection of dependencies.
     *
     * @return the field injected object - the same that was passed as the method parameter.
     *
     * @throws ComponentContainer.ResolutionException
     *          when dependency resolution fails
     */
    <T> T initialize(T component) throws ResolutionException;

    /**
     * Created to be add component bindings to a registry.
     */
    interface Bindings {

        /**
         * Add component bindings into the given registry.
         *
         * @param registry is an interface to register component bindings.
         */
        void bindComponents(ComponentContainer.Registry registry);
    }

    /**
     * Allow registration of components into a container. This registry supports access to a nested container in the parent by means of linking. The nested
     * container exposes one component that will be accessible through the parent but its dependencies will be resolved in the context of the nested container.
     * <p/>
     * This object is mainly used by {@link org.fluidity.composition.spi.PackageBindings} objects when the host application populates its dependency injection
     * container.
     * <p/>
     * Objects implementing this interface can be acquired by calling {@link OpenComponentContainer#getRegistry()}.
     *
     * @author Tibor Varga
     */
    interface Registry {

        /**
         * Binds a component class to its component interface. The component interface is taken either from the {@link Component#api()} annotation parameter or
         * the single interface the class implements, unless the {@link Component#fallback()} annotation parameter is <code>true</code>, in which case the
         * component is bound as a default implementation that can be overridden by another binding against the same API interface.
         * <p/>
         * Two special cases must be handled by the receiver: when the implementation is either a {@link org.fluidity.composition.spi.ComponentFactory} or a {@link
         * org.fluidity.composition.spi.ComponentVariantFactory}. In the latter case, two bindings must take place, one for the variant factory and one for the
         * component it creates variants of, and they can take place in any order. Irrespective of the order, the variant factory must receive any component
         * lookup for the target component.
         * <p/>
         * This is a convenience method whose functionality is also provided by other methods collectively, namely {@link #bindDefault(Class)} and {@link
         * #bindComponent(Class, Class)}.
         *
         * @param implementation the component class.
         *
         * @throws ComponentContainer.BindingException
         *          when the binding cannot be performed
         */
        void bindComponent(Class<?> implementation) throws BindingException;

        /**
         * Binds a component class to its interface. Two special cases must be handled by the receiver: when the implementation is either a {@link
         * org.fluidity.composition.spi.ComponentFactory} or a {@link org.fluidity.composition.spi.ComponentVariantFactory}. In the latter case, two bindings must take
         * place, one for the variant factory and one for the component it creates variants of, and they can take place in any order. Irrespective of the order,
         * the variant factory must receive any component lookup for the target component.
         *
         * @param key            the key by which to register the component; preferably an interface class.
         * @param implementation the component class.
         *
         * @throws ComponentContainer.BindingException
         *          when the binding cannot be performed
         */
        <T> void bindComponent(Class<T> key, Class<? extends T> implementation) throws BindingException;

        /**
         * Calls {@link #bindComponent(Class, Class)} passing implementation as both parameters.
         *
         * @param implementation the class of the component to register.
         *
         * @throws ComponentContainer.BindingException
         *          when the binding cannot be performed
         */
        <T> void bindDefault(Class<? extends T> implementation) throws BindingException;

        /**
         * Binds a component instance to its interface. In most case you should use the other registration methods that accept a class rather then instantiating
         * the component yourself. Use this method when you have no control over the instantiation of a class, such as those created by third party tools, but
         * you still want to make them available for components in the container to depend on.
         * <p/>
         * The supplied component instance may be a {@link org.fluidity.composition.spi.ComponentFactory} or a {@link
         * org.fluidity.composition.spi.ComponentVariantFactory} instance, in which case the receiver must support their respective functionality as described in
         * their documentation and at {@link #bindComponent(Class, Class)}.
         *
         * @param key      the key by which to register the component; preferably an interface class.
         * @param instance the component instance.
         *
         * @throws ComponentContainer.BindingException
         *          when the binding cannot be performed
         */
        <T> void bindInstance(Class<? super T> key, T instance) throws BindingException;

        /**
         * Returns a new child registry whose container will use the container for this registry as its parent. See {@link
         * OpenComponentContainer#makeNestedContainer()}.
         *
         * @return an open container, never <code>null</code>.
         */
        OpenComponentContainer makeNestedContainer();

        /**
         * Returns a new child registry whose container will use the container for this registry as its parent.
         *
         * @param key            the key under which the child container will be accessible through its parent.
         * @param implementation the component whose dependencies the child container will help resolving.
         *
         * @return an open container, never <code>null</code>.
         *
         * @throws ComponentContainer.BindingException
         *          when the binding cannot be performed
         */
        <T> OpenComponentContainer makeNestedContainer(Class<T> key, Class<? extends T> implementation) throws BindingException;
    }

    class ContainerException extends RuntimeException {

        public ContainerException(final String format, final Object... data) {
            super(String.format(format, data));
        }
    }

    class BindingException extends ContainerException {

        public BindingException(final String format, final Object... data) {
            super(format, data);
        }
    }

    class ResolutionException extends ContainerException {

        public ResolutionException(final String format, final Object... data) {
            super(format, data);
        }
    }

    class CircularReferencesException extends ResolutionException {

        public CircularReferencesException(final Class<?> componentClass) {
            super("Circular dependency detected involving %s", componentClass);
        }

        public CircularReferencesException(final Class<?> componentClass, final String resolutions) {
            super("Circular dependency detected involving %s: %s", componentClass, resolutions);
        }
    }
}
