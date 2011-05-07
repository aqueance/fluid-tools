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
import java.util.Set;

import org.fluidity.composition.spi.ComponentVariantFactory;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Component mapping for a {@link ComponentVariantFactory} component.
 *
 * @author Tibor Varga
 */
abstract class VariantResolver extends AbstractFactoryResolver {

    private final SimpleContainer parent;

    private ComponentResolver delegate;     // the one creating instances

    public VariantResolver(final int priority,
                           final SimpleContainer container,
                           final Class<?> api,
                           final Class<? extends ComponentVariantFactory> factoryClass,
                           final ComponentCache cache,
                           final LogFactory logs) {
        super(factoryClass, priority, api, cache, logs);
        this.parent = container.parentContainer();

        if (factoryClass.getAnnotation(Context.class) == null) {
            throw new ComponentContainer.BindingException("Factory %s is not annotated by @%s", factoryClass, Context.class);
        }
    }

    @Override
    public boolean replaces(final ComponentResolver resolver) {
        final int check = resolver.priority();

        if (resolver.isVariantMapping()) {
            final VariantResolver variants = (VariantResolver) resolver;
            if (variants == this) {
                return false;
            } else if (check == priority()) {
                throw new ComponentContainer.BindingException("Component %s already hijacked by %s", api, variants.factoryClass());
            } else {
                final boolean replaces = super.replaces(variants);

                if (replaces) {
                    resolverReplaced(api, null, variants.delegate());
                }

                return replaces;
            }
        } else if (resolver.isInstanceMapping()) {
            throw new ComponentContainer.BindingException("Component instance %s cannot be hijacked by %s", api, factoryClass());
        } else {
            if (delegate == null || resolver.replaces(delegate)) {
                resolverReplaced(api, delegate, resolver);
            }

            return true;
        }
    }

    private ComponentResolver findDelegate() {
        if (delegate == null) {
            delegate = parent == null ? null : parent.resolver(api, true);

            if (delegate == null) {
                throw new ComponentContainer.ResolutionException("No component bound to %s", api);
            }
        }

        return delegate;
    }

    public Annotation[] annotations() {
        return null;
    }

    public Set<Class<? extends Annotation>> acceptedContext() {
        return AbstractResolver.acceptedContext(factoryClass());
    }

    @Override
    public void resolverReplaced(final Class<?> api, final ComponentResolver previous, final ComponentResolver replacement) {
        if (api == this.api && delegate == previous) {
            if (replacement.isInstanceMapping()) {
                throw new ComponentContainer.BindingException("Component %s cannot be hijacked by %s", this.api, ((VariantResolver) replacement).factoryClass());
            }

            delegate = replacement;
        }
    }

    public ComponentResolver delegate() {
        return delegate;
    }

    @Override
    public boolean isVariantMapping() {
        return true;
    }

    public DependencyGraph.Node resolve(final ParentContainer domain, final DependencyGraph.Traversal traversal, final SimpleContainer container, final ContextDefinition context) {
        final SimpleContainer child = container.newChildContainer(false);

        child.bindResolver(api, findDelegate());

        return resolve(domain, traversal, container, context, child);
    }

    @Override
    public String toString() {
        return String.format("%s (via %s)", delegate == null ? api : delegate.toString(), factoryClass().getName());
    }
}
