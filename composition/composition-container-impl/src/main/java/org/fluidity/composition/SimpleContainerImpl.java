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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.network.ContextDefinition;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.ComponentMapping;
import org.fluidity.composition.spi.ComponentVariantFactory;
import org.fluidity.foundation.Strings;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * @author Tibor Varga
 */
final class SimpleContainerImpl implements ParentContainer {

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

    public void bindResolver(final Class<?> api, final ComponentResolver entry) {
        synchronized (components) {
            final ComponentResolver resolver = components.get(api);

            if (resolver == null) {
                replace(api, null, entry);
            } else if (resolver.isVariantMapping() && resolver.replaces(entry)) {
                replace(api, entry, resolver);
            } else if (entry.replaces(resolver)) {
                replace(api, resolver, entry);
            }
        }
    }

    public GroupResolver bindGroup(final Class<?> api) {
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

    public void bindResolvers(Class<?> implementation,
                              final Class<?>[] componentInterfaces,
                              final Class<?>[] groupInterfaces,
                              final boolean stateful,
                              final ContentResolvers resolvers) {
        // TODO: factories and group interfaces?
        if (resolvers.isVariantFactory()) {

            // bind the variant factory to its class
            bindResolver(implementation, resolvers.component(implementation, services.newCache(true), true));

            final ComponentCache cache = services.newCache(true);
            for (final Class<?> api : componentInterfaces) {

                // bind the variant resolver to the component interface
                bindResolver(api, resolvers.variant(api, cache));
            }
        } else if (resolvers.isCustomFactory()) {

            // bind the factory to its class
            bindResolver(implementation, resolvers.component(implementation, services.newCache(true), true));

            final ComponentCache cache = services.newCache(true);
            for (final Class<?> api : componentInterfaces) {

                // bind the factory resolver to the component interface
                bindResolver(api, resolvers.factory(api, cache));
            }
        } else {
            final ComponentCache cache = services.newCache(!stateful);

            if (componentInterfaces != null) {
                for (final Class<?> api : componentInterfaces) {
                    bindResolver(api, resolvers.component(api, cache, false));
                }
            }

            if (groupInterfaces != null && groupInterfaces.length > 0) {
                bindResolver(implementation, resolvers.component(implementation, cache, false));

                for (final Class<?> api : groupInterfaces) {
                    bindGroup(api).add(implementation);
                }
            }
        }
    }

    public void bindComponent(final Class<?> implementation, final Class<?>[] componentInterfaces, final Class<?>[] groupInterfaces) {
        if (implementation == null) {
            throw new ComponentContainer.BindingException("Component class for %s is null", Arrays.toString(componentInterfaces));
        }

        /*
         * No synthetic classes or anonymous inner classes are allowed.
         */
        final boolean isSyntheticClass = implementation.isSynthetic();
        final boolean isAnonymousClass = implementation.isAnonymousClass();

        if (isSyntheticClass || isAnonymousClass) {
            throw new ComponentContainer.BindingException("Component %s is not instantiable (%s)",
                                                          implementation,
                                                          isSyntheticClass ? "synthetic" : "anonymous");
        }

        final Component componentSpec = implementation.getAnnotation(Component.class);
        final boolean isStateful = componentSpec != null && componentSpec.stateful();
        final boolean isFallback = componentSpec != null && !componentSpec.primary();

        if (componentInterfaces != null) {
            for (final Class<?> api : componentInterfaces) {
                log.info("%s: binding %s to %s (%s, %s)",
                         this,
                         api,
                         implementation,
                         isStateful ? "stateful" : "stateless",
                         isFallback ? "fallback" : "primary");
            }
        }

        if (groupInterfaces != null) {
            for (final Class<?> api : groupInterfaces) {
                log.info("%s: adding %s to group %s (%s, %s)",
                         this,
                         implementation,
                         api,
                         isStateful ? "stateful" : "stateless",
                         isFallback ? "fallback" : "primary");
            }
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
            throw new ComponentContainer.BindingException("Component instance for %s is null", Arrays.toString(componentInterfaces));
        }

        final Class<?> implementation = instance.getClass();
        final Component componentSpec = implementation.getAnnotation(Component.class);
        final boolean isFallback = componentSpec != null && !componentSpec.primary();

        final String value = instance instanceof String || instance instanceof Number
                             ? ('\'' + String.valueOf(instance) + '\'')
                             : ("instance of " + Strings.arrayNotation(implementation));

        if (componentInterfaces != null) {
            for (final Class<?> api : componentInterfaces) {
                log.info("%s: binding %s to '%s' (%s)", this, Strings.arrayNotation(api), value, isFallback ? "fallback" : "primary");
            }
        }

        if (groupInterfaces != null) {
            for (final Class<?> api : groupInterfaces) {
                log.info("%s: adding '%s' to group %s (%s)", this, value, Strings.arrayNotation(api), isFallback ? "fallback" : "primary");
            }
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

    public String id() {
        final String id = String.format("%x", System.identityHashCode(this));
        return parent == null ? id : String.format("%s > %s", id, parent.id());
    }

    public void replaceResolver(final Class<?> key, final ComponentResolver previous, final ComponentResolver replacement) {
        for (final ComponentResolver resolver : components.values()) {
            resolver.resolverReplaced(key, previous, replacement);
        }

        if (parent != null) {
            parent.replaceResolver(key, previous, replacement);
        }
    }

    @Override
    public String toString() {
        return String.format("container %s", id());
    }

    public Object initialize(final Object component, final ContextDefinition context, final Traversal.Observer observer) {
        return injector.fields(services.graphTraversal(), this, new InstanceMapping(component), context, component, observer);
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
            return parent == null ? new Node.Constant(api, empty, null) : parent.resolveGroup(api, context, traversal);
        } else {
            final Class<?> arrayApi = empty.getClass();

            return traversal.follow(this, context, new Reference() {
                public Class<?> api() {
                    return arrayApi;
                }

                public Node resolve(final Traversal traversal, final ContextDefinition context) {
                    return groupNode(api, resolveGroup(api, traversal, context), context);
                }
            });
        }
    }

    public List<Node> resolveGroup(final Class<?> api, final Traversal traversal, final ContextDefinition context) {
        @SuppressWarnings("unchecked")
        final List<Node> enclosing = parent == null ? (List<Node>) Collections.EMPTY_LIST : parent.resolveGroup(api, traversal, context);
        final GroupResolver group = groups.get(api);

        if (group == null) {
            return enclosing;
        } else {
            final List<Node> list = new ArrayList<Node>();
            list.addAll(enclosing);

            group.resolve(traversal, this, context, list);

            return list;
        }
    }

    private Node groupNode(final Class<?> api, final List<Node> list, final ContextDefinition context) {
        assert list != null : api;
        final ComponentContext componentContext = context.create();

        return new Node() {
            public Class<?> type() {
                return api;
            }

            public Object instance() {
                final Object[] instances = (Object[]) Array.newInstance(api, list.size());
                int i = 0;
                for (final Node node : list) {
                    instances[i++] = node.instance();
                }

                return instances;
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

        public <T extends Annotation> T contextSpecification(final Class<T> type) {
            return componentClass.getAnnotation(type);
        }

        public Annotation[] providedContext() {
            return componentClass.getAnnotations();
        }
    }
}
