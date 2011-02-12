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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.ComponentMapping;
import org.fluidity.composition.spi.ComponentVariantFactory;
import org.fluidity.foundation.Strings;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.spi.LogFactory;

/**
 * @author Tibor Varga
 */
final class SimpleContainerImpl implements SimpleContainer {

    // cache listener to capture instantiation of components
    // TODO: implement dependency graph discovery and get rid of this listener functionality
    private final LocalListener listener = new LocalListener();

    private final ContainerServices services;
    private final Log log;

    private final SimpleContainer parent;
    private final Map<Class<?>, ComponentResolver> contents = new HashMap<Class<?>, ComponentResolver>();
    private final DependencyInjector injector;

    public SimpleContainerImpl(final SimpleContainer parent, final ContainerServices services) {
        this.parent = parent;
        this.services = services;
        this.log = this.services.logs().createLog(getClass());
        this.injector = this.services.dependencyInjector();
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
        synchronized (contents) {
            final ComponentResolver resolver = contents.get(api);

            if (resolver == null) {
                replace(api, null, entry);
            } else if (resolver.isVariantMapping() && resolver.replaces(entry)) {
                replace(api, entry, resolver);
            } else if (entry.replaces(resolver)) {
                replace(api, resolver, entry);
            }
        }
    }

    public void replace(final Class<?> key, final ComponentResolver previous, final ComponentResolver replacement) {
        if (contents.remove(key) == previous) {
            replaceResolver(key, previous, replacement);
        }

        contents.put(key, replacement);
    }

    public <T> T resolveComponent(final Class<T> type, final ContextDefinition context) {
        return component(type, context);
    }

    public <T> T[] resolveGroup(final Class<T> type, final ContextDefinition context) throws ComponentContainer.ResolutionException {
        return group(type, context);
    }

    public ComponentContainer container(final ContextDefinition context) {
        return new ComponentContainerShell(this, services, context, false);
    }

    public ComponentMapping mapping(final Class<?> type) {
        return resolver(type, true);
    }

    public void bindResolvers(Class<?> implementation, final Class<?>[] componentInterfaces, final Class<?>[] groupInterfaces, final boolean stateful, final ContentResolvers resolvers) {
        if (resolvers.isVariantFactory()) {

            // bind the variant factory to its class
            bindResolver(implementation, resolvers.component(implementation, services.newCache(listener, true), true));

            final ComponentCache cache = services.newCache(listener, true);
            for (final Class<?> api : componentInterfaces) {

                // bind the variant resolver to the component interface
                bindResolver(api, resolvers.variant(api, cache));
            }
        } else if (resolvers.isFactory()) {

            // bind the factory to its class
            bindResolver(implementation, resolvers.component(implementation, services.newCache(listener, true), true));

            final ComponentCache cache = services.newCache(listener, true);
            for (final Class<?> api : componentInterfaces) {

                // bind the factory resolver to the component interface
                bindResolver(api, resolvers.factory(api, cache));
            }
        } else {
            final ComponentCache cache = services.newCache(listener, !stateful);

            if (componentInterfaces != null) {
                for (final Class<?> api : componentInterfaces) {
                    bindResolver(api, resolvers.component(api, cache, false));
                }
            }

            if (groupInterfaces != null && groupInterfaces.length > 0) {
                bindResolver(implementation, resolvers.component(implementation, cache, false));

                for (final Class<?> api : groupInterfaces) {
                    final ComponentResolver resolver = resolver(api, false);

                    final GroupResolver group;

                    if (resolver == null) {
                        bindResolver(api, group = new GroupResolver());
                    } else if (!resolver.isGroupMapping()) {
                        throw new ComponentContainer.BindingException("Component %s is already bound", api);
                    } else {
                        group = (GroupResolver) resolver;
                    }

                    group.add(implementation);
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
                log.info("%s: binding %s to %s (%s, %s)", this, api, implementation, isStateful ? "stateful" : "stateless", isFallback ? "fallback" : "primary");
            }
        }

        if (groupInterfaces != null) {
            for (final Class<?> api : groupInterfaces) {
                log.info("%s: adding %s to group %s (%s, %s)", this, implementation, api, isStateful ? "stateful" : "stateless", isFallback ? "fallback" : "primary");
            }
        }

        bindResolvers(implementation, componentInterfaces, groupInterfaces, isStateful, new ContentResolvers() {
            public boolean isVariantFactory() {
                return ComponentVariantFactory.class.isAssignableFrom(implementation);
            }

            public boolean isFactory() {
                return ComponentFactory.class.isAssignableFrom(implementation);
            }

            public ComponentResolver component(final Class<?> api, final ComponentCache cache, final boolean resolvesFactory) {
                return new ConstructingResolver(isFallback ? 0 : 1, api, implementation, resolvesFactory, cache, injector, services.logs());
            }

            public VariantResolver variant(final Class<?> api, final ComponentCache cache) {
                final Class<? extends ComponentVariantFactory> factory = implementation.asSubclass(ComponentVariantFactory.class);
                return new VariantResolverClass(isFallback ? 0 : 1, SimpleContainerImpl.this, api, factory, cache, services.logs());
            }

            public FactoryResolver factory(final Class<?> api, final ComponentCache cache) {
                final Class<? extends ComponentFactory> factory = implementation.asSubclass(ComponentFactory.class);
                return new FactoryResolverClass(isFallback ? 0 : 1, api, factory, cache, services.logs());
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

            public boolean isFactory() {
                return instance instanceof ComponentFactory;
            }

            public ComponentResolver component(final Class<?> api, final ComponentCache cache, final boolean resolvesFactory) {
                return new InstanceResolver(isFallback ? 0 : 1, api, instance, services.logs());
            }

            @SuppressWarnings("ConstantConditions")
            public VariantResolver variant(final Class<?> api, final ComponentCache cache) {
                return new VariantResolverInstance(isFallback ? 0 : 1,
                                                   SimpleContainerImpl.this,
                                                   api,
                                                   (ComponentVariantFactory) instance,
                                                   cache,
                                                   services.logs());
            }

            @SuppressWarnings("ConstantConditions")
            public FactoryResolver factory(final Class<?> api, final ComponentCache cache) {
                return new FactoryResolverInstance(isFallback ? 0 : 1, api, (ComponentFactory) instance, cache, services.logs());
            }
        });
    }

    public SimpleContainer linkComponent(final Class<?> implementation, final Class<?>[] componentInterfaces, final Class<?>[] groupInterfaces) throws ComponentContainer.BindingException {
        final LogFactory logs = services.logs();
        final SimpleContainer child = newChildContainer();

        child.bindComponent(implementation, componentInterfaces, groupInterfaces);


        if (componentInterfaces != null) {
            for (final Class<?> api : componentInterfaces) {
                bindResolver(api, new LinkingResolver(child, api, child.resolver(api, false), logs));
            }
        }

        return child;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> allSingletons(final Class<T> api) {
        final List<T> instances = new ArrayList<T>();

        listener.capture(new ComponentCache.Listener() {
            public void created(final Class<?> ignore, Object component) {
                if (api.isAssignableFrom(component.getClass())) {
                    instances.add((T) component);
                }
            }
        }, new Runnable() {
            public void run() {
                for (final Class<?> type : contents.keySet()) {
                    if (api.isAssignableFrom(type)) {
                        component(type, null);
                    }
                }
            }
        });

        return instances;
    }

    public ComponentResolver resolver(final Class<?> api, final boolean ascend) {
        final ComponentResolver resolver = contents.get(api);
        return resolver == null && parent != null && ascend ? parent.resolver(api, ascend) : resolver;
    }

    @SuppressWarnings("unchecked")
    public <T> T component(final Class<? extends T> key, final ContextDefinition context) {
        final ComponentResolver resolver = contents.get(key);

        if (resolver == null) {
            return parent == null ? null : parent.component(key, context);
        } else if (resolver.isGroupMapping()) {
            return null;
        } else {
            return services.dependencyChain().follow(context, resolver, new DependencyChain.Command<T>() {
                public T run(final DependencyChain.Lineage lineage, final ContextDefinition context) {
                    return injector.injectFields(SimpleContainerImpl.this,
                                                 resolver,
                                                 context,
                                                 (T) resolver.getComponent(lineage, context, SimpleContainerImpl.this, key))[0];
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T[] group(final Class<? extends T> key, final ContextDefinition context) throws ComponentContainer.ResolutionException {
        final ComponentResolver resolver = contents.get(key);

        if (resolver == null) {
            return parent == null ? null : parent.group(key, context);
        } else if (!resolver.isGroupMapping()) {
            return null;
        } else {
            final T[] nested = services.dependencyChain().follow(context, resolver, new DependencyChain.Command<T[]>() {
                public T[] run(final DependencyChain.Lineage lineage, final ContextDefinition context) {
                    return injector.injectFields(SimpleContainerImpl.this,
                                                 resolver,
                                                 context,
                                                 (T[]) resolver.getComponent(lineage, context, SimpleContainerImpl.this, key));
                }
            });

            final T[] enclosing = parent == null ? null : parent.group(key, context);

            if (enclosing != null && enclosing.length > 0) {
                final List<T> list = new ArrayList<T>(enclosing.length + nested.length);

                list.addAll(Arrays.asList(enclosing));
                list.addAll(Arrays.asList(nested));

                return list.toArray((T[]) Array.newInstance(key, list.size()));
            } else {
                return nested;
            }
        }
    }

    public <T> T initialize(final T component, final ContextDefinition context) {
        final ComponentMapping mapping = new InstanceMapping(component.getClass());

        return services.dependencyChain().follow(context, mapping, new DependencyChain.Command<T>() {
            public T run(final DependencyChain.Lineage lineage, final ContextDefinition context) {
                return injector.injectFields(SimpleContainerImpl.this, mapping, context, component)[0];
            }
        });
    }

    public String id() {
        final String id = String.format("%x", System.identityHashCode(this));
        return parent == null ? id : String.format("%s > %s", id, parent.id());
    }

    public void replaceResolver(final Class<?> key, final ComponentResolver previous, final ComponentResolver replacement) {
        for (final ComponentResolver resolver : contents.values()) {
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
        boolean isFactory();

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

    private static class LocalListener implements ComponentCache.Listener {

        private ThreadLocal<ComponentCache.Listener> delegate = new ThreadLocal<ComponentCache.Listener>();

        public void created(final Class<?> type, final Object component) {
            final ComponentCache.Listener delegate = this.delegate.get();
            if (delegate != null) {
                delegate.created(type, component);
            }
        }

        public void capture(final ComponentCache.Listener delegate, final Runnable command) {
            this.delegate.set(delegate);
            try {
                command.run();
            } finally {
                this.delegate.remove();
            }
        }
    }

    private class InstanceMapping implements ComponentMapping {

        private final Class<?> componentClass;

        public InstanceMapping(Class<?> componentClass) {
            this.componentClass = componentClass;
        }

        public boolean isFactoryMapping() {
            return false;
        }

        public <T extends Annotation> T contextSpecification(final Class<T> type) {
            return componentClass.getAnnotation(type);
        }

        public Annotation[] providedContext() {
            return componentClass.getAnnotations();
        }
    }
}
