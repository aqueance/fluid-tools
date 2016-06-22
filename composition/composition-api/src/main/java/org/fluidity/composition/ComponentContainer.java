/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.ComponentInterceptor;
import org.fluidity.foundation.Strings;

/**
 * The external API of a fully populated dependency injection container.
 * <p>
 * The static, automatically populated, containers in an application form a hierarchy that matches part of the class loader hierarchy in the application, while
 * dynamically created, and manually populated, containers attach to a static container in that hierarchy. Containers in a hierarchy co-operate in such a way
 * that if a component is not found in a child container, a look-up is performed in its parent. The act of looking up a dependency by its referenced type is
 * called <em>dependency resolution</em>. Telling the container what class to resolve a component interface to is called <em>component binding</em> where a
 * component class is bound to the component interface. The act of a container using its parent to resolve missing dependencies is called <em>defaulting</em>
 * to the parent container.
 * <p>
 * In case of a child container bound to a class loader or one returned by the {@link #makeChildContainer(Bindings...) makeChildContainer()} method,
 * dependencies of a component resolved in a parent container will be resolved in that parent container or its ancestors, never in the original child
 * container. In a child container returned by the {@link #makeDomainContainer(Bindings...) makeDomainContainer()} method, however, transitive dependencies
 * will also be resolved from the original child container and its ancestry. The latter case allows segregating the application into dependency resolution
 * domains as long as those domains do not overlap, meaning application code reaching out of one domain and into another.
 * <p>
 * Dependency resolution is performed based on the referenced type of the dependency. If there is no binding for the given interface then no component will be
 * injected for that reference.
 * <p>
 * Components are instantiated by the container on demand and their dependencies, defined by constructor parameters and {@link Inject @Inject} annotated
 * fields, are resolved in this container or its parent. No setter injection or other means of dependency injection are supported.
 * <p>
 * Components instantiated outside a container can still be field injected by the container using its {@link #initialize(Object) initialize()} method.
 * Component instantiation may also be invoked for a component class not in the container using the {@link #instantiate(Class, ComponentContainer.Bindings...)
 * instantiate()} method.
 * <p>
 * Components may be context dependent, meaning that distinct instances may be created for different contexts. Entire chains of dependencies, with components
 * themselves not necessarily context dependent, may be instantiated multiple times for different contexts. Although semantically correct, this may not be what
 * you expect so be aware of this effect when working with context dependent components. This is discussed in more detail in the <a
 * href="https://github.com/aqueance/fluid-tools/wiki/User-Guide---Composition#working-with-component-contexts">User Guide</a>.
 * <p>
 * Containers can also be used to peek into the static dependency graph of your application. This functionality is provided by the {@link
 * ObservedContainer} object returned by the {@link #observed(Observer) observed()} method.
 * <h3>Usage</h3>
 * All examples below assume the following enclosing boilerplate:
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
 *   private void myMethod() throws <b>SomeCheckedException</b> {
 *     <i>&hellip; example code snippet from below &hellip;</i>
 *   }
 * }
 * </pre>
 * <h4>Local Instantiation</h4>
 * A container can be used to quickly instantiate some helper class without polluting the host component with the dependencies of its helpers.
 * <ul>
 * <li>If the helper has <b>no</b> dependencies specific to this particular use:
 * <pre>
 * final <span class="hl2">MyHelper</span> helper = container.<span class="hl1">instantiate</span>(<span class="hl2">MyHelper</span>.class);
 * </pre></li>
 * <li>If the helper <b>does</b> have dependencies specific to this particular use:
 * <pre>
 * final <span class="hl2">MyHelper</span> helper = container.<span class="hl1">instantiate</span>(<span class="hl2">MyHelper</span>.class, new {@linkplain ComponentContainer.Bindings}() {
 *   public void bindComponents({@linkplain ComponentContainer.Registry} registry) {
 *     registry.bindInstance("local context", String.class);
 *     &hellip;
 *   }
 * });
 * </pre></li>
 * <li>If <b>multiple</b> helpers share dependencies specific to this particular use:
 * <pre>
 * &#47;* &hellip; acquire a child container with the necessary bindings: *&#47;
 *
 * final <span class="hl1">ComponentContainer</span> child = container.<span class="hl1">makeChildContainer</span>(new {@linkplain ComponentContainer.Bindings}() {
 *   public void bindComponents({@linkplain ComponentContainer.Registry} registry) {
 *     registry.bindInstance("local context", String.class);
 *     &hellip;
 *   }
 * });
 *
 * &#47;* &hellip; and get your helpers instantiated: *&#47;
 *
 * final <span class="hl2">MyHelper1</span> helper1 = child.<span class="hl1">instantiate</span>(<span class="hl2">MyHelper1</span>.class);
 * final <span class="hl2">MyHelper2</span> helper2 = child.<span class="hl1">instantiate</span>(<span class="hl2">MyHelper2</span>.class);
 * </pre></li>
 * <li>If <b>multiple</b> helpers share dependencies specific to this particular use and they also have local dependencies that should <b>not</b> be shared:
 * <pre>
 * // acquire a child container with the shared bindings:
 *
 * final <span class="hl1">ComponentContainer</span> child = container.<span class="hl1">makeChildContainer</span>(new {@linkplain ComponentContainer.Bindings}() {
 *   public void bindComponents({@linkplain ComponentContainer.Registry} registry) {
 *     registry.bindInstance("local context", String.class);
 *     &hellip;
 *   }
 * };
 *
 * // and get your helpers instantiated with the bindings not shared:
 *
 * final <span class="hl2">MyHelper1</span> helper1 = child.<span class="hl1">instantiate</span>(<span class="hl2">MyHelper1</span>.class, new {@linkplain ComponentContainer.Bindings}() {
 *   public void bindComponents({@linkplain ComponentContainer.Registry} registry) {
 *     registry.bindInstance(1234, int.class);
 *     &hellip;
 *   }
 * });
 *
 * final <span class="hl2">MyHelper2</span> helper2 = child.<span class="hl1">instantiate</span>(<span class="hl2">MyHelper2</span>.class, new {@linkplain ComponentContainer.Bindings}() {
 *   public void bindComponents({@linkplain ComponentContainer.Registry} registry) {
 *     registry.bindInstance(5678, int.class);
 *     &hellip;
 *   }
 * };
 * </pre></li>
 * </ul>
 * <h4>Method Parameter Injection</h4>
 * A container can be used to call methods on a component and inject missing method parameters. There are three variants to this:
 * <ul>
 * <li><b>automatic parameter injection</b>: automatically injects all parameters of all component interface methods that have {@link Inject @Inject} annotated
 * parameters with no argument supplied in the original method call. The component's interfaces must be actual Java interfaces.
 * <pre>
 * public interface <span class="hl2">InjectableMethods</span> {
 *
 *   int <span class="hl2">someMethod</span>(<span class="hl3">int given</span>, <span class="hl1">{@linkplain Inject @Inject}</span> MyDependency1 mandatory, <span class="hl1">{@linkplain Inject @Inject}</span> {@linkplain Optional @Optional} MyDependency2 optional)
 *     throws SomeCheckedException;
 * }
 * </pre>
 * <pre>
 * <span class="hl1">{@linkplain Component @Component}(automatic = false)</span>
 * final class MyHelper implements <span class="hl2">InjectableMethods</span> {
 *   &hellip;
 * }
 * </pre>
 * <pre>
 * final <span class="hl2">InjectableMethods</span> helper = container.<span class="hl1">complete</span>(container.instantiate(MyHelper.class));
 *
 * // pass null for parameters to automatically inject
 * final int result = helper.<span class="hl2">someMethod</span>(<span class="hl3">1234</span>, null, null);
 * </pre></li>
 * <li><b>ad-hoc method invocation</b>: any method parameter not provided a value will be injected; if a method parameter cannot be
 * resolved and it is not annotated with {@link Optional @Optional} then a {@link ResolutionException} is thrown.
 * <pre>
 * final class <span class="hl2">MyHelper</span> {
 *
 *   <span class="hl2">int</span> <span class="hl2">someMethod</span>(final <span class="hl3">int given</span>, final MyDependency1 mandatory, {@linkplain Optional @Optional} MyDependency2 optional);
 *     throws SomeCheckedException {
 *     &hellip;
 *     }
 * }
 * </pre>
 * <pre>
 * final <span class="hl2">MyHelper</span> helper = new <span class="hl2">MyHelper()</span>;
 *
 * // make sure our assumptions implicit in the reflection code below
 * // have not been broken by changes to <span class="hl2">MyHelper</span>
 * if (false) {
 *   final Integer result = helper.someMethod((Integer) null, (MyDependency1) null, (MyDependency1) null);
 * }
 *
 * final Method method = <span class="hl2">MyHelper</span>.class.getMethod("<span class="hl2">someMethod</span>",
 *                                                int.class,
 *                                                MyDependency1.class,
 *                                                MyDependency2.class);
 *
 * final <span class="hl2">int</span> result;
 * try {
 *
 *   // handle checked exceptions ...
 *   result = {@linkplain org.fluidity.foundation.Exceptions}.wrap(
 *
 *       // pass known parameters as the last parameters to this call
 *       () -&gt; (<span class="hl2">Integer</span>) container.<span class="hl1">invoke</span>(helper, method, <span class="hl3">1234</span>);
 *   );
 * } catch (final {@linkplain org.fluidity.foundation.Exceptions.Wrapper} e) {
 *   throw e.{@linkplain org.fluidity.foundation.Exceptions.Wrapper#rethrow(Class) rethrow}(<b>SomeCheckedException</b>.class);
 * }
 *
 * </pre></li>
 * </ul>
 * <h4>Observing Dependency Resolution</h4>
 * A container can be used to explore the static and dynamic dependencies starting at any component interface. See {@link ObservedContainer} for an example.
 *
 * @author Tibor Varga
 */
public interface ComponentContainer {

    /**
     * Creates a child container with this one as its parent. Dependencies of components resolved, through the returned container, in this container or its
     * ancestry will be resolved in the <em>same container or its ancestry</em>, never in the child.
     * <p>
     * This method can be used to bind components to a dependency injection container without polluting any shared one with new bindings after it has been set
     * up. The bindings supplied and added to the returned container will <b>not</b> be visible to clients of, or components in, this container.
     *
     * @param bindings list of component bindings to add to the child container.
     *
     * @return a container that defaults to this container for satisfying component dependencies.
     */
    OpenContainer makeChildContainer(Bindings... bindings);

    /**
     * Creates a domain container with this one as its parent. Dependencies of components found, through the returned c, in this container or its ancestry
     * will be resolved <em>in the returned child container</em>.
     * <p>
     * Use this with care as a domain container will cause singleton components to be singletons within the represented domain. This is only safe if your
     * application guarantees that the this container is never used directly but only as a parent of the returned domain container, and that domain containers
     * and the components they instantiate never talk to other domain containers or components they instantiated.
     *
     * @param bindings list of component bindings to add to the child container.
     *
     * @return a container that defaults to this container for satisfying component dependencies and which will also be used defaulted to by the ancestor
     *         components when they cannot resolve a dependency.
     */
    OpenContainer makeDomainContainer(Bindings... bindings);

    /**
     * Returns a child container with this one as its parent and with the given component interceptors active, in addition to those found in the class loader
     * ancestry of this container.
     *
     * @param interceptors the component interceptors to activate in the container.
     *
     * @return a new child container.
     */
    ComponentContainer intercepting(ComponentInterceptor... interceptors);

    /**
     * Returns a new container that calls the given observer whenever a dependency is resolved while resolving a component interface via the returned container.
     *
     * @param observer the observer to call, may be <code>null</code>.
     *
     * @return a new container instance backed by this one and using the provided resolution observer.
     */
    ObservedContainer observed(Observer observer);

    /**
     * Resolves and injects the {@link Inject @Inject} annotated fields of the given object. You only need to use this method if the
     * supplied component was instantiated outside the container. Only non-<code>final</code> <code>null</code> valued fields get injected. The fields can be
     * defined anywhere in the object's class hierarchy.
     *
     * @param component a component that needs field injection of dependencies.
     * @param <T>       the type of component to initialize.
     *
     * @return the supplied object.
     *
     * @throws ComponentContainer.ResolutionException when dependency resolution fails.
     */
    <T> T initialize(T component) throws ResolutionException;

    /**
     * This method creates a {@link #makeChildContainer(ComponentContainer.Bindings...) child container},
     * {@link ComponentContainer.Registry#bindComponent(Class, Class[]) binds} the given class and the given <code>bindings</code>, and then
     * {@link OpenContainer#getComponent(Class) asks} for an instance of the given class from the child container. No caching takes place with this last step,
     * a new component instance is created at every invocation of this method.
     * <p>
     * If the given class object represents a concrete class, this method instantiates that class as a component, injecting in the process its constructor and
     * field dependencies from the given <code>bindings</code> and the receiving container.
     * <p>
     * If the given class is an interface, this method binds, in addition to the given <code>bindings</code>, every component class whose
     * {@link Component#scope() scope} is set to the given class <em>including</em> the implementation of the given interface itself, and then performs the
     * foregoing.
     *
     * @param componentClass the component class to instantiate.
     * @param bindings       the local component bindings.
     * @param <T>            the type of the component to instantiate.
     *
     * @return the new component.
     *
     * @throws ComponentContainer.ResolutionException when dependency resolution fails.
     */
    <T> T instantiate(Class<T> componentClass, Bindings... bindings) throws ResolutionException;

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
     * @throws ComponentContainer.ResolutionException when dependency resolution fails.
     * @throws InvocationTargetException when the supplied method throws an exception.
     */
    Object invoke(Object component, Method method, Object... arguments) throws ResolutionException, InvocationTargetException;

    /**
     * Wraps the given component in a proxy that injects all missing {@link Inject @Inject} annotated parameters of any method of all, or the given, component
     * interfaces of the supplied component.
     *
     * @param component a component that needs field injection of dependencies.
     * @param api       optional list of component interfaces to expose by the returned object.
     * @param <T>       the type of the component to wrap.
     *
     * @return a new component that implements all, or the given, component interfaces and injects missing method parameters from this container.
     *
     * @throws ComponentContainer.ResolutionException when dependency resolution fails.
     */
    @SuppressWarnings("unchecked")
    <T> T complete(T component, Class<? super T>... api) throws ResolutionException;

    /**
     * Embodies a list of mappings from component class to component interface and / or component group. {@link OpenContainer ComponentContainers} use bindings
     * to map component and component group interfaces to component classes when resolving dependencies.
     * <p>
     * Bindings are mostly used when automatically populating an application's dependency injection container hierarchy. At build time, the
     * <code>org.fluidity.maven:composition-maven-plugin</code> produces bindings to capture the list of {@link Component @Component} annotated classes in a
     * module as component bindings, while at run time Fluid Tools finds these bindings and loads them before the first query is made to a dependency injection
     * container.
     * <h3>Usage</h3>
     * Programmatic use of bindings allows working with components not automatically bound by design. For example, some library may want to capture the
     * instantiation of some component that depends on information at the site of its use rather than, or in addition to, application-wide state.
     * <pre>
     * {@linkplain Component @Component}
     * public final class MyComponentFactory {
     *
     *   public <span class="hl2">MyComponent</span> create(final {@linkplain OpenContainer} container, final <span class="hl3">MyLocalDependency</span> dependency) {
     *     return container.instantiate(<span class="hl2">MyComponentImpl</span>.class, new <span class="hl1">ComponentContainer.Bindings</span>() {
     *       public void <span class="hl1">bindComponents</span>(final {@linkplain ComponentContainer.Registry} registry) {
     *         registry.bindInstance(dependency, <span class="hl3">MyLocalDependency</span>.class);
     *       }
     *     });
     *   }
     * }
     * </pre>
     * <pre>
     * {@linkplain Component @Component}<span class="hl1">(automatic = false)</span>
     * final class <span class="hl2">MyComponentImpl</span> implements <span class="hl2">MyComponent</span> {
     *
     *   <span class="hl2">MyComponentImpl</span>(final <span class="hl3">MyLocalDependency</span> local, final MyGlobalDependency global) {
     *     &hellip;
     *   }
     *
     *   &hellip;
     * }
     * </pre>
     */
    @FunctionalInterface
    interface Bindings {

        /**
         * Add the component bindings to the given registry.
         *
         * @param registry is an object to register component bindings with.
         */
        void bindComponents(ComponentContainer.Registry registry);
    }

    /**
     * Allows adding component {@linkplain ComponentContainer.Bindings bindings} to a {@linkplain OpenContainer dependency injection container}.
     * <p>
     * The registry offers several ways to map an implementation to an interface in the host container. Which one you need depends on your requirements. These
     * methods are mostly invoked from the {@link ComponentContainer.Bindings#bindComponents(ComponentContainer.Registry) bindComponents()} method of your
     * {@linkplain org.fluidity.composition.spi.PackageBindings binding} implementation.
     * <ul>
     * <li>To simply register a component implementation for its component interfaces, use {@link #bindComponent(Class, Class[]) bindComponent()}. This is
     * exactly what the <code>org.fluidity.maven:composition-maven-plugin</code> Maven plugin does for a {@link Component @Component} annotated class with no
     * {@link Component#automatic() automatic = false} setting so if this method is all you need then you should simply use the plugin
     * instead of creating your own binding class.</li>
     * <li>To register an already instantiated component implementation for a component interface, use {@link #bindInstance(Object,
     * Class[]) bindInstance()}. If
     * the implementation is annotated with <code>@Component</code> then its {@link Component#automatic() automatic} parameter must be set to
     * <code>false</code>.</li>
     * <li>To register a component implementation when some or all of its dependencies are - by design - not accessible in the same container, use {@link
     * #isolateComponent(Class, Class[]) isolateComponent()} method and use the returned container's {@link
     * MutableContainer#getRegistry() getRegistry()} method to gain access to the registry in which to bind the hidden dependencies. If the implementation is
     * annotated with <code>@Component</code> then its {@link Component#automatic() automatic} parameter must be set to <code>false</code>.</li>
     * </ul>
     * <h3>Usage</h3>
     * <ul>
     * <li>Populating transient containers:
     * <pre>
     * private void someMethod() {
     *
     *   // some component
     *   {@linkplain Component @Component}<span class="hl1">(automatic = false)</span>
     *   class <span class="hl3">MyComponentImpl</span> implements <span class="hl3">MyComponent</span> {
     *     &hellip;
     *   }
     *
     *   // some list of bindings
     *   final <span class="hl2">{@linkplain ComponentContainer.Bindings}</span> bindings = new <span class="hl2">{@linkplain ComponentContainer.Bindings}</span>() {
     *     public void <span class="hl2">bindComponents</span>(final <span class="hl1">ComponentContainer.Registry</span> registry) {
     *       registry.<span class="hl1">bindComponent</span>(<span class="hl3">MyComponentImpl</span>.class);
     *       &hellip;
     *     }
     *   }
     *
     *   // a container received from somewhere
     *   final <span class="hl2">{@linkplain ComponentContainer}</span> container = &hellip;;
     *
     *   final <span class="hl2">{@linkplain OpenContainer}</span> child = container.<span class="hl2">makeChildContainer</span>(bindings);
     *
     *   // use the child to get a component instance
     *   final <span class="hl3">MyComponent</span> component = child.getComponent(<span class="hl3">MyComponent</span>.class);
     *
     *   &hellip;
     * }
     * </pre></li>
     * <li>Overriding bindings: as an alternative to using explicitly context dependent components as dependencies, you can elect to isolate the dependencies
     * of a component from the rest of the container, even if some or all of those dependencies could otherwise be found in the container.
     * <pre>
     * final class MyPackageBindings extends {@linkplain org.fluidity.composition.spi.EmptyPackageBindings} {
     *   public void bindComponents(final <span class="hl1">ComponentContainer.Registry</span> registry) {
     *
     *     // SomeComponentImpl will be visible in the global container
     *     final <span class="hl2">ComponentContainer.Registry</span> isolated = registry.<span class="hl2">isolateComponent</span>(<span class="hl3">SomeComponentImpl</span>.class);
     *
     *     // AlternateDependencyImpl will not be visible in the global container
     *     isolated.<span class="hl2">bindComponent</span>(<span class="hl3">AlternateDependencyImpl</span>.class);
     *   }
     * }
     * </pre></li>
     * </ul>
     *
     * @author Tibor Varga
     */
    @SuppressWarnings("JavadocReference")
    interface Registry {

        /**
         * If the given class is an interface, or if it represents a concrete class and its {@link Component#scope() scope} is set to any of its component
         * interfaces, this method binds, in the container behind this registry, every component class whose {@link Component#scope() scope} is set to the
         * given class, to their respective component interfaces.
         * <p>
         * If the given class object represents a concrete class <em>without</em> its {@link Component#scope() scope} set to its component interface, this method
         * binds, in the container behind this registry, that component class to its component interfaces.
         * <p>
         * The component class will be instantiated on demand by the container when it resolves any of the provided component interfaces.
         * <p>
         * The supplied component class may implement {@link org.fluidity.composition.spi.ComponentFactory}, in which case the receiver must support
         * the factory functionality as described in the <code>ComponentFactory</code> documentation.
         * <p>
         * When the <code>interfaces</code> parameter is empty, the component interfaces of the bound component will be determined by the algorithm
         * documented at {@link Components}.
         *
         * @param type       the component class.
         * @param interfaces optional list of interfaces that should resolve to the supplied component class.
         * @param <T>        the component class to bind.
         *
         * @throws ComponentContainer.BindingException
         *          when component registration fails.
         */
        @SuppressWarnings("unchecked")
        <T> void bindComponent(Class<T> type, Class<? super T>... interfaces) throws BindingException;

        /**
         * Binds a component instance to a list of component interfaces in the container behind this registry. Use this method when you have no control over
         * the instantiation of a class as in case of third party tools handing you the component, but you still want to make the instances available for
         * components in the container to depend on.
         * <p>
         * The supplied component instance may be a {@link org.fluidity.composition.spi.ComponentFactory} instance, in which case the receiver must support
         * the factory functionality as described in the <code>ComponentFactory</code> documentation.
         * <p>
         * When the <code>interfaces</code> parameter is empty, the list of interfaces that resolve to the component will be determined by the algorithm
         * documented at {@link Components}.
         *
         * @param instance   the component instance.
         * @param interfaces optional list of interfaces that should resolve to the supplied component class.
         * @param <T>        the type of the component to bind.
         *
         * @throws ComponentContainer.BindingException
         *          when component registration fails.
         */
        @SuppressWarnings("unchecked")
        <T> void bindInstance(T instance, Class<? super T>... interfaces) throws BindingException;

        /**
         * Binds all elements of an ad-hoc component group. The group interface need not be annotated with {@link ComponentGroup @ComponentGroup}.
         *
         * @param group the component group to bind the elements of.
         * @param types the elements of the group.
         * @param <T>   the group interface type to bind.
         */
        @SuppressWarnings("unchecked")
        <T> void bindComponentGroup(Class<T> group, Class<? extends T>... types);

        /**
         * Binds a component factory instance to a list of component interfaces in the container behind this registry. Use this method when the list component
         * interfaces that the factory can provide an implementation for is computed at run time.
         * <p>
         * The receiver must support the factory functionality as described in the <code>ComponentFactory</code> documentation.
         * <p>
         * When the <code>interfaces</code> parameter is empty, the list of interfaces that resolve to the component will be determined by the algorithm
         * documented at {@link Components}.
         *
         * @param factory    the component factory instance.
         * @param interfaces optional list of interfaces that should resolve to the supplied component class.
         *
         * @throws ComponentContainer.BindingException
         *          when component registration fails.
         */
        void bindFactory(ComponentFactory factory, Class<?>... interfaces) throws BindingException;

        /**
         * Binds a component class to a list of component interfaces behind this registry and allows, via the returned registry, the caller to add further
         * bindings that will only be visible to the supplied component and each other. These bindings can, for the supplied component, override bindings in
         * the container behind this registry.
         * <p>
         * When the <code>interfaces</code> parameter is empty, the list of interfaces that resolve to the component will be determined by the algorithm
         * documented at {@link Components}.
         *
         * @param type       the component whose dependencies the child container will resolve.
         * @param interfaces the interfaces that the returned container should be able resolve through this container.
         * @param <T>        the type of the component to isolate.
         *
         * @return the registry for the linked child container, never <code>null</code>.
         *
         * @throws ComponentContainer.BindingException
         *          when component registration fails.
         */
        @SuppressWarnings("unchecked")
        <T> Registry isolateComponent(Class<T> type, Class<? super T>... interfaces) throws BindingException;
    }

    /**
     * Observes component dependency resolutions.
     * <h3>Usage</h3>
     * A {@link ComponentContainer} can be used to explore the static and dynamic dependencies starting at any component interface. See {@link
     * ObservedContainer} for an example.
     *
     * @author Tibor Varga
     */
    interface Observer {

        /**
         * Notifies the receiver that a dependency is being resolved. Calls to this method are balanced by calls to {@link #ascending ascending()}.
         *
         * @param declaringType        the class having the dependency to the <code>dependencyType</code>.
         * @param dependencyType       the class with which the <code>declaringType</code> references the dependency.
         * @param typeAnnotations      the annotations of the declaring class, and the constructor or method involved in the dependency, if any; may be
         *                             <code>null</code>.
         * @param referenceAnnotations the annotations of the dependency reference; may be <code>null</code>.
         */
        void descending(Class<?> declaringType, Class<?> dependencyType, Annotation[] typeAnnotations, Annotation[] referenceAnnotations);

        /**
         * Notifies the receiver that a dependency has been resolved. A call to this method balances a previous call to {@link #descending descending()}.
         *
         * @param declaringType        the class having the dependency to the <code>dependencyType</code>.
         * @param dependencyType       the class with which the <code>declaringType</code> references the dependency.
         */
        void ascending(Class<?> declaringType, Class<?> dependencyType);

        /**
         * Notifies the receiver that a loop has been detected in the dependency graph. Invoked for static dependencies only and at the point when the loop
         * refers back to a component in the dependency path. The {@link DependencyPath#tail()} returns the component that depends on another in its reference
         * path.
         *
         * @param path the dependency path that is circular.
         */
        void circular(DependencyPath path);

        /**
         * Notifies the receiver that a dependency has been resolved. Elements of the path are reference declarations and might not be the actual classes that
         * will be instantiated for those references.
         *
         * @param path the dependency path at which the given type has been resolved.
         * @param type the type that has been resolved at the given dependency path.
         */
        void resolved(DependencyPath path, Class<?> type);

        /**
         * Notifies the receiver that a dependency has been instantiated. The path is final; elements of the path are actual classes that will be.
         * <p>
         * The {@link DependencyPath#tail()} returns details about the just instantiated component.
         *
         * @param path      the dependency path at which the given type has been instantiated.
         * @param reference a reference to the component that has just been instantiated. The reference will be set <em>after</em> this method returns to
         *                  prevent the receiver from wreaking havoc by accessing the just instantiated component.
         */
        void instantiated(DependencyPath path, AtomicReference<?> reference);
    }

    /**
     * Implements the {@link ComponentContainer.Observer} interface with empty methods.
     *
     * @author Tibor Varga
     */
    abstract class ObserverSupport implements Observer {

        public void descending(final Class<?> declaringType,
                               final Class<?> dependencyType,
                               final Annotation[] typeAnnotations,
                               final Annotation[] referenceAnnotations) {
            // empty
        }

        public void ascending(final Class<?> declaringType, final Class<?> dependencyType) {
            // empty
        }

        public void circular(final DependencyPath path) {
            // empty
        }

        public void resolved(final DependencyPath path, final Class<?> type) {
            // empty
        }

        public void instantiated(final DependencyPath path, final AtomicReference<?> reference) {
            // empty
        }
    }

    /**
     * Top level exception for errors related to the dependency injection.
     */
    class InjectionException extends RuntimeException {

        private String path;

        /**
         * Creates a new instance using the given formatted text and with the given cause.
         *
         * @param cause  the exception that triggered this error.
         * @param format the Java format specification.
         * @param data   the details to format.
         */
        InjectionException(final Throwable cause, final String format, final Object... data) {
            super(String.format(format, data), cause);
        }

        /**
         * Creates a new instance using the given formatted text.
         *
         * @param format the Java format specification.
         * @param data   the details to format.
         */
        InjectionException(final String format, final Object... data) {
            super(String.format(format, data));
        }

        /**
         * Creates a nondescript instance to be used internally.
         *
         * @param cause the exception that triggered this error.
         */
        InjectionException(final Throwable cause) {
            super(cause);
        }

        /**
         * Updates the dependency path at which the exception occurred.
         *
         * @param path the dependency path; never <code>null</code>.
         */
        public final void path(final DependencyPath path) {
            if (this.path == null) {
                this.path = path.toString(false);
            }
        }

        @Override
        public String getMessage() {
            final String message = super.getMessage();
            return path == null ? message : String.format("%s: %s", message, path);
        }
    }

    /**
     * Reports an error that occurred when resolving a component reference to a component.
     */
    class ResolutionException extends InjectionException {

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

        /**
         * Creates a nondescript instance to be used internally.
         *
         * @param cause the exception that triggered this error.
         */
        public ResolutionException(final Throwable cause) {
            super(cause);
        }
    }

    /**
     * Reports that some chain of dependencies is circular.
     */
    class CircularReferencesException extends ResolutionException {

        /**
         * Creates a new instance for the given component interface resolved with the given instantiation path.
         *
         * @param api  the component interface that could not be resolved.
         */
        public CircularReferencesException(final Class<?> api) {
            super("Circular dependency detected while resolving %s", Strings.formatClass(true, true, api));
        }
    }

    /**
     * Reports an error that occurred while trying to instantiate a component during dependency resolution.
     */
    class InstantiationException extends ResolutionException {

        /**
         * Creates a new instance for the given instantiation path and cause error.
         *
         * @param cause the error that occurred.
         */
        public InstantiationException(final Exception cause) {
            super(cause);
        }
    }

    /**
     * Reports an error that occurred when binding a component class or instance to its component interfaces.
     */
    class BindingException extends InjectionException {

        /**
         * Creates a new instance using the given formatted text and cause exception.
         *
         * @param cause  the exception that triggered this one.
         * @param format the Java format specification.
         * @param data   the details to format.
         */
        public BindingException(final Throwable cause, final String format, final Object... data) {
            super(cause, format, data);
        }

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
