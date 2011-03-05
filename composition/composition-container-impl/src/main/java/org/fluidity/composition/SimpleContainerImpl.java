/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.composition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.ComponentMapping;
import org.fluidity.composition.spi.ComponentResolutionObserver;
import org.fluidity.composition.spi.ComponentVariantFactory;
import org.fluidity.foundation.Strings;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * @author Tibor Varga
 */
final class SimpleContainerImpl implements ParentContainer {

    // allows traversal path to propagate between containers
    private static final ThreadLocal<DependencyGraph.Traversal> traversal = new InheritableThreadLocal<DependencyGraph.Traversal>();

    // allows per container maintenance of current resolver
    private static final ThreadLocal<ComponentResolver> resolved = new InheritableThreadLocal<ComponentResolver>();

    private final ContainerServices services;
    private final Log log;

    private final ParentContainer parent;

    private final Map<Class<?>, ComponentResolver> components = new HashMap<Class<?>, ComponentResolver>();
    private final Map<Class<?>, GroupResolver> groups = new HashMap<Class<?>, GroupResolver>();

    private final DependencyInjector injector;
    private final LogFactory logs;

    public SimpleContainerImpl(final ContainerServices services) {
        this(null, services);
    }

    private SimpleContainerImpl(final ParentContainer parent, final ContainerServices services) {
        this.parent = parent;
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

    public SimpleContainer newChildContainer() {
        return new SimpleContainerImpl(this, services);
    }

    public ComponentResolver resolved() {
        return resolved.get();
    }

    public void bindResolver(final Class<?> api, final ComponentResolver resolver) {
        synchronized (components) {
            final ComponentResolver previous = components.get(api);

            if (previous == null) {
                replace(api, null, resolver);
            } else if (previous.isVariantMapping() && previous.replaces(resolver)) {
                replace(api, resolver, previous);
            } else if (resolver.replaces(previous)) {
                replace(api, previous, resolver);
            }
        }
    }

    private GroupResolver bindGroup(final Class<?> api) {
        GroupResolver resolver;

        synchronized (groups) {
            resolver = groups.get(api);

            if (resolver == null) {
                groups.put(api, resolver = new GroupResolver());
            }
        }

        return resolver;
    }

    public void replace(final Class<?> key, final ComponentResolver previous, final ComponentResolver replacement) {
        if (components.remove(key) == previous) {
            replaceResolver(key, previous, replacement);
        }

        components.put(key, replacement);
    }

    private ComponentResolver local(final ComponentResolver resolver) {
        return new LocalResolver(resolver);
    }

    public void bindResolvers(Class<?> implementation,
                              final Class<?>[] componentInterfaces,
                              final Class<?>[] groupInterfaces,
                              final boolean stateful,
                              final ContentResolvers resolvers) {
        if (resolvers.isVariantFactory()) {
            bindResolver(implementation, local(resolvers.component(implementation, services.newCache(true), true)));

            final ComponentCache cache = services.newCache(true);
            if (componentInterfaces != null) {
                for (final Class<?> api : componentInterfaces) {
                    bindResolver(api, local(resolvers.variant(api, cache)));
                }
            }

            if (groupInterfaces != null) {
                assert componentInterfaces != null : implementation;

                for (final Class<?> api : groupInterfaces) {
                    for (final Class<?> component : componentInterfaces) {
                        bindGroup(api).addResolver(component, resolver(component, false));
                    }
                }
            }
        } else if (resolvers.isCustomFactory()) {
            bindResolver(implementation, local(resolvers.component(implementation, services.newCache(true), true)));

            final ComponentCache cache = services.newCache(true);
            if (componentInterfaces != null) {
                for (final Class<?> api : componentInterfaces) {
                    bindResolver(api, local(resolvers.factory(api, cache)));
                }
            }

            if (groupInterfaces != null) {
                assert componentInterfaces != null : implementation;

                for (final Class<?> api : groupInterfaces) {
                    for (final Class<?> component : componentInterfaces) {
                        bindGroup(api).addResolver(component, resolver(component, false));
                    }
                }
            }
        } else {
            final ComponentCache cache = services.newCache(!stateful);
            if (componentInterfaces == null || componentInterfaces.length == 0) {
                throw new ComponentContainer.BindingException("Component interface list empty for %s", implementation);
            }

            for (final Class<?> api : componentInterfaces) {
                bindResolver(api, local(resolvers.component(api, cache, false)));
            }

            if (groupInterfaces != null) {
                for (final Class<?> api : groupInterfaces) {
                    bindGroup(api).addResolver(implementation, resolver(implementation, false));
                }
            }
        }
    }

    public void bindComponent(final Class<?> implementation, final Class<?>[] componentInterfaces, final Class<?>[] groupInterfaces) {
        if (implementation == null) {
            throw new ComponentContainer.BindingException("Component class for %s is null", print(null, componentInterfaces));
        }

        /*
         * No synthetic classes or anonymous inner classes are allowed.
         */
        final boolean isSyntheticClass = implementation.isSynthetic();
        final boolean isAnonymousClass = implementation.isAnonymousClass();

        if (isSyntheticClass || isAnonymousClass) {
            throw new ComponentContainer.BindingException("Component %s is not instantiable (%s)", implementation, isSyntheticClass ? "synthetic" : "anonymous");
        }

        final Component componentSpec = implementation.getAnnotation(Component.class);
        final boolean isStateful = componentSpec != null && componentSpec.stateful();
        final boolean isFallback = componentSpec != null && !componentSpec.primary();

        if (componentInterfaces != null) {
            log.info("%s: binding %s to %s (%s, %s)", this, implementation, print(implementation, componentInterfaces), isStateful ? "stateful" : "stateless", isFallback ? "fallback" : "primary");
        }

        if (groupInterfaces != null) {
            log.info("%s: adding %s to group %s (%s, %s)", this, implementation, print(null, groupInterfaces), isStateful ? "stateful" : "stateless", isFallback ? "fallback" : "primary");
        }

        bindResolvers(implementation, componentInterfaces, groupInterfaces, isStateful, new ContentResolvers() {
            public boolean isVariantFactory() {
                return ComponentVariantFactory.class.isAssignableFrom(implementation);
            }

            public boolean isCustomFactory() {
                return ComponentFactory.class.isAssignableFrom(implementation);
            }

            public ComponentResolver component(final Class<?> api, final ComponentCache cache, final boolean resolvesFactory) {
                return new ConstructingResolver(isFallback ? 0 : 1, api, implementation, resolvesFactory, cache, injector, logs);
            }

            public VariantResolver variant(final Class<?> api, final ComponentCache cache) {
                final Class<? extends ComponentVariantFactory> factory = implementation.asSubclass(ComponentVariantFactory.class);
                return new VariantResolverClass(isFallback ? 0 : 1, SimpleContainerImpl.this, api, factory, cache, logs);
            }

            public FactoryResolver factory(final Class<?> api, final ComponentCache cache) {
                final Class<? extends ComponentFactory> factory = implementation.asSubclass(ComponentFactory.class);
                return new FactoryResolverClass(isFallback ? 0 : 1, api, factory, cache, logs);
            }
        });
    }

    public void bindInstance(final Object instance, final Class<?>[] componentInterfaces, final Class<?>[] groupInterfaces) {
        if (instance == null) {
            throw new ComponentContainer.BindingException("Component instance for %s is null", print(null, componentInterfaces));
        }

        final Class<?> implementation = instance.getClass();
        final Component componentSpec = implementation.getAnnotation(Component.class);
        final boolean isFallback = componentSpec != null && !componentSpec.primary();

        final String value = instance instanceof String || instance instanceof Number
                             ? ('\'' + String.valueOf(instance) + '\'')
                             : ("instance of " + Strings.arrayNotation(implementation));

        if (componentInterfaces != null) {
            log.info("%s: binding '%s' to %s (%s)", this, value, print(implementation, componentInterfaces), isFallback ? "fallback" : "primary");
        }

        if (groupInterfaces != null) {
            log.info("%s: adding '%s' to group %s (%s)", this, value, print(null, groupInterfaces), isFallback ? "fallback" : "primary");
        }

        bindResolvers(implementation, componentInterfaces, groupInterfaces, false, new ContentResolvers() {
            public boolean isVariantFactory() {
                return instance instanceof ComponentVariantFactory;
            }

            public boolean isCustomFactory() {
                return instance instanceof ComponentFactory;
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
                return new FactoryResolverInstance(isFallback ? 0 : 1, api, (ComponentFactory) instance, cache, logs);
            }
        });
    }

    private String print(final Class<?> self, final Class<?>[] classes) {
        final StringBuilder text = new StringBuilder();

        boolean multiple = false;
        for (final Class<?> type : classes) {
            if (type != self) {
                if (text.length() > 0) {
                    text.append(", ");
                    multiple = true;
                }

                text.append(Strings.arrayNotation(type));
            }
        }

        return (multiple ? text.insert(0, '[').append(']') : text).toString();
    }

    public SimpleContainer linkComponent(final Class<?> implementation, final Class<?>[] componentInterfaces, final Class<?>[] groupInterfaces)
            throws ComponentContainer.BindingException {
        final LogFactory logs = this.logs;
        final SimpleContainer child = newChildContainer();

        child.bindComponent(implementation, componentInterfaces, groupInterfaces);

        if (componentInterfaces != null) {
            for (final Class<?> api : componentInterfaces) {
                bindResolver(api, new LinkingResolver(child, api, child.resolver(api, false), logs));
            }
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

    public Object initialize(final Object component, final ContextDefinition context, final ComponentResolutionObserver observer) {
        return injector.fields(services.graphTraversal(), this, new InstanceMapping(component), context, component);
    }

    public Node resolveComponent(final Class<?> api, final ContextDefinition context, final Traversal traversal) {
        final ComponentResolver resolver = components.get(api);

        if (resolver == null) {
            return parent == null ? null : parent.resolveComponent(api, context, traversal);
        } else {
            return resolver.resolve(traversal, SimpleContainerImpl.this, context);
        }
    }

    public Node resolveGroup(final Class<?> api, final ContextDefinition context, final Traversal traversal) {
        final GroupResolver group = groups.get(api);
        final Object empty = Array.newInstance(api, 0);

        if (group == null) {
            return parent == null ? null : parent.resolveGroup(api, context, traversal);
        } else {
            final Class<?> arrayApi = empty.getClass();

            return traversal.follow(this, context, new Node.Reference() {
                public Class<?> api() {
                    return arrayApi;
                }

                public Node resolve(final Traversal traversal, final ContextDefinition context) {
                    return groupNode(api, resolveGroup(api, traversal, context), context);
                }
            });
        }
    }

    public List<GroupResolver.Node> resolveGroup(final Class<?> api, final Traversal traversal, final ContextDefinition context) {
        final List<GroupResolver.Node> enclosing = parent == null ? null : parent.resolveGroup(api, traversal, context);
        final GroupResolver group = groups.get(api);

        if (group == null) {
            return enclosing;
        } else {
            final List<GroupResolver.Node> list = new ArrayList<GroupResolver.Node>();

            if (enclosing != null) {
                list.addAll(enclosing);
            }

            list.add(group.resolve(api, traversal, this, context));

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
            public Object instance() {
                final List output = new ArrayList();

                for (final GroupResolver.Node node : list) {
                    output.addAll(node.instance());
                }

                return output.toArray((Object[]) Array.newInstance(api, output.size()));
            }

            public ComponentContext context() {
                return componentContext;
            }
        };
    }

    public ComponentMapping mapping(final Class<?> type) {
        return resolver(type, true);
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

    private class InstanceMapping implements ComponentMapping {

        private final Class<?> componentClass;

        public InstanceMapping(final Object component) {
            this.componentClass = component.getClass();
        }

        public Set<Class<? extends Annotation>> acceptedContext() {
            return AbstractResolver.acceptedContext(componentClass);
        }

        public Annotation[] annotations() {
            return componentClass.getAnnotations();
        }
    }

    /**
     * Keeps track of the last resolver being invoked in this container.
     */
    private class LocalResolver implements ComponentResolver {

        private final ComponentResolver delegate;

        public LocalResolver(final ComponentResolver delegate) {
            this.delegate = delegate;
        }

        public Node resolve(final Traversal traversal, final SimpleContainer container, final ContextDefinition context) {
            final ComponentResolver saved = resolved.get();
            resolved.set(this);
            try {
                return delegate.resolve(traversal, container, context);
            } finally {
                resolved.set(saved);
            }
        }

        public int priority() {
            return delegate.priority();
        }

        public boolean isVariantMapping() {
            return delegate.isVariantMapping();
        }

        public boolean isFactoryMapping() {
            return delegate.isFactoryMapping();
        }

        public boolean isInstanceMapping() {
            return delegate.isInstanceMapping();
        }

        public boolean replaces(final ComponentResolver another) {
            return delegate.replaces(another);
        }

        public void resolverReplaced(final Class<?> api, final ComponentResolver previous, final ComponentResolver replacement) {
            delegate.resolverReplaced(api, previous, replacement);
        }

        public ComponentResolver unlink() {
            return delegate;
        }

        public Set<Class<? extends Annotation>> acceptedContext() {
            return delegate.acceptedContext();
        }

        public Annotation[] annotations() {
            return delegate.annotations();
        }

        @Override
        public boolean equals(final Object obj) {
            return delegate.equals(obj instanceof LocalResolver ? ((LocalResolver) obj).delegate : obj);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
