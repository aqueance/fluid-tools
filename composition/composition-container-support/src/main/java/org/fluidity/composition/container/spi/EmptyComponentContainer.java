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

package org.fluidity.composition.container.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.Components;
import org.fluidity.composition.Inject;
import org.fluidity.composition.MutableContainer;
import org.fluidity.composition.ObservedContainer;
import org.fluidity.composition.OpenContainer;
import org.fluidity.composition.container.ContainerServices;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.spi.ComponentInterceptor;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Proxies;
import org.fluidity.foundation.Security;

import static org.fluidity.foundation.Command.Function;

/**
 * Implements basic method relationships and functionality on behalf of actual container and registry implementations. The public facade of dependency
 * injection container implementations must extend this base class and wrap a {@link DependencyGraph} implementation.
 * <h3>Usage</h3>
 * You don't interact with an internal interface.
 *
 * @param <C> the type of dependency graph used internally by a particular subclass.
 *
 * @author Tibor Varga
 */
public abstract class EmptyComponentContainer<C extends DependencyGraph> implements MutableContainer, ObservedContainer, ComponentRegistry {

    // allows traversal path and observers to propagate between containers
    private static final ThreadLocal<DependencyGraph.Traversal> traversal = new InheritableThreadLocal<DependencyGraph.Traversal>();

    /**
     * The container services supplied in the constructor.
     */
    protected final ContainerServices services;

    /**
     * The context definition supplied in the constructor.
     */
    protected final ContextDefinition context;

    /**
     * The observer supplied in the constructor.
     */
    protected final Observer observer;

    /**
     * The container supplied in the constructor.
     */
    protected final C container;

    private final Registry registry = new EmptyRegistry(this);

    /**
     * Creates a new instance.
     *
     * @param container the dependency graph to resolve components and component groups.
     * @param services  the container services to use.
     * @param context   the base context of this container.
     * @param observer  the observer to use when resolving components or component groups; may be <code>null</code>.
     */
    protected EmptyComponentContainer(final C container, final ContainerServices services, final ContextDefinition context, final Observer observer) {
        assert container != null;
        this.container = container;

        assert services != null;
        this.services = services;

        assert context != null;
        this.context = context;

        this.observer = observer;
    }

    /**
     * Returns a new container that calls the given observer whenever a dependency is resolved while resolving a component interface via the returned
     * container.
     *
     * @param graph    the dependency graph.
     * @param context  the base context of the dependency graph.
     * @param observer the observer to call, never <code>null</code>.
     *
     * @return a new container instance backed by this one and using the provided resolution observer.
     */
    protected abstract ObservedContainer container(C graph, ContextDefinition context, Observer observer);

    /**
     * Invokes the given method of the given object after resolving and injecting its applicable parameters that the given argument list contains no
     * (or <code>null</code>) value for.
     * <p/>
     *
     * @param component the method to invoke on the provided object.
     * @param explicit  tells if all parameters are subject to injection (<code>true</code>) or only those annotated with {@link Inject @Inject}
     *                  (<code>false</code>).
     * @param method    the method that needs its parameters injected.
     * @param arguments the method parameters matching the method's signature with <code>null</code> values where injection is needed.
     *
     * @return the result of the method invocation.
     *
     * @throws ResolutionException       when dependency resolution fails.
     * @throws InvocationTargetException when the method throws an exception.
     */
    protected abstract Object invoke(final Object component, boolean explicit, final Method method, final Object... arguments)
            throws ResolutionException, InvocationTargetException;

    /**
     * {@inheritDoc}
     */
    public final ObservedContainer observed(final Observer observer) {
        return observer == null
               ? this
               : container(container, context, services.aggregateObserver(this.observer, observer));    // TODO: this is the only call to ContainerServices#aggregateObserver()
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public final <T> T getComponent(final Class<T> api) {
        return traverse(services, new Function<T, DependencyGraph.Traversal, RuntimeException>() {
            public T run(final DependencyGraph.Traversal traversal) {
                final DependencyGraph.Node node = container.resolveComponent(api, context.advance(api, false), traversal, api);
                return node == null ? null : (T) node.instance(traversal);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public final <T> T[] getComponentGroup(final Class<T> api) {
        return traverse(services, new Function<T[], DependencyGraph.Traversal, RuntimeException>() {
            public T[] run(final DependencyGraph.Traversal traversal) {
                final DependencyGraph.Node node = container.resolveGroup(api, context.advance(Array.newInstance(api, 0).getClass(), false).expand(null), traversal, api);
                return node == null ? null : (T[]) node.instance(traversal);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public final void resolveComponent(final Class<?> api) {
        traverse(services, new Function<Void, DependencyGraph.Traversal, RuntimeException>() {
            public Void run(final DependencyGraph.Traversal traversal) {
                container.resolveComponent(api, context.advance(api, false), traversal, api);
                return null;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public final void resolveGroup(final Class<?> api) {
        traverse(services, new Function<Void, DependencyGraph.Traversal, RuntimeException>() {
            public Void run(final DependencyGraph.Traversal traversal) {
                container.resolveGroup(api, context.advance(Array.newInstance(api, 0).getClass(), false).expand(null), traversal, api);
                return null;
            }
        });
    }

    private <T> T traverse(final ContainerServices services, final Function<T, DependencyGraph.Traversal, RuntimeException> command) {
        final DependencyGraph.Traversal saved = traversal.get();
        final DependencyGraph.Traversal current = saved == null ? services.graphTraversal(observer) : saved.observed(observer);

        traversal.set(current);
        try {
            return command.run(current);
        } finally {
            traversal.set(saved);
        }
    }

    /**
     * Adds the given list of component bindings to this container.
     *
     * @param list the list of bindings to add.
     *
     * @return this container.
     */
    protected final MutableContainer addBindings(final Bindings... list) {
        for (final Bindings bindings : list) {
            bindings.bindComponents(registry);
        }

        return this;
    }

    /**
     * Calls {@link #invoke(Object, boolean, Method, Object...) invoke}<code>(component, <b>true</b>, method, arguments)</code>.
     * <p/>
     * {@inheritDoc}
     */
    public final Object invoke(final Object component, final Method method, final Object... arguments) throws ResolutionException, InvocationTargetException {
        return invoke(component, true, method, arguments);
    }

    /**
     * {@inheritDoc}
     */
    public final <T> T instantiate(final Class<T> componentClass, final Bindings... bindings) throws ResolutionException {
        final OpenContainer container = makeChildContainer(new Bindings() {
            @SuppressWarnings("unchecked")
            public void bindComponents(final Registry registry) {
                registry.bindComponent(componentClass, componentClass);

                for (final Bindings binding : bindings) {
                    binding.bindComponents(registry);
                }
            }
        });

        return container.getComponent(componentClass);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public final <T> T complete(final T component, final Class<? super T>... api) throws ResolutionException {
        final Class<T> type = (Class<T>) component.getClass();
        final List<Class<?>> interfaces = new ArrayList<Class<?>>();

        for (final Components.Specification specification : Components.inspect(type, api).api) {
            if (specification.api.isInterface()) {
                interfaces.add(specification.api);
            } else {
                throw new IllegalArgumentException(String.format("Component %s may only have interfaces as its component interfaces", type.getName()));
            }
        }

        final ClassLoader loader = !Security.CONTROLLED ? type.getClassLoader() : AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return type.getClassLoader();
            }
        });

        return (T) Proxies.create(loader, Lists.asArray(Class.class, interfaces), new InvocationHandler() {

            private final Map<Method, Boolean> injectMap = new ConcurrentHashMap<Method, Boolean>();

            public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                Boolean inject = injectMap.get(method);

                if (inject == null) {
                    LOOP:
                    for (final Annotation[] annotations : method.getParameterAnnotations()) {
                        for (final Annotation annotation : annotations) {
                            if (annotation instanceof Inject) {
                                injectMap.put(method, inject = true);
                                break LOOP;
                            }
                        }
                    }

                    if (inject == null) {
                        injectMap.put(method, inject = false);
                    }

                    final PrivilegedAction<Method> access = Security.setAccessible(method);

                    if (access != null) {
                        AccessController.doPrivileged(access);
                    }
                }

                return inject ? EmptyComponentContainer.this.invoke(component, false, method, args) : method.invoke(component, args);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public final ComponentContainer intercepting(final ComponentInterceptor... interceptors) {
        return makeChildContainer(new Bindings() {
            @SuppressWarnings("unchecked")
            public void bindComponents(final Registry registry) {
                for (final ComponentInterceptor interceptor : interceptors) {
                    registry.bindInstance(interceptor);
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public final Registry getRegistry() {
        return registry;
    }
}
