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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.ComponentVariantFactory;
import org.fluidity.foundation.logging.Log;

/**
 * @author Tibor Varga
 */
final class SimpleContainerImpl implements SimpleContainer {

    // cache listener to capture instantiation of components
    private final LocalListener listener = new LocalListener();

    private final ContainerServices services;
    private final Log log;

    private final SimpleContainer parent;
    private final Map<Class<?>, ComponentResolver> contents = new HashMap<Class<?>, ComponentResolver>();
    private final ReferenceChain referenceChain;
    private final ContextChain contextChain;
    private final ContextFactory contextFactory;
    private final DependencyInjector injector;

    public SimpleContainerImpl(final SimpleContainer parent, final ContainerServices services) {
        this.parent = parent;
        this.services = services;
        this.log = this.services.logs().createLog(getClass());
        this.referenceChain = this.services.referenceChain();
        this.contextChain = this.services.contextChain();
        this.contextFactory = this.services.contextFactory();
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
                replaceResolver(api, null, entry);
            } else {

                // TODO: variants handling is not OO
                if (entry.isVariantMapping()) {
                    final VariantResolver variants = (VariantResolver) entry;

                    if (resolver.isVariantMapping()) {
                        if (variants.isFallback() == resolver.isFallback()) {
                            throw new ComponentContainer.BindingException("Component %s already hijacked by %s", api, resolver.factoryClass());
                        } else if (!variants.isFallback()) {
                            replaceResolver(api, resolver, entry);
                            variants.resolverReplaced(null, ((VariantResolver) resolver).delegate());
                        }
                    } else if (resolver.isInstanceMapping()) {
                        throw new ComponentContainer.BindingException("Component instance %s cannot be hijacked by %s", api, entry.factoryClass());
                    } else {
                        replaceResolver(api, resolver, entry);
                        variants.resolverReplaced(null, resolver);
                    }
                } else {
                    if (resolver.isVariantMapping()) {
                        if (entry.isInstanceMapping()) {
                            throw new ComponentContainer.BindingException("Component instance %s cannot be hijacked by %s", api, resolver.factoryClass());
                        } else {
                            final VariantResolver variants = (VariantResolver) resolver;
                            final ComponentResolver delegate = variants.delegate();

                            if (delegate == null || (delegate.isFallback() && !entry.isFallback())) {
                                variants.resolverReplaced(delegate, entry);
                            } else {
                                throw new ComponentContainer.BindingException("Component %s already bound", api);
                            }
                        }
                    } else {
                        if (entry.isFallback() == resolver.isFallback()) {
                            throw new ComponentContainer.BindingException("Component %s already bound", api);
                        }

                        if (!entry.isFallback()) {
                            replaceResolver(api, resolver, entry);
                        }
                    }
                }
            }
        }
    }

    public void replaceResolver(final Class<?> key, final ComponentResolver previous, final ComponentResolver replacement) {
        contents.remove(key);
        replaceResolver(previous, replacement);
        contents.put(key, replacement);
    }

    public <T> T resolve(final Class<T> type, final ComponentContext context) {
        return get(type, context);
    }

    public <T> T create(final Class<T> type, final ComponentContext context) {
        final SimpleContainer child = new SimpleContainerImpl(this, services);
        child.bindComponent(type, type);
        return child.get(type, context);
    }

    public ComponentContainer container(final ComponentContext context) {
        return new EmbeddedContainer(this, context);
    }

    public void bindFactory(final Class<?>[] interfaces, Class<?> implementation, final boolean stateful, final ContentResolvers factories) {
        if (factories.isVariantFactory()) {

            // bind the variant factory to its class
            bindResolver(implementation, factories.component(implementation, services.newCache(listener, true)));

            final ComponentCache cache = services.newCache(listener, true);
            for (final Class<?> api : interfaces) {

                // bind the variant resolver to the component interface
                bindResolver(api, factories.variant(api, cache));
            }
        } else if (factories.isFactory()) {

            // bind the factory to its class
            bindResolver(implementation, factories.component(implementation, services.newCache(listener, true)));

            final ComponentCache cache = services.newCache(listener, true);
            for (final Class<?> api : interfaces) {

                // bind the factory resolver to the component interface
                bindResolver(api, factories.factory(api, cache));
            }
        } else {
            final ComponentCache cache = services.newCache(listener, !stateful);

            for (final Class<?> api : interfaces) {
                bindResolver(api, factories.component(api, cache));
            }
        }
    }

    public void bindComponent(final Class<?> implementation, final Class<?>... interfaces) {
        if (implementation == null) {
            throw new ComponentContainer.BindingException("Component class for %s is null", Arrays.toString(interfaces));
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

        final Component annotation = implementation.getAnnotation(Component.class);
        final boolean isStateful = annotation != null && annotation.stateful();
        final boolean isFallback = annotation != null && !annotation.primary();

        for (final Class<?> api : interfaces) {
            log.info("%s: binding %s to %s (%s, %s)", this, api, implementation, isStateful ? "stateful" : "stateless", isFallback ? "fallback" : "primary");
        }

        bindFactory(interfaces, implementation, isStateful, new ContentResolvers() {
            public boolean isVariantFactory() {
                return ComponentVariantFactory.class.isAssignableFrom(implementation);
            }

            public boolean isFactory() {
                return ComponentFactory.class.isAssignableFrom(implementation);
            }

            public ComponentResolver component(final Class<?> api, final ComponentCache cache) {
                return new ConstructingResolver(api, implementation, isFallback, cache, referenceChain, contextFactory, injector, services.logs());
            }

            public VariantResolver variant(final Class<?> api, final ComponentCache cache) {
                final Class<? extends ComponentVariantFactory> factory = implementation.asSubclass(ComponentVariantFactory.class);
                return new VariantResolverClass(SimpleContainerImpl.this, api, factory, isFallback, referenceChain, cache, services.logs());
            }

            public FactoryResolver factory(final Class<?> api, final ComponentCache cache) {
                final Class<? extends ComponentFactory> factory = implementation.asSubclass(ComponentFactory.class);
                return new FactoryResolverClass(api, factory, isFallback, referenceChain, cache, services.logs());
            }
        });
    }

    // TODO: duplicate in DependencyInjectorImpl
    private String toString(final Class<?> type) {
        final StringBuilder builder = new StringBuilder();

        Class<?> componentType = type;
        for (; componentType.isArray(); componentType = componentType.getComponentType()) {
            builder.append("[]");
        }

        return builder.insert(0, componentType).toString();
    }

    public void bindInstance(final Object instance, final Class<?>... interfaces) {
        if (instance == null) {
            throw new ComponentContainer.BindingException("Component instance for %s is null", Arrays.toString(interfaces));
        }

        final Component annotation = instance.getClass().getAnnotation(Component.class);
        final boolean isFallback = annotation != null && !annotation.primary();

        final String value = instance instanceof String || instance instanceof Number
                             ? ('\'' + String.valueOf(instance) + '\'')
                             : ("instance of " + toString(instance.getClass()));
        for (final Class<?> api : interfaces) {
            log.info("%s: binding %s to '%s' (%s)", this, toString(api), value, isFallback ? "fallback" : "primary");
        }

        bindFactory(interfaces, instance.getClass(), false, new ContentResolvers() {
            public boolean isVariantFactory() {
                return instance instanceof ComponentVariantFactory;
            }

            public boolean isFactory() {
                return instance instanceof ComponentFactory;
            }

            public ComponentResolver component(final Class<?> api, final ComponentCache cache) {
                return new InstanceResolver(api, instance.getClass(), instance, isFallback, referenceChain, services.logs());
            }

            @SuppressWarnings("ConstantConditions")
            public VariantResolver variant(final Class<?> api, final ComponentCache cache) {
                return new VariantResolverInstance(SimpleContainerImpl.this, api, (ComponentVariantFactory) instance, isFallback, referenceChain, cache, services.logs());
            }

            @SuppressWarnings("ConstantConditions")
            public FactoryResolver factory(final Class<?> api, final ComponentCache cache) {
                return new FactoryResolverInstance(api, (ComponentFactory) instance, isFallback, referenceChain, cache, services.logs());
            }
        });
    }

    public SimpleContainer linkComponent(final Class<?> implementation, final Class<?>... interfaces) throws ComponentContainer.BindingException {
        final SimpleContainer child = newChildContainer();

        child.bindComponent(implementation, interfaces);

        for (final Class<?> api : interfaces) {
            bindResolver(api, new LinkingResolver(child, api, child.resolver(api, false), referenceChain, services.logs()));
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
                        get(type, null);
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

    public <T> T get(final Class<? extends T> key) {
        final ComponentResolver resolver = contents.get(key);

        if (resolver == null) {
            return parent == null ? null : parent.get(key);
        } else {
            return referenceChain.track(resolver, key, new CreateCommand<T>(resolver, key));
        }
    }

    public <T> T get(final Class<? extends T> key, final ComponentContext context) {
        final ComponentResolver resolver = contents.get(key);

        if (resolver == null) {
            return parent == null ? null : parent.get(key, context);
        } else {
            return contextChain.track(context, new ContextChain.Command<T>() {
                public T run(final ComponentContext context) {
                    return referenceChain.track(resolver, key, new CreateCommand<T>(resolver, key));
                }
            });
        }
    }

    public <T> T initialize(final T component) {
        return injector.injectFields(this, component.getClass(), contextChain.currentContext(), component);
    }

    public String id() {
        final String id = String.format("%x", System.identityHashCode(this));
        return parent == null ? id : String.format("%s > %s", id, parent.id());
    }

    public void replaceResolver(ComponentResolver previous, ComponentResolver replacement) {
        for (final ComponentResolver resolver : contents.values()) {
            resolver.resolverReplaced(previous, replacement);
        }

        if (parent != null) {
            parent.replaceResolver(previous, replacement);
        }
    }

    @Override
    public String toString() {
        return String.format("container %s", id());
    }

    private class CreateCommand<T> implements ReferenceChain.Command<T> {

        private final ComponentResolver resolver;
        private final Class<? extends T> api;

        public CreateCommand(final ComponentResolver resolver, final Class<? extends T> api) {
            this.resolver = resolver;
            this.api = api;
        }

        public T run(final boolean circular) {
            final ContextChain.Command<T> command = new ContextChain.Command<T>() {
                @SuppressWarnings("unchecked")
                public T run(final ComponentContext context) {
                    return injector.injectFields(SimpleContainerImpl.this, api, context, (T) resolver.create(SimpleContainerImpl.this, api, circular));
                }
            };

            // TODO: falls through the variants to its shadowed resolver!
            final ComponentContext extracted = contextFactory.extractContext(resolver.componentClass().getAnnotations());
            if (extracted == null) {
                return command.run(contextChain.currentContext());
            } else {
                return contextChain.track(extracted, command);
            }
        }
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
         * @param api   the interface to which the component will be bound.
         * @param cache the cache to use for the component.
         *
         * @return a component resolver that will resolve, and cache if necessary, a single instance of the component.
         */
        ComponentResolver component(Class<?> api, final ComponentCache cache);

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
}
