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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.DependencyPath;
import org.fluidity.foundation.Strings;

/**
 * This is the external API of a fully populated dependency injection container. The root container is populated automatically by some bootstrap object based on
 * metadata produced by the <code>org.fluidity.maven:maven-composition-plugin</code> Maven plugin. Bootstrap classes are / must be created for the various
 * application containers that are / may be used, such as the JRE application launcher, a web application or an OSGi bundle.
 * <p/>
 * Any class loader that loads a bootstrap object will have its own container that will be either the root container or a direct or indirect child thereof. Thus
 * your application may have a hierarchy of containers that matches the application's class loader hierarchy.
 * <p/>
 * Containers in a hierarchy co-operate in such a way that if a component is not found in a child container, a look-up is performed in its parent.
 * <p/>
 * Components are instantiated by the container on demand and their dependencies are resolved by means of constructor and then field based dependency injection.
 * No setter injection or other means of dependency injection are supported. Components instantiated outside a container can still be field injected by the
 * container using its {@link ComponentContainer#initialize(Object)} method. Component instantiation may also be invoked internally for a component class not
 * in the container using the {@link ComponentContainer#instantiate(Class)} method.
 * <p/>
 * Components may be context aware, meaning that separate instances may be created for different contexts. Entire chains of dependencies, themselves not
 * necessarily context aware, may be instantiated multiple times for different contexts. This is not always what you expect so be aware of this effect when
 * working with context aware components. This is discussed in more detail in the <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide">User Guide</a>.
 * <p/>
 * Most of your components should never interact directly with this interface. Exceptions to this are management of objects created by third party tools or
 * components with dynamic dependencies, e.g., dependencies determined based on some run-time criteria.
 * <p/>
 * Each container has a registry, which allows components to be programmatically added to the container when it is booting. Except in rare circumstances, you
 * do not need to directly interact with a registry as the <code>org.fluidity.maven:maven-composition-plugin</code> Maven plugin does that for you. You should
 * not need to know about the registry unless you really have some fancy component binding logic. In those rare cases, the followings are provided to help you.
 * <p/>
 * The registry offers several ways to map an implementation to an interface in the host container. Which one you need depends on your requirements. These
 * methods are mostly invoked from the {@link org.fluidity.composition.spi.PackageBindings#bindComponents(ComponentContainer.Registry)} method of your binding
 * implementation.
 * <ul>
 * <li>To simply register a component implementation for its component interfaces, use {@link ComponentContainer.Registry#bindComponent(Class, Class[])}. This
 * is exactly what the Maven plugin uses for a @{@link Component} annotated class with no @{@link Component#automatic()} setting so if this method is all you
 * need then you should simply use the plugin instead of creating your own binding class.</li>
 * <li>To register an already instantiated component implementation for a component interface, use {@link ComponentContainer.Registry#bindInstance(Object,
 * Class[])}. If the implementation is annotated with @{@link Component} then its @{@link Component#automatic()} setting must be set to
 * <code>false</code>.</li>
 * <li>To register a component implementation when some or all of its dependencies are not accessible in the same container, use {@link
 * ComponentContainer.Registry#makeChildContainer(Class, Class[])} method and use the returned container's {@link OpenComponentContainer#getRegistry()} method
 * to gain access to the registry in which to bind the hidden dependencies. If the implementation is annotated with @{@link Component} then its @{@link
 * Component#automatic()} setting must be set to <code>false</code>.</li>
 * </ul>
 * <p/>
 * Containers can also be used to peek into the static dependency graph of your application. This functionality is provided by the {@link
 * ObservedComponentContainer} object returned by the {@link #observed(ComponentResolutionObserver)} method.
 *
 * @author Tibor Varga
 */
public interface ComponentContainer {

    /**
     * Returns a new container that calls the given observer whenever a dependency is resolved while resolving a component via the returned container.
     *
     * @param observer the observer to call, may be <code>null</code>.
     *
     * @return a new container instance backed by this one but using a possibly different resolution observer.
     */
    ObservedComponentContainer observed(ComponentResolutionObserver observer);

    /**
     * Returns a component by interface or (super)class. This method is provided for boundary objects (objects created outside the container by third party
     * tools) to acquire their dependencies. If there is no explicit binding to the provided class, no component will be returned.
     *
     * @param api a class object that was used to bind a component against; never <code>null</code>.
     *
     * @return the component bound against the give class or <code>null</code> when none was found.
     *
     * @throws ResolutionException when dependency resolution fails
     */
    <T> T getComponent(Class<T> api) throws ResolutionException;

    /**
     * Returns the list of components implementing the given interface, provided that they each, or the given interface itself, has been marked with the {@link
     * ComponentGroup} annotation.
     *
     * @param api the group interface class.
     *
     * @return an array of components that belong to the given group; may be <code>null</code>.
     */
    <T> T[] getComponentGroup(Class<T> api);

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
     * Creates another container whose components' dependencies will be satisfied from itself first, then from this container when the child could find no
     * component to satisfy a dependency with, and any dependency not found in this container or its parent will be attempted to be satisfied from the returned
     * child container.
     * <p/>
     * Use this with care as a domain container may cause its parent containers to return multiple instances of the same, supposedly singleton, component. This
     * is only safe if your application guarantees that the parent container is never used outside a domain container and that domain containers never talk to
     * one another. Hence the term "domain".
     *
     * @return a container that defaults to this container for satisfying component dependencies and which will also be used defaulted to by the ancestor
     *         components when they cannot resolve a dependency.
     */
    OpenComponentContainer makeDomainContainer();

    /**
     * Returns a component by interface or (super)class. This method is provided for factory creating transient components as a convenient
     * shortcut to acquire a child container, register component bindings in it and then get the child container instantiate the requested component.
     *
     * @param api      an interface or class that the provided bindings will register an implementation or extension for.
     * @param bindings invoked to add component bindings to the child container.
     *
     * @return the component bound against the give class or <code>null</code> when none was bound.
     *
     * @throws ResolutionException when dependency resolution fails
     */
    <T> T getComponent(Class<T> api, Bindings bindings) throws ResolutionException;

    /**
     * Post-initialize the @{@link Inject} annotated fields of the given object. You only need to use this method if the supplied component was instantiated
     * outside the container.
     *
     * @param component a component that needs field injection of dependencies.
     *
     * @return the supplied object.
     *
     * @throws ResolutionException when dependency resolution fails
     */
    <T> T initialize(T component) throws ResolutionException;

    /**
     * Invoke the given method of the given object. Parameters of the method will be dependency injected.
     *
     * @param component the method to invoke on.
     * @param method    is the method that needs its parameters injected.
     *
     * @return the result of the method invocation.
     *
     * @throws ResolutionException when dependency resolution fails
     */
    Object invoke(Object component, Method method) throws ResolutionException;

    /**
     * Instantiates the given class as a component, injecting its constructor and field dependencies in the process. No caching takes place, a new instance is
     * created at every invocation.
     *
     * @param componentClass is the component class to instantiate.
     *
     * @return the new component.
     *
     * @throws ResolutionException when dependency resolution fails
     */
    <T> T instantiate(Class<T> componentClass) throws ResolutionException;

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
        void bindComponents(ComponentContainer.Registry registry);
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
         * by the algorithm documented at {@link org.fluidity.composition.Components}.
         *
         * @param implementation the component class.
         * @param interfaces     optional list of interfaces that should resolve to the supplied component class.
         *
         * @throws BindingException when component registration fails
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
         * by the algorithm documented at {@link org.fluidity.composition.Components}.
         *
         * @param instance   the component instance.
         * @param interfaces optional list of interfaces that should resolve to the supplied component class.
         *
         * @throws BindingException when component registration fails
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
         * @throws BindingException when component registration fails
         */
        <T> OpenComponentContainer makeChildContainer(Class<T> implementation, Class<? super T>... interfaces) throws BindingException;
    }

    /**
     * Top level exception for exceptions related to the dependency injection container.
     */
    class ContainerException extends RuntimeException {

        public ContainerException(final Throwable cause, final String format, final Object... data) {
            super(String.format(format, data), cause);
        }

        public ContainerException(final String format, final Object... data) {
            super(String.format(format, data));
        }
    }

    /**
     * Reports errors when binding a component class or instance to its component interfaces.
     */
    class BindingException extends ContainerException {

        public BindingException(final String format, final Object... data) {
            super(format, data);
        }
    }

    /**
     * Reports error when resolving a component reference to a component class or instance.
     */
    class ResolutionException extends ContainerException {

        public ResolutionException(final Throwable e, final String format, final Object... data) {
            super(e, format, data);
        }

        public ResolutionException(final String format, final Object... data) {
            super(format, data);
        }
    }

    /**
     * Reports that some chain of dependencies is circular and there was no interface reference along the chain that could be used to break the circularity.
     */
    class CircularReferencesException extends ResolutionException {

        public CircularReferencesException(final Class<?> api, final String path) {
            super("Circular dependency detected while resolving %s: %s", Strings.arrayNotation(api), path);
        }
    }

    /**
     * Reports that some chain of dependencies is circular and that although there was at least one interface reference along the chain that could be used to
     * break the circularity, all such interface references were attempted to be dynamically resolved by the constructor of of the class owning the reference.
     */
    class CircularInvocationException extends ContainerException {

        private static List<String> methodNames(final Set<Method> methods) {
            final List<String> list = new ArrayList<String>();

            for (final Method method : methods) {
                final String name = method.toString();
                final String owner = method.getDeclaringClass().getName();

                list.add(name.substring(name.indexOf(owner) + owner.length() + 1));
            }

            return list;
        }

        public CircularInvocationException(final Object object, final Set<Method> methods) {
            super("Circular method invocation detected on %s@%x involving method(s) %s",
                  object.getClass().getName(),
                  System.identityHashCode(object),
                  methodNames(methods));
        }
    }

    /**
     * Reports an error that occurred while trying to instantiate a component class during dependency resolution.
     */
    @SuppressWarnings("UnusedDeclaration")
    class InstantiationException extends ResolutionException {

        private final DependencyPath path;

        public InstantiationException(final DependencyPath path, final Exception e) {
            super(e, path.toString(false));
            this.path = path;
        }

        /**
         * Returns the instantiation path that led to the error.
         *
         * @return the instantiation path that led to the error.
         */
        public DependencyPath getInstantiationPath() {
            return path;
        }
    }
}
