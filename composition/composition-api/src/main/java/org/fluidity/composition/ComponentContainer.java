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

import java.lang.reflect.Method;

import org.fluidity.composition.network.Graph;

/**
 * This is a dependency injection container.
 * <p/>
 * Containers may form a hierarchy. If a component is not found in a child container, it tries to find the component in its parent.
 * <p/>
 * Components are instantiated by the container on demand and their dependencies are resolved by means of constructor or field based dependency injection. No
 * setter injection or other means of injections are supported.
 * <p/>
 * Components may be context aware, meaning that separate instances are created for different contexts. Entire chains of dependencies may be instantiated
 * multiple times for different contexts.
 * <p/>
 * Most of your components should never interact directly with this interface. Exceptions to this are objects created by third party tools, or components with
 * dynamic dependencies, e.g. that depend on some run-time criteria.
 * <p/>
 * Each container has a registry, which allows components to be programmatically added to the container when it is booting. Except in rare circumstances, you
 * do not need to directly interact with a registry as the <code>org.fluidity.maven:maven-composition-plugin</code> does that for you. You should not need to
 * know about the registry unless you really have some fancy component binding logic. In those rare cases, the followings are provided to help you.
 * <p/>
 * The registry offers several ways to map an implementation to an interface in the host container. Which one you need depends on your requirements. These
 * methods are invoked from the {@link org.fluidity.composition.spi.PackageBindings#bindComponents(ComponentContainer.Registry)} method of your binding
 * implementation.
 * <ul>
 * <li>To simply register a component implementation for its component interfaces, use {@link ComponentContainer.Registry#bindComponent(Class)}. This is
 * exactly what the Maven plugin uses for a @{@link Component} annotated class with no @{@link Component#automatic()} setting so if this method is all you need
 * then you should simply use the plugin instead of creating your own binding class.</li>
 * <li>To register an already instantiated component implementation for a component interface, use {@link ComponentContainer.Registry#bindInstance(Object,
 * Class[])}. If the implementation is annotated with @{@link Component} then its @{@link Component#automatic()} setting must be set to
 * <code>false</code>.</li>
 * <li>To register a component implementation without having some or all of its dependencies accessible in the same container, use {@link
 * ComponentContainer.Registry#makeChildContainer(Class, Class[])} method and use the returned container's {@link OpenComponentContainer#getRegistry()} method
 * to gain access to the registry in which to bind the hidden dependencies. If the implementation is annotated with @{@link Component} then its @{@link
 * Component#automatic()} setting must be set to <code>false</code>.</li>
 * </ul>
 *
 * @author Tibor Varga
 */
public interface ComponentContainer {

    /**
     * Returns an explorable dependency graph.
     *
     * @return an explorable dependency graph.
     */
    Graph getGraph();

    /**
     * Returns a component by interface or (super)class. This method is provided for boundary objects (objects created outside the container by third party
     * tools) to acquire their dependencies.
     * <p/>
     * If there is no component bound to the given class itself, an attempt is made to locate a single component that implements the given interface or is an
     * instance of the given class or any of its subclasses.
     *
     * @param api is a class object that was used to bind a component against; never <code>null</code>.
     *
     * @return the component bound against the give class or <code>null</code> when none was found.
     *
     * @throws ComponentContainer.ResolutionException
     *          when dependency resolution fails
     */
    <T> T getComponent(Class<T> api) throws ResolutionException;

    /**
     * Returns a component by interface or (super)class. This method is provided for factory objects (objects creating transient components) as a convenient
     * shortcut to acquire a child container, register component bindings in it and get the child container instantiate the requested component.
     *
     * @param api      is a class object that was used to bind a component against.
     * @param bindings is invoked to add component bindings to the child container.
     *
     * @return the component bound against the give class or <code>null</code> when none was found.
     *
     * @throws ComponentContainer.ResolutionException
     *          when dependency resolution fails
     */
    <T> T getComponent(Class<T> api, Bindings bindings) throws ResolutionException;

    /**
     * Creates another container whose components' dependencies will be satisfied from itself first and then from this container when the child could find no
     * component to satisfy a dependency with.
     * <p/>
     * This method can be used to gain access to the dependency resolution and injection functionality of the container without polluting it with new
     * components after it has been set up. Components placed in the child container will not be visible to clients of, or components in, this container.
     *
     * @return a container that defaults to this container for satisfying component dependencies.
     */
    OpenComponentContainer makeChildContainer();

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
     * Returns the list of components implementing the given interface, provided that they each, or the given interface itself, has been marked with the {@link
     * ComponentGroup} annotation.
     *
     * @param api the group interface class.
     *
     * @return a possibly empty array of components that belong to the given group.
     */
    <T> T[] getComponentGroup(Class<T> api);

    /**
     * Component bindings.
     */
    interface Bindings {

        /**
         * Add component bindings to the given registry.
         *
         * @param registry is an interface to register component bindings.
         */
        void bindComponents(ComponentContainer.Registry registry);
    }

    /**
     * Allow registration of components into a container. This registry supports access to a child container in the parent by means of linking. The child
     * container exposes one component that will be accessible through the parent but its dependencies will be resolved in the context of the child container.
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
         * Binds a component class to its component interfaces.
         * <p/>
         * What interface the component is bound is determined first by it being or not being annotated by @{link Component}. If it is not, it will be bound to
         * its own class.
         * <p/>
         * If the component is annotated with @{link Component} then what it is bound to is determined by the annotation having or not having the @{@link
         * Component#api()} parameter. If the annotation has that parameter, the component will be bound to the class(es) specified as the value of the @{@link
         * Component#api()} parameter.
         * <p/>
         * If the @{@link Component} annotation has no @{@link Component#api()} parameter, the following algorithm is employed to find the classes to bind the
         * component to:
         * <ol>
         * <li>If the class implements no interfaces and its super class is {@link Object} then the component is bound to its own class.</li>
         * <li>If the class implements no interfaces and its super class is not {@link Object} then this algorithm is run from the beginning through the
         * super class.</li>
         * <li>If the class directly implements one or more interfaces then the component will be bound to all of those interfaces.
         * </ol>
         * <p/>
         * Two special cases must be handled by the receiver: when <code>implementation</code> is either a {@link
         * org.fluidity.composition.spi.ComponentFactory}
         * or a {@link org.fluidity.composition.spi.ComponentVariantFactory}. In the latter case, a total of two bindings must take place, one for the variant
         * factory and one for the component it creates variants of, and they can take place in any order. Irrespective of the order, the variant factory must
         * receive any component lookup for the target component.
         *
         * @param implementation the component class.
         *
         * @throws ComponentContainer.BindingException
         *          when the binding cannot be performed
         */
        <T> void bindComponent(Class<T> implementation) throws BindingException;

        /**
         * Binds a component instance to its component interfaces. In most case you should use the other registration methods that accept a class rather then
         * instantiating the component yourself. Use this method when you have no control over the instantiation of a class, such as those performed by third
         * party tools, but you still want to make the instances available for components in the container to depend on.
         * <p/>
         * The supplied component instance may be a {@link org.fluidity.composition.spi.ComponentFactory} or a {@link
         * org.fluidity.composition.spi.ComponentVariantFactory} instance, in which case the receiver must support their respective functionality as described
         * in their documentation and at {@link #bindComponent(Class)}.
         *
         * @param instance the component instance.
         *
         * @throws ComponentContainer.BindingException
         *          when the binding cannot be performed
         */
        <T> void bindInstance(T instance) throws BindingException;

        /**
         * Binds a list of components to a component group.
         *
         * @param api             the group interface to bind the components against.
         * @param implementations the components to bind against the group interface.
         *
         * @throws ComponentContainer.BindingException
         *          when the binding cannot be performed
         */
        <T> void bindGroup(Class<T> api, Class<? extends T>... implementations) throws BindingException;

        /**
         * Binds a component class to the given component interfaces.
         * <p/>
         * Two special cases must be handled by the receiver: when <code>implementation</code> is either a {@link
         * org.fluidity.composition.spi.ComponentFactory}
         * or a {@link org.fluidity.composition.spi.ComponentVariantFactory}. In the latter case, a total of two bindings must take place, one for the variant
         * factory and one for the component it creates variants of, and they can take place in any order. Irrespective of the order, the variant factory must
         * receive any component lookup for the target component.
         *
         * @param implementation the component class.
         * @param interfaces     the interfaces against which to bind the component; preferably an interface class.
         *
         * @throws ComponentContainer.BindingException
         *          when the binding cannot be performed
         */
        <T> void bindComponent(Class<T> implementation, Class<? super T>... interfaces) throws BindingException;

        /**
         * Binds a factory class to the given component interfaces.
         * <p/>
         * Two special cases must be handled by the receiver: when <code>implementation</code> is either a {@link
         * org.fluidity.composition.spi.ComponentFactory}
         * or a {@link org.fluidity.composition.spi.ComponentVariantFactory}. In the latter case, a total of two bindings must take place, one for the variant
         * factory and one for the component it creates variants of, and they can take place in any order. Irrespective of the order, the variant factory must
         * receive any component lookup for the target component.
         *
         * @param factory    the component class.
         * @param interfaces the interfaces against which to bind the component; preferably an interface class.
         *
         * @throws ComponentContainer.BindingException
         *          when the binding cannot be performed
         */
        void bindFactory(Class<?> factory, Class<?>... interfaces) throws BindingException;

        /**
         * Binds a component instance to its interface. In most case you should use the other registration methods that accept a class rather then
         * instantiating the component yourself. Use this method when you have no control over the instantiation of a class, such as those performed by third
         * party tools,
         * but you still want to make the instances available for components in the container to depend on.
         * <p/>
         * The supplied component instance may be a {@link org.fluidity.composition.spi.ComponentFactory} or a {@link
         * org.fluidity.composition.spi.ComponentVariantFactory} instance, in which case the receiver must support their respective functionality as described
         * in their documentation and at {@link #bindComponent(Class)}.
         *
         * @param instance   the component instance.
         * @param interfaces the interfaces to which to bind the component; preferably an interface class.
         *
         * @throws ComponentContainer.BindingException
         *          when the binding cannot be performed
         */
        <T> void bindInstance(T instance, Class<? super T>... interfaces) throws BindingException;

        /**
         * Returns a new child container that will use the container for this registry as its parent. See {@link ComponentContainer#makeChildContainer()}.
         *
         * @return an open container, never <code>null</code>.
         */
        OpenComponentContainer makeChildContainer();

        /**
         * Returns a new child container that will use the container for this registry as its parent, for the purpose of resolving through the parent
         * dependencies on <code>interfaces</code>.
         *
         * @param implementation the component whose dependencies the child container will help resolving.
         *
         * @return an open container, never <code>null</code>.
         *
         * @throws ComponentContainer.BindingException
         *          when the binding cannot be performed
         */
        <T> OpenComponentContainer makeChildContainer(Class<T> implementation) throws BindingException;

        /**
         * Returns a new child container that will use the container for this registry as its parent, for the purpose of resolving through the parent
         * dependencies on <code>interfaces</code>.
         *
         * @param implementation the component whose dependencies the child container will help resolving.
         * @param interfaces     the interfaces to bind <code>implementation</code> to in both the parent and the child container.
         *
         * @return an open container, never <code>null</code>.
         *
         * @throws ComponentContainer.BindingException
         *          when the binding cannot be performed
         */
        <T> OpenComponentContainer makeChildContainer(Class<T> implementation, Class<? super T>... interfaces) throws BindingException;
    }

    /**
     * Exceptions related to the dependency injection container.
     */
    class ContainerException extends RuntimeException {

        public ContainerException(final String format, final Object... data) {
            super(String.format(format, data));
        }
    }

    /**
     * Exception related to component bindings.
     */
    class BindingException extends ContainerException {

        public BindingException(final String format, final Object... data) {
            super(format, data);
        }
    }

    /**
     * Exception related to dependency resolution.
     */
    class ResolutionException extends ContainerException {

        public ResolutionException(final String format, final Object... data) {
            super(format, data);
        }
    }

    /**
     * Exception signifying a circular dependency.
     */
    class CircularReferencesException extends ResolutionException {

        public CircularReferencesException(final Class<?> api, final String path) {
            super("Circular dependency detected when resolving %s: %s", api, path);
        }
    }

    public class CircularInvocationException extends ContainerException {

        public CircularInvocationException(final Object object, final Method method) {
            super("Circular method invocation detected when calling %s on %s@%x", method, object.getClass().getName(), System.identityHashCode(object));
        }
    }
}
