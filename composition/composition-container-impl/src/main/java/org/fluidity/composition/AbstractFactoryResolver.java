package org.fluidity.composition;

import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.composition.spi.Factory;
import org.fluidity.foundation.spi.LogFactory;

import java.util.Arrays;

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
        final ContextDefinition collected = context.copy();

        factory(container, traversal, context).newComponent(new ComponentContainerShell(child, collected.reduce(null), false), reduced.create());

        return cachingNode(child.resolveComponent(api, context.collect(Arrays.asList(reduced, collected)), traversal), child);
    }
}
