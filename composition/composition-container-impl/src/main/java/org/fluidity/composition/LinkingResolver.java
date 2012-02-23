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
import java.lang.reflect.Type;

import org.fluidity.composition.spi.ContextDefinition;
import org.fluidity.composition.spi.DependencyGraph;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Links a component binding in the parent container to a resolver in the child. The component when looked up in the parent ends up handled by the child.
 *
 * @author Tibor Varga
 */
final class LinkingResolver extends AbstractResolver {

    private final SimpleContainer target;
    private ComponentResolver delegate;

    public LinkingResolver(final SimpleContainer container, final Class<?> api, final ComponentResolver delegate, final LogFactory logs) {
        super(delegate.priority(), api, null, logs);

        this.delegate = delegate;
        this.target = container;

        this.delegate.skipParent();
    }

    public Annotation[] providedContext() {
        return delegate.providedContext();
    }

    public Class<?> contextConsumer() {
        return delegate.contextConsumer();
    }

    public DependencyGraph.Node resolve(final ParentContainer domain,
                                        final DependencyGraph.Traversal traversal,
                                        final SimpleContainer container,
                                        final ContextDefinition context,
                                        final Type reference) {
        assert target.parentContainer() == container : api;
        return delegate.resolve(domain, traversal, target, context, reference);
    }

    @Override
    public boolean isVariantMapping() {
        return delegate.isVariantMapping();
    }

    @Override
    public boolean isInstanceMapping() {
        return delegate.isInstanceMapping();
    }

    @Override
    public Object cached(final Object domain, final String source, final ComponentContext context) {
        return delegate.cached(domain, source, context);
    }

    @Override
    public boolean replaces(final ComponentResolver resolver) {
        return delegate.replaces(resolver);
    }

    @Override
    public void resolverReplaced(final Class<?> api, final ComponentResolver previous, final ComponentResolver replacement) {
        if (delegate == previous) {
            delegate = replacement;
        }
    }

    @Override
    public String toString() {
        return String.format("%s (link)", delegate.toString());
    }
}
