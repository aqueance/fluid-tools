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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.spi.ComponentMapping;
import org.fluidity.composition.spi.ContainerProvider;
import org.fluidity.composition.spi.EmptyPackageBindings;
import org.fluidity.composition.spi.PackageBindings;
import org.fluidity.foundation.NullLogFactory;
import org.fluidity.foundation.spi.LogFactory;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Abstract test case for {@link ContainerProvider} implementations.
 *
 * @author Tibor Varga
 */
public abstract class ContainerProviderAbstractTest extends MockGroupAbstractTest {

    private final LogFactory logs = new NullLogFactory();

    private final ContainerServices services = addControl(ContainerServices.class);
    private final ClassDiscovery classDiscovery = addControl(ClassDiscovery.class);
    private final ReferenceChain referenceChain = addControl(ReferenceChain.class);
    private final ContextChain contextChain = addControl(ContextChain.class);
    private final ContextFactory contextFactory = addControl(ContextFactory.class);
    private final DependencyInjector dependencyInjector = addControl(DependencyInjector.class);
    private final ComponentCache componentCache = addControl(ComponentCache.class);

    private final Map<String, String> map = new HashMap<String, String>();

    private final ContainerProvider provider;      // to be provided by subclasses

    public ContainerProviderAbstractTest(final ContainerProvider provider) {
        this.provider = provider;

        map.clear();
        map.put("key", "value");
    }

    @BeforeMethod
    public void dependencies() {
        EasyMock.expect(services.logs()).andReturn(logs).anyTimes();
        EasyMock.expect(services.classDiscovery()).andReturn(classDiscovery).anyTimes();
        EasyMock.expect(services.referenceChain()).andReturn(referenceChain).anyTimes();
        EasyMock.expect(services.contextChain()).andReturn(contextChain).anyTimes();
        EasyMock.expect(services.contextFactory()).andReturn(contextFactory).anyTimes();
        EasyMock.expect(services.dependencyInjector()).andReturn(dependencyInjector).anyTimes();
        EasyMock.expect(services.newCache()).andReturn(componentCache).anyTimes();
    }

    @Test
    public void createsContainer() throws Exception {
        replay();
        assert provider.newContainer(services) != null;
        verify();
    }

    @Test
    public void standaloneBindings() throws Exception {
        final List assemblies = Collections.singletonList(StandalonePackageBindingsImpl.class);

        final PackageBindings bindings = new StandalonePackageBindingsImpl();

        EasyMock.expect(contextChain.nested(EasyMock.<ComponentContext>isNull(), EasyMock.<ContextChain.Command>notNull())).andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                assert EasyMock.getCurrentArguments()[0] == null;
                return ((ContextChain.Command) EasyMock.getCurrentArguments()[1]).run(null);
            }
        }).anyTimes();

        EasyMock.expect(referenceChain.nested(EasyMock.<ComponentMapping>notNull(), EasyMock.same(Map.class), EasyMock.<ReferenceChain.Command>notNull()))
                .andAnswer(new IAnswer<Object>() {
                    public Object answer() throws Throwable {
                        return map;
                    }
                });

        EasyMock.expect(referenceChain.nested(EasyMock.<ComponentMapping>notNull(),
                                              EasyMock.same(StandalonePackageBindingsImpl.class),
                                              EasyMock.<ReferenceChain.Command>notNull())).andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                return bindings;
            }
        });

        EasyMock.expect(referenceChain.nested(EasyMock.<ComponentMapping>notNull(), EasyMock.<Class<?>>notNull(), EasyMock.<ReferenceChain.Command>notNull()))
                .andAnswer(new IAnswer<Object>() {
                    public Object answer() throws Throwable {
                        return null;
                    }
                })
                .anyTimes();

        replay();
        provider.instantiateBindings(services, map, assemblies);
        verify();
    }

    @Test
    public void connectedBindings() throws Exception {
        final List assemblies = new ArrayList(Arrays.asList(PackageBindingsImpl.class,
                                                            DependentPackageBindingsImpl.class,
                                                            ResponsiblePackageBindingsImpl.class));

        final ResponsiblePackageBindingsImpl bindings1 = new ResponsiblePackageBindingsImpl();
        final PackageBindingsImpl bindings2 = new PackageBindingsImpl(bindings1);
        final DependentPackageBindingsImpl bindings3 = new DependentPackageBindingsImpl(bindings2);

        EasyMock.expect(contextChain.nested(EasyMock.<ComponentContext>isNull(), EasyMock.<ContextChain.Command>notNull())).andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                assert EasyMock.getCurrentArguments()[0] == null;
                return ((ContextChain.Command) EasyMock.getCurrentArguments()[1]).run(null);
            }
        }).anyTimes();

        EasyMock.expect(referenceChain.nested(EasyMock.<ComponentMapping>notNull(), EasyMock.same(Map.class), EasyMock.<ReferenceChain.Command>notNull()))
                .andAnswer(new IAnswer<Object>() {
                    public Object answer() throws Throwable {
                        return map;
                    }
                });

        EasyMock.expect(referenceChain.nested(EasyMock.<ComponentMapping>notNull(),
                                              EasyMock.same(ResponsiblePackageBindingsImpl.class),
                                              EasyMock.<ReferenceChain.Command>notNull())).andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                return bindings1;
            }
        });

        EasyMock.expect(referenceChain.nested(EasyMock.<ComponentMapping>notNull(),
                                              EasyMock.same(PackageBindingsImpl.class),
                                              EasyMock.<ReferenceChain.Command>notNull())).andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                return bindings2;
            }
        });

        EasyMock.expect(referenceChain.nested(EasyMock.<ComponentMapping>notNull(),
                                              EasyMock.same(DependentPackageBindingsImpl.class),
                                              EasyMock.<ReferenceChain.Command>notNull())).andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                return bindings3;
            }
        });

        EasyMock.expect(referenceChain.nested(EasyMock.<ComponentMapping>notNull(), EasyMock.<Class<?>>notNull(), EasyMock.<ReferenceChain.Command>notNull()))
                .andAnswer(new IAnswer<Object>() {
                    public Object answer() throws Throwable {
                        return null;
                    }
                })
                .anyTimes();

        replay();
        provider.instantiateBindings(services, map, assemblies);
        verify();
    }

    public static class ResponsiblePackageBindingsImpl extends EmptyPackageBindings {
        // empty
    }

    public static class StandalonePackageBindingsImpl extends EmptyPackageBindings {

    }

    public static class PackageBindingsImpl extends EmptyPackageBindings {

        @SuppressWarnings("UnusedDeclaration")
        public PackageBindingsImpl(final ResponsiblePackageBindingsImpl dependent) {
            // empty
        }
    }

    public static class DependentPackageBindingsImpl extends EmptyPackageBindings {

        @SuppressWarnings("UnusedDeclaration")
        public DependentPackageBindingsImpl(final PackageBindingsImpl dependent) {
            // empty
        }
    }
}