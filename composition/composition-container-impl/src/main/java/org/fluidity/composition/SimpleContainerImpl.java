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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.spi.ComponentCache;
import org.fluidity.composition.spi.ContainerServices;
import org.fluidity.composition.spi.ContextChain;
import org.fluidity.composition.spi.ContextFactory;
import org.fluidity.composition.spi.DependencyInjector;
import org.fluidity.composition.spi.ReferenceChain;
import org.fluidity.foundation.Log;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
final class SimpleContainerImpl implements SimpleContainer {

    private final ContainerServices services;
    private final Log log;

    private final SimpleContainer parent;
    private final Map<Class<?>, ComponentProducer> contents = new HashMap<Class<?>, ComponentProducer>();
    private final ReferenceChain referenceChain;
    private final ContextChain contextChain;
    private final DependencyInjector injector;

    public SimpleContainerImpl(final SimpleContainer parent, final ContainerServices services) {
        this.parent = parent;
        this.services = services;
        this.log = services.logs().createLog(getClass());

        this.referenceChain = this.services.referenceChain();
        this.contextChain = this.services.contextChain();
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

    public ComponentProducer bindProducer(final Class<?> key, final ComponentProducer entry) {
        assert !entry.isVariantMapping();

        synchronized (contents) {
            final ComponentProducer producer = contents.get(key);

            if (producer == null) {
                contents.put(key, entry);
            } else if (producer.isVariantMapping()) {
                if (entry.isInstanceMapping()) {
                    throw new ComponentContainer.BindingException("Component instance %s cannot be hijacked by %s",
                                                                  producer.componentInterface(),
                                                                  producer.factoryClass());
                } else {
                    ((VariantProducer) producer).setDelegate(entry);
                }
            } else {
                throw new ComponentContainer.BindingException("Component %s already bound", key);
            }
        }

        return entry;
    }

    private <T> ComponentProducer bindVariants(final Class<T> key, final VariantProducer entry) {
        synchronized (contents) {
            final ComponentProducer producer = contents.get(key);

            if (producer == null) {
                contents.put(key, entry);
            } else if (producer.isVariantMapping()) {
                throw new ComponentContainer.BindingException("Component %s already hijacked by %s", producer.componentInterface(), producer.factoryClass());
            } else if (producer.isInstanceMapping()) {
                throw new ComponentContainer.BindingException("Component instance %s cannot be hijacked by %s",
                                                              producer.componentInterface(),
                                                              entry.factoryClass());
            } else {
                entry.setDelegate(producer);
                contents.put(key, entry);
            }
        }

        return entry;
    }

    public ReferenceChain referenceChain() {
        return referenceChain;
    }

    public ContextChain contextChain() {
        return contextChain;
    }

    public ContextFactory contextFactory() {
        return services.contextFactory();
    }

    public <T> T resolve(final Class<T> type, final ComponentContext context) {
        return get(type, context);
    }

    public <T> T create(final Class<T> type, final ComponentContext context) {
        final SimpleContainer nested = new SimpleContainerImpl(this, services);
        nested.bindComponent(type, type);
        return nested.get(type, context);
    }

    public ComponentContainer container(final ComponentContext context) {
        return new EmbeddedContainer(this, context);
    }

    /**
     * Internal interface to generalize the binding of components, including ordinary ones, factories and variant factories.
     */
    private static interface ContentProducers {

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
         * Creates a component producer for the component being processed.
         *
         * @param cache the cache to use by the producer if necessary.
         *
         * @return a component producer that will produce, and cache if necessary, a single instance of a component.
         */
        ComponentProducer component(ComponentCache cache);

        /**
         * Creates a variant factory for the component being processed.
         *
         * @param cache the cache to use by the producer if necessary.
         *
         * @return a variant producer that will produce, and cache if necessary, a single instance of a variant factory.
         */
        VariantProducer variant(ComponentCache cache);

        /**
         * Creates a component factory for the component being processed.
         *
         * @param cache the cache to use by the producer if necessary.
         *
         * @return a component producer that will produce, and cache if necessary, a single instance of a component factory.
         */
        FactoryProducer factory(ComponentCache cache);
    }

    public ComponentProducer bindFactory(final ContentProducers factories) {
        final ComponentProducer producer = factories.component(services.newCache());

        if (factories.isVariantFactory()) {
            bindProducer(producer.componentClass(), producer);
            final VariantProducer factory = factories.variant(services.newCache());
            return bindVariants(factory.componentInterface(), factory);
        } else if (factories.isFactory()) {
            bindProducer(producer.componentClass(), producer);
            final FactoryProducer factory = factories.factory(services.newCache());
            return bindProducer(factory.componentInterface(), factory);
        } else {
            return bindProducer(producer.componentInterface(), producer);
        }
    }

    public ComponentProducer bindComponent(final Class<?> key, final Class<?> implementation) {
        if (implementation == null) {
            throw new ComponentContainer.BindingException("Component class for %s is null", key.getName());
        }

        log.info("%s: binding %s to %s", this, key, implementation);

        return bindFactory(new ContentProducers() {
            public boolean isVariantFactory() {
                return ComponentVariantFactory.class.isAssignableFrom(implementation);
            }

            public boolean isFactory() {
                return ComponentFactory.class.isAssignableFrom(implementation);
            }

            public ComponentProducer component(final ComponentCache cache) {
                return new ConstructingProducer(key, implementation, cache, injector);
            }

            public VariantProducer variant(final ComponentCache cache) {
                return new VariantProducerClass(implementation.asSubclass(ComponentVariantFactory.class), cache, SimpleContainerImpl.this);
            }

            public FactoryProducer factory(final ComponentCache cache) {
                return new FactoryProducerClass(implementation.asSubclass(ComponentFactory.class), cache);
            }
        });
    }

    private String handleArrayClass(final Class<?> rootType) {
        final StringBuilder builder = new StringBuilder();

        Class<?> componentType = rootType;
        for (; componentType.isArray(); componentType = componentType.getComponentType()) {
            builder.append("[]");
        }
        return builder.insert(0, componentType).toString();
    }

    public ComponentProducer bindInstance(final Class<?> key, final Object instance) {
        if (instance == null) {
            throw new ComponentContainer.BindingException("Component instance for %s is null", key);
        }

        final String value = instance instanceof String || instance instanceof Number
                             ? ('\'' + String.valueOf(instance) + '\'')
                             : ("instance of " + handleArrayClass(instance.getClass()));
        log.info("%s: binding %s to '%s'", this, handleArrayClass(key), value);

        return bindFactory(new ContentProducers() {
            public boolean isVariantFactory() {
                return instance instanceof ComponentVariantFactory;
            }

            public boolean isFactory() {
                return instance instanceof ComponentFactory;
            }

            public ComponentProducer component(final ComponentCache cache) {
                return new InstanceProducer(key, instance.getClass(), instance);
            }

            @SuppressWarnings("ConstantConditions")
            public VariantProducer variant(final ComponentCache cache) {
                return new VariantProducerInstance((ComponentVariantFactory) instance, cache, SimpleContainerImpl.this);
            }

            @SuppressWarnings("ConstantConditions")
            public FactoryProducer factory(final ComponentCache cache) {
                return new FactoryProducerInstance((ComponentFactory) instance, cache);
            }
        });
    }

    public SimpleContainer linkComponent(final Class<?> key, final Class<?> implementation) throws ComponentContainer.BindingException {
        final SimpleContainer child = newChildContainer();
        bindProducer(key, new LinkingProducer(child.bindComponent(key, implementation), child));
        return child;
    }

    public <T> List<T> allSingletons(final Class<T> componentInterface) {
        final List<T> instances = new ArrayList<T>();

        final ComponentCache.Listener listener = new ComponentCache.Listener() {
            public void created(final Class<?> ignored, final Object component) {
                if (componentInterface.isAssignableFrom(component.getClass())) {
                    instances.add((T) component);
                }
            }
        };

        // This smells like kludge: AbstractProducer may not be the best place for this conceptually
        AbstractProducer.captureCreation(listener, new Runnable() {
            public void run() {
                for (final Class<?> type : contents.keySet()) {
                    get(type, null);
                }
            }
        });

        return instances;
    }

    public ComponentProducer producer(final Class<?> componentInterface, final boolean ascend) {
        final ComponentProducer producer = contents.get(componentInterface);
        return producer == null && parent != null && ascend ? parent.producer(componentInterface, ascend) : producer;
    }

    public <T> T get(final Class<? extends T> key) {
        final ComponentProducer producer = contents.get(key);

        if (producer == null) {
            final T found = find(key);

            if (found != null || parent == null) {
                return found;
            } else {
                return parent.get(key);
            }
        } else {
            return referenceChain.nested(producer, key, new CreateCommand<T>(producer));
        }
    }

    public <T> T get(final Class<? extends T> key, final ComponentContext context) {
        final ComponentProducer producer = contents.get(key);

        if (producer == null) {
            final T found = contextChain.nested(context, new ContextChain.Command<T>() {
                public T run(final ComponentContext ignore) {
                    return find(key);
                }
            });

            if (found != null || parent == null) {
                return found;
            } else {
                return parent.get(key, context);
            }
        } else {
            return contextChain.nested(context, new ContextChain.Command<T>() {
                public T run(final ComponentContext context) {
                    return referenceChain.nested(producer, key, new CreateCommand<T>(producer));
                }
            });
        }
    }

    public <T> T initialize(final T component) {
        return injector.injectFields(this, component.getClass(), contextChain.currentContext(), component);
    }

    private <T> T find(final Class<? extends T> key) {
        ComponentProducer found = null;

        synchronized (contents) {
            for (final ComponentProducer producer : contents.values()) {
                final Class<?> componentClass = producer.componentClass();

                if (key.isAssignableFrom(componentClass)) {
                    if (found != null && found != producer) {
                        throw new ComponentContainer.ResolutionException("Multiple components found matching %s", key);
                    }

                    found = producer;
                }
            }

            if (found != null) {
                contents.put(key, found);
            }
        }

        if (found == null) {
            return null;
        } else {
            return referenceChain.nested(found, key, new CreateCommand<T>(found));
        }
    }

    private class CreateCommand<T> implements ReferenceChain.Command<T> {

        private final ComponentProducer producer;

        public CreateCommand(final ComponentProducer producer) {
            this.producer = producer;
        }

        public T run(final boolean circular) {
            final ComponentContext extracted = contextFactory().extractContext(producer.componentClass().getAnnotations());

            if (extracted == null) {
                return injector.injectFields(SimpleContainerImpl.this,
                                             producer.componentInterface(),
                                             contextChain.currentContext(),
                                             (T) producer.create(SimpleContainerImpl.this, circular));
            } else {
                return contextChain.nested(extracted, new ContextChain.Command<T>() {
                    public T run(final ComponentContext context) {
                        return injector.injectFields(SimpleContainerImpl.this,
                                                     producer.componentInterface(),
                                                     context,
                                                     (T) producer.create(SimpleContainerImpl.this, circular));
                    }
                });
            }
        }
    }

    public String id() {
        final String id = String.format("%x", System.identityHashCode(this));
        return parent == null ? id : String.format("%s > %s", id, parent.id());
    }

    @Override
    public String toString() {
        return String.format("container %s", id());
    }
}
