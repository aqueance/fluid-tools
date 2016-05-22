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

package org.fluidity.composition.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;

/**
 * Creates instances of a component when mere constructor and field injection is not adequate.
 * <p>
 * <b>Note</b>: Component factories expose an internal process to the external world and thus hard measures are taken to protect the former from the latter by
 * wrapping sensitive functionality under highly focused interfaces.
 * <p>
 * Component instantiation follows a certain protocol and <code>ComponentFactory</code> objects must follow that protocol to integrate well to Fluid Tools.
 * This interface defines the necessary methods and interfaces for that integration.
 * <p>
 * The protocol is as follows:
 * <ul>
 * <li>The {@link #resolve(ComponentContext, ComponentFactory.Resolver) ComponentFactory.resolve()} method is called with the context for the new
 * component</li>
 * <li>The receiver uses the provided {@link ComponentFactory.Resolver resolver} to resolve the dependencies of the new component without actually
 * instantiating anything.</li>
 * <li>At some point later, the {@link ComponentFactory.Instance#bind(ComponentFactory.Registry) ComponentFactory.Instance.bind()} method is invoked on the
 * returned object to bind, in a transient container, the component to be instantiated and its local dependencies.</li>
 * <li>At some point later, the transient container is asked for the component by the component interface the factory is {@linkplain Component bound against}
 * and then the transient container is discarded.</li>
 * </ul>
 * <h3>Usage</h3>
 * <h4>Basic Pattern</h4>
 * <pre>
 * {@linkplain Component @Component}(api = <span class="hl2">MyComponent</span>.class)
 * final class MyComponentFactory implements <span class="hl1">{@linkplain ComponentFactory}</span> {
 *
 *   public {@linkplain ComponentFactory.Instance Instance} <span class="hl1">resolve</span>(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Resolver Resolver} dependencies) throws Exception {
 *     dependencies.discover(<span class="hl3">MyComponentImpl</span>.class);
 *
 *     return new {@linkplain ComponentFactory.Instance Instance}() {
 *       public void bind(final {@linkplain ComponentFactory.Registry Registry} registry) throws Exception {
 *         registry.bindComponent(<span class="hl3">MyComponentImpl</span>.class);
 *       }
 *     }
 *   }
 * }
 * </pre>
 * <pre>
 * {@linkplain Component @Component}(automatic = false)
 * final class <span class="hl3">MyComponentImpl</span> implements <span class="hl2">MyComponent</span> {
 *   &hellip;
 * }
 * </pre>
 * <h4>Context Adaptation</h4>
 * <pre>
 * {@linkplain Component @Component}(api = <span class="hl2">CreatedComponent</span>.class)
 * {@linkplain org.fluidity.composition.Component.Qualifiers @Component.Qualifiers}(<span class="hl3">CustomContext</span>.class)
 * final class CustomFactory implements <span class="hl1">{@linkplain ComponentFactory}</span> {
 *
 *   public {@linkplain ComponentFactory.Instance Instance} resolve(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Resolver Resolver} dependencies) throws Exception { {
 *     final <span class="hl3">CustomContext</span>[] contexts = context.qualifiers(<span class="hl3">CustomContext</span>.class);
 *
 *     dependencies.discover(<span class="hl2">CreatedComponent</span>.class);
 *
 *     return new Instance() {
 *       public void bind(final {@linkplain ComponentFactory.Registry Registry} registry) throws Exception {
 *         registry.bindInstance(contexts, <span class="hl3">CustomContext</span>[].class);
 *         registry.bindComponent(<span class="hl2">CreatedComponent</span>.class);
 *       }
 *     };
 *   }
 * }
 * </pre>
 * <pre>
 * public final class <span class="hl2">CreatedComponent</span> {
 *
 *   <span class="hl2">CreatedComponent</span>(final {@linkplain org.fluidity.composition.Optional @Optional} <span class="hl2">CustomContext</span>[] contexts) {
 *     &hellip;
 *   }
 * }
 * </pre>
 */
public interface ComponentFactory {

    /**
     * Informs the caller about the static dependencies the created component has in the given context. Actual component instantiation does _not_ take place in
     * this method.
     * <p>
     * The caller is informed about the static dependencies of the component by calls to methods of the {@link ComponentFactory.Resolver resolver} parameter.
     * <p>
     * In the general case, a call to {@link ComponentFactory.Resolver#discover(Class)} will find the injectable constructor of the given type – the component
     * implementation class – and inform the caller about the dependencies found therein. In case some of those declared dependencies don't match the type of
     * the actual object injected by the factory to the component instance, further calls to {@link ComponentFactory.Resolver#resolve(Class, Type,
     * Annotation[])} should be made, one for each such dependency.
     * <p>
     * If the general case above does not apply, more specific methods of the {@link ComponentFactory.Resolver} class can be used to make sure the caller is
     * informed about all dependencies of the component instance.
     * <p>
     * The {@link ComponentFactory.Instance#bind(ComponentFactory.Registry) ComponentFactory.Instance.bind()} method of the returned object will be invoked, at
     * the appropriate time, to actually bind the created component, and its local, internal dependencies to a container. Actual instantiation should, when
     * possible at all, be left to the caller of that method. If the factory absolutely has to instantiate the component on account of, for instance, having to
     * call an external factory, then that instantiation must not take place in this method but in {@link
     * ComponentFactory.Instance#bind(ComponentFactory.Registry) ComponentFactory.Instance.bind()}.
     * <p>
     * The following code snippet shows the basic pattern of the implementation:
     * <pre>
     * public {@linkplain ComponentFactory.Instance Instance} <span class="hl1">resolve</span>(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Resolver Resolver} dependencies) throws Exception {
     *   dependencies.<span class="hl2">discover</span>(<span class="hl3">CreatedComponent</span>.class);
     *
     *   return new {@linkplain ComponentFactory.Instance Instance}() {
     *     public void bind(final {@linkplain ComponentFactory.Registry Registry} registry) throws Exception {
     *       registry.bindComponent(<span class="hl3">CreatedComponent</span>.class);
     *     }
     *   }
     * }
     * </pre>
     * <p>
     * The following code snippet shows the pattern of the implementation in case it has to call the component's constructor:
     * <pre>
     * public {@linkplain ComponentFactory.Instance Instance} <span class="hl1">resolve</span>(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Resolver Resolver} dependencies) throws Exception {
     *   final Constructor&lt;?&gt; <span class="hl2">constructor</span> = dependencies.{@linkplain ComponentFactory.Resolver#constructor(Class) constructor}(<span class="hl2">CreatedComponent</span>.class);
     *   final Dependency&lt;?&gt;[] <span class="hl3">parameters</span> = dependencies.{@linkplain ComponentFactory.Resolver#resolve(Class, java.lang.reflect.Constructor, int) resolve}(null, <span class="hl2">constructor</span>);
     *
     *   return new {@linkplain ComponentFactory.Instance Instance}() {
     *     public void bind(final {@linkplain ComponentFactory.Registry Registry} registry) throws Exception {
     *
     *       // make sure the assumptions implicit in the code below
     *       // are not broken by changes to MyComponentImpl:
     *       if (false) {
     *         final MyComponent result = new <span class="hl2">CreatedComponent</span>((Dependency1) null,
     *                                                         (Dependency2) null,
     *                                                         (String) null,
     *                                                         0);
     *       }
     *
     *       <span class="hl3">parameters</span>[2] = dependencies.{@linkplain ComponentFactory.Resolver#constant(Object) constant}("some text");
     *       <span class="hl3">parameters</span>[3] = dependencies.{@linkplain ComponentFactory.Resolver#constant(Object) constant}(5);
     *
     *       registry.bindInstance(<span class="hl2">constructor</span>.{@linkplain Constructor#newInstance(Object...) newInstance}(dependencies.{@linkplain ComponentFactory.Resolver#instantiate(org.fluidity.composition.spi.ComponentFactory.Dependency[]) instantiate}(<span class="hl3">parameters</span>)));
     *     }
     *   }
     * }
     * </pre>
     * <p>
     * The following code snippet shows the pattern of the implementation in case it has to call some external factory:
     * <pre>
     * public {@linkplain ComponentFactory.Instance Instance} <span class="hl1">resolve</span>(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Resolver Resolver} dependencies) throws Exception {
     *
     *   // make sure the assumptions implicit in the code below
     *   // are not broken by changes to ExternalLibrary:
     *   if (false) {
     *     final MyComponent result = <span class="hl2">external</span>.<span class="hl2">factoryMethod</span>((Dependency1) null,
     *                                                       (Dependency2) null,
     *                                                       (String) null,
     *                                                       (Integer) null);
     *   }
     *
     *   final Method <span class="hl2">method</span> = <span class="hl2">external</span>.getClass().getMethod("<span class="hl2">factoryMethod</span>",
     *                                                       Dependency1.class,
     *                                                       Dependency2.class,
     *                                                       String.class,
     *                                                       int.class);
     *   final Dependency&lt;?&gt;[] <span class="hl3">parameters</span> = dependencies.{@linkplain ComponentFactory.Resolver#resolve(Class, java.lang.reflect.Method) resolve}(null, <span class="hl2">method</span>);
     *
     *   return new {@linkplain ComponentFactory.Instance Instance}() {
     *     public void bind(final {@linkplain ComponentFactory.Registry Registry} registry) throws Exception {
     *       <span class="hl3">parameters</span>[2] = dependencies.{@linkplain ComponentFactory.Resolver#constant(Object) constant}("some text");
     *       <span class="hl3">parameters</span>[3] = dependencies.{@linkplain ComponentFactory.Resolver#constant(Object) constant}(5);
     *
     *       registry.bindInstance(<span class="hl2">method</span>.{@linkplain Method#invoke(Object, Object...) invoke}(null, dependencies.{@linkplain ComponentFactory.Resolver#instantiate(ComponentFactory.Dependency[]) instantiate}(<span class="hl3">parameters</span>)));
     *     }
     *   }
     * }
     * </pre>
     *
     * @param context      the context under which component creation will take place. This context contains only those annotations that this factory lists in
     *                     its {@link org.fluidity.composition.Component.Qualifiers @Component.Qualifiers} annotation.
     * @param dependencies the dependency resolver to notify of any dependencies the created class will have.
     *
     * @return an object that will bind the created components and its dependencies, or <code>null</code> if no component will be instantiated.
     *
     * @throws Exception when an error occurs.
     */
    Instance resolve(ComponentContext context, Resolver dependencies) throws Exception;

    /**
     * Represents a future instance of the component created by a {@link ComponentFactory}.
     * <h3>Usage</h3>
     * See {@link ComponentFactory}.
     *
     * @author Tibor Varga
     */
    interface Instance {

        /**
         * Binds the component to be created, and all its local dependencies, e.g., those not found in the application's containers by design, to the supplied
         * registry.
         *
         * @param registry the registry to bind components in.
         *
         * @throws Exception when an error occurs.
         */
        void bind(Registry registry) throws Exception;
    }

    /**
     * Represents a resolved dependency of the component being created by a {@link ComponentFactory} in its {@link ComponentFactory#resolve(ComponentContext,
     * ComponentFactory.Resolver) resolve()} method.
     * <h3>Usage</h3>
     * See {@link ComponentFactory}.
     *
     * @param <T> the type of the dependency.
     *
     * @author Tibor Varga
     */
    interface Dependency<T> {

        /**
         * Returns an actual instance of the dependency. This method is intended to be invoked only from the {@link
         * ComponentFactory.Instance#bind(ComponentFactory.Registry) bind()} method of the <code>Instance</code> returned from the factory's {@link
         * ComponentFactory#resolve(ComponentContext, ComponentFactory.Resolver) resolve()} method.
         *
         * @return an actual instance of the dependency; may be <code>null</code>.
         */
        T instance();
    }

    /**
     * Allows a {@link ComponentFactory} to resolve dependencies without instantiating them.
     * <h3>Usage</h3>
     * See {@link ComponentFactory}.
     *
     * @author Tibor Varga
     */
    @SuppressWarnings("JavadocReference")
    interface Resolver extends Container {

        /**
         * Resolves a component by its component interface without instantiating the component. The resolved component will see, if it accepts the {@link
         * org.fluidity.composition.Component.Reference @Component.Reference} qualifier annotation, the specified component interface as the dependency
         * reference to it unless the <code>reference</code> parameter is present. The component interface must be assignable to the reference if it is
         * specified.
         * <p>
         * <b>Note</b>: This is a low level method in case none of the {@link #resolve(Class, Constructor, int)}, {@link #resolve(Class, Class, Field)}, or
         * {@link #resolve(Class, Class, Method, int)} satisfies your needs. Please favor those methods to this one.
         *
         * @param api         the component interface to resolve, or <code>null</code> of it can be derived from the <code>reference</code> parameter.
         * @param reference   the reference to use when resolving the component or <code>null</code> to use the component interface.
         * @param annotations the annotations at the point of reference to the dependency; may be <code>null</code>.
         * @param <T>         the type of the dependency to resolve.
         *
         * @return an object that can return an instance of the resolved dependency.
         */
        <T> Dependency<T> resolve(Class<T> api, Type reference, Annotation[] annotations);

        /**
         * Informs the recipient about the injectable constructor parameters of the given component class, and then returns a list of dependencies, each
         * corresponding to the parameters of the dependency injectable constructor of the supplied component class. For the algorithm on constructor
         * resolution, see the {@link org.fluidity.composition.container.DependencyInjector#constructor(Class, DependencyGraph.Traversal, DependencyResolver,
         * ContextNode, org.fluidity.composition.container.ContextDefinition, Constructor) DependencyInjector.constructor()} method.
         * <p>
         * The constructor discovered by this method is also returned by the {@link #constructor(Class)} method and can then be used to inject the dependencies
         * discovered by this one.
         *
         * @param type the component class.
         *
         * @return a list of dependencies corresponding to the constructor parameters.
         */
        Dependency<?>[] discover(Class<?> type);

        /**
         * Informs the recipient about the injectable field dependencies of the given component class.
         *
         * @param type the component class.
         */
        void fields(Class<?> type);

        /**
         * Returns the dependency injectable constructor of the given component class. The returned value can be used to instantiate the component using the
         * dependencies discovered by the {@link #discover(Class)} method.
         *
         * @param type the component class.
         *
         * @return the dependency injectable constructor of the given component class.
         */
        Constructor<?> constructor(Class<?> type);

        /**
         * Informs the recipient about the injectable constructor parameters of the given component class, and then returns a list of dependencies, each
         * corresponding to the parameters of the supplied constructor.
         * <p>
         * Use this method if the component has multiple constructors and the invoking dependency injection container would not be able to figure out which one
         * to use. Otherwise use the {@link #discover(Class)} and {@link #constructor(Class)} methods instead.
         *
         * @param constructor the constructor.
         *
         * @return a list of dependencies corresponding to the parameters of the constructor parameters.
         */
        Dependency<?>[] discover(Constructor<?> constructor);

        /**
         * Informs the recipient about the injectable constructor parameters of the given component class, and then returns a list of dependencies, each
         * corresponding to the parameters of the supplied parameter injected method.
         *
         * @param method the method.
         *
         * @return a list of dependencies corresponding to the parameters of the supplied factory method.
         */
        Dependency<?>[] discover(Method method);

        /**
         * Creates a transient container and applies the given bindings thereto. The returned container can be used to instantiate dependencies local to the
         * component when the component itself is instantiated by the calling factory.
         *
         * @param type     component class to derive context from; may be <code>null</code>.
         * @param bindings the component bindings for the local dependencies of the component.
         *
         * @return a container to instantiate local dependencies.
         */
        Container local(Class<?> type, Bindings bindings);

        /**
         * Wraps the given object as a dependency. Useful to fill in missing {@linkplain #resolve(Constructor) constructor} or {@linkplain #resolve(Class,
         * Method) method} parameters in the {@link Dependency} array returned by those methods before feeding the array to {@link
         * #instantiate(ComponentFactory.Dependency[])}.
         *
         * @param object the object to wrap as a dependency.
         * @param <T>    the dependency type the given <code>object</code> satisfies.
         *
         * @return the dependency whose {@link ComponentFactory.Dependency#instance()} method will return the given <code>object</code>.
         */
        <T> Dependency<T> constant(T object);

        /**
         * Returns an array containing the {@linkplain ComponentFactory.Dependency#instance() instance} of each dependency in the given array.
         *
         * @param dependencies the dependency array; may not be <code>null</code>.
         *
         * @return an array of objects; never <code>null</code>.
         */
        Object[] instantiate(Dependency<?>... dependencies);
    }

    /**
     * Registry to bind components into by a {@link ComponentFactory}. Methods of this interface are to be invoked from the {@link
     * ComponentFactory.Instance#bind(ComponentFactory.Registry) bind()} method of the {@link ComponentFactory.Instance} returned from the factory's {@link
     * ComponentFactory#resolve(ComponentContext, ComponentFactory.Resolver) resolve()} method.
     * <p>
     * The intent for this interface is to restrict access to a {@link org.fluidity.composition.ComponentContainer.Registry} object through a smaller set
     * of methods.
     * <h3>Usage</h3>
     * See {@link ComponentFactory}.
     *
     * @author Tibor Varga
     */
    interface Registry {

        /**
         * Binds a component class to an optional list of component interfaces in the container behind this registry.
         *
         * @param implementation see {@link org.fluidity.composition.ComponentContainer.Registry#bindComponent(Class, Class...)
         *                       ComponentContainer.Registry.bindComponent()}.
         * @param interfaces     see {@link org.fluidity.composition.ComponentContainer.Registry#bindComponent(Class, Class...)
         *                       ComponentContainer.Registry.bindComponent()}.
         * @param <T>            the component class to bind.
         *
         * @throws ComponentContainer.BindingException
         *          see {@link org.fluidity.composition.ComponentContainer.Registry#bindComponent(Class, Class...)
         *          ComponentContainer.Registry.bindComponent()}.
         */
        <T> void bindComponent(Class<T> implementation, Class<? super T>... interfaces) throws ComponentContainer.BindingException;

        /**
         * Binds a component instance to an optional list of component interfaces in the container behind this registry.
         *
         * @param instance   see {@link org.fluidity.composition.ComponentContainer.Registry#bindInstance(Object, Class...)
         *                   ComponentContainer.Registry.bindInstance()}.
         * @param interfaces see {@link org.fluidity.composition.ComponentContainer.Registry#bindInstance(Object, Class...)
         *                   ComponentContainer.Registry.bindInstance()}.
         * @param <T>        the type of component to bind.
         *
         * @throws ComponentContainer.BindingException
         *          see {@link org.fluidity.composition.ComponentContainer.Registry#bindInstance(Object, Class...)
         *          ComponentContainer.Registry.bindInstance()}.
         */
        <T> void bindInstance(T instance, Class<? super T>... interfaces) throws ComponentContainer.BindingException;
    }

    /**
     * Container to resolve local dependencies of some component that will be instantiated by the {@linkplain ComponentFactory component factory} rather than
     * the invoking container.
     *
     * <h3>Usage</h3>
     * <pre>
     * {@linkplain Component @Component}(api = MyComponent.class)
     * final class MyComponentFactory implements {@linkplain ComponentFactory} {
     *
     *   public {@linkplain ComponentFactory.Instance Instance} resolve(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Resolver Resolver} dependencies) throws Exception {
     *     final Constructor&lt;?&gt; <span class="hl2">constructor</span> = dependencies.<span class="hl1">constructor</span>(<span class="hl3">MyComponentImpl</span>.class);
     *
     *     final <span class="hl1">Container</span> container = dependencies.<span class="hl1">local</span>(<span class="hl2">MyComponentImpl</span>.class, new {@linkplain Container.Bindings}() {
     *       public void bindComponents(final {@linkplain Container.Registry} registry) {
     *         registry.<span class="hl1">bindComponent</span>(<span class="hl3">LocalDependency</span>.class);
     *       }
     *     }));
     *
     *     final Dependency&lt;?&gt;[] <span class="hl3">parameters</span> = container.<span class="hl1">resolve</span>(null, <span class="hl2">constructor</span>);
     *
     *     return new {@linkplain ComponentFactory.Instance Instance}() {
     *       public void bind(final {@linkplain ComponentFactory.Registry Registry} registry) throws Exception {
     *         registry.bindComponent(<span class="hl2">constructor</span>.newInstance(dependencies.<span class="hl1">instantiate</span>(<span class="hl3">parameters</span>)));
     *       }
     *     }
     *   }
     * }
     * </pre>
     * <pre>
     * {@linkplain Component @Component}(automatic = false)
     * final class <span class="hl2">MyComponentImpl</span> implements MyComponent {
     *
     *   <span class="hl2">MyComponentImpl</span>(final <span class="hl3">LocalDependency</span> local, &hellip;) {
     *     &hellip;
     *   }
     *
     *   &hellip;
     * }
     * </pre>
     * <pre>
     * {@linkplain Component @Component}(automatic = false)
     * final class <span class="hl3">LocalDependency</span> {
     *   &hellip;
     * }
     * </pre>
     *
     * @author Tibor Varga
     */
    interface Container {

        /**
         * Resolves a component by its component interface without instantiating the component. The resolved component will see, if it accepts the {@link
         * org.fluidity.composition.Component.Reference @Component.Reference} qualifier annotation, the specified component interface as the dependency
         * reference to it unless the <code>reference</code> parameter is present. The component interface must be assignable to the reference if it is
         * specified.
         *
         * @param api         the component interface to resolve; may not be <code>null</code>.
         * @param constructor constructor to derive context from.
         * @param reference   index of the constructor parameter to derive generic type and context from.
         * @param <T>         the type of the dependency to resolve.
         *
         * @return an object that can return an instance of the resolved dependency.
         */
        <T> Dependency<T> resolve(Class<T> api, Constructor<?> constructor, int reference);

        /**
         * Resolves a component by its component interface without instantiating the component. The resolved component will see, if it accepts the {@link
         * org.fluidity.composition.Component.Reference @Component.Reference} qualifier annotation, the specified component interface as the dependency
         * reference to it unless the <code>reference</code> parameter is present. The component interface must be assignable to the reference if it is
         * specified.
         *
         * @param api       the component interface to resolve; may not be <code>null</code>.
         * @param type      the component class to resolve the dependency of; used to derive context from; may be <code>null</code>, in which case the method's
         *                  declaring class will be used.
         * @param method    method to derive context from.
         * @param reference index of the constructor parameter to derive generic type and context from.
         * @param <T>       the type of the dependency to resolve.
         *
         * @return an object that can return an instance of the resolved dependency.
         */
        <T> Dependency<T> resolve(Class<T> api, Class<?> type, Method method, int reference);

        /**
         * Resolves a component by its component interface without instantiating the component. The resolved component will see, if it accepts the {@link
         * org.fluidity.composition.Component.Reference @Component.Reference} qualifier annotation, the specified component interface as the dependency
         * reference to it unless the <code>reference</code> parameter is present. The component interface must be assignable to the reference if it is
         * specified.
         *
         * @param api   the component interface to resolve; may not be <code>null</code>.
         * @param type  the component class to resolve the dependency of; used to derive context from; may be <code>null</code>, in which case the field's
         *              declaring class will be used.
         * @param field field to derive generic type and context from.
         * @param <T>   the type of the dependency to resolve.
         *
         * @return an object that can return an instance of the resolved dependency.
         */
        <T> Dependency<T> resolve(Class<T> api, Class<?> type, Field field);

        /**
         * Resolves each resolvable parameter of the given method and returns a list of dependencies for each method parameter, resolvable or not. The returned
         * array is mutable and can be fed to {@link Resolver#instantiate(ComponentFactory.Dependency[])} to get an argument list to invoke the
         * <code>method</code> with.
         *
         * @param type the component class to derive context from; may be <code>null</code>, in which case the method's declaring class is used.
         * @param method the method to resolve the parameters of.
         *
         * @return a list of dependencies that can then be used to get a dependency {@linkplain ComponentFactory.Dependency#instance()} of.
         */
        Dependency<?>[] resolve(Class<?> type, Method method);

        /**
         * Resolves each resolvable parameter of the given constructor and returns a list of dependencies for each method parameter, resolvable or not. The
         * returned array is mutable and can be fed to {@link Resolver#instantiate(ComponentFactory.Dependency[])} to get an argument list to invoke the
         * <code>constructor</code> with.
         *
         * @param constructor the constructor to resolve the parameters of.
         *
         * @return a list of dependencies that can then be used to get a dependency {@linkplain ComponentFactory.Dependency#instance()} of.
         */
        Dependency<?>[] resolve(Constructor<?> constructor);

        /**
         * Adds component bindings to a {@linkplain ComponentFactory.Container local dependency container}.
         *
         * <h3>Usage</h3>
         * See {@link ComponentFactory.Container}.
         *
         * @author Tibor Varga
         */
        interface Registry {

            /**
             * Binds a component class to an optional list of component interfaces in the container behind this registry.
             *
             * @param implementation see {@link org.fluidity.composition.ComponentContainer.Registry#bindComponent(Class, Class...)
             *                       ComponentContainer.Registry.bindComponent()}.
             * @param interfaces     see {@link org.fluidity.composition.ComponentContainer.Registry#bindComponent(Class, Class...)
             *                       ComponentContainer.Registry.bindComponent()}.
             * @param <T>            the component class to bind.
             *
             * @throws ComponentContainer.BindingException
             *          see {@link org.fluidity.composition.ComponentContainer.Registry#bindComponent(Class, Class...)
             *          ComponentContainer.Registry.bindComponent()}.
             */
            <T> void bindComponent(Class<T> implementation, Class<? super T>... interfaces) throws ComponentContainer.BindingException;
        }

        /**
         * Adds component bindings to a {@link ComponentFactory.Container}.
         *
         * <h3>Usage</h3>
         * See {@link ComponentFactory.Container}.
         *
         * @author Tibor Varga
         */
        interface Bindings {

            /**
             * Adds component bindings to the given registry.
             *
             * @param registry the registry to add the bindings to.
             */
            void bindComponents(Registry registry);
        }
    }
}
