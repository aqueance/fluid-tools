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
 * This is the external API of a fully populated dependency injection container. For a discussion of dependency injection and dependency injection containers,
 * see the <a href="http://code.google.com/p/fluid-tools/wiki/UserGuide#Dependency_Injection_Concept">User Guide</a>.
 * <p/>
 * TODO: copy the next few paragraphs to the user guide
 * <p/>
 * An application based on Fluid Tools has a root container, which is the container associated with the highest level class loader that can load the necessary
 * bootstrap classes. You configure the contents of a container, root or its descendants, by configuring the class path of the various class loaders in your
 * application. How that is done is beyond the scope of this introduction.
 * <p/>
 * Any class loader that loads the class of a bootstrap object, i.e., one that calls {@link ContainerBoundary}, will have its own container that will be either
 * the root container or a direct or indirect child thereof. Thus your application may have a hierarchy of containers that matches the application's class
 * loader hierarchy.
 * <p/>
 * The root container is populated automatically by some bootstrap object based on metadata produced by the
 * <code>org.fluidity.maven:maven-composition-plugin</code> Maven plugin. Bootstrap classes are / must be created for the various application containers that
 * are / may be used to host your application, such as the JRE application launcher, a web application or an OSGi bundle.
 * <p/>
 * Containers in a hierarchy co-operate in such a way that if a component is not found in a child container, a look-up is performed in its parent. The act of
 * looking up a dependency by its referenced type is called <i>dependency resolution</i>. Telling the container what class to resolve a component interface to
 * is called <i>component binding</i> where a component class is bound to the component interface. The act of a container using its parent to resolve missing
 * dependencies is called <i>defaulting to</i> the parent container.
 * <p/>
 * Dependency resolution is performed based on the referenced type of the dependency. If there was no explicit binding for the given interface then no component
 * will be injected for that reference.
 * <p/>
 * Components are instantiated by the container on demand and their dependencies, defined by constructor parameters and {@link Inject @Inject} annotated fields,
 * are resolved in this container or its parent. No setter injection or other means of dependency injection are supported.
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
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
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
     * is only safe if your application guarantees that the parent container is never used outside a domain container and that domain containers and the
     * components they instantiate never talk to other domain containers or components they instantiated. Hence the term "domain".
     *
     * @return a container that defaults to this container for satisfying component dependencies and which will also be used defaulted to by the ancestor
     *         components when they cannot resolve a dependency.
     */
    OpenComponentContainer makeDomainContainer();

    /**
     * Instantiates and returns a transient component by its interface or (super)class after invoking the bindings implemented by the supplied
     * {@link OpenComponentContainer.Bindings} object. This method is a convenient shortcut to {@link #makeChildContainer() acquire a child container}, {@link
     * OpenComponentContainer.Bindings#bindComponents(OpenComponentContainer.Registry) register component bindings} in it, and
     * then get the child container to {@link #getComponent(Class) instantiate} the requested component.
     *
     * @param api      an interface or class that the provided bindings will register an implementation or extension for, along with its dependencies missing
     *                 from this container or its parent or intended to be overridden.
     * @param bindings invoked to add component bindings to the child container.
     *
     * @return the component bound to the given class or <code>null</code> if none was bound.
     *
     * @throws ResolutionException when dependency resolution fails
     */
    <T> T getComponent(Class<T> api, OpenComponentContainer.Bindings bindings) throws ResolutionException;

    /**
     * Resolves and injects the {@link Inject @Inject} annotated fields of the given object. You only need to use this method if the supplied component was
     * instantiated outside the container.
     *
     * @param component a component that needs field injection of dependencies.
     *
     * @return the supplied object.
     *
     * @throws ResolutionException when dependency resolution fails
     */
    <T> T initialize(T component) throws ResolutionException;

    /**
     * Invokes the given method of the given object after resolving and injecting its parameters.
     *
     * @param component the method to invoke on the provided object.
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
    @SuppressWarnings("UnusedDeclaration")
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
}
