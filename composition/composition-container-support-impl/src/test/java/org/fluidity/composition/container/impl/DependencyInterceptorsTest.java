/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.container.impl;

import java.io.Serializable;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.container.spi.DependencyResolver;
import org.fluidity.composition.spi.ComponentInterceptor;
import org.fluidity.composition.spi.Dependency;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public class DependencyInterceptorsTest extends Simulator {

    private final MockObjects dependencies = dependencies();

    private final InterceptorFilter annotations = dependencies.normal(InterceptorFilter.class);
    private final ContextDefinition context = dependencies.normal(ContextDefinition.class);
    private final ContextDefinition copy = dependencies.normal(ContextDefinition.class);
    private final ContextDefinition accepted = dependencies.normal(ContextDefinition.class);
    private final ComponentContext passed = dependencies.normal(ComponentContext.class);

    private final DependencyResolver resolver = dependencies.normal(DependencyResolver.class);
    private final DependencyGraph.Traversal traversal = dependencies.normal(DependencyGraph.Traversal.class);
    private final DependencyGraph.Node node = dependencies.normal(DependencyGraph.Node.class);
    private final DependencyGraph.Node group = dependencies.normal(DependencyGraph.Node.class);

    private final ComponentInterceptor interceptor1 = dependencies.normal(ComponentInterceptor.class);
    private final Dependency dependency1 = dependencies.normal(Dependency.class);

    private final ComponentInterceptor interceptor2 = dependencies.normal(ComponentInterceptor.class);
    private final Dependency dependency2 = dependencies.normal(Dependency.class);

    private final ComponentInterceptor interceptor3 = dependencies.normal(ComponentInterceptor.class);
    private final Dependency dependency3 = dependencies.normal(Dependency.class);

    private final DependencyInterceptors interceptors = new DependencyInterceptorsImpl(annotations, NoLogFactory.consume(DependencyInterceptorsImpl.class));

    @Test
    public void testNoNode() throws Exception {
        final DependencyGraph.Node node = verify(() -> interceptors.replace(resolver, context, traversal, Serializable.class, null));

        assert node == null;
    }

    @Test
    public void testNoInterceptors() throws Exception {
        EasyMock.expect(resolver.resolveGroup(EasyMock.same(ComponentInterceptor.class),
                                              EasyMock.notNull(),
                                              EasyMock.same(traversal),
                                              EasyMock.same(ComponentInterceptor.class)))
                .andReturn(null);

        EasyMock.expect(annotations.filter(EasyMock.same(context), EasyMock.aryEq(DependencyInterceptorsImpl.NO_INTERCEPTORS))).andReturn(DependencyInterceptorsImpl.NO_INTERCEPTORS);

        final DependencyGraph.Node replaced = verify(() -> interceptors.replace(resolver, context, traversal, Serializable.class, node));

        assert replaced == node;
    }

    @Test
    public void testEmptyInterceptors() throws Exception {
        EasyMock.expect(resolver.resolveGroup(EasyMock.same(ComponentInterceptor.class),
                                              EasyMock.notNull(),
                                              EasyMock.same(traversal),
                                              EasyMock.same(ComponentInterceptor.class)))
                .andReturn(group);

        EasyMock.expect(group.instance(traversal)).andReturn(new ComponentInterceptor[0]);
        EasyMock.expect(annotations.filter(EasyMock.same(context), EasyMock.aryEq(DependencyInterceptorsImpl.NO_INTERCEPTORS))).andReturn(DependencyInterceptorsImpl.NO_INTERCEPTORS);

        final DependencyGraph.Node replaced = verify(() -> interceptors.replace(resolver, context, traversal, Serializable.class, node));

        assert replaced == node;
    }

    @Test
    public void testNoMatchingInterceptor() throws Exception {
        EasyMock.expect(resolver.resolveGroup(EasyMock.same(ComponentInterceptor.class),
                                              EasyMock.notNull(),
                                              EasyMock.same(traversal),
                                              EasyMock.same(ComponentInterceptor.class)))
                .andReturn(group);

        final ComponentInterceptor[] found = { interceptor1 };

        EasyMock.expect(group.instance(traversal)).andReturn(found);
        EasyMock.expect(annotations.filter(EasyMock.same(context), EasyMock.notNull())).andAnswer(filter(true, found));

        final DependencyGraph.Node replaced = verify(() -> interceptors.replace(resolver, context, traversal, Serializable.class, node));

        assert replaced == node;
    }

    @Test
    public void testMatchingInterceptor() throws Exception {
        EasyMock.expect(resolver.resolveGroup(EasyMock.same(ComponentInterceptor.class),
                                              EasyMock.notNull(),
                                              EasyMock.same(traversal),
                                              EasyMock.same(ComponentInterceptor.class)))
                .andReturn(group);

        final ComponentInterceptor[] found = { interceptor1, interceptor2, interceptor3 };
        final Dependency[] dependencies = {
                null,
                dependency1,
                dependency2,
                dependency3
        };

        assert dependencies.length == found.length + 1;

        EasyMock.expect(group.instance(traversal)).andReturn(found);
        EasyMock.expect(annotations.filter(EasyMock.same(context), EasyMock.notNull())).andAnswer(filter(false, found));

        for (int i = 0, limit = found.length; i < limit; i++) {
            final ComponentInterceptor interceptor = found[i];
            EasyMock.expect(context.copy()).andReturn(copy);
            EasyMock.expect(copy.accept(interceptor.getClass())).andReturn(accepted);
            EasyMock.expect(accepted.create()).andReturn(passed);

            EasyMock.expect(interceptor.intercept(EasyMock.same(Serializable.class),
                                                  EasyMock.same(passed),
                                                  dependencies[i] == null ? EasyMock.notNull() : EasyMock.same(dependencies[i])))
                    .andReturn(dependencies[i + 1]);
        }

        final DependencyGraph.Node replacement = verify(() -> interceptors.replace(resolver, context, traversal, Serializable.class, node));

        assert replacement != node;

        test(() -> {
            final ComponentContext context = arguments().normal(ComponentContext.class);

            EasyMock.expect(dependency3.type()).andReturn(Serializable.class);
            EasyMock.expect(node.context()).andReturn(context);

            verify(() -> {
                assert replacement.type() == Serializable.class;
                assert replacement.context() == context;
            });
        });

        test(() -> {
            final Object value = new Object();

            EasyMock.expect(node.type()).andReturn((Class) Serializable.class);
            EasyMock.expect(dependencies[found.length].instance()).andReturn(value);

            final Object instance = verify(() -> replacement.instance(traversal));

            assert instance == value;
        });
    }

    @Test
    public void testComponentSink() throws Exception {
        EasyMock.expect(resolver.resolveGroup(EasyMock.same(ComponentInterceptor.class),
                                              EasyMock.notNull(),
                                              EasyMock.same(traversal),
                                              EasyMock.same(ComponentInterceptor.class)))
                .andReturn(group);

        final ComponentInterceptor[] found = { interceptor1, interceptor2, interceptor3 };
        final Dependency[] dependencies = {
                null,
                dependency1,
        };

        assert dependencies.length < found.length + 1;

        EasyMock.expect(group.instance(traversal)).andReturn(found);
        EasyMock.expect(annotations.filter(EasyMock.same(context), EasyMock.notNull())).andAnswer(filter(false, found));

        for (int i = 0, limit = found.length; i < limit; i++) {
            final ComponentInterceptor interceptor = found[i];
            EasyMock.expect(context.copy()).andReturn(copy);
            EasyMock.expect(copy.accept(interceptor.getClass())).andReturn(accepted);
            EasyMock.expect(accepted.create()).andReturn(passed);

            final boolean last = i == dependencies.length - 1;
            EasyMock.expect(interceptor.intercept(EasyMock.same(Serializable.class),
                                                  EasyMock.same(passed),
                                                  dependencies[i] == null ? EasyMock.notNull() : EasyMock.same(dependencies[i])))
                    .andReturn(last ? null : dependencies[i + 1]);

            if (last) {
                break;
            }
        }

        final DependencyGraph.Node replaced = verify(() -> interceptors.replace(resolver, context, traversal, Serializable.class, node));

        assert replaced == null;
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*intercept.*")
    public void testPrematureDereference() throws Exception {
        EasyMock.expect(resolver.resolveGroup(EasyMock.same(ComponentInterceptor.class),
                                              EasyMock.notNull(),
                                              EasyMock.same(traversal),
                                              EasyMock.same(ComponentInterceptor.class)))
                .andReturn(group);

        final ComponentInterceptor[] found = {
                (reference, context, dependency) -> {
                    try {
                        return interceptor1.intercept(reference, context, dependency);
                    } finally {
                        dependency.instance();
                        assert false : "Should not have reached this point.";
                    }
                },
                interceptor2
        };

        final Dependency[] dependencies = {
                null,
                dependency1,
                dependency2
        };

        assert dependencies.length == found.length + 1;

        EasyMock.expect(group.instance(traversal)).andReturn(found);
        EasyMock.expect(annotations.filter(EasyMock.same(context), EasyMock.notNull())).andAnswer(filter(false, found));

        final ComponentInterceptor interceptor = found[0];

        EasyMock.expect(context.copy()).andReturn(copy);
        EasyMock.expect(copy.accept(interceptor.getClass())).andReturn(accepted);
        EasyMock.expect(accepted.create()).andReturn(passed);

        EasyMock.expect(interceptor1.intercept(EasyMock.same(Serializable.class),
                                               EasyMock.same(passed),
                                               dependencies[0] == null ? EasyMock.notNull() : EasyMock.same(dependencies[0])))
                .andReturn(dependencies[1]);

        guarantee((Task) () -> interceptors.replace(resolver, context, traversal, Serializable.class, node));
    }

    private IAnswer<ComponentInterceptor[]> filter(final boolean empty, final ComponentInterceptor... interceptors) {
        return () -> {
            expect(EasyMock.getCurrentArguments()[1], interceptors);
            return empty ? DependencyInterceptorsImpl.NO_INTERCEPTORS : interceptors;
        };
    }

    private void expect(final Object argument, final ComponentInterceptor... actual) throws Exception {
        final ComponentInterceptor[] expected = (ComponentInterceptor[]) argument;
        assert expected != null;
        assert expected.length == actual.length;

        for (int i = 0, limit = expected.length; i < limit; i++) {
            assert expected[i] == actual[i];
        }
    }
}
