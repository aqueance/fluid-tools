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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.spi.ComponentCache;
import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.ComponentVariantFactory;
import org.fluidity.composition.spi.ContainerServices;
import org.fluidity.composition.spi.ContextDefinition;
import org.fluidity.composition.spi.ContextNode;
import org.fluidity.composition.spi.CustomComponentFactory;
import org.fluidity.composition.spi.DependencyInjector;
import org.fluidity.composition.spi.DependencyResolver;
import org.fluidity.composition.spi.EmptyDependencyGraph;
import org.fluidity.composition.spi.PlatformContainer;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.Strings;
import org.fluidity.foundation.spi.LogFactory;

/**
 * @author Tibor Varga
 */
final class SimpleContainerImpl extends EmptyDependencyGraph implements ParentContainer {

    // allows traversal path to propagate between containers
    private static final ThreadLocal<Traversal> traversal = new InheritableThreadLocal<Traversal>();

    private final ContainerServices services;
    private final Log log;

    private final ParentContainer parent;
    private final ParentContainer domain;

    private final Map<Class<?>, ComponentResolver> components = new HashMap<Class<?>, ComponentResolver>();
    private final Map<Class<?>, GroupResolver> groups = new HashMap<Class<?>, GroupResolver>();

    private final DependencyInjector injector;
    private final LogFactory logs;

    public SimpleContainerImpl(final ContainerServices services, final PlatformContainer platform) {
        this(platform == null ? null : new SuperContainer(platform), null, services);
    }

    private SimpleContainerImpl(final ParentContainer parent, final ParentContainer domain, final ContainerServices services) {
        this.parent = parent;
        this.domain = domain == null ? this : domain;
        this.services = services;
        this.log = this.services.logs().createLog(getClass());
        this.injector = this.services.dependencyInjector();
        this.logs = this.services.logs();
    }

    public ContainerServices services() {
        return services;
    }

    public SimpleContainer parentContainer() {
        return parent;
    }

    public SimpleContainer newChildContainer(final boolean domain) {
        return new SimpleContainerImpl(this, domain ? null : this, services);
    }

    public ComponentResolver bindResolver(final Class<?> api, final ComponentResolver resolver) {
        synchronized (components) {
            final ComponentResolver previous = components.get(api);

            if (previous == null) {
                return replace(api, null, resolver);
            } else if (previous.isVariantMapping() && previous.replaces(resolver)) {
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

    private static interface Resolver {

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

    private void bindResolvers(final Class<?> implementation,
                               final Components.Specification[] interfaces,
                               final boolean stateful,
                               final ContentResolvers resolvers) {
        if (resolvers.isVariantFactory()) {
            bindResolver(implementation, resolvers.component(implementation, services.newCache(true), true));

            final ComponentCache cache = services.newCache(true);

            bindResolvers(interfaces, new Resolver() {
                public ComponentResolver resolver(final Class<?> type) {
                    return resolvers.variant(type, cache);
                }
            });
        } else if (resolvers.isCustomFactory()) {
            bindResolver(implementation, resolvers.component(implementation, services.newCache(true), true));

            final ComponentCache cache = services.newCache(true);

            bindResolvers(interfaces, new Resolver() {
                public ComponentResolver resolver(final Class<?> type) {
                    return resolvers.factory(type, cache);
                }
            });
        } else {
            final ComponentCache cache = services.newCache(!stateful);

            bindResolvers(interfaces, new Resolver() {
                public ComponentResolver resolver(final Class<?> type) {
                    return resolvers.component(type, cache, false);
                }
            });
        }
    }

    public void bindComponent(final Components.Interfaces interfaces) {
        final Class<?> implementation = interfaces.implementation;

        /*
         * No synthetic classes or anonymous inner classes are allowed.
         */
        final boolean isSyntheticClass = implementation.isSynthetic();
        final boolean isAnonymousClass = implementation.isAnonymousClass();

        if (isSyntheticClass || isAnonymousClass) {
            throw new OpenComponentContainer.BindingException("Component %s is not instantiable (%s)", implementation, isSyntheticClass ? "synthetic" : "anonymous");
        }

        final Component componentSpec = implementation.getAnnotation(Component.class);
        final boolean isStateful = componentSpec != null && componentSpec.stateful();
        final boolean isFallback = componentSpec != null && !componentSpec.primary();

        log.debug("%s: binding %s to %s (%s, %s)",
                  this,
                  implementation,
                  interfaces,
                  isStateful ? "stateful" : "stateless",
                  isFallback ? "fallback" : "primary");

        bindResolvers(implementation, interfaces.api, isStateful, new ContentResolvers() {
            public boolean isVariantFactory() {
                return ComponentVariantFactory.class.isAssignableFrom(implementation);
            }

            public boolean isCustomFactory() {
                return CustomComponentFactory.class.isAssignableFrom(implementation);
            }

            public ComponentResolver component(final Class<?> api, final ComponentCache cache, final boolean resolvesFactory) {
                return new ConstructingResolver(isFallback ? 0 : 1, api, implementation, resolvesFactory, cache, injector, logs);
            }

            public VariantResolver variant(final Class<?> api, final ComponentCache cache) {
                final Class<? extends ComponentVariantFactory> factory = implementation.asSubclass(ComponentVariantFactory.class);
                return new VariantResolverClass(isFallback ? 0 : 1, SimpleContainerImpl.this, api, factory, cache, logs);
            }

            public FactoryResolver factory(final Class<?> api, final ComponentCache cache) {
                final Class<? extends CustomComponentFactory> factory = implementation.asSubclass(CustomComponentFactory.class);
                return new FactoryResolverClass(isFallback ? 0 : 1, api, factory, cache, logs);
            }
        });
    }

    public void bindInstance(final Object instance, final Components.Interfaces interfaces) {
        if (instance == null) {
            throw new OpenComponentContainer.BindingException("Component instance for %s is null", interfaces.implementation);
        }

        final Class<?> implementation = instance.getClass();
        final Component componentSpec = implementation.getAnnotation(Component.class);
        final boolean isFallback = componentSpec != null && !componentSpec.primary();

        final String value = instance instanceof String || instance instanceof Number
                             ? ('\'' + String.valueOf(instance) + '\'')
                             : ("instance of " + Strings.arrayNotation(implementation));

        log.debug("%s: binding %s to %s (%s)", this, value, interfaces, isFallback ? "fallback" : "primary");

        bindResolvers(implementation, interfaces.api, false, new ContentResolvers() {
            public boolean isVariantFactory() {
                return instance instanceof ComponentVariantFactory;
            }

            public boolean isCustomFactory() {
                return instance instanceof CustomComponentFactory;
            }

            public ComponentResolver component(final Class<?> api, final ComponentCache cache, final boolean resolvesFactory) {
                return new InstanceResolver(isFallback ? 0 : 1, api, instance, logs);
            }

            @SuppressWarnings("ConstantConditions")
            public VariantResolver variant(final Class<?> api, final ComponentCache cache) {
                return new VariantResolverInstance(isFallback ? 0 : 1, SimpleContainerImpl.this, api, (ComponentVariantFactory) instance, cache, logs);
            }

            @SuppressWarnings("ConstantConditions")
            public FactoryResolver factory(final Class<?> api, final ComponentCache cache) {
                return new FactoryResolverInstance(isFallback ? 0 : 1, api, (CustomComponentFactory) instance, cache, logs);
            }
        });
    }

    public SimpleContainer linkComponent(final Components.Interfaces interfaces) throws OpenComponentContainer.BindingException {
        final LogFactory logs = this.logs;
        final SimpleContainer child = newChildContainer(false);

        child.bindComponent(interfaces);

        for (final Components.Specification specification : interfaces.api) {
            bindResolver(specification.api, new LinkingResolver(child, specification.api, child.resolver(specification.api, false), logs));
        }

        return child;
    }

    public ComponentResolver resolver(final Class<?> api, final boolean ascend) {
        final ComponentResolver resolver = components.get(api);
        return resolver == null && parent != null && ascend ? parent.resolver(api, ascend) : resolver;
    }

    public void replaceResolver(final Class<?> key, final ComponentResolver previous, final ComponentResolver replacement) {
        for (final ComponentResolver resolver : components.values()) {
            resolver.resolverReplaced(key, previous, replacement);
        }

        if (parent != null) {
            parent.replaceResolver(key, previous, replacement);
        }
    }

    public <T> T observe(final ComponentResolutionObserver recent, final Observed<T> command) {
        final Traversal saved = traversal.get();
        final Traversal current = saved == null ? services.graphTraversal(recent) : saved.observed(recent);

        traversal.set(current);
        try {
            return command.run(current);
        } finally {
            traversal.set(saved);
        }
    }

    public DependencyResolver dependencyResolver(final ParentContainer domain) {
        return new DelegatingDependencyResolver(domain);
    }

    public Object initialize(final Object component, final ContextDefinition context, final ComponentResolutionObserver observer) {
        return injector.fields(component, services.graphTraversal(), dependencyResolver(domain), new InstanceDescriptor(component), context);
    }

    public Object invoke(final Object component, final Method method, final ContextDefinition context) {
        return injector.invoke(component, method, services.graphTraversal(), dependencyResolver(domain), new InstanceDescriptor(component), context);
    }

    public Node resolveComponent(final ParentContainer domain,
                                 final boolean ascend,
                                 final Class<?> api,
                                 final ContextDefinition context,
                                 final Traversal traversal,
                                 final Type reference) {
        final ComponentResolver resolver = components.get(api);

        if (resolver == null) {
            return !ascend ? null : parent == null
                                    ? domain == null || domain == this ? null : domain.resolveComponent(null, false, api, context, traversal, reference)
                                    : parent.resolveComponent(domain, true, api, context, traversal, reference);
        } else {
            final Node node = resolver.resolve(domain, traversal, SimpleContainerImpl.this, context, reference);

            return new Node() {
                public Class<?> type() {
                    return node.type();
                }

                // whenever a component is instantiated, all groups it belongs to are notified
                public Object instance(final Traversal traversal) {
                    final Collection<Class<?>> interfaces = resolver.groups();
                    final Set<ComponentResolutionObserver> observers = new HashSet<ComponentResolutionObserver>();

                    for (final Class<?> api : interfaces) {
                        final List<GroupResolver> resolvers = groupResolvers(api);

                        for (final GroupResolver resolver : resolvers) {
                            observers.add(resolver.observer());
                        }
                    }

                    return node.instance(observers.isEmpty() ? traversal : traversal.observed(CompositeObserver.combine(observers)));
                }

                public ComponentContext context() {
                    return node.context();
                }
            };
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
        return resolveGroup(domain, api, context, traversal, null, reference);
    }

    public Node resolveGroup(final ParentContainer domain,
                             final Class<?> api,
                             final ContextDefinition context,
                             final Traversal traversal,
                             final Annotation[] annotations,
                             final Type reference) {
        final GroupResolver group = groups.get(api);

        if (group == null) {
            return parent == null
                   ? domain == null || domain == this ? null : domain.resolveGroup(null, api, context, traversal, annotations, reference)
                   : parent.resolveGroup(domain, api, context, traversal, annotations, reference);
        } else {
            final Class<?> arrayApi = Array.newInstance(api, 0).getClass();

            return traversal.follow(this, context, new Node.Reference() {
                public Class<?> type() {
                    return arrayApi;
                }

                public Annotation[] annotations() {
                    return annotations;
                }

                public Node resolve(final Traversal traversal, final ContextDefinition context) {
                    return groupNode(api, resolveGroup(domain, api, traversal, context.copy().expand(null, reference), reference), context);
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

                return output.toArray((Object[]) Array.newInstance(api, output.size()));
            }

            public ComponentContext context() {
                return componentContext;
            }
        };
    }

    public ContextNode contexts(final ParentContainer domain, final Class<?> type, final ContextDefinition context) {
        final ComponentResolver resolver = resolver(type, true);
        return resolver == null
               ? parent != null ? parent.contexts(domain, type, context) : domain != null && domain != this ? domain.contexts(null, type, context) : resolver
               : resolver;
    }

    public ComponentContainer container(final ContextDefinition context) {
        return new ComponentContainerShell(this, context, false);
    }

    public String id() {
        final String id = String.format("%x", System.identityHashCode(this));
        return parent == null ? id : String.format("%s > %s", id, parent.id());
    }

    @Override
    public String toString() {
        return String.format("container %s", id());
    }

    /**
     * Internal interface to generalize the binding of components, including ordinary ones, factories and variant factories.
     */
    private static interface ContentResolvers {

        /**
         * Tells if we are processing a variant factory.
         *
         * @return <code>true</code> if we are processing a variant factory, <code>false</code> otherwise.
         */
        boolean isVariantFactory();

        /**
         * Tells if we are processing an ordinary factory.
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
        ComponentResolver component(Class<?> api, final ComponentCache cache, final boolean resolvesFactory);

        /**
         * Creates a variant factory for the component being processed.
         *
         * @param api   the interface to which the variant factory will be bound.
         * @param cache the cache to use for the variant factory.
         *
         * @return a variant resolver that will produce, and cache if necessary, a single instance of a variant factory.
         */
        VariantResolver variant(Class<?> api, final ComponentCache cache);

        /**
         * Creates a component factory for the component being processed.
         *
         * @param api   the interface to which the factory will be bound.
         * @param cache the cache to use for the factory.
         *
         * @return a component resolver that will produce, and cache if necessary, a single instance of a component factory.
         */
        FactoryResolver factory(Class<?> api, final ComponentCache cache);
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

    private static class SuperContainer extends EmptyDependencyGraph implements ParentContainer {

        private final PlatformContainer platform;
        private final List<GroupResolver> emptyList = Collections.emptyList();

        private final ContextNode noContexts = new ContextNode() {
            public Class<?> contextConsumer() {
                return null;
            }

            public Annotation[] providedContext() {
                return new Annotation[0];
            }
        };

        public SuperContainer(final PlatformContainer platform) {
            this.platform = platform;
        }

        public Node resolveComponent(final Class<?> api, final ContextDefinition context, final Traversal traversal, final Type reference) {
            return !platform.containsComponent(api, context) ? null : new Node() {
                public Class<?> type() {
                    return api;
                }

                public Object instance(final Traversal traversal) {
                    return platform.getComponent(api, context);
                }

                public ComponentContext context() {
                    return null;
                }
            };
        }

        public Node resolveGroup(final Class<?> api, final ContextDefinition context, final Traversal traversal, final Type reference) {
            return !platform.containsComponentGroup(api, context) ? null : new Node() {
                public Class<?> type() {
                    return api;
                }

                public Object instance(final Traversal traversal) {
                    return platform.getComponentGroup(api, context);
                }

                public ComponentContext context() {
                    return null;
                }
            };
        }

        public List<GroupResolver.Node> resolveGroup(final ParentContainer domain,
                                                     final Class<?> api,
                                                     final Traversal traversal,
                                                     final ContextDefinition context,
                                                     final Type reference) {
            return !platform.containsComponentGroup(api, context) ? null : Collections.<GroupResolver.Node>singletonList(new GroupResolver.Node() {
                public Collection<?> instance(final Traversal traversal) {
                    return Arrays.asList(platform.getComponentGroup(api, context));
                }
            });
        }

        public Node resolveGroup(final ParentContainer domain,
                                 final Class<?> api,
                                 final ContextDefinition context,
                                 final Traversal traversal,
                                 final Annotation[] annotations,
                                 final Type reference) {
            return resolveGroup(api, context, traversal, reference);
        }

        public List<GroupResolver> groupResolvers(final Class<?> api) {
            return emptyList;
        }

        public ContextNode contexts(final ParentContainer domain, final Class<?> type, final ContextDefinition context) {
            return platform.containsComponent(type, context) ? noContexts : null;
        }

        public ComponentResolver resolver(final Class<?> api, final boolean ascend) {
            return null;
        }

        public void replaceResolver(final Class<?> key, final ComponentResolver previous, final ComponentResolver replacement) {
            // empty
        }

        public String id() {
            return platform.id();
        }

        public Node resolveComponent(final ParentContainer domain,
                                     final boolean ascend,
                                     final Class<?> api,
                                     final ContextDefinition context,
                                     final Traversal traversal,
                                     final Type reference) {
            throw new UnsupportedOperationException();
        }

        public ContainerServices services() {
            throw new UnsupportedOperationException();
        }

        public SimpleContainer parentContainer() {
            throw new UnsupportedOperationException();
        }

        public SimpleContainer newChildContainer(final boolean domain) {
            throw new UnsupportedOperationException();
        }

        public ComponentResolver bindResolver(final Class<?> key, final ComponentResolver entry) throws OpenComponentContainer.BindingException {
            throw new UnsupportedOperationException();
        }

        public void bindComponent(final Components.Interfaces interfaces) throws OpenComponentContainer.BindingException {
            throw new UnsupportedOperationException();
        }

        public void bindInstance(final Object instance, final Components.Interfaces interfaces) throws OpenComponentContainer.BindingException {
            throw new UnsupportedOperationException();
        }

        public SimpleContainer linkComponent(final Components.Interfaces interfaces) throws OpenComponentContainer.BindingException {
            throw new UnsupportedOperationException();
        }

        public Object initialize(final Object component, final ContextDefinition context, final ComponentResolutionObserver observer) throws ComponentContainer.ResolutionException {
            throw new UnsupportedOperationException();
        }

        public Object invoke(final Object component, final Method method, final ContextDefinition copy) {
            throw new UnsupportedOperationException();
        }

        public <T> T observe(final ComponentResolutionObserver observer, final Observed<T> command) {
            throw new UnsupportedOperationException();
        }

        public DependencyResolver dependencyResolver(final ParentContainer domain) {
            throw new UnsupportedOperationException();
        }
    }

    private class DelegatingDependencyResolver extends EmptyDependencyGraph implements DependencyResolver {

        private final ParentContainer domain;

        public DelegatingDependencyResolver(final ParentContainer domain) {
            this.domain = domain;
        }

        public ContextNode contexts(final Class<?> type, final ContextDefinition context) {
            return SimpleContainerImpl.this.contexts(domain, type, context);
        }

        public ComponentContainer container(final ContextDefinition context) {
            return SimpleContainerImpl.this.container(context);
        }

        public Node resolveGroup(final Class<?> api,
                                 final ContextDefinition context,
                                 final Traversal traversal,
                                 final Annotation[] annotations,
                                 final Type reference) {
            return SimpleContainerImpl.this.resolveGroup(domain, api, context, traversal, annotations, reference);
        }

        public Node resolveComponent(final Class<?> api, final ContextDefinition context, final Traversal traversal, final Type reference) {
            return SimpleContainerImpl.this.resolveComponent(domain, true, api, context, traversal, reference);
        }

        public Node resolveGroup(final Class<?> api, final ContextDefinition context, final Traversal traversal, final Type reference) {
            return SimpleContainerImpl.this.resolveGroup(domain, api, context, traversal, null, reference);
        }
    }
}
