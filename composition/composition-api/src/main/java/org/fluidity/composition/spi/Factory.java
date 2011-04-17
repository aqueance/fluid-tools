package org.fluidity.composition.spi;

import java.lang.reflect.Constructor;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;

/**
 * Defines the behaviour of component factories. Component creation follows a certain protocol and {@link Factory} objects must follow that protocol to
 * integrate well to Fluid Tools. This interface defines the necessary methods and interfaces for that integration.
 */
public interface Factory {

    /**
     * The factory in this method simply lets the caller know what dependencies the created component will have in the given context. Actual component
     * instantiation does not take place in this method. The  {@link Instance#bind(Registry)} method of the returned object will be invoked, at the right time,
     * to actually bind the created component, and its local, internal dependencies to a registry. Actual instantiation should, when possible at all, left to
     * the caller of that method. If a factory absolutely has to instantiate the component on account of, for instance, having to call an external factory,
     * that instantiation must not take place in this method but in {@link Instance#bind(Registry)}.
     * <p/>
     * The following boilerplate demonstrate the basic pattern of the implementation:
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
     * The following boilerplate demonstrate the pattern of the implementation in case it has to call the component's constructor:
     * <pre>
     * public Instance resolve(final Resolver dependencies, final ComponentContext context) throws ComponentContainer.ResolutionException {
     *     final Dependency<?>[] parameters = dependencies.discover(CreatedComponent.class);
     *
     *     return new Instance() {
     *         public void bind(final Registry registry) throws ComponentContainer.ResolutionException {
     *             registry.bindInstance(new CreatedComponent((Dependency1) parameters[0], (Dependency1) parameters[2], "some text", 5);
     *         }
     *     }
     * }
     * </pre>
     * <p/>
     * The following boilerplate demonstrate the pattern of the implementation in case it has to call some external factory:
     * <pre>
     * public Instance resolve(final Resolver dependencies, final ComponentContext context) throws ComponentContainer.ResolutionException {
     *     final Dependency<Dependency1> dependency1 = dependencies.resolve(Dependency1.class);
     *     final Dependency<Dependency2> dependency2 = dependencies.resolve(Dependency2.class);
     *
     *     return new Instance() {
     *         public void bind(final Registry registry) throws ComponentContainer.ResolutionException {
     *             registry.bindInstance(external.create(dependency1.instance(), dependency2.instance(), "some text", 5);
     *         }
     *     }
     * }
     * </pre>
     *
     * @param dependencies the dependency resolver to notify of any dependencies the created class will have.
     * @param context      the context under which component creation will take place. This context contains only those annotations that the implementing class
     *                     lists in its {@link org.fluidity.composition.Context} annotation.
     *
     * @return an object that will bind the created components and its dependencies.
     *
     * @throws ComponentContainer.ResolutionException
     *          when resolution of the component or any of its dependencies fails.
     */
    Instance resolve(Resolver dependencies, ComponentContext context) throws ComponentContainer.ResolutionException;

    /**
     * Represents an instance of the created component.
     */
    interface Instance {

        /**
         * Binds the created component and all dependencies thereof to the supplied registry.
         *
         * @param registry the registry to bind components in.
         *
         * @throws ComponentContainer.BindingException
         *          when binding fails.
         */
        void bind(Registry registry) throws ComponentContainer.BindingException;
    }

    /**
     * Represents a resolved dependency of the component being created by the {@link Factory} in its {@link Factory#resolve(Resolver, ComponentContext)} method.
     *
     * @param <T> the type of the dependency.
     */
    interface Dependency<T> {

        /**
         * Returns an actual instance of the dependency.
         *
         * @return an actual instance of the dependency; may be <code>null</code>.
         */
        T instance();
    }

    /**
     * Resolves dependencies without instantiating them.
     */
    interface Resolver {

        /**
         * Resolves a component known by its component interface.
         *
         * @param api the component interface to resolve.
         * @param <T> the component interface to resolve.
         *
         * @return an object that can return an instance of the resolved dependency.
         */
        <T> Dependency<T> resolve(Class<T> api);

        /**
         * Returns a list of dependencies, each corresponding to the dependency injectable constructor of the supplied component class. For the algorithm on
         * constructor resolution, see the <code>DependencyInjector.constructor()</code> method.
         *
         * @param type the component class.
         *
         * @return a list of dependencies corresponding to the constructor parameters.
         */
        Dependency<?>[] discover(Class<?> type);

        /**
         * Returns a list of dependencies, each corresponding to the supplied constructor.
         *
         * @param constructor the constructor.
         *
         * @return a list of dependencies corresponding to the constructor parameters.
         */
        Dependency<?>[] discover(Constructor<?> constructor);
    }

    /**
     * Registry to bind components in.
     */
    interface Registry {

        /**
         * @param implementation see {@link ComponentContainer.Registry#bindComponent(Class, Class[])}.
         * @param interfaces     see {@link ComponentContainer.Registry#bindComponent(Class, Class[])}.
         *
         * @throws org.fluidity.composition.ComponentContainer.BindingException
         *          see {@link ComponentContainer.Registry#bindComponent(Class, Class[])}.
         */
        <T> void bindComponent(Class<T> implementation, Class<? super T>... interfaces) throws ComponentContainer.BindingException;

        /**
         * @param instance   see {@link ComponentContainer.Registry#bindInstance(Object, Class[])}.
         * @param interfaces see {@link ComponentContainer.Registry#bindInstance(Object, Class[])}.
         *
         * @throws ComponentContainer.BindingException
         *          see {@link ComponentContainer.Registry#bindInstance(Object, Class[])}.
         */
        <T> void bindInstance(T instance, Class<? super T>... interfaces) throws ComponentContainer.BindingException;

        /**
         * Returns the registry of a new child container that will use this registry as its parent. See {@link ComponentContainer#makeChildContainer()}.
         *
         * @return an registry, never <code>null</code>.
         */
        Registry makeChildContainer();
    }
}
