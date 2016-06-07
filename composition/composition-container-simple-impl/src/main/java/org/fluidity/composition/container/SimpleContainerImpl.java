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

package org.fluidity.composition.container;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Components;
import org.fluidity.composition.container.spi.ContextNode;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.container.spi.DependencyResolver;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.Generics;
import org.fluidity.foundation.Lists;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.Strings;

/**
 * @author Tibor Varga
 */
final class SimpleContainerImpl implements ParentContainer {

    private static AtomicReference<Log> log = new AtomicReference<Log>();

    private final ContainerServices services;

    private final ParentContainer parent;
    private final ParentContainer domain;
    private final boolean quiet;

    private final Map<Class<?>, ComponentResolver> components = new HashMap<Class<?>, ComponentResolver>();
    private final Map<Class<?>, GroupResolver> groups = new HashMap<Class<?>, GroupResolver>();

    private final DependencyInjector injector;

    SimpleContainerImpl(final ContainerServices services, final boolean quiet) {
        this(false, null, services, quiet);
    }

    private SimpleContainerImpl(final boolean domain, final ParentContainer parent, final ContainerServices services, final boolean quiet) {
        this.parent = parent;
        this.domain = domain ? this : null;
        this.services = services;
        this.quiet = quiet;

        this.injector = this.services.dependencyInjector();

        if (!quiet) {
            log.compareAndSet(null, this.services.createLog(log.get(), getClass()));
        }
    }

    public boolean quiet() {
        return quiet;
    }

    public void log(final Log log, final String format, final Object... arguments) {
        if (!quiet) {
            log.debug(format, arguments);
        }
    }

    public ContainerServices services() {
        return services;
    }

    public SimpleContainer parentContainer() {
        return parent;
    }

    public ParentContainer newChildContainer(final boolean domain) {
        return new SimpleContainerImpl(domain, this, services, quiet);
    }

    public ComponentResolver bindResolver(final Class<?> api, final ComponentResolver resolver) {
        synchronized (components) {
            final ComponentResolver previous = components.get(api);

            if (previous == null) {
                return replace(api, null, resolver);
            } else if (previous.replaces(resolver)) {
                return replace(api, resolver, previous);
            } else if (resolver.replaces(previous)) {
                return replace(api, previous, resolver);
            } else {
                return previous;
            }
        }
    }

    private GroupResolver bindGroup(final Class<?> api) {
        GroupResolver resolver;

        synchronized (groups) {
            resolver = groups.get(api);

            if (resolver == null) {
                groups.put(api, resolver = new GroupResolver(api));
            }
        }

        return resolver;
    }

    public ComponentResolver replace(final Class<?> key, final ComponentResolver previous, final ComponentResolver replacement) {
        if (components.remove(key) == previous) {
            replaceResolver(key, previous, replacement);
        }

        components.put(key, replacement);

        return replacement;
    }

    private interface Resolver {

        ComponentResolver resolver(Class<?> type);
    }

    private void bindResolvers(final Components.Specification[] interfaces, final Resolver main) {
        final Set<Class<?>> groups = new HashSet<Class<?>>();
        final Set<ComponentResolver> resolvers = new HashSet<ComponentResolver>();

        for (final Components.Specification api : interfaces) {
            final Class<?> component = api.api;

            resolvers.add(bindResolver(component, main.resolver(component)));

            for (final Class<?> group : api.groups) {
                bindGroup(group).addResolver(component);
                groups.add(group);
            }
        }

        for (final ComponentResolver resolver : resolvers) {
            resolver.addGroups(groups);
        }
    }

    @SuppressWarnings("unchecked")
    private void bindResolvers(final Class<?> scope,
                               final Class<?> component,
                               final Components.Specification[] interfaces,
                               final boolean stateful,
                               final ContentResolvers resolvers) {
        Class root = null;  // root class to discover scoped components for

        final boolean dummy = component.isInterface();

        if (dummy) {
            root = component;
        } else {
            final Component componentSpec = component.getAnnotation(Component.class);
            final Class scoped = componentSpec == null ? Object.class : componentSpec.scope();

            if (scoped != Object.class) {
                for (final Components.Specification specification : interfaces) {
                    if (specification.api == scoped) {
                        root = scoped;
                        break;
                    }
                }
            }
        }

        if (root != null && root != scope) {
            for (final Class<?> dependency : services.componentDiscovery().findComponentClasses(Component.SCOPE, root, root.getClassLoader(), false, false)) {
                if (dependency != component) {
                    bindComponent(scope == null ? root : scope, Components.inspect(dependency));
                }
            }
        }

        if (!dummy) {
            final ComponentCache cache = services.newCache(!stateful);

            if (resolvers.isCustomFactory()) {
                bindResolver(component, resolvers.component(component, services.newCache(true), true));
                bindResolvers(interfaces, new Resolver() {
                    public ComponentResolver resolver(final Class<?> type) {
                        return resolvers.factory(type, cache);
                    }
                });
            } else {
                bindResolvers(interfaces, new Resolver() {
                    public ComponentResolver resolver(final Class<?> type) {
                        return resolvers.component(type, cache, false);
                    }
                });
            }
        }
    }

    public void bindComponent(final Components.Interfaces interfaces) {
        bindComponent(null, interfaces);
    }

    private void bindComponent(final Class<?> scope, final Components.Interfaces interfaces) {
        final Class<?> implementation = interfaces.implementation;

        /*
         * No synthetic classes or anonymous inner classes are allowed.
         */
        final boolean isSyntheticClass = implementation.isSynthetic();
        final boolean isAnonymousClass = implementation.isAnonymousClass();

        if (isSyntheticClass || isAnonymousClass) {
            throw new ComponentContainer.BindingException("Component %s is not instantiable (%s)",
                                                          Strings.formatClass(true, true, implementation),
                                                          isSyntheticClass ? "synthetic" : "anonymous");
        }

        final Component componentSpec = implementation.getAnnotation(Component.class);
        final boolean isStateful = componentSpec != null && componentSpec.stateful();
        final boolean isFallback = componentSpec != null && !componentSpec.primary();

        log(log.get(),
            "%s: binding %s to %s (%s, %s)",
            this,
            Strings.formatClass(true, true, implementation),
            interfaces,
            isStateful ? "stateful" : "stateless",
            isFallback ? "fallback" : "primary");

        bindResolvers(scope, implementation, interfaces.api, isStateful, new ContentResolvers() {

            public boolean isCustomFactory() {
                return ComponentFactory.class.isAssignableFrom(implementation);
            }

            public ComponentResolver component(final Class<?> api, final ComponentCache cache, final boolean resolvesFactory) {
                return new ConstructingResolver(isFallback ? 0 : 1, api, implementation, resolvesFactory, cache, injector);
            }

            public FactoryResolver factory(final Class<?> api, final ComponentCache cache) {
                final Class<? extends ComponentFactory> factory = implementation.asSubclass(ComponentFactory.class);
                return new FactoryResolverClass(isFallback ? 0 : 1, api, factory, cache);
            }
        });
    }

    public void bindInstance(final Object instance, final Components.Interfaces interfaces) {
        bindInstance(null, instance, interfaces);
    }

    public void bindInstance(final Class<?> scope, final Object instance, final Components.Interfaces interfaces) {
        if (instance != null) {
            final Class<?> implementation = instance.getClass();
            final Component componentSpec = implementation.getAnnotation(Component.class);

            final boolean isFactory = instance instanceof ComponentFactory;
            assert !isFactory || componentSpec != null;

            final boolean isFallback = componentSpec != null && !componentSpec.primary();
            final boolean isStateful = isFactory && componentSpec.stateful();

            final String value = instance instanceof String || instance instanceof Number ? String.format("'%s'", instance) : Strings.formatId(instance);

            log(log.get(), "%s: binding %s to %s (%s)", this, value, interfaces, isFallback ? "fallback" : "primary");

            bindResolvers(scope, implementation, interfaces.api, isStateful, new ContentResolvers() {

                public boolean isCustomFactory() {
                    return isFactory;
                }

                public ComponentResolver component(final Class<?> api, final ComponentCache cache, final boolean resolvesFactory) {
                    return new InstanceResolver(isFallback ? 0 : 1, api, instance);
                }

                @SuppressWarnings("ConstantConditions")
                public FactoryResolver factory(final Class<?> api, final ComponentCache cache) {
                    return new FactoryResolverInstance(isFallback ? 0 : 1, api, (ComponentFactory) instance, cache);
                }
            });
        }
    }

    public SimpleContainer linkComponent(final Components.Interfaces interfaces) throws ComponentContainer.BindingException {
        final ParentContainer child = newChildContainer(false);

        child.bindComponent(interfaces);

        for (final Components.Specification specification : interfaces.api) {
            bindResolver(specification.api, new LinkingResolver(child, specification.api, child.resolver(specification.api, false)));
        }

        return child;
    }

    public ComponentResolver resolver(final Class<?> api, final boolean ascend) {
        final ComponentResolver resolver = components.get(api);
        return resolver == null && parent != null && ascend ? parent.resolver(api, true) : resolver;
    }

    public void replaceResolver(final Class<?> key, final ComponentResolver previous, final ComponentResolver replacement) {
        for (final ComponentResolver resolver : components.values()) {
            resolver.resolverReplaced(key, previous, replacement);
        }

        if (parent != null) {
            parent.replaceResolver(key, previous, replacement);
        }
    }

    public DependencyResolver dependencyResolver(final ParentContainer domain) {
        return new DelegatingDependencyResolver(domain);
    }

    public Object initialize(final Object component, final ContextDefinition context, final ComponentContainer.Observer observer) {
        return initialize(component, context, services.graphTraversal(observer));
    }

    public Object initialize(final Object component, final ContextDefinition context, final Traversal traversal) {
        final DependencyResolver resolver = dependencyResolver(domain);
        final InstanceDescriptor descriptor = new InstanceDescriptor(component);

        final Class<?> type = component.getClass();

        final Node node = traversal.follow(component, type, type, context, new DependencyGraph.Node.Reference() {
            public DependencyGraph.Node resolve() {
                return new ResolvedNode(type, injector.fields(component, traversal, resolver, descriptor, context), context.create());
            }
        });

        return node.instance(traversal);
    }

    public Object invoke(final Object component, final Method method, final ContextDefinition context, final Object[] arguments, final boolean explicit)
            throws InvocationTargetException {
        return injector.invoke(component, method, arguments, services.graphTraversal(), dependencyResolver(domain), new InstanceDescriptor(component), context, explicit);
    }

    public Object cached(final ParentContainer domain, final Class<?> api, final ComponentContext context) {
        final ComponentResolver resolver = components.get(api);
        final ParentContainer container = domain == null ? this.domain : domain;

        if (resolver == null) {
            return parent != null ? parent.cached(container, api, context) : null;
        } else {
            return resolver.cached(container == null ? this : container, toString(), context);
        }
    }

    public Node resolveComponent(final ParentContainer domain,
                                 final boolean ascend,
                                 final Class<?> api,
                                 final ContextDefinition context,
                                 final Traversal traversal,
                                 final Type reference) {
        final ComponentResolver resolver = components.get(api);

        if (resolver == null) {
            return !ascend
                   ? null
                   : parent == null
                     ? domain == null || domain == this ? null : domain.resolveComponent(null, false, api, context, traversal, reference)
                     : parent.resolveComponent(domain == null ? this.domain : domain, true, api, context, traversal, reference);
        } else {
            final Class<?> group = Generics.rawType(Generics.arrayComponentType(reference));

            return traversal.follow(resolver, group != null && group.isAssignableFrom(api) ? group : api, api, context, new Node.Reference() {
                @Override
                public Node resolve() {
                    final Node node = resolver.resolve(domain, SimpleContainerImpl.this, traversal, context, reference);

                    return new Node() {
                        public Class<?> type() {
                            return node.type();
                        }

                        public Object instance(final Traversal traversal) {
                            final Collection<Class<?>> interfaces = resolver.groups();
                            final Set<ComponentContainer.Observer> observers = new HashSet<ComponentContainer.Observer>();

                            // whenever a component is instantiated, all groups it belongs to are notified
                            for (final Class<?> api : interfaces) {
                                final List<GroupResolver> resolvers = groupResolvers(api);

                                for (final GroupResolver resolver : resolvers) {
                                    observers.add(resolver.observer());
                                }
                            }

                            return node.instance(traversal.observed(Lists.asArray(ComponentContainer.Observer.class, observers)));
                        }

                        public ComponentContext context() {
                            return node.context();
                        }
                    };
                }
            });
        }
    }

    public Node resolveComponent(final Class<?> api, final ContextDefinition context, final Traversal traversal, final Type reference) {
        return resolveComponent(domain, true, api, context, traversal, reference);
    }

    public List<GroupResolver> groupResolvers(final Class<?> api) {
        final List<GroupResolver> list = new ArrayList<GroupResolver>();

        final GroupResolver resolver = groups.get(api);
        if (resolver != null) {
            list.add(resolver);
        }

        if (parent != null) {
            list.addAll(parent.groupResolvers(api));
        }

        return list;
    }

    public Node resolveGroup(final Class<?> api, final ContextDefinition context, final Traversal traversal, final Type reference) {
        return resolveGroup(domain, api, context, traversal, reference);
    }

    public Node resolveGroup(final ParentContainer domain, final Class<?> api, final ContextDefinition context, final Traversal traversal, final Type reference) {
        final GroupResolver group = groups.get(api);

        if (group == null) {
            return parent == null
                   ? domain == null || domain == this ? null : domain.resolveGroup(null, api, context, traversal, reference)
                   : parent.resolveGroup(domain, api, context, traversal, reference);
        } else {
            final Class<?> arrayApi = Array.newInstance(api, 0).getClass();

            return traversal.follow(group, arrayApi, arrayApi, context, new Node.Reference() {
                public Node resolve() {
                    return groupNode(api, resolveGroup(domain, api, traversal, context, reference), context);
                }
            });
        }
    }

    public List<GroupResolver.Node> resolveGroup(final ParentContainer domain,
                                                 final Class<?> api,
                                                 final Traversal traversal,
                                                 final ContextDefinition context,
                                                 final Type reference) {
        final List<GroupResolver.Node> enclosing = parent == null ? null : parent.resolveGroup(domain, api, traversal, context, reference);
        final GroupResolver group = groups.get(api);

        if (group == null) {
            return enclosing;
        } else {
            final List<GroupResolver.Node> list = new ArrayList<GroupResolver.Node>();

            if (enclosing != null) {
                list.addAll(enclosing);
            }

            list.add(group.resolve(domain, traversal, this, context, reference));

            return list;
        }
    }

    private Node groupNode(final Class<?> api, final List<GroupResolver.Node> list, final ContextDefinition context) {
        final ComponentContext componentContext = context.create();

        return list == null ? null : new Node() {
            public Class<?> type() {
                return api;
            }

            @SuppressWarnings("unchecked")
            public Object instance(final Traversal traversal) {
                final List output = new ArrayList();

                for (final GroupResolver.Node node : list) {
                    output.addAll(node.instance(traversal));
                }

                return Lists.asArray((Class) api, output);
            }

            public ComponentContext context() {
                return componentContext;
            }
        };
    }

    public ContextNode contexts(final ParentContainer domain, final Class<?> type, final ContextDefinition context) {
        final ComponentResolver resolver = resolver(type, true);
        return resolver == null
               ? parent != null ? parent.contexts(domain, type, context) : domain != null && domain != this ? domain.contexts(null, type, context) : null
               : resolver;
    }

    public ComponentContainer container(final ContextDefinition context) {
        return new ComponentContainerShell(this, context, false);
    }

    public String id() {
        final String id = String.format("%x", System.identityHashCode(this));
        return parent == null ? id : String.format("%s > %s", parent.id(), id);
    }

    @Override
    public String toString() {
        return String.format("container %s", id());
    }

    /**
     * Internal interface to generalize the binding of components, including ordinary ones, factories and variant factories.
     */
    private interface ContentResolvers {

        /**
         * Tells if we are processing an custom component factory.
         *
         * @return <code>true</code> if we are processing an ordinary factory, <code>false</code> otherwise.
         */
        boolean isCustomFactory();

        /**
         * Creates a component resolver for the component being processed.
         *
         * @param api             the interface to which the component will be bound.
         * @param cache           the cache to use for the component.
         * @param resolvesFactory tells if the component resolves a factory instance.
         *
         * @return a component resolver that will resolve, and cache if necessary, a single instance of the component.
         */
        ComponentResolver component(Class<?> api, ComponentCache cache, boolean resolvesFactory);

        /**
         * Creates a component factory resolver for the factory being processed.
         *
         * @param api   the interface to which the factory will be bound.
         * @param cache the cache to use for the factory.
         *
         * @return a component resolver that will produce, and cache if necessary, a single instance of the component factory.
         */
        FactoryResolver factory(Class<?> api, ComponentCache cache);
    }

    private class InstanceDescriptor implements ContextNode {

        private final Class<?> componentClass;

        public InstanceDescriptor(final Object component) {
            this.componentClass = component.getClass();
        }

        public Class<?> contextConsumer() {
            return componentClass;
        }

        public Annotation[] providedContext() {
            return componentClass.getAnnotations();
        }
    }

    private class DelegatingDependencyResolver implements DependencyResolver {

        private final ParentContainer domain;
        private final ParentContainer container;

        DelegatingDependencyResolver(final ParentContainer domain) {
            this.domain = domain;
            this.container = domain == null ? SimpleContainerImpl.this : domain;
        }

        public ComponentContainer container(final ContextDefinition context) {
            return container.container(context);
        }

        public Object cached(final Class<?> api, final ComponentContext context) {
            return container.cached(domain, api, context);
        }

        public Node resolveComponent(final Class<?> api, final ContextDefinition context, final Traversal traversal, final Type reference) {
            return container.resolveComponent(domain, true, api, context, traversal, reference);
        }

        public Node resolveGroup(final Class<?> api, final ContextDefinition context, final Traversal traversal, final Type reference) {
            return container.resolveGroup(domain, api, context, traversal, reference);
        }

        @Override
        public String toString() {
            return container.toString();
        }
    }
}
