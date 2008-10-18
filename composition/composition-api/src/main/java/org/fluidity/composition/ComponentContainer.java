/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.composition;

/**
 * This is a dependency injection container. Containers may form a hierarchy. If a component is not found in a nested
 * container, it tries to find the component in its parent. Components are instantiated by the container on demand and
 * their dependencies are resolved by means of constructor based dependency injection. <p/> Most of your components
 * should never interact directly with this interface. Exceptions are boundary objects, i.e. objects created by third
 * party tools, or components with dynamic dependencies, e.g. that depend on some run-time criteria.
 *
 * <p/>
 *
 * <h1>Using the <code>ComponentContainer.Registry</code></h1>
 *
 * The registry exposes several <code>bind()</code> methods to bind an interface to implementation mapping to its host
 * container. Which one you need depends on your requirements. These methods are normally invoked from the {@link
 * PackageBindings#registerComponents(ComponentContainer.Registry)} method.
 *
 * <ul>
 *
 * <li>If you want to bind component that will be visible in the container itself <b>AND</b> you expect all its
 * dependencies to be visible in that same container <b>AND</b> you want to map the component against its own class
 * rather than against an interface <b>AND</b> you don't need to override constructor parameters:<p/><blockquote>{@link
 * ComponentContainer.Registry#bind(Class)}</blockquote><p/></li>
 *
 * <li>If you want to bind an application scoped singleton component that will be visible in the container itself
 * <b>AND</b> you expect all its dependencies to be visible in that same container:<p/><blockquote>{@link
 * ComponentContainer.Registry#bind(Class,Class,ConstructorParameter[])}</blockquote><p/></li>
 *
 * <li>If you want to bind component that will be visible in the container itself <b>AND</b> you expect all its
 * dependencies to be visible in that same container <b>AND</b> you want to map an already instantiated component object
 * to its interface:<p/><blockquote>{@link ComponentContainer.Registry#bind(Class,Object)}</blockquote><p/></li>
 *
 * <li>If you want to bind a component that will be visible in the container itself <b>AND</b> you expect all its
 * dependencies to be visible in that same container <b>AND</b> you want to make it (thread local <b>OR</b>
 * proxied):<p/><blockquote>{@link ComponentContainer.Registry#bind(Class,Class,boolean,boolean,boolean,ConstructorParameter[])}</blockquote><p/></li>
 *
 * <li>If you want to bind a component that will be visible in the container itself <b>AND</b> you expect all its
 * dependencies to be visible in that same container <b>AND</b> you want to (make it thread local <b>OR</b> proxied
 * <b>OR</b> you want to supply your own dependency injected factory class to instantiate the
 * component):<p/><blockquote>{@link ComponentContainer.Registry#bind(Class,Class,boolean,boolean,boolean,Class)}</blockquote><p/></li>
 *
 * <li>If you want to bind a component that will be visible in the container itself <b>AND</b> you expect all its
 * dependencies to be visible in that same container <b>AND</b> you want to (make it thread local <b>OR</b> proxied
 * <b>OR</b> you want to supply your own already instantiated factory object to instantiate the
 * component):<p/><blockquote>{@link ComponentContainer.Registry#bind(Class,Class,boolean,boolean,boolean,
 * ComponentContainer.ComponentFactory)}</blockquote><p/></li>
 *
 * <li>If you want to bind an application scoped singleton component that will be visible in the container itself
 * <b>AND</b> you want to also bind some of its dependencies in a way so as to hide them from view in the container
 * itself:<p/><blockquote> use the {@link ComponentContainer.Registry#makeNestedContainer(Class,Class,ConstructorParameter[])}
 * method</blockquote><p/> and use the returned container's {@link OpenComponentContainer#getRegistry()} method to bind
 * your hidden dependencies.</li>
 *
 * <li>If you want to bind a component that will be visible in the container itself <b>AND</b> you want to also bind
 * some of its dependencies in a way so as to hide them from view in the container itself <b>AND</b> you want to make it
 * (thread local <b>OR</b> proxied):<p/><blockquote> use the {@link ComponentContainer.Registry#makeNestedContainer(Class,
 * Class,boolean,boolean,boolean,ConstructorParameter[])} method</blockquote><p/> and use the returned container's
 * {@link OpenComponentContainer#getRegistry()} method to bind your hidden dependencies.</li>
 *
 * <li>If you want to bind a component that will be visible in the container itself <b>AND</b> you want to also bind
 * some of its dependencies in a way so as to hide them from view in the container itself <b>AND</b> you want to (make
 * it thread local <b>OR</b> proxied <b>OR</b> you want to supply your own dependency injected factory class to
 * instantiate the component):<p/><blockquote> use the {@link ComponentContainer.Registry#makeNestedContainer(Class,
 * Class,boolean,boolean,boolean,ConstructorParameter[])} method</blockquote><p/> and use the returned container's
 * {@link OpenComponentContainer#getRegistry()} method to bind your hidden dependencies.</li>
 *
 * <li>If you want to bind a component that will be visible in the container itself <b>AND</b> you want to also bind
 * some of its dependencies in a way so as to hide them from view in the container itself <b>AND</b> you want to (make
 * it thread local <b>OR</b> proxied <b>OR</b> you want to supply your own already instantiated factory object to
 * instantiate the component):<p/><blockquote> use the {@link ComponentContainer.Registry#makeNestedContainer(Class,
 * Class,boolean,boolean,boolean,ConstructorParameter[])} method</blockquote><p/> and use the returned container's
 * {@link OpenComponentContainer#getRegistry()} method to bind your hidden dependencies.</li>
 *
 * </ul>
 *
 * @author Tibor Varga
 */
public interface ComponentContainer {

    /**
     * Returns a component by interface or (super)class. This method is provided for boundary objects (objects created
     * outside the container by third party tools) to acquire their dependencies.
     *
     * <p/>
     *
     * If there is no component bound to the given class itself, an attempt is made to locate a single component that
     * implements the given interface or is an instance of the given class or any of its subclasses.
     *
     * @param componentClass is a class object that was used to register a component against; never <code>null</code>.
     *
     * @return the component registered against the give class or <code>null</code> when none was found.
     */
    <T> T getComponent(final Class<T> componentClass);

    /**
     * Returns a component by interface or (super)class. This method is provided for factory objects (objects creating
     * transient components) as a convenient shortcut to acquire a nested container, bind components to it and get the
     * nested container instantiate the requested component.
     *
     * @param componentClass is a class object that was used to register a component against; never <code>null</code>.
     * @param bindings       is invoked to add component bindings to the nested container.
     *
     * @return the component registered against the give class or <code>null</code> when none was found.
     */
    <T> T getComponent(final Class<T> componentClass, final Bindings bindings);

    /**
     * Creates another container whose components' dependencies will be satisfied from itself first and which will
     * default to this container when it could find no component to satisfy a dependency with. <p/> This method can be
     * used to gain access to the dependency resolution and injection functionality of the container without polluting
     * it with new components after it has been set up. Components placed in the nested container will not be visible to
     * clients of, or components in, this container.
     *
     * @return a container that defaults to this container for satisfying component dependencies.
     */
    OpenComponentContainer makeNestedContainer();

    /**
     * Created to be add component bindings to a registry.
     */
    interface Bindings {

        /**
         * Add component bindings into the given registry.
         *
         * @param registry is an interface to register component bindings.
         */
        void registerComponents(final ComponentContainer.Registry registry);
    }

    /**
     * Allow registration of components into a container. This registry supports access to a nested container in the
     * parent by means of linking. The nested container exposes one component that will be accessible through the parent
     * but its dependencies will be resolved in the context of the nested container. <p/> This object is mainly used by
     * <code>PackageBinding</code> objects when the host application populates its dependency injection container. <p/>
     * Objects implementing this interface can be aquired by calling <code>OpenComponentContainer.getRegistry()</code>.
     *
     * @author Tibor Varga
     */
    interface Registry {

        /**
         * Adds an expected dependency interface to the registry. The list of such interface classes will be verified to
         * have been bound when the container is populated and if any one of them is not bound, a report is logged to
         * make the fact known and application startup is prevented. <p/> This method is useful for modules that expose
         * an interface that they themselves do not implemebt but expect at run-time to be bound.
         *
         * TODO: see if this is actually useful
         *
         * @param dependencyInterface is the expected dependency interface.
         * @param dependentIClass     is the class that has the dependency.
         */
        void requireDependency(final Class dependencyInterface, final Class dependentIClass);

        /**
         * Calls <code>bind(implementation, implementation, true, false, false)</code>.
         *
         * @param implementation the class of the component to register.
         */
        <T> void bind(final Class<? extends T> implementation);

        /**
         * Calls <code>bind(key, implementation, true, false, false, parameters)</code>.
         *
         * @param key            the key by which to register the component; preferrably an interface class.
         * @param implementation the component class.
         * @param parameters     the non-empty list of parameters to use to instantiate the component, never
         *                       <code>null</code>.
         *
         * @see #component(Class)
         * @see #constant(Object)
         */
        <T> void bind(final Class<T> key,
                      final Class<? extends T> implementation,
                      final ConstructorParameter... parameters);

        /**
         * Binds a component class to its interface with forced constructor parameters. Use this when you want to supply
         * a more specific dependency or the constructor you intend to be used has non-component parameters.
         *
         * @param key            the key by which to register the component; preferrably an interface class.
         * @param implementation the component class.
         * @param singleton      specifies whether the component should be singleton or not.
         * @param thread         specifies whether the component should be thread local.
         * @param deferred       specifies whether the component's instantiation should be deferred until the first
         *                       method call.
         * @param parameters     the non-empty list of parameters to use to instantiate the component, never
         *                       <code>null</code>.
         *
         * @see #component(Class)
         * @see #constant(Object)
         */
        <T> void bind(final Class<T> key,
                      final Class<? extends T> implementation,
                      final boolean singleton,
                      final boolean thread,
                      final boolean deferred,
                      final ConstructorParameter... parameters);

        /**
         * Binds a component class to its interface with forced constructor parameters. Use this when you want to supply
         * a more specific dependency or the constructor you intend to be used has non-component parameters.
         *
         * @param key            the key by which to register the component; preferrably an interface class.
         * @param implementation the component class.
         * @param singleton      specifies whether the component should be singleton or not.
         * @param thread         specifies whether the component should be thread local.
         * @param deferred       specifies whether the component's instantiation should be deferred until the first
         *                       method call.
         * @param factory        is an object that will produce instances of the given implementation class.
         *
         * @see #component(Class)
         * @see #constant(Object)
         */
        <T> void bind(final Class<T> key,
                      final Class<? extends T> implementation,
                      final boolean singleton,
                      final boolean thread,
                      final boolean deferred,
                      final ComponentFactory<T> factory);

        /**
         * Binds a component class to its interface with forced constructor parameters. Use this when you want to supply
         * a more specific dependency or the constructor you intend to be used has non-component parameters.
         *
         * @param key            the key by which to register the component; preferrably an interface class.
         * @param implementation the component class.
         * @param singleton      specifies whether the component should be singleton or not.
         * @param thread         specifies whether the component should be thread local.
         * @param deferred       specifies whether the component's instantiation should be deferred until the first
         *                       method call.
         * @param factory        is a class whose singleton instance that will produce instances of the given
         *                       implementation class.
         *
         * @see #component(Class)
         * @see #constant(Object)
         */
        <T> void bind(final Class<T> key,
                      final Class<? extends T> implementation,
                      final boolean singleton,
                      final boolean thread,
                      final boolean deferred,
                      final Class<? extends ComponentFactory<T>> factory);

        /**
         * Binds a component instance to its interface. In most case you should use the other registration methods that
         * accept a class rather then instantiating the component yourself. Use this method when you have no control
         * over the instantiation of a class, such as those created by third party tools, but you still want to make
         * them available for components in the container to depend on.
         *
         * @param key      the key by which to register the component; preferrably an interface class.
         * @param instance the component instance.
         */
        <T> void bind(final Class<? super T> key, final T instance);

        /**
         * Returns a new child registry whose container will use the container for this registry as its parent. See
         * <code>OpenComponentContainer.makeNestedContainer()</code>.
         *
         * @return an open container, never <code>null</code>.
         */
        OpenComponentContainer makeNestedContainer();

        /**
         * Returns a new child registry whose container will use the container for this registry as its parent.
         *
         * @param key            the key under which the child container will be accessible through its parent.
         * @param implementation the component whose dependencies the child container will help resolving.
         * @param parameters     the non-empty list of parameters to use to instantiate the component, never
         *                       <code>null</code>.
         *
         * @return an open container, never <code>null</code>.
         */
        <T> OpenComponentContainer makeNestedContainer(final Class<T> key,
                                                       final Class<? extends T> implementation,
                                                       final ConstructorParameter... parameters);

        /**
         * Returns a new child registry whose container will use the container for this registry as its parent.
         *
         * @param key            the key under which the child container will be accessible through its parent.
         * @param implementation the component whose dependencies the child container will help resolving.
         * @param singleton      specifies whether the component should be singleton or not.
         * @param thread         specifies whether the component should be thread local.
         * @param deferred       specifies whether the component's instantiation should be deferred until the first
         *                       method call.
         * @param parameters     the non-empty list of parameters to use to instantiate the component, never
         *                       <code>null</code>.
         *
         * @return an open container, never <code>null</code>.
         */
        <T> OpenComponentContainer makeNestedContainer(final Class<T> key,
                                                       final Class<? extends T> implementation,
                                                       final boolean singleton,
                                                       final boolean thread,
                                                       final boolean deferred,
                                                       final ConstructorParameter... parameters);

        /**
         * Returns a new child registry whose container will use the container for this registry as its parent.
         *
         * @param key            the key under which the child container will be accessible through its parent.
         * @param implementation the component whose dependencies the child container will help resolving.
         * @param singleton      specifies whether the component should be singleton or not.
         * @param thread         specifies whether the component should be thread local.
         * @param deferred       specifies whether the component's instantiation should be deferred until the first
         *                       method call.
         * @param factory        is an object that will produce instances of the given implementation class.
         *
         * @return an open container, never <code>null</code>.
         */
        <T> OpenComponentContainer makeNestedContainer(final Class<T> key,
                                                       final Class<? extends T> implementation,
                                                       final boolean singleton,
                                                       final boolean thread,
                                                       final boolean deferred,
                                                       final ComponentFactory<T> factory);

        /**
         * Returns a new child registry whose container will use the container for this registry as its parent.
         *
         * @param key            the key under which the child container will be accessible through its parent.
         * @param implementation the component whose dependencies the child container will help resolving.
         * @param singleton      specifies whether the component should be singleton or not.
         * @param thread         specifies whether the component should be thread local.
         * @param deferred       specifies whether the component's instantiation should be deferred until the first
         *                       method call.
         * @param factory        is an object that will produce instances of the given implementation class.
         *
         * @return an open container, never <code>null</code>.
         */
        <T> OpenComponentContainer makeNestedContainer(final Class<T> key,
                                                       final Class<? extends T> implementation,
                                                       final boolean singleton,
                                                       final boolean thread,
                                                       final boolean deferred,
                                                       final Class<? extends ComponentFactory<T>> factory);

        /**
         * Returns a component parameter that is linked to a component in the container that has been bound to the given
         * interface.
         *
         * @param key the key of the component to link to.
         *
         * @return a parameter object, never <code>null</code>.
         */
        ConstructorParameter component(final Class key);

        /**
         * Returns a component parameter that encapsulates a constant value.
         *
         * @param value the constant value to encapsulate.
         *
         * @return a parameter object, never <code>null</code>.
         */
        ConstructorParameter constant(final Object value);

        /**
         * Returns a component parameter that encapsulates a constant value.
         *
         * @param value the constant value to encapsulate.
         *
         * @return a parameter, never <code>null</code>.
         */
        ConstructorParameter constant(final char value);

        /**
         * Returns a component parameter that encapsulates a constant value.
         *
         * @param value the constant value to encapsulate.
         *
         * @return a parameter, never <code>null</code>.
         */
        ConstructorParameter constant(final byte value);

        /**
         * Returns a component parameter that encapsulates a constant value.
         *
         * @param value the constant value to encapsulate.
         *
         * @return a parameter, never <code>null</code>.
         */
        ConstructorParameter constant(final short value);

        /**
         * Returns a component parameter that encapsulates a constant value.
         *
         * @param value the constant value to encapsulate.
         *
         * @return a parameter, never <code>null</code>.
         */
        ConstructorParameter constant(final int value);

        /**
         * Returns a component parameter that encapsulates a constant value.
         *
         * @param value the constant value to encapsulate.
         *
         * @return a parameter, never <code>null</code>.
         */
        ConstructorParameter constant(final long value);

        /**
         * Returns a component parameter that encapsulates a constant value.
         *
         * @param value the constant value to encapsulate.
         *
         * @return a parameter, never <code>null</code>.
         */
        ConstructorParameter constant(final boolean value);

        /**
         * Returns a component parameter that encapsulates an array.
         *
         * @param componentClass is the component class for the array.
         *
         * @return a parameter, never <code>null</code>.
         */
        ConstructorParameter array(final Class componentClass);
    }

    /**
     * Poses as a factory for a particular component.
     */
    interface ComponentFactory<T> {

        /**
         * Creates a new instance of the component that this is a factory for.
         *
         * @param container is the container where the key that this factory should provide a component for expects its
         *                  dependencies to be.
         *
         * @return an object implementing <code>T</code>, never <code>null</code>.
         */
        T makeComponent(final ComponentContainer container);
    }
}
