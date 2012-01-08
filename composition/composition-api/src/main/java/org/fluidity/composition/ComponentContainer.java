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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.DependencyPath;
import org.fluidity.foundation.Strings;

/**
 * The external API of a fully populated <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Concept">dependency injection</a>
 * <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Containers">container</a>. See <a href="TODO"></a> for a discussion on
 * the automatic container population.
 * <p/>
 * Containers in an application may form a hierarchy that matches the class loaders in the application. Containers in a hierarchy co-operate in such a way that
 * if a component is not found in a child container, a look-up is performed in its parent. The act of looking up a dependency by its referenced type is called
 * <em>dependency resolution</em>. Telling the container what class to resolve a component interface to is called <em>component binding</em> where a component
 * class is bound to the component interface. The act of a container using its parent to resolve missing dependencies is called <em>defaulting to</em> the
 * parent container.
 * <p/>
 * In case of a child container bound to a class loader or one returned by the {@link #makeChildContainer()} method, dependencies of a component resolved in a
 * parent container will be resolved in that parent container or its ancestors, never in the original child container. In a child container returned by the
 * {@link #makeDomainContainer()} method, however, transitive dependencies will also be resolved from the original child container and its ancestry. The latter
 * case allows segregating the application into dependency resolution domains as long as those domains do not overlap, meaning application code reaches out of
 * one domain and into another.
 * <p/>
 * Dependency resolution is performed based on the referenced type of the dependency. If there was no explicit binding for the given interface then no
 * component will be injected for that reference.
 * <p/>
 * Components are instantiated by the container on demand and their dependencies, defined by constructor parameters and {@link Inject @Inject} annotated
 * fields, are resolved in this container or its parent. No setter injection or other means of dependency injection are supported.
 * <p/>
 * Components instantiated outside a container can still be field injected by the container using its {@link ComponentContainer#initialize(Object)} method.
 * Component instantiation may also be invoked for a component class not in the container using the {@link ComponentContainer#instantiate(Class)} method.
 * <p/>
 * Components may be context aware, meaning that separate instances may be created for different contexts. Entire chains of dependencies, themselves not
 * necessarily context aware, may be instantiated multiple times for different contexts. This is not always what you expect so be aware of this effect when
 * working with context aware components. This is discussed in more detail in the
 * <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Component_Context">User Guide</a>.
 * <p/>
 * Most of your components should never interact directly with this interface. Exceptions to this are management of objects created by third party tools or
 * components with dynamic dependencies, e.g., dependencies determined based on some run-time criteria.
 * <p/>
 * Containers can also be used to peek into the static dependency graph of your application. This functionality is provided by the {@link
 * ObservedComponentContainer} object returned by the {@link #observed(ComponentResolutionObserver)} method.
 * <h3>Usage</h3>
 * All examples below assume the following component boilerplate:
 * <pre>
 * {@linkplain Component @Component}
 * final class MyComponent {
 *
 *   private final <span class="hl1">ComponentContainer</span> container;
 *
 *   public MyComponent(final <span class="hl1">ComponentContainer</span> container) {
 *     this.container = container;
 *     ...
 *   }
 *
 *   private void myMethod() throws throws <b>MyCheckedException</b> {
 *     ... example code snippet from below ...
 *   }
 * }
 * </pre>
 * <h4>Local Instantiation</h4>
 * A container can be used to quickly instantiate some helper class without polluting the host component with the dependencies of its helpers.
 * <ul>
 * <li>If the helper has <b>no</b> dependencies specific to this particular use:
 * <pre>
 * final <span class="hl2">MyHelper</span> helper = container.<span class="hl1">instantiate(</span><span class="hl2">MyHelper</span>.class<span class="hl1">)</span>;
 * </pre></li>
 * <li>If the helper <b>does</b> have dependencies specific to this particular use:
 * <pre>
 * final <span class="hl2">MyHelper</span> helper = container.<span class="hl1">instantiate(</span><span class="hl2">MyHelper</span>.class, new {@linkplain ComponentContainer.Bindings}() {
 *   public void bindComponents({@linkplain ComponentContainer.Registry} registry) {
 *     registry.bindInstance("local context", String.class);
 *     ...
 *   }
 * }<span class="hl1">)</span>;
 * </pre></li>
 * <li>If <b>multiple</b> helpers share dependencies specific to this particular use, then
 * <pre>
 * &#47;* ... acquire a child container with the necessary bindings: *&#47;
 *
 * final <span class="hl1">ComponentContainer</span> child = container.<span class="hl1">makeChildContainer(</span>new {@linkplain ComponentContainer.Bindings}() {
 *   public void bindComponents({@linkplain ComponentContainer.Registry} registry) {
 *     registry.bindInstance("local context", String.class);
 *     ...
 *   }
 * }<span class="hl1">)</span>;
 *
 * &#47;* ... and get your helpers instantiated: *&#47;
 *
 * final <span class="hl2">MyHelper1</span> helper1 = child.<span class="hl1">instantiate(</span><span class="hl2">MyHelper1</span>.class<span class="hl1">)</span>;
 * final <span class="hl2">MyHelper2</span> helper2 = child.<span class="hl1">instantiate(</span><span class="hl2">MyHelper2</span>.class<span class="hl1">)</span>;
 * </pre></li>
 * <li>If <b>multiple</b> helpers share dependencies specific to this particular use and they also have local dependencies that should <b>not</b> be shared,
 * then
 * <pre>
 * &#47;* ... acquire a child container with the necessary bindings: *&#47;
 *
 * final <span class="hl1">ComponentContainer</span> child = container.<span class="hl1">makeChildContainer(</span>new {@linkplain ComponentContainer.Bindings}() {
 *   public void bindComponents({@linkplain ComponentContainer.Registry} registry) {
 *     registry.bindInstance("local context", String.class);
 *     ...
 *   }
 * }<span class="hl1">)</span>;
 *
 * &#47;* ... and get your helpers instantiated with the bindings not shared: *&#47;
 *
 * final <span class="hl2">MyHelper1</span> helper1 = child.<span class="hl1">instantiate(</span><span class="hl2">MyHelper1</span>.class, new {@linkplain ComponentContainer.Bindings}() {
 *   public void bindComponents({@linkplain ComponentContainer.Registry} registry) {
 *     registry.bindInstance(1234, Integer.TYPE);
 *     ...
 *   }
 * }<span class="hl1">)</span>;
 *
 * final <span class="hl2">MyHelper2</span> helper2 = child.<span class="hl1">instantiate(</span><span class="hl2">MyHelper2</span>.class, new {@linkplain ComponentContainer.Bindings}() {
 *   public void bindComponents({@linkplain ComponentContainer.Registry} registry) {
 *     registry.bindInstance(5678, Integer.TYPE);
 *     ...
 *   }
 * }<span class="hl1">)</span>;
 * </pre></li>
 * </ul>
 * <h4>Method Parameter Injection</h4>
 * A container can be used to call methods on a component and inject missing method parameters. There are three variants to this:
 * <ul>
 * <li><b>automatic parameter injection</b>: automatically injects all parameters of all component interface methods that have {@link Inject @Inject} annotated
 * parameters with no argument supplied in the original method call. The component's interfaces must be actual Java interfaces.
 * <pre>
 * <span class="hl1">{@linkplain Component @Component}(automatic = false)</span>
 * final class MyHelper implements <span class="hl2">InjectableMethods</span> {
 *   ...
 *   int <span class="hl2">someMethod</span>(final <span class="hl3">int given</span>, final <span class="hl1">{@linkplain Inject @Inject}</span> MyDependency1 mandatory, <span class="hl1">{@linkplain Inject @Inject}</span> {@linkplain Optional @Optional} MyDependency2 optional)
 *     throws SomeCheckedException;
 *   ...
 * }
 *
 * final <span class="hl2">InjectableMethods</span> helper = container.<span class="hl1">complete(</span>new MyHelper()</span class="hl1">)</span>;
 *
 * &#47;* ... pass null for unknown parameters *&#47;
 * final int result = helper.<span class="hl2">someMethod</span>(<span class="hl3">1234</span>, null, null);
 * </pre></li>
 * <li><b>ad-hoc method invocation</b>: any method parameter not provided a value will be injected; if a method parameter cannot be
 * resolved and it is not annotated with {@link Optional @Optional} then an exception is thrown.
 * <pre>
 * final class <span class="hl2">MyHelper</span> {
 *   ...
 *   <span class="hl2">int</span> <span class="hl2">someMethod</span>(final <span class="hl3">int given</span>, final MyDependency1 mandatory, {@linkplain Optional @Optional} MyDependency2 optional);
 *     throws SomeCheckedException;
 *   ...
 * }
 *
 * final <span class="hl2">MyHelper</span> helper = new <span class="hl2">MyHelper()</span>;
 * final Method method = <span class="hl2">MyHelper</span>.class.getMethod("<span class="hl2">someMethod</span>", Integer.TYPE, MyDependency1.class, MyDependency2.class);
 *
 * final <span class="hl2">int</span> result;
 * try {
 *
 *   &#47;* ... handle checked exceptions ... *&#47;
 *   result = {@linkplain org.fluidity.foundation.Exceptions}.wrap(new Exceptions.Command&lt;<span class="hl2">Integer</span>>() {
 *     public <span class="hl2">Integer</span> run() throws Throwable {
 *
 *       &#47;* ... pass known parameters as the last method parameters *&#47;
 *       return (<span class="hl2">Integer</span>) container.<span class="hl1">invoke</span>(helper, method, <span class="hl3">1234</span>);
 *     }
 *   });
 * } catch (final {@linkplain org.fluidity.foundation.Exceptions.Wrapper} e) {
 *   throw e.rethrow(<b>MyCheckedException</b>.class);
 * }
 *
 * </pre></li>
 * </ul>
 * <h4>Observing Dependency Resolution</h4>
 * A container can be used to explore the static and dynamic dependencies starting at any component interface. See {@link ObservedComponentContainer} for an
 * example.
 *
 * @author Tibor Varga
 */
public interface ComponentContainer {

    /**
     * Returns a new container that calls the given observer whenever a dependency is resolved while resolving a component interface via the returned container.
     *
     * @param observer the observer to call, may be <code>null</code>.
     *
     * @return a new container instance backed by this one and using the provided resolution observer.
     */
    ObservedComponentContainer observed(ComponentResolutionObserver observer);

    /**
     * Returns a component by interface or (super)class. This method is provided for boundary objects (objects created outside the container by third party
     * tools) to acquire their dependencies. If there was no explicit binding to the provided class, no component is returned.
     *
     * @param api a class object that was used to bind a component to; never <code>null</code>.
     *
     * @return the component bound to the give class or <code>null</code> when none was found.
     *
     * @throws ResolutionException when dependency resolution fails.
     */
    <T> T getComponent(Class<T> api) throws ResolutionException;

    /**
     * Returns the list of components implementing the given interface, provided that they each, or the given interface itself, has been marked with the {@link
     * ComponentGroup @ComponentGroup} annotation.
     *
     * @param api the group interface class.
     *
     * @return an array of components that belong to the given group; may be <code>null</code>.
     */
    <T> T[] getComponentGroup(Class<T> api);

    /**
     * Finds by its component interface and instantiates a transient component after invoking the bindings implemented by the supplied
     * {@link ComponentContainer.Bindings} object. This method is a convenient shortcut to {@linkplain #makeChildContainer() acquire a child container},
     * {@linkplain ComponentContainer.Bindings#bindComponents(ComponentContainer.Registry) register component bindings} in it, and then get the child container
     * to {@linkplain #getComponent(Class) find and instantiate} the requested component.
     *
     * @param api      an interface or class that the provided bindings will register an implementation or extension for, along with its dependencies missing
     *                 from this container or its parent or intended to be overridden.
     * @param bindings invoked to add component bindings to the child container.
     *
     * @return the component bound to the given class or <code>null</code> if none was bound.
     *
     * @throws ResolutionException when dependency resolution fails.
     */
    <T> T getComponent(Class<T> api, ComponentContainer.Bindings bindings) throws ResolutionException;

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
     * Creates another container whose components' dependencies will be satisfied from itself first and then from this container when the child could find no
     * component to satisfy a dependency with.
     * <p/>
     * This method can be used to gain access to the dependency resolution and injection functionality of the container without polluting it with new
     * components after it has been set up. Components placed in the child container will not be visible to clients of, or components in, this container.
     *
     * @param bindings invoked to add component bindings to the child container.
     *
     * @return a container that defaults to this container for satisfying component dependencies.
     */
    ComponentContainer makeChildContainer(Bindings bindings);

    /**
     * Creates another container whose components' dependencies will be satisfied from itself first, then from this container when the child could find no
     * component to satisfy a dependency with, and any dependency not found in this container or its parent will be attempted to be satisfied from the returned
     * child container.
     * <p/>
     * Use this with care as a domain container may cause its parent containers to return multiple instances of the same, supposedly singleton, component. This
     * is only safe if your application guarantees that the parent container is never used outside a domain container and that domain containers and the
     * components they instantiate never talk to other domain containers or components they instantiated. Hence the term "domain".
     *
     * @return a container that defaults to this container for satisfying component dependencies and which will also be used defaulted to by the ancestor
     *         components when they cannot resolve a dependency.
     */
    OpenComponentContainer makeDomainContainer();

    /**
     * Resolves and injects the {@link Inject @Inject} annotated fields of the given object. You only need to use this method if the supplied component was
     * instantiated outside the container.
     *
     * @param component a component that needs field injection of dependencies.
     *
     * @return the supplied object.
     *
     * @throws ResolutionException when dependency resolution fails.
     */
    <T> T initialize(T component) throws ResolutionException;

    /**
     * Instantiates the given class as a component, injecting its constructor and field dependencies in the process from the receiving container. No caching
     * takes place, a new instance is created at every invocation.
     *
     * @param componentClass the component class to instantiate.
     *
     * @return the new component.
     *
     * @throws ResolutionException when dependency resolution fails.
     */
    <T> T instantiate(Class<T> componentClass) throws ResolutionException;

    /**
     * Instantiates the given class as a component, injecting its constructor and field dependencies in the process from the given bindings and the receiving
     * container. No caching takes place, a new instance is created at every invocation.
     *
     * @param componentClass the component class to instantiate.
     * @param bindings       the local component bindings.
     *
     * @return the new component.
     *
     * @throws ResolutionException when dependency resolution fails.
     */
    <T> T instantiate(Class<T> componentClass, ComponentContainer.Bindings bindings) throws ResolutionException;

    /**
     * Invokes the given method of the given object after resolving and injecting its applicable parameters that the given argument list contains no
     * (or <code>null</code>) value for.
     *
     * @param component the method to invoke on the provided object.
     * @param method    the method that needs its parameters injected.
     * @param arguments the method parameters matching the method's signature with <code>null</code> values where injection is needed.
     *
     * @return the result of the method invocation.
     *
     * @throws ResolutionException when dependency resolution fails.
     * @throws InvocationTargetException when the supplied method throws an exception.
     */
    Object invoke(Object component, Method method, Object... arguments) throws ResolutionException, InvocationTargetException;

    /**
     * Wraps the given component in a proxy that inject all missing {@link Inject @Inject} annotated parameters of any method of all, or the given, component
     * interfaces of the supplied component.
     *
     * @param component a component that needs field injection of dependencies.
     * @param api       optional list of component interfaces to expose by the returned object.
     *
     * @return a new component that implements all, or the given, component interfaces and injects missing method parameters from this container.
     *
     * @throws ResolutionException when dependency resolution fails.
     */
    @SuppressWarnings("JavadocReference")
    <T> T complete(T component, Class<? super T>... api) throws ResolutionException;

    /**
     * Top level exception for errors related to the dependency injection container.
     */
    class ContainerException extends RuntimeException {

        /**
         * Creates a new instance using the given formatted text and with the given cause.
         *
         * @param cause  the exception that triggered this error.
         * @param format the Java format specification.
         * @param data   the details to format.
         */
        public ContainerException(final Throwable cause, final String format, final Object... data) {
            super(String.format(format, data), cause);
        }

        /**
         * Creates a new instance using the given formatted text.
         *
         * @param format the Java format specification.
         * @param data   the details to format.
         */
        public ContainerException(final String format, final Object... data) {
            super(String.format(format, data));
        }
    }

    /**
     * Reports an error that occurred when resolving a component reference to a component.
     */
    class ResolutionException extends ContainerException {

        /**
         * Creates a new instance using the given formatted text and with the cause.
         *
         * @param cause  the exception that triggered this error.
         * @param format the Java format specification.
         * @param data   the details to format.
         */
        public ResolutionException(final Throwable cause, final String format, final Object... data) {
            super(cause, format, data);
        }

        /**
         * Creates a new instance using the given formatted text.
         *
         * @param format the Java format specification.
         * @param data   the details to format.
         */
        public ResolutionException(final String format, final Object... data) {
            super(format, data);
        }
    }

    /**
     * Reports that some chain of dependencies is circular and there was no interface reference along the chain that could be used to break the circularity.
     */
    class CircularReferencesException extends ResolutionException {

        /**
         * Creates a new instance for the given component interface resolved with the given instantiation path.
         *
         * @param api  the component interface that could not be resolved.
         * @param path the instantiation path that led to this error.
         */
        public CircularReferencesException(final Class<?> api, final String path) {
            super("Circular dependency detected while resolving %s: %s", Strings.arrayNotation(true, api), path);
        }
    }

    /**
     * Reports that some chain of dependencies is circular and that although there was at least one interface reference along the chain that could be used to
     * break the circularity, all such interface references were attempted to be dynamically resolved by the constructor of the class owning the reference.
     */
    class CircularInvocationException extends ResolutionException {

        private static List<String> methodNames(final Set<Method> methods) {
            final List<String> list = new ArrayList<String>();

            for (final Method method : methods) {
                final String name = method.toString();
                final String owner = method.getDeclaringClass().getName();

                list.add(name.substring(name.indexOf(owner) + owner.length() + 1));
            }

            return list;
        }

        /**
         * Creates a new instance for the given object.
         *
         * @param object  the object some method of which could not be invoked.
         * @param methods the list of method invocations that led to this error.
         */
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
    class InstantiationException extends ResolutionException {

        private final DependencyPath path;

        /**
         * Creates a new instance for the given instantiation path and cause error.
         *
         * @param path  the instantiation path that led to this error.
         * @param cause the error that occurred.
         */
        public InstantiationException(final DependencyPath path, final Exception cause) {
            super(cause, path.toString(false));
            this.path = path;
        }

        /**
         * Returns the instantiation path that led to the error.
         *
         * @return the instantiation path that led to the error.
         */
        public DependencyPath path() {
            return path;
        }
    }

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
        void bindComponents(ComponentContainer.Registry registry);
    }

    /**
     * Allows registration of <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Composition">components</a> into a
     * <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Concept">dependency injection</a>
     * <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Containers">container</a>.
     * <p/>
     * This interface is mainly used by {@link org.fluidity.composition.spi.PackageBindings} objects that are invoked when the host application populates its
     * dependency injection containers.
     * <p/>
     * Outside of <code>PackageBindings</code>, an object implementing this interface for an {@linkplain OpenComponentContainer uninitialized container} can be
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
         * @throws ComponentContainer.BindingException
         *          when component registration fails.
         */
        @SuppressWarnings("JavadocReference")
        <T> void bindComponent(Class<T> implementation, Class<? super T>... interfaces) throws ComponentContainer.BindingException;

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
         * @throws ComponentContainer.BindingException
         *          when component registration fails.
         */
        @SuppressWarnings("JavadocReference")
        <T> void bindInstance(T instance, Class<? super T>... interfaces) throws ComponentContainer.BindingException;

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
         * @throws ComponentContainer.BindingException
         *          when component registration fails.
         */
        <T> OpenComponentContainer makeChildContainer(Class<T> implementation, Class<? super T>... interfaces) throws ComponentContainer.BindingException;
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
