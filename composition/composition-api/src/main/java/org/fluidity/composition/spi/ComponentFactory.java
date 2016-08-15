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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.foundation.Methods;

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
 * <li>The {@link #resolve(ComponentContext, Container) ComponentFactory.resolve()} method is invoked.</li>
 * <li>The receiver uses the provided {@link Container container} to discover and resolve the created component and its dependencies <em>without</em> actually
 * instantiating any component, and returns a future instance of the created component.</li>
 * <li>The {@link Instance#bind(ComponentFactory.Registry) ComponentFactory.Instance.bind()} method is invoked, at a later time, on the returned future
 * component instance.</li>
 * </ul>
 * <h3>Usage</h3>
 * <h4>Basic Pattern</h4>
 * <pre>
 * {@linkplain Component @Component}(api = <span class="hl2">MyComponent</span>.class)
 * final class MyComponentFactory implements <span class="hl1">{@linkplain ComponentFactory}</span> {
 *
 *   public {@linkplain ComponentFactory.Instance Instance} <span class="hl1">resolve</span>(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Container Container} dependencies) throws Exception {
 *     return dependencies.instance(<span class="hl3">MyComponentImpl</span>.class);
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
 * Context adaption may be necessary when you need to pass some Fluid Tools {@link ComponentContext context}, collected from a list of {@link
 * org.fluidity.composition.Qualifier qualifiers}, to a component not managed by Fluid Tools. The basic mechanism is:<ul>
 *     <li>the factory collects the context qualifiers,</li>
 *     <li>then passes them on directly as dependencies to the created component.</li>
 * </ul>
 * <pre>
 * {@linkplain Component @Component}(api = <span class="hl2">CreatedComponent</span>.class)
 * {@linkplain org.fluidity.composition.Component.Qualifiers @Component.Qualifiers}(<span class="hl3">CustomQualifier</span>.class)
 * final class CustomFactory implements <span class="hl1">{@linkplain ComponentFactory}</span> {
 *
 *   public {@linkplain ComponentFactory.Instance Instance} resolve(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Container Container} dependencies) throws Exception { {
 *     final <span class="hl3">CustomQualifier</span>[] contexts = context.qualifiers(<span class="hl3">CustomQualifier</span>.class);
 *
 *     return dependencies.instance(<span class="hl2">CreatedComponent</span>.class, {@linkplain ComponentFactory.Registry registry} -&gt; {
 *       registry.bindInstance(contexts, <span class="hl3">CustomQualifier</span>[].class);
 *       registry.bindComponent(<span class="hl2">CreatedComponent</span>.class);
 *     });
 *   }
 * }
 * </pre>
 * <pre>
 * public final class <span class="hl2">CreatedComponent</span> {
 *
 *   <span class="hl2">CreatedComponent</span>(final <span class="hl2">CustomQualifier</span>[] contexts) {
 *     &hellip;
 *   }
 * }
 * </pre>
 *
 * For more examples, see the {@link #resolve(ComponentContext, Container)} method.
 */
public interface ComponentFactory {

    /**
     * Resolves and discovers the dependencies of the component created by this factory. Actual component instantiation will only take place when {@link
     * Instance#bind(ComponentFactory.Registry) bind} is invoked on the returned object.
     * <p>
     * The static dependencies of the component are discovered by calls to methods of the {@link ComponentFactory.Container dependencies} argument, the simplest
     * of which are {@link Container#instance(Class) instance(Class)} and {@link Container#instance(Class, ComponentFactory.Bindings) instance(Class, Bindings)}.
     * <p>
     * More elaborate factory logic may require acquisition of a {@link ComponentFactory.Resolver resolver} via {@link Container#resolver()},
     * {@link Container#resolver(Class)}, or {@link Container#resolver(Class, ComponentFactory.Bindings)}, and then calls to {@link
     * ComponentFactory.Resolver#discover(Class) discover(Class)} to resolve dependencies of specific component classes, and even more elaborate cases may
     * require other, more specific, calls to {@link ComponentFactory.Resolver}.
     * <p>
     * The {@link ComponentFactory.Instance#bind(ComponentFactory.Registry) ComponentFactory.Instance.bind()} method of the returned object will be invoked, at
     * the appropriate time, to actually bind the created component, and its local, internal dependencies to a container. Actual instantiation should, when
     * possible at all, be left to the caller of that method. If the factory absolutely has to instantiate the component on account of, for instance, having to
     * call an external factory, then that instantiation should only take place in {@link
     * ComponentFactory.Instance#bind(ComponentFactory.Registry) ComponentFactory.Instance.bind()}.
     * <h3>Usage</h3>
     * The {@link ComponentFactory} shows the basic pattern of the implementation.
     * <p>
     * The following code snippet shows the pattern of the implementation in case it has to call the component's constructor:
     * <pre>
     * public {@linkplain ComponentFactory.Instance Instance} <span class="hl1">resolve</span>(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Container Container} dependencies) throws Exception {
     *   final {@linkplain ComponentFactory.Resolver Resolver} resolver = dependencies.resolver(<span class="hl2">CreatedComponent</span>.class);
     *   final Constructor&lt;?&gt; <span class="hl2">constructor</span> = resolver.{@linkplain ComponentFactory.Resolver#constructor(Class) constructor}(<span class="hl2">CreatedComponent</span>.class);
     *   final Dependency&lt;?&gt;[] <span class="hl3">arguments</span> = resolver.{@linkplain ComponentFactory.Resolver#resolve(Constructor, int) resolve}(<span class="hl2">constructor</span>);
     *
     *   // make sure the assumptions implicit in the code below
     *   // are not broken by changes to MyComponentImpl:
     *   if (false) {
     *     final MyComponent result = new <span class="hl2">CreatedComponent</span>((Dependency1) null,
     *                                                     (Dependency2) null,
     *                                                     (String) null,
     *                                                     0);
     *   }
     *
     *   <span class="hl3">arguments</span>[2] = resolver.{@linkplain ComponentFactory.Resolver#constant(Object) constant}("some text");
     *   <span class="hl3">arguments</span>[3] = resolver.{@linkplain ComponentFactory.Resolver#constant(Object) constant}(5);
     *
     *   return {@linkplain Instance Instance}.{@linkplain Instance#of(Class, ComponentFactory.Bindings) of}(<span class="hl2">CreatedComponent</span>.class,
     *                      {@linkplain ComponentFactory.Registry registry} -&gt; registry.bindInstance(<span class="hl2">constructor</span>.{@linkplain Constructor#newInstance(Object...) newInstance}(resolver.{@linkplain ComponentFactory.Resolver#instantiate(org.fluidity.composition.spi.Dependency[]) instantiate}(<span class="hl3">arguments</span>))));
     * }
     * </pre>
     * <p>
     * The following code snippet shows the pattern of the implementation in case it has to call some external factory:
     * <pre>
     * public {@linkplain ComponentFactory.Instance Instance} <span class="hl1">resolve</span>(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Container Container} dependencies) throws Exception {
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
     *
     *   final {@linkplain ComponentFactory.Resolver Resolver} resolver = dependencies.resolver(<span class="hl2">external</span>.getClass());
     *   final Dependency&lt;?&gt;[] <span class="hl3">arguments</span> = resolver.{@linkplain ComponentFactory.Resolver#resolve(Class, Method) resolve}(<span class="hl2">external</span>.getClass(), <span class="hl2">method</span>);
     *
     *   <span class="hl3">arguments</span>[2] = resolver.{@linkplain ComponentFactory.Resolver#constant(Object) constant}("some text");
     *   <span class="hl3">arguments</span>[3] = resolver.{@linkplain ComponentFactory.Resolver#constant(Object) constant}(5);
     *
     *   return {@linkplain Instance Instance}.{@linkplain Instance#of(Class, ComponentFactory.Bindings) of}(method.getReturnedType(),
     *                      {@linkplain ComponentFactory.Registry registry} -&gt; registry.bindInstance(resolver.{@linkplain ComponentFactory.Resolver#invoke(Object, Method, Dependency[])  invoke}(<span class="hl2">external</span>.getClass(), <span class="hl2">method</span>, <span class="hl3">arguments</span>)));
     * }
     * </pre>
     *
     * See {@link org.fluidity.foundation.Methods#get(Class, Methods.Selector)} to see how you can get an <em>interface</em> method in a refactoring friendly
     * manner.
     *
     * @param context      the context under which component creation will take place. This context contains only those annotations that this factory lists in
     *                     its {@link Component.Qualifiers @Component.Qualifiers} annotation.
     * @param dependencies the dependency resolver to notify of any dependencies the created class will have.
     *
     * @return an object that will bind the created components and its dependencies, or <code>null</code> if no component will be instantiated.
     *
     * @throws Exception when an error occurs.
     */
    Instance resolve(ComponentContext context, Container dependencies) throws Exception;

    /**
     * Bindings for a component created by a {@link ComponentFactory}. Can be passed as a Lambda expression to {@link
     * Instance#of(Class, ComponentFactory.Bindings)} or {@link Container#instance(Class, ComponentFactory.Bindings)}.
     * <h3>Usage</h3>
     * See {@link ComponentFactory}.
     *
     * @author Tibor Varga
     */
    @FunctionalInterface
    interface Bindings {

        /**
         * Binds the component to be created, and all its local dependencies, e.g., those not found in the application's containers <em>by design</em>, to the
         * supplied registry.
         *
         * @param registry the registry to bind components in.
         *
         * @throws Exception when an error occurs.
         */
        void bind(Registry registry) throws Exception;
    }

    /**
     * Represents a future component instance created by a {@link ComponentFactory}.
     * <h3>Usage</h3>
     * See {@link ComponentFactory}.
     *
     * @author Tibor Varga
     */
    interface Instance extends Bindings {

        /**
         * Returns the class of the component instance this object represents.
         *
         * @return a class object; never <code>null</code>.
         */
        Class<?> type();

        /**
         * Creates a new {@link Instance} with the given type and bindings.
         *
         * @param type    supplies the component class; never <code>null</code>.
         * @param bindings binds the component to a registry.
         *
         * @return a new {@link Instance} object; never <code>null</code>.
         */
        static Instance of(final Class<?> type, final Bindings bindings) {
            return new Instance() {
                @Override
                public Class<?> type() {
                    return type;
                }

                @Override
                public void bind(final Registry registry) throws Exception {
                    bindings.bind(registry);
                }
            };
        }
    }

    /**
     * Registry to bind components to by a {@link ComponentFactory}. Used by {@link Bindings} passed to {@link
     * Container#instance(Class, ComponentFactory.Bindings)} and {@link Instance#bind(ComponentFactory.Registry)}.
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
        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
        <T> void bindInstance(T instance, Class<? super T>... interfaces) throws ComponentContainer.BindingException;
    }

    /**
     * A {@link Resolver} that can create local {@link Resolver resolvers}.
     * <h3>Usage</h3>
     * The {@link ComponentFactory} shows the basic pattern of using this interface.
     * <p>
     * In case the component created by the factory is not a Fluid Tools component, a more elaborate pattern is required. In the below example,
     * <code>MyComponentImpl</code> is not a Fluid Tools component while its dependency, <code>LocalDependency</code>, is:
     * <pre>
     * {@linkplain Component @Component}(api = MyComponent.class)
     * final class MyComponentFactory implements {@linkplain ComponentFactory} {
     *
     *   public {@linkplain ComponentFactory.Instance Instance} resolve(final {@linkplain ComponentContext} context, final {@linkplain ComponentFactory.Container Container} dependencies) throws Exception {
     *     final {@linkplain ComponentFactory.Bindings} locals = {@linkplain ComponentFactory.Registry registry} -&gt; registry.<span class="hl1">bindComponent</span>(<span class="hl3">LocalDependency</span>.class);
     *     final <span class="hl1">Container</span> container = dependencies.<span class="hl1">resolver</span>(<span class="hl2">MyComponentImpl</span>.class, locals));
     *
     *     final Constructor&lt;?&gt; <span class="hl2">constructor</span> = <span class="hl3">MyComponentImpl</span>.class.getConstructor(&hellip;);
     *     final Dependency&lt;?&gt;[] <span class="hl3">arguments</span> = container.<span class="hl1">resolve</span>(null, <span class="hl2">constructor</span>);
     *
     *     return Instance.of(<span class="hl2">MyComponentImpl</span>.class, {@linkplain ComponentFactory.Registry registry} -&gt; {
     *       locals.bind(registry);
     *       registry.bindComponent(<span class="hl2">constructor</span>.newInstance(container.<span class="hl1">instantiate</span>(<span class="hl3">arguments</span>)));
     *     });
     *   }
     * }
     * </pre>
     * <pre>
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
     * If <code>LocalDependency</code> were not a Fluid Tools component either, rather binding its class directly, its dependencies would have to be discovered
     * using more specific methods of {@link ComponentFactory.Container}, similarly how <code>MyComponentImpl</code> was handled.
     */
    interface Container {

        /**
         * Convenience method to resolve and instantiate a local component. Calling this method on a <code>container</code> instance is equivalent to the
         * following sequence:
         * <pre>
         *   final Instance resolve(final ComponentContext context, Container dependencies) throws Exception {
         *     final Class type = &hellip;
         *     final {@linkplain ComponentFactory.Bindings} locals = &hellip;
         *
         *     dependencies.resolver(locals).discover(type);
         *
         *     return Instance.of(type, locals);
         *   }
         * </pre>
         *
         * @param type     the component class to add to resolve and instantiate.
         * @param bindings the local bindings necessary to instantiate the component.
         *
         * @return an object that will bind the created components and its dependencies.
         *
         * @throws Exception whatever <code>bindings</code> throw.
         */
        Instance instance(Class<?> type, Bindings bindings) throws Exception;

        /**
         * Convenience method to resolve and instantiate a local component. Calling this method on an instance, <code>container</code> is equivalent to the
         * following sequence:
         * <pre>
         *   final Instance resolve(final ComponentContext context, Container dependencies) throws Exception {
         *     final Class type = &hellip;
         *     return dependencies.instance(type, registry -&gt; registry.bindComponent(type));
         *   }
         * </pre>
         *
         * @param type the component class to add to resolve and instantiate.
         *
         * @return an object that will bind the created components and its dependencies.
         */
        Instance instance(Class<?> type);

        /**
         * Returns a resolver without any local dependencies bound. The returned container can be used to resolve dependencies of some component.
         *
         * @return a container to resolve dependencies.
         */
        Resolver resolver();

        /**
         * Returns a resolver without bound <code>type</code> bound as local dependency, but with {@link org.fluidity.composition.Qualifier component context}
         * derived from the given type. The returned container can be used to resolve dependencies of the given <code>type</code> or some other component.
         *
         * @param type component class to derive context from; may be <code>null</code>.
         *
         * @return a container to resolve dependencies.
         */
        Resolver resolver(Class<?> type);

        /**
         * Returns a resolver local dependencies bound by the given <code>bindings</code>. The returned container can be used to resolve dependencies of some
         * component.
         *
         * @param bindings the component bindings for the local dependencies.
         *
         * @return a container to resolve dependencies.
         *
         * @throws Exception whatever <code>bindings</code> throws.
         */
        Resolver resolver(Bindings bindings) throws Exception;

        /**
         * Returns a resolver with local dependencies bound by the given <code>bindings</code> and {@link org.fluidity.composition.Qualifier component context}
         * derived from the given type. The returned container can be used to resolve dependencies of the given <code>type</code> or some other component.
         *
         * @param type     component class to derive context from; may be <code>null</code>.
         * @param bindings the component bindings for the local dependencies; may be <code>null</code>.
         *
         * @return a container to resolve dependencies.
         *
         * @throws Exception whatever <code>bindings</code> throws.
         */
        Resolver resolver(Class<?> type, Bindings bindings) throws Exception;
    }

    /**
     * Container to resolve dependencies of some component to be created by the {@linkplain ComponentFactory component factory}. <h3>Usage</h3>
     *
     * @author Tibor Varga
     * @see ComponentFactory
     * @see ComponentFactory.Container
     */
    interface Resolver {

        /**
         * Resolves the given <code>parameter</code> of the given <code>constructor</code>. Qualifier defined on the constructor's declaring class, the
         * constructor itself, and the parameter, and {@link org.fluidity.composition.Component.Qualifiers consumed} by the resolved component, will be added to
         * its context. Component context is automatically passed on to the found component.
         *
         * @param constructor the constructor to resolve the given <code>parameter</code> of.
         * @param parameter   index of the constructor parameter to derive generic type and context from.
         *
         * @return an object that can return an instance of the resolved dependency; may be <code>null</code>.
         */
        Dependency<?> resolve(Constructor<?> constructor, int parameter);

        /**
         * Resolves the given <code>parameter</code> of the given <code>method</code> of the given <code>type</code> class. Qualifier defined on the given
         * class, the method, and the parameter, and {@link org.fluidity.composition.Component.Qualifiers consumed} by the resolved component, will be added to
         * its context. Component context is automatically passed on to the found component.
         *
         * @param type      the class the method will be invoked on; may be <code>null</code>, in which case the method's declaring class will be used.
         * @param method    the method to resolve the given <code>parameter</code> of.
         * @param parameter index of the constructor parameter to derive generic type and context from.
         *
         * @return an object that can return an instance of the resolved dependency; may be <code>null</code>.
         */
        Dependency<?> resolve(Class<?> type, Method method, int parameter);

        /**
         * Resolves the given <code>field</code> of the given <code>type</code> class. Qualifier defined on the given class and the field, and {@link
         * org.fluidity.composition.Component.Qualifiers consumed} by the resolved component, will be added to its context. Component context is automatically
         * passed on to the found component.
         *
         * @param type  the class the field will used with; may be <code>null</code>, in which case the field's declaring class will be used.
         * @param field field to derive generic type and context from.
         *
         * @return an object that can return an instance of the resolved dependency; may be <code>null</code>.
         */
        Dependency<?> resolve(Class<?> type, Field field);

        /**
         * Invokes {@link #resolve(Class, Method, int)} for every parameter of the given <code>method</code> with the given <code>type</code> class, and returns
         * the result. Component context is automatically passed on to the found component.
         *
         * @param type   the class the method will be invoked on; may be <code>null</code>, in which case the method's declaring class will be used.
         * @param method the method to resolve the parameters of.
         *
         * @return a list of dependencies that can then be used to get a dependency {@linkplain Dependency#instance()} of; never <code>null</code>, but the
         * elements may be <code>null</code>.
         */
        Dependency<?>[] resolve(Class<?> type, Method method);

        /**
         * Resolves each resolvable parameter of the given constructor and returns a list of dependencies for each method parameter, resolvable or not. The
         * returned array is mutable and can be fed to {@link Resolver#instantiate(Dependency[])} to get an argument list to invoke the
         * <code>constructor</code> with. Component context is automatically passed on to the found component.
         *
         * @param constructor the constructor to resolve the parameters of.
         *
         * @return a list of dependencies that can then be used to get {@linkplain Dependency#instance() instances} of.
         */
        Dependency<?>[] resolve(Constructor<?> constructor);

        /**
         * Calls {@link #constructor(Class)} on the given type, calls {@link #resolve(Constructor)} therewith, and returns the result. Component context is
         * automatically passed on to the found component.
         *
         * @param type the component class.
         *
         * @return a list of dependencies that can then be used to get {@linkplain Dependency#instance() instances} of.
         */
        Dependency<?>[] discover(Class<?> type);

        /**
         * Dynamically looks up a component bound to a component interface that is assignable to the given reference, and then calls {@link #discover(Class)}
         * with it.
         *
         * @param reference the generic reference to the component to look up.
         *
         * @return an object that can return an instance of the resolved dependency; may be <code>null</code>.
         */
        Dependency<?> lookup(Type reference);

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
         * corresponding to the parameters of the supplied constructor. If no dependency is found, and the class that defines the <code>constructor</code> is
         * annotated with @{@link Component}, and @{@link org.fluidity.composition.Optional} is not among <code>annotations</code>, this method throws a {@link
         * org.fluidity.composition.ComponentContainer.ResolutionException}.
         * <p>
         * Use this method if the component has multiple constructors and the invoking dependency injection container would not be able to figure out which one
         * to use. Otherwise use the {@link #discover(Class)}.
         *
         * @param constructor the constructor.
         *
         * @return a list of dependencies corresponding to the constructor parameters.
         */
        Dependency<?>[] discover(Constructor<?> constructor);

        /**
         * Informs the recipient about the injectable constructor parameters of the given component class, and then returns a list of dependencies, each
         * corresponding to the parameters of the supplied parameter injected method. If no dependency is found, and the given <code>type</code> class is
         * annotated with @{@link Component}, and @{@link org.fluidity.composition.Optional} is not among <code>annotations</code>, this method throws a {@link
         * org.fluidity.composition.ComponentContainer.ResolutionException}.
         *
         * @param type   the class that the given method will be invoked on; may not be <code>null</code>.
         * @param method the method.
         *
         * @return a list of dependencies corresponding to the parameters of the supplied factory method.
         */
        Dependency<?>[] discover(Class<?> type, Method method);

        /**
         * Wraps the given object as a dependency. Useful to fill in missing {@linkplain #resolve(Constructor) constructor} or {@linkplain #resolve(Class,
         * Method) method} arguments in the {@link Dependency} array returned by those methods before feeding the array to {@link
         * #instantiate(Dependency[])}.
         *
         * @param object the object to wrap as a dependency.
         * @param <T>    the dependency type the given <code>object</code> satisfies.
         *
         * @return the dependency whose {@link Dependency#instance()} method will return the given <code>object</code>.
         */
        <T> Dependency<T> constant(T object);

        /**
         * Returns an array containing the {@linkplain Dependency#instance() instance} of each dependency in the given array.
         *
         * @param dependencies the dependency array; may not be <code>null</code>.
         *
         * @return an array of objects; never <code>null</code>.
         */
        Object[] instantiate(Dependency<?>... dependencies);

        /**
         * Invokes the given <code>constructor</code> with the given <code>arguments</code>. This method is provided as a convenience and intended to be used in
         * {@link Instance#bind(ComponentFactory.Registry)}.
         *
         * @param <T>         the class in which the constructor is declared.
         *
         * @param constructor the constructor to call {@linkplain Constructor#newInstance(Object...)} on; never <code>null</code>.
         * @param arguments   the arguments to pass to {@linkplain Constructor#newInstance(Object...)} to.
         * @return the constructed object.
         *
         * @throws Exception whatever <code>constructor</code> throws with the given <code>arguments</code>.
         */
        <T> T invoke(Constructor<T> constructor, Dependency<?>... arguments) throws Exception;

        /**
         * Invokes the given <code>method</code> with the given <code>arguments</code>. This method is provided as a convenience and intended to be used in
         * {@link Instance#bind(ComponentFactory.Registry)}.
         *
         * @param target the object to call the method on; may be <code>null</code> for static methods.
         * @param method    the method to invoke; never <code>null</code>.
         * @param arguments the arguments to invoke the <code>method</code> with.
         *
         * @return whatever <code>method</code> returns with the given <code>arguments</code>.
         *
         * @throws Exception whatever <code>method</code> throws with the given <code>arguments</code>.
         */
        Object invoke(Object target, Method method, Dependency<?>... arguments) throws Exception;
    }
}
