/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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
 * <p/>
 * <b>Note</b>: Component factories expose an internal process to the external world and thus hard measures are taken to protect the former from the latter by
 * wrapping sensitive functionality under highly focused interfaces.
 * <p/>
 * Component instantiation follows a certain protocol and <code>ComponentFactory</code> objects must follow that protocol to integrate well to Fluid Tools.
 * This interface defines the necessary methods and interfaces for that integration.
 * <p/>
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
 * <pre>
 * {@linkplain Component @Component}(api = <span class="hl2">MyComponent</span>.class)
 * final class MyComponentFactory implements <span class="hl1">{@linkplain ComponentFactory}</span> {
 *
 *   public {@linkplain ComponentFactory.Instance Instance} <span class="hl1">resolve</span>(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Resolver Resolver} dependencies) throws {@linkplain org.fluidity.composition.ComponentContainer.ResolutionException} {
 *     dependencies.discover(<span class="hl3">MyComponentImpl</span>.class);
 *
 *     return new {@linkplain ComponentFactory.Instance Instance}() {
 *       public void bind(final {@linkplain ComponentFactory.Registry Registry} registry) throws {@linkplain org.fluidity.composition.ComponentContainer.ResolutionException} {
 *         registry.bindComponent(<span class="hl3">MyComponentImpl</span>.class);
 *       }
 *     }
 *   }
 * }
 *
 * {@linkplain Component @Component}(automatic = false)
 * final class <span class="hl3">MyComponentImpl</span> implements <span class="hl2">MyComponent</span> {
 *   &hellip;
 * }
 * </pre>
 * <h4>Context Adaptation</h4>
 * If the created component is context aware, the {@link ComponentFactory} has two distinct ways to communicate the contextual information to the
 * new component:<ol>
 * <li>The factory can extract from the <code>context</code> received in {@link
 * ComponentFactory#resolve(ComponentContext, ComponentFactory.Resolver) ComponentFactory.resolve()} all contextual information the created component needs and
 * pass them to the new component as a dependency. For example:
 * <pre>
 * {@linkplain Component @Component}(api = <span class="hl2">CreatedComponent</span>.class)
 * {@linkplain org.fluidity.composition.Component.Context @Component.Context}(<span class="hl3">CustomContext</span>.class)
 * final class CustomFactory implements <span class="hl1">{@linkplain ComponentFactory}</span> {
 *
 *   public {@linkplain ComponentFactory.Instance Instance} resolve(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Resolver Resolver} dependencies) throws {@linkplain org.fluidity.composition.ComponentContainer.ResolutionException} { {
 *     final <span class="hl3">CustomContext</span>[] contexts = context.annotations(<span class="hl3">CustomContext</span>.class);
 *
 *     dependencies.discover(<span class="hl2">CreatedComponent</span>.class);
 *
 *     return new Instance() {
 *       public void bind(final {@linkplain ComponentFactory.Registry Registry} registry) throws {@linkplain org.fluidity.composition.ComponentContainer.ResolutionException} {
 *         registry.bindInstance(contexts, <span class="hl3">CustomContext</span>[].class);
 *         registry.bindComponent(<span class="hl2">CreatedComponent</span>.class);
 *       }
 *     };
 *   }
 * }
 *
 * final class <span class="hl2">CreatedComponent</span> {
 *
 *   public <span class="hl2">CreatedComponent</span>(final {@linkplain org.fluidity.composition.Optional @Optional} <span class="hl2">CustomContext</span>[] contexts) {
 *     &hellip;
 *   }
 * }
 * </pre></li>
 * <li>The new component can <em>duplicate</em> the part of the {@linkplain org.fluidity.composition.Component.Context context annotation} of its
 * factory that it actually uses and declare a dependency on {@link ComponentContext} to extract the required details from. The context annotation at
 * the factory must still be complete as that is used to distinguish between one reference to the component from other references to the same
 * component. For example:
 * <pre>
 * {@linkplain Component @Component}(api = <span class="hl2">CreatedComponent</span>.class)
 * {@linkplain org.fluidity.composition.Component.Context @Component.Context}(<span class="hl3">CustomContext</span>.class)
 * final class CustomFactory implements <span class="hl1">{@linkplain ComponentFactory}</span> {
 *
 *   public {@linkplain ComponentFactory.Instance Instance} resolve(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Resolver Resolver} dependencies) throws {@linkplain org.fluidity.composition.ComponentContainer.ResolutionException} { {
 *     dependencies.discover(<span class="hl2">CreatedComponent</span>.class);
 *
 *     return new Instance() {
 *       public void bind(final {@linkplain ComponentFactory.Registry Registry} registry) throws {@linkplain org.fluidity.composition.ComponentContainer.ResolutionException} {
 *         registry.bindComponent(<span class="hl2">CreatedComponent</span>.class);
 *       }
 *     };
 *   }
 * }
 *
 * {@linkplain org.fluidity.composition.Component.Context @Component.Context}(<span class="hl3">CustomContext</span>.class)
 * final class <span class="hl2">CreatedComponent</span> {
 *
 *   public <span class="hl2">CreatedComponent</span>(final {@linkplain ComponentContext} context) {
 *     final <span class="hl3">CustomContext</span>[] contexts = context.annotations(<span class="hl3">CustomContext</span>.class);
 *     &hellip;
 *   }
 * }
 * </pre></li>
 * </ol>
 */
public interface ComponentFactory {

    /**
     * Informs the caller about the static dependencies the created component has in the given context. Actual component instantiation does _not_ take place in
     * this method.
     * <p/>
     * The caller is informed about the static dependencies of the component by calls to methods of the {@link ComponentFactory.Resolver resolver} parameter.
     * <p/>
     * In the general case, a call to {@link ComponentFactory.Resolver#discover(Class)} will find the injectable constructor of the given type – the component
     * implementation class – and inform the caller about the dependencies found therein. In case some of those declared dependencies don't match the type of
     * the actual object injected by the factory to the component instance, further calls to {@link ComponentFactory.Resolver#resolve(Class, Type,
     * Annotation[])} should be made, one for each such dependency.
     * <p/>
     * If the general case above does not apply, more specific methods of the {@link ComponentFactory.Resolver} class can be used to make sure the caller is
     * informed about all dependencies of the component instance.
     * <p/>
     * The {@link ComponentFactory.Instance#bind(ComponentFactory.Registry) ComponentFactory.Instance.bind()} method of the returned object will be invoked, at
     * the appropriate time, to actually bind the created component, and its local, internal dependencies to a container. Actual instantiation should, when
     * possible at all, be left to the caller of that method. If the factory absolutely has to instantiate the component on account of, for instance, having to
     * call an external factory, then that instantiation must not take place in this method but in {@link
     * ComponentFactory.Instance#bind(ComponentFactory.Registry) ComponentFactory.Instance.bind()}.
     * <p/>
     * The following code snippet shows the basic pattern of the implementation:
     * <pre>
     * public {@linkplain ComponentFactory.Instance Instance} <span class="hl1">resolve</span>(final {@linkplain ComponentContext} context, final {@linkplain
     * ComponentFactory.Resolver Resolver} dependencies) throws {@linkplain org.fluidity.composition.ComponentContainer.ResolutionException} {
     *   dependencies.<span class="hl2">discover</span>(<span class="hl3">CreatedComponent</span>.class);
     *
     *   return new {@linkplain ComponentFactory.Instance Instance}() {
     *     public void bind(final {@linkplain ComponentFactory.Registry Registry} registry) throws {@linkplain org.fluidity.composition.ComponentContainer.ResolutionException} {
     *       registry.bindComponent(<span class="hl3">CreatedComponent</span>.class);
     *     }
     *   }
     * }
     * </pre>
     * <p/>
     * The following code snippet shows the pattern of the implementation in case it has to call the component's constructor:
     * <pre>
     * public {@linkplain ComponentFactory.Instance Instance} <span class="hl1">resolve</span>(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Resolver Resolver} dependencies) throws {@linkplain org.fluidity.composition.ComponentContainer.ResolutionException} {
     *   final Dependency&lt;?>[] <span class="hl2">parameters</span> = dependencies.discover(<span class="hl3">CreatedComponent</span>.class);
     *
     *   return new {@linkplain ComponentFactory.Instance Instance}() {
     *     public void bind(final {@linkplain ComponentFactory.Registry Registry} registry) throws {@linkplain org.fluidity.composition.ComponentContainer.ResolutionException} {
     *       registry.bindInstance(new <span class="hl3">CreatedComponent</span>((Dependency1) <span class="hl2">parameters</span>[0], (Dependency2) <span class="hl2">parameters</span>[2], "some text", 5);
     *     }
     *   }
     * }
     * </pre>
     * <p/>
     * The following code snippet shows the pattern of the implementation in case it has to call some external factory:
     * <pre>
     * public {@linkplain ComponentFactory.Instance Instance} <span class="hl1">resolve</span>(final {@linkplain ComponentContext} context, final {@linkplain
     * ComponentFactory.Resolver Resolver} dependencies) throws {@linkplain org.fluidity.composition.ComponentContainer.ResolutionException} {
     *   final Dependency&lt;Dependency1> <span class="hl2">dependency1</span> = dependencies.resolve(Dependency1.class);
     *   final Dependency&lt;Dependency2> <span class="hl2">dependency2</span> = dependencies.resolve(Dependency2.class);
     *
     *   return new {@linkplain ComponentFactory.Instance Instance}() {
     *     public void bind(final {@linkplain ComponentFactory.Registry Registry} registry) throws {@linkplain org.fluidity.composition.ComponentContainer.ResolutionException} {
     *       registry.bindInstance(external.create(<span class="hl2">dependency1</span>.instance(), <span class="hl2">dependency2</span>.instance(), "some text", 5);
     *     }
     *   }
     * }
     * </pre>
     *
     * @param context      the context under which component creation will take place. This context contains only those annotations that this factory lists in
     *                     its {@link org.fluidity.composition.Component.Context @Component.Context} annotation.
     * @param dependencies the dependency resolver to notify of any dependencies the created class will have.
     *
     * @return an object that will bind the created components and its dependencies, or <code>null</code> if no component will be instantiated.
     *
     * @throws ComponentContainer.ResolutionException
     *          when resolution of the component or any of its dependencies fails.
     */
    Instance resolve(ComponentContext context, Resolver dependencies) throws ComponentContainer.ResolutionException;

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
         * @throws ComponentContainer.BindingException
         *          when binding fails.
         */
        void bind(Registry registry) throws ComponentContainer.BindingException;
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
    interface Resolver {

        /**
         * Resolves a component by its component interface without instantiating the component. The resolved component will see, if it accepts the {@link
         * org.fluidity.composition.Component.Reference @Component.Reference} context annotation, the specified component interface as the dependency reference
         * to it unless the <code>reference</code> parameter is present. The component interface must be assignable to the reference if it is specified.
         *
         * @param api         the component interface to resolve; may not be <code>null</code>.
         * @param constructor constructor to derive context annotations from.
         * @param parameter   index of the constructor parameter to derive generic type and context annotations from.
         *
         * @return an object that can return an instance of the resolved dependency.
         */
        <T> Dependency<T> resolve(Class<T> api, Constructor<T> constructor, int parameter);

        /**
         * Resolves a component by its component interface without instantiating the component. The resolved component will see, if it accepts the {@link
         * org.fluidity.composition.Component.Reference @Component.Reference} context annotation, the specified component interface as the dependency reference
         * to it unless the <code>reference</code> parameter is present. The component interface must be assignable to the reference if it is specified.
         *
         * @param api       the component interface to resolve; may not be <code>null</code>.
         * @param type      the component class to resolve the dependency of; used to derive context annotations from; may be <code>null</code>, in which case
         *                  the method's declaring class will be used.
         * @param method    method to derive context annotations from.
         * @param parameter index of the constructor parameter to derive generic type and context annotations from.
         *
         * @return an object that can return an instance of the resolved dependency.
         */
        <T> Dependency<T> resolve(Class<T> api, Class<?> type, Method method, int parameter);

        /**
         * Resolves a component by its component interface without instantiating the component. The resolved component will see, if it accepts the {@link
         * org.fluidity.composition.Component.Reference @Component.Reference} context annotation, the specified component interface as the dependency reference
         * to it unless the <code>reference</code> parameter is present. The component interface must be assignable to the reference if it is specified.
         *
         * @param api   the component interface to resolve; may not be <code>null</code>.
         * @param type  the component class to resolve the dependency of; used to derive context annotations from; may be <code>null</code>, in which case
         *              the field's declaring class will be used.
         * @param field field to derive generic type and context annotations from.
         *
         * @return an object that can return an instance of the resolved dependency.
         */
        <T> Dependency<T> resolve(Class<T> api, Class<?> type, Field field);

        /**
         * Resolves a component by its component interface without instantiating the component. The resolved component will see, if it accepts the {@link
         * org.fluidity.composition.Component.Reference @Component.Reference} context annotation, the specified component interface as the dependency reference
         * to it unless the <code>reference</code> parameter is present. The component interface must be assignable to the reference if it is specified.
         *
         * @param api         the component interface to resolve, or <code>null</code> of it can be derived from the <code>reference</code> parameter.
         * @param reference   the reference to use when resolving the component or <code>null</code> to use the component interface.
         * @param annotations the annotations at the point of reference to the dependency; may be <code>null</code>.
         *
         * @return an object that can return an instance of the resolved dependency.
         *
         * @deprecated This is a low level method in case none of the {@link #resolve(Class, Constructor, int)}, {@link #resolve(Class}, or {@link
         *             #resolve(Class} satisfies your needs. Please favor those methods to this one.
         */
        <T> Dependency<T> resolve(Class<T> api, Type reference, Annotation[] annotations);

        /**
         * Informs the recipient about the injectable constructor parameters of the given component class, and then returns a list of dependencies, each
         * corresponding to the parameters of the dependency injectable constructor of the supplied component class. For the algorithm on constructor
         * resolution, see the {@link org.fluidity.composition.container.DependencyInjector#constructor(Class, DependencyGraph.Traversal, DependencyResolver,
         * ContextNode, org.fluidity.composition.container.ContextDefinition, Constructor) DependencyInjector.constructor()} method.
         * <p/>
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
         * <p/>
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
         * @param bindings the component bindings for the local dependencies of the component.
         *
         * @return a container to instantiate local dependencies.
         */
        Container local(Container.Bindings bindings);
    }

    /**
     * Registry to bind components into by a {@link ComponentFactory}. Methods of this interface are to be invoked from the {@link
     * ComponentFactory.Instance#bind(ComponentFactory.Registry) bind()} method of the {@link ComponentFactory.Instance} returned from the factory's {@link
     * ComponentFactory#resolve(ComponentContext, ComponentFactory.Resolver) resolve()} method.
     * <p/>
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
     * {@linkplain Component @Component}(api = MyComponent<.class)
     * final class MyComponentFactory implements {@linkplain ComponentFactory} {
     *
     *   public {@linkplain ComponentFactory.Instance Instance} resolve(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Resolver Resolver} dependencies) throws {@linkplain org.fluidity.composition.ComponentContainer.ResolutionException} {
     *     final Constructor&lt;?> <span class="hl2">constructor</span> = dependencies.<span class="hl1">constructor</span>(<span class="hl3">MyComponentImpl</span>.class);
     *
     *     dependencies.<span class="hl1">discover</span>(<span class="hl2">constructor</span>);
     *
     *     final <span class="hl1">Container</span> container = dependencies.<span class="hl1">local</span>(<span class="hl2">MyComponentImpl</span>.class, new {@linkplain Container.Bindings}() {
     *       public void bindComponents(final {@linkplain Container.Registry} registry) {
     *         registry.<span class="hl1">bindComponent</span>(<span class="hl3">LocalDependency</span>.class);
     *       }
     *     }));
     *
     *     final Dependency&lt;<span class="hl3">LocalDependency</span>> <span class="hl3">dependency</span> = container.<span class="hl1">resolve</span>(<span class="hl2">constructor</span>, 0, <span class="hl3">LocalDependency</span>.class);
     *
     *     return new {@linkplain ComponentFactory.Instance Instance}() {
     *       public void bind(final {@linkplain ComponentFactory.Registry Registry} registry) throws {@linkplain org.fluidity.composition.ComponentContainer.ResolutionException} {
     *         registry.bindComponent(new <span class="hl2">MyComponentImpl</span>(<span class="hl3">dependency</span>.<span class="hl1">instance</span>()));
     *       }
     *     }
     *   }
     * }
     *
     * {@linkplain Component @Component}(automatic = false)
     * final class <span class="hl2">MyComponentImpl</span> implements MyComponent {
     *
     *   public <span class="hl2">MyComponentImpl</span>(final <span class="hl3">LocalDependency</span> local) {
     *     &hellip;
     *   }
     *
     *   &hellip;
     * }
     *
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
         * Returns the component instance bound to the given component interface.
         *
         * @param api         the component interface.
         * @param constructor the constructor of the component this binding is intended to resolve.
         * @param parameter   the index in the <code>constructor</code>'s parameter array of the dependency that this binding is intended to resolve.
         *
         * @return the component instance bound to the given component interface, or <code>null</code> if none found.
         */
        <T> Dependency<T> resolve(Class<T> api, Constructor<?> constructor, int parameter);

        /**
         * Returns the component instance bound to the given component interface.
         *
         * @param api       the component interface.
         * @param type      the component class to resolve the dependency of; used to derive context annotations from; may be <code>null</code>, in which case
         *                  the method's declaring class will be used.
         * @param method    the method of the factory this binding is intended to resolve.
         * @param parameter the index in the <code>constructor</code>'s parameter array of the dependency that this binding is intended to resolve.
         *
         * @return the component instance bound to the given component interface, or <code>null</code> if none found.
         */
        <T> Dependency<T> resolve(Class<T> api, Class<?> type, Method method, int parameter);

        /**
         * Returns the component instance bound to the given component interface.
         *
         * @param api   the component interface.
         * @param type  the component class to resolve the dependency of; used to derive context annotations from; may be <code>null</code>, in which case
         *              the field's declaring class will be used.
         * @param field the field that this binding is intended to resolve.
         *
         * @return the component instance bound to the given component interface, or <code>null</code> if none found.
         */
        <T> Dependency<T> resolve(Class<T> api, Class<?> type, Field field);

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
             */
            void bindComponents(Registry registry);
        }
    }
}
