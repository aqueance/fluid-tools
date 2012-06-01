/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import java.lang.reflect.Type;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.ComponentContextDescriptor;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.container.spi.DependencyGraph;
import org.fluidity.composition.container.spi.DependencyResolver;
import org.fluidity.composition.spi.ComponentInterceptor;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class DependencyInterceptorsTest extends MockGroupAbstractTest {

    private final ContextDefinition context = mock(ContextDefinition.class);
    private final ContextDefinition copy = mock(ContextDefinition.class);
    private final ContextDefinition accepted = mock(ContextDefinition.class);
    private final ComponentContext passed = mock(ComponentContext.class);

    private final DependencyResolver resolver = mock(DependencyResolver.class);
    private final DependencyGraph.Traversal traversal = mock(DependencyGraph.Traversal.class);
    private final DependencyGraph.Node node = mock(DependencyGraph.Node.class);
    private final DependencyGraph.Node group = mock(DependencyGraph.Node.class);

    private final ComponentInterceptor interceptor1 = mock(ComponentInterceptor.class);
    private final ComponentInterceptor.Dependency dependency1 = mock(ComponentInterceptor.Dependency.class);

    private final ComponentInterceptor interceptor2 = mock(ComponentInterceptor.class);
    private final ComponentInterceptor.Dependency dependency2 = mock(ComponentInterceptor.Dependency.class);

    private final ComponentInterceptor interceptor3 = mock(ComponentInterceptor.class);
    private final ComponentInterceptor.Dependency dependency3 = mock(ComponentInterceptor.Dependency.class);

    private final DependencyInterceptors interceptors = new DependencyInterceptorsImpl(NoLogFactory.consume(DependencyInterceptorsImpl.class));

    @Test
    public void testNoNode() throws Exception {
        replay();
        assert interceptors.replace(resolver, context, traversal, Serializable.class, null) == null;
        verify();
    }

    @Test
    public void testNoInterceptors() throws Exception {
        EasyMock.expect(resolver.resolveGroup(EasyMock.same(ComponentInterceptor.class),
                                              EasyMock.<ContextDefinition>notNull(),
                                              EasyMock.same(traversal)))
                .andReturn(null);

        EasyMock.expect(context.filter(EasyMock.aryEq(DependencyInterceptorsImpl.NO_INTERCEPTORS))).andReturn(DependencyInterceptorsImpl.NO_INTERCEPTORS);

        replay();
        assert interceptors.replace(resolver, context, traversal, Serializable.class, node) == node;
        verify();
    }

    @Test
    public void testEmptyInterceptors() throws Exception {
        EasyMock.expect(resolver.resolveGroup(EasyMock.same(ComponentInterceptor.class), EasyMock.<ContextDefinition>notNull(), EasyMock.same(traversal)))
                .andReturn(group);

        EasyMock.expect(group.instance(traversal)).andReturn(new ComponentInterceptor[0]);
        EasyMock.expect(context.filter(EasyMock.aryEq(DependencyInterceptorsImpl.NO_INTERCEPTORS))).andReturn(DependencyInterceptorsImpl.NO_INTERCEPTORS);

        replay();
        assert interceptors.replace(resolver, context, traversal, Serializable.class, node) == node;
        verify();
    }

    @Test
    public void testNoMatchingInterceptor() throws Exception {
        EasyMock.expect(resolver.resolveGroup(EasyMock.same(ComponentInterceptor.class),
                                              EasyMock.<ContextDefinition>notNull(),
                                              EasyMock.same(traversal)))
                .andReturn(group);

        final ComponentInterceptor[] found = { interceptor1 };

        EasyMock.expect(group.instance(traversal)).andReturn(found);
        EasyMock.expect(context.filter(EasyMock.<ComponentContextDescriptor[]>notNull())).andAnswer(filter(true, found));

        replay();
        assert interceptors.replace(resolver, context, traversal, Serializable.class, node) == node;
        verify();
    }

    @Test
    public void testMatchingInterceptor() throws Exception {
        EasyMock.expect(resolver.resolveGroup(EasyMock.same(ComponentInterceptor.class),
                                              EasyMock.<ContextDefinition>notNull(),
                                              EasyMock.same(traversal)))
                .andReturn(group);

        final ComponentInterceptor[] found = { interceptor1, interceptor2, interceptor3 };
        final ComponentInterceptor.Dependency[] dependencies = {
                null,
                dependency1,
                dependency2,
                dependency3
        };

        assert dependencies.length == found.length + 1;

        EasyMock.expect(group.instance(traversal)).andReturn(found);
        EasyMock.expect(context.filter(EasyMock.<ComponentContextDescriptor[]>notNull())).andAnswer(filter(false, found));

        for (int i = 0, limit = found.length; i < limit; i++) {
            final ComponentInterceptor interceptor = found[i];
            EasyMock.expect(context.copy()).andReturn(copy);
            EasyMock.expect(copy.accept(interceptor.getClass())).andReturn(accepted);
            EasyMock.expect(accepted.create()).andReturn(passed);

            EasyMock.expect(interceptor.intercept(EasyMock.same(Serializable.class),
                                                  EasyMock.same(passed),
                                                  dependencies[i] == null ? EasyMock.<ComponentInterceptor.Dependency>notNull() : EasyMock.same(dependencies[i])))
                    .andReturn(dependencies[i + 1]);
        }

        replay();
        final DependencyGraph.Node replacement = interceptors.replace(resolver, context, traversal, Serializable.class, node);
        verify();

        assert replacement != node;

        final ComponentContext context = localMock(ComponentContext.class);

        EasyMock.expect((Class) node.type()).andReturn(Serializable.class);
        EasyMock.expect(node.context()).andReturn(context);

        replay();
        assert replacement.type() == Serializable.class;
        assert replacement.context() == context;
        verify();

        final Object value = new Object();

        EasyMock.expect(dependencies[found.length].create()).andReturn(value);

        replay();
        assert replacement.instance(traversal) == value;
        verify();
    }

    @Test
    public void testComponentSink() throws Exception {
        EasyMock.expect(resolver.resolveGroup(EasyMock.same(ComponentInterceptor.class),
                                              EasyMock.<ContextDefinition>notNull(),
                                              EasyMock.same(traversal)))
                .andReturn(group);

        final ComponentInterceptor[] found = { interceptor1, interceptor2, interceptor3 };
        final ComponentInterceptor.Dependency[] dependencies = {
                null,
                dependency1,
        };

        assert dependencies.length < found.length + 1;

        EasyMock.expect(group.instance(traversal)).andReturn(found);
        EasyMock.expect(context.filter(EasyMock.<ComponentContextDescriptor[]>notNull())).andAnswer(filter(false, found));

        for (int i = 0, limit = found.length; i < limit; i++) {
            final ComponentInterceptor interceptor = found[i];
            EasyMock.expect(context.copy()).andReturn(copy);
            EasyMock.expect(copy.accept(interceptor.getClass())).andReturn(accepted);
            EasyMock.expect(accepted.create()).andReturn(passed);

            final boolean last = i == dependencies.length - 1;
            EasyMock.expect(interceptor.intercept(EasyMock.same(Serializable.class),
                                                  EasyMock.same(passed),
                                                  dependencies[i] == null ? EasyMock.<ComponentInterceptor.Dependency>notNull() : EasyMock.same(dependencies[i])))
                    .andReturn(last ? null : dependencies[i + 1]);

            if (last) {
                break;
            }
        }

        replay();
        assert interceptors.replace(resolver, context, traversal, Serializable.class, node) == null;
        verify();
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*intercept.*")
    public void testPrematureDereference() throws Exception {
        EasyMock.expect(resolver.resolveGroup(EasyMock.same(ComponentInterceptor.class),
                                              EasyMock.<ContextDefinition>notNull(),
                                              EasyMock.same(traversal)))
                .andReturn(group);

        final ComponentInterceptor[] found = {
                new ComponentInterceptor() {
                    public Dependency intercept(final Type reference, final ComponentContext context, final Dependency dependency) {
                        try {
                            return interceptor1.intercept(reference, context, dependency);
                        } finally {
                            dependency.create();
                            assert false : "Should not have reached this point.";
                        }
                    }
                },
                interceptor2
        };
        final ComponentInterceptor.Dependency[] dependencies = {
                null,
                dependency1,
                dependency2
        };

        assert dependencies.length == found.length + 1;

        EasyMock.expect(group.instance(traversal)).andReturn(found);
        EasyMock.expect(context.filter(EasyMock.<ComponentContextDescriptor[]>notNull())).andAnswer(filter(false, found));

        final ComponentInterceptor interceptor = found[0];

        EasyMock.expect(context.copy()).andReturn(copy);
        EasyMock.expect(copy.accept(interceptor.getClass())).andReturn(accepted);
        EasyMock.expect(accepted.create()).andReturn(passed);

        EasyMock.expect(interceptor1.intercept(EasyMock.same(Serializable.class),
                                               EasyMock.same(passed),
                                               dependencies[0] == null ? EasyMock.<ComponentInterceptor.Dependency>notNull() : EasyMock.same(dependencies[0])))
                .andReturn(dependencies[1]);

        replay();
        try {
            interceptors.replace(resolver, context, traversal, Serializable.class, node);
        } finally {
            verify();
        }
    }

    private IAnswer<ComponentContextDescriptor[]> filter(final boolean empty, final ComponentInterceptor... interceptors) {
        return new IAnswer<ComponentContextDescriptor[]>() {
            public ComponentContextDescriptor[] answer() throws Throwable {
                expect(EasyMock.getCurrentArguments()[0], interceptors);
                return empty ? DependencyInterceptorsImpl.NO_INTERCEPTORS : describe(interceptors);
            }

            private DependencyInterceptorsImpl.InterceptorDescriptor[] describe(final ComponentInterceptor[] interceptors) {
                final DependencyInterceptorsImpl.InterceptorDescriptor[] descriptors = new DependencyInterceptorsImpl.InterceptorDescriptor[interceptors.length];

                for (int i = 0, length = interceptors.length; i < length; i++) {
                    descriptors[i] = new DependencyInterceptorsImpl.InterceptorDescriptor(interceptors[i]);
                }

                return descriptors;
            }
        };
    }

    private void expect(final Object argument, final ComponentInterceptor... interceptors) throws Exception {
        DependencyInterceptorsImpl.InterceptorDescriptor[] descriptors = (DependencyInterceptorsImpl.InterceptorDescriptor[]) argument;
        assert descriptors != null;
        assert descriptors.length == interceptors.length;

        for (int i = 0, limit = descriptors.length; i < limit; i++) {
            assert descriptors[i].interceptor == interceptors[i];
        }
    }
}
