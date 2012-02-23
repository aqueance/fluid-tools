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

package org.fluidity.composition.spi;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;

/**
 * Creates instances of a component when mere constructor and field injection is not adequate. You don't directly use this interface but use {@link
 * CustomComponentFactory} or {@link ComponentVariantFactory} instead.
 * <p/>
 * Component instantiation follows a certain protocol and <code>ComponentFactory</code> objects must follow that protocol to integrate well to Fluid Tools.
 * This interface defines the necessary methods and interfaces for that integration.
 * <p/>
 * The protocol is as follows:
 * <ul>
 * <li>The {@link #resolve(ComponentContext, ComponentFactory.Resolver)} method is called with the context for the new component</li>
 * <li>The receiver uses the provided {@link ComponentFactory.Resolver resolver} to resolve the dependencies of the to be new component without actually
 * instantiating anything.</li>
 * <li>At some point later, the {@link ComponentFactory.Instance#bind(ComponentFactory.Registry)} method is invoked on the returned object to bind, in a
 * transient container, the component to be instantiated and its local dependencies.</li>
 * <li>At some point later, the transient container is asked for the component by the component interface the factory is {@linkplain
 * org.fluidity.composition.Component bound against}.</li>
 * </ul>
 * <h3>Usage</h3>
 * You use this interface via one of its sub-interfaces: {@link CustomComponentFactory} or {@link ComponentVariantFactory}.
 */
public interface ComponentFactory {

    /**
     * Informs the caller about the static dependencies the created component has in the given context. Actual component instantiation does not take place in
     * this method. The {@link ComponentFactory.Instance#bind(ComponentFactory.Registry)} method of the returned object will be invoked, at the right time, to
     * actually bind the created component, and its local, internal dependencies to a registry. Actual instantiation should, when possible at all, be left to
     * the caller of that method. If the factory absolutely has to instantiate the component on account of, for instance, having to call an external factory,
     * then that instantiation must not take place in this method but in {@link ComponentFactory.Instance#bind(ComponentFactory.Registry)}.
     * <p/>
     * The following boilerplate demonstrates the basic pattern of the implementation:
     * <pre>
     * public Instance resolve(final Resolver dependencies, final ComponentContext context) throws ComponentContainer.ResolutionException {
     *     dependencies.discover(CreatedComponent.class);
     *
     *     return new Instance() {
     *         public void bind(final Registry registry) throws ComponentContainer.ResolutionException {
     *             registry.bindComponent(CreatedComponent.class);
     *         }
     *     }
     * }
     * </pre>
     * <p/>
     * The following boilerplate demonstrates the pattern of the implementation in case it has to call the component's constructor:
     * <pre>
     * public Instance resolve(final Resolver dependencies, final ComponentContext context) throws ComponentContainer.ResolutionException {
     *     final Dependency&lt;?>[] parameters = dependencies.discover(CreatedComponent.class);
     *
     *     return new Instance() {
     *         public void bind(final Registry registry) throws ComponentContainer.ResolutionException {
     *             registry.bindInstance(new CreatedComponent((Dependency1) parameters[0], (Dependency1) parameters[2], "some text", 5);
     *         }
     *     }
     * }
     * </pre>
     * <p/>
     * The following boilerplate demonstrates the pattern of the implementation in case it has to call some external factory:
     * <pre>
     * public Instance resolve(final Resolver dependencies, final ComponentContext context) throws ComponentContainer.ResolutionException {
     *     final Dependency&lt;Dependency1> dependency1 = dependencies.resolve(Dependency1.class);
     *     final Dependency&lt;Dependency2> dependency2 = dependencies.resolve(Dependency2.class);
     *
     *     return new Instance() {
     *         public void bind(final Registry registry) throws ComponentContainer.ResolutionException {
     *             registry.bindInstance(external.create(dependency1.instance(), dependency2.instance(), "some text", 5);
     *         }
     *     }
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
     * Represents a future instance of the component a {@link ComponentFactory} is a factory for.
     */
    interface Instance {

        /**
         * Binds the created component and all its local dependencies, e.g., those not found in the application's containers by design, to the supplied
         * registry.
         * <p/>
         * Note: if the created component is context aware, the {@link ComponentFactory} must extract from the context received in {@link
         * ComponentFactory#resolve(ComponentContext, ComponentFactory.Resolver)} all context annotations declared in its {@link
         * org.fluidity.composition.Component.Context @Component.Context} annotations and, if not <code>null</code>, bind them in the <code>registry</code>
         * parameter of this method, and the component itself must declare an {@link org.fluidity.composition.Optional @Optional} array dependency on each
         * context annotation that it accepts rather than depending on {@link ComponentContext} and extracting those context annotations therefrom. For
         * example:
         * <pre>
         * &#64;Component(api = CreatedComponent.class)
         * &#64;Component.Context(CustomContext.class)
         * final class CustomFactory implements CustomComponentFactory {
         *     public Instance resolve(final ComponentContext context, final Resolver dependencies) throws ComponentContainer.ResolutionException {
         *         final CustomContext[] contexts = context.annotations(CustomContext.class);
         *
         *         dependencies.discover(CreatedComponent.class);
         *
         *         return new Instance() {
         *
         *             &#64;SuppressWarnings("unchecked")
         *             public void bind(final Registry registry) throws ComponentContainer.BindingException {
         *                 if (contexts != null) {
         *                     registry.bindInstance(contexts, CreatedContext[].class);
         *                 }
         * I
         *                 registry.bindComponent(CreatedComponent.class);
         *             }
         *         };
         *     }
         * }
         *
         * final class CreatedComponent {
         *
         *     public CreatedComponent(final @Optional CustomContext[] contexts) {
         *         ....
         *     }
         * }
         * </pre>
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
     *
     * @param <T> the type of the dependency.
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
     */
    interface Resolver {

        /**
         * Resolves a component by its component interface without instantiating the component. The resolved component will see, if it accepts the {@link
         * org.fluidity.composition.Component.Reference @Component.Reference} context annotation, the specified component interface as the dependency reference
         * to it unless <code>reference</code> parameter is present. The component interface must be assignable to the reference if it is specified.
         *
         * @param api       the component interface to resolve, or <code>null</code> of it can be derived from the <code>reference</code> parameter.
         * @param reference the reference to use when resolving the component or <code>null</code> to use the component interface.
         *
         * @return an object that can return an instance of the resolved dependency.
         */
        <T> Dependency<T> resolve(Class<T> api, Type reference);

        /**
         * Returns a list of dependencies, each corresponding to the parameters of the dependency injectable constructor of the supplied component class. For
         * the algorithm on constructor resolution, see the <code>DependencyInjector.constructor()</code> method.
         *
         * @param type the component class.
         *
         * @return a list of dependencies corresponding to the constructor parameters.
         */
        Dependency<?>[] discover(Class<?> type);

        /**
         * Returns a list of dependencies, each corresponding to the parameters of the supplied constructor.
         *
         * @param constructor the constructor.
         *
         * @return a list of dependencies corresponding to the parameters of the constructor parameters.
         */
        Dependency<?>[] discover(Constructor<?> constructor);


        /**
         * Returns a list of dependencies, each corresponding to the parameters of the supplied factory method.
         *
         * @param method the constructor.
         *
         * @return a list of dependencies corresponding to the parameters of the supplied factory method.
         */
        Dependency<?>[] discover(Method method);
    }

    /**
     * Registry to bind components in by a {@link ComponentFactory}. Methods of this interface are to be invoked from the {@link
     * ComponentFactory.Instance#bind(ComponentFactory.Registry) bind()} method of the <code>Instance</code> returned from the factory's {@link
     * ComponentFactory#resolve(ComponentContext, ComponentFactory.Resolver) resolve()} method.
     * <p/>
     * The intent for this interface is to restrict access to a {@link org.fluidity.composition.ComponentContainer.Registry} object through a smaller set
     * of methods.
     */
    interface Registry {

        /**
         * Binds a component class to a list of component interfaces in the container behind this registry.
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
         * Binds a component instance to a list of component interfaces in the container behind this registry.
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

        /**
         * Returns the registry of a new child container that will use this registry as its parent. See {@link
         * ComponentContainer#makeChildContainer(ComponentContainer.Bindings...)}.
         *
         * @return an registry, never <code>null</code>.
         */
        Registry makeChildContainer();
    }
}
