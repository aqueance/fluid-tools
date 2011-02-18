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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.fluidity.composition.network.ContextDefinition;
import org.fluidity.composition.network.Graph;

/**
 * Component mapping for a component group.
 *
 * @author Tibor Varga
 */
final class GroupResolver implements ComponentResolver {

    private final Set<Class<?>> members = new LinkedHashSet<Class<?>>();

    public boolean isFactoryMapping() {
        return false;
    }

    public <T extends Annotation> T contextSpecification(final Class<T> type) {
        return null;
    }

    public Annotation[] providedContext() {
        return null;
    }

    public Graph.Node resolve(final Graph.Traversal traversal, final SimpleContainer container, final ContextDefinition context, final boolean explore) {
        // TODO: this is not right, groups is not really an edge
        throw new UnsupportedOperationException();
    }

    // TODO: this is not right, groups is not really an edge
    public void resolve(final Graph.Traversal traversal, final SimpleContainer container, final ContextDefinition context, final boolean explore, final List<Graph.Node> list) {
        for (final Class<?> member : members) {
            list.add(container.resolver(member, false).resolve(traversal, container, context, explore));
        }
    }

    public int priority() {
        return 0;
    }

    public boolean isVariantMapping() {
        return false;
    }

    public boolean isInstanceMapping() {
        return false;
    }

    public boolean isGroupMapping() {
        return true;
    }

    public boolean replaces(final ComponentResolver resolver) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public Object getComponent(final ContextDefinition context, final SimpleContainer container, final Class<?> api) {
        final List instances = new ArrayList();

        for (final Class<?> member : members) {
            instances.add(container.component(member, context));
        }

        return instances.toArray((Object[]) Array.newInstance(api, instances.size()));
    }

    public void add(final Class<?> implementation) {
        members.add(implementation);
    }

    public void resolverReplaced(final Class<?> api, final ComponentResolver previous, final ComponentResolver replacement) {
        // empty
    }
}
