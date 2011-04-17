package org.fluidity.composition;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.fluidity.composition.spi.Factory;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Encapsulates common functionality among various factory resolvers.
 */
abstract class AbstractFactoryResolver extends AbstractResolver {

    private final Class<? extends Factory> factoryClass;

    public AbstractFactoryResolver(final Class<? extends Factory> factoryClass,
                                   final int priority,
                                   final Class<?> api,
                                   final ComponentCache cache,
                                   final LogFactory logs) {
        super(priority, api, cache, logs);
        this.factoryClass = factoryClass;
    }

    protected final Class<? extends Factory> factoryClass() {
        return factoryClass;
    }

    /**
     * Returns the {@link org.fluidity.composition.spi.Factory} instance this is a mapping for.
     *
     * @param container  the container in which to resolve dependencies of the factory.
     * @param traversal  the current graph traversal.
     * @param definition the context in which the resolution takes place.
     *
     * @return the {@link org.fluidity.composition.spi.Factory} instance this is a mapping for.
     */
    protected abstract Factory factory(final SimpleContainer container, final DependencyGraph.Traversal traversal, final ContextDefinition definition);

    /**
     * Invokes the factory and performs proper context housekeeping.
     *
     * @param traversal the current dependency traversal.
     * @param container the original container.
     * @param context   the current component context.
     * @param child     the child of the original container to pass to the factory.
     *
     * @return the graph node for the component.
     */
    protected final DependencyGraph.Node resolve(final DependencyGraph.Traversal traversal,
                                                 final SimpleContainer container,
                                                 final ContextDefinition context,
                                                 final SimpleContainer child) {
        final ContextDefinition reduced = context.copy().reduce(acceptedContext());
        final ContextDefinition collected = context.copy().reduce(null);
        final ComponentContext passed = reduced.create();

        final Factory factory = factory(container, traversal, context);

        final List<ContextDefinition> list = new ArrayList<ContextDefinition>();
        list.add(reduced);

        final DependencyInjector injector = container.services().dependencyInjector();

        final Factory.Instance instance = factory.resolve(new Factory.Resolver() {
            public <T> Factory.Dependency<T> resolve(final Class<T> api) {
                return new NodeDependency<T>(resolve(api, null), traversal);
            }

            public DependencyGraph.Node resolve(final Class<?> api, final Annotation[] annotations) {
                final ContextDefinition copy = collected.copy();
                list.add(copy);
                // TODO: use injector to resolve special dependencies, such as context and container
                return child.resolveComponent(api, annotations == null ? copy : copy.expand(annotations), traversal);
            }

            public Factory.Dependency<?>[] discover(final Class<?> type) {
                return discover(injector.findConstructor(type));
            }

            public Factory.Dependency<?>[] discover(final Constructor<?> constructor) {
                final Class<?>[] types = constructor.getParameterTypes();
                final Annotation[][] annotations = constructor.getParameterAnnotations();

                final List<Factory.Dependency<?>> nodes = new ArrayList<Factory.Dependency<?>>();
                for (int i = 0, limit = types.length; i < limit; i++) {
                    nodes.add(new NodeDependency<Object>(resolve(types[i], annotations[i]), traversal));
                }

                return nodes.toArray(new Factory.Dependency<?>[nodes.size()]);
            }
        }, passed);

        final ContextDefinition saved = context.collect(list).copy();
        final ComponentContext actual = saved.create();

        return cachingNode(new DependencyGraph.Node() {
            public Class<?> type() {
                return api;
            }

            public Object instance(final DependencyGraph.Traversal traversal) {
                instance.bind(new RegistryWrapper(child));
                return child.resolveComponent(api, saved, traversal).instance(traversal);
            }

            public ComponentContext context() {
                return actual;
            }
        }, child);
    }

    @SuppressWarnings("unchecked")
    private class RegistryWrapper implements Factory.Registry {
        private final SimpleContainer container;

        public RegistryWrapper(final SimpleContainer container) {
            this.container = container;
        }

        public <T> void bindComponent(final Class<T> implementation, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
            container.bindComponent(Components.inspect(implementation, interfaces));
        }

        public <T> void bindInstance(final T instance, final Class<? super T>... interfaces) throws ComponentContainer.BindingException {
            assert instance != null;
            final Class<T> implementation = (Class<T>) instance.getClass();
            container.bindInstance(instance, Components.inspect(implementation, interfaces));
        }

        public Factory.Registry makeChildContainer() {
            return new RegistryWrapper(container.newChildContainer());
        }
    }

    private class NodeDependency<T> implements Factory.Dependency<T> {
        private final DependencyGraph.Node node;
        private final DependencyGraph.Traversal traversal;

        public NodeDependency(final DependencyGraph.Node node, final DependencyGraph.Traversal traversal) {
            this.node = node;
            this.traversal = traversal;
        }

        @SuppressWarnings("unchecked")
        public T instance() {
            return node == null ? null : (T) node.instance(traversal);
        }
    }
}
