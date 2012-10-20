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

package org.fluidity.foundation.testing;

import org.fluidity.features.DynamicConfiguration;
import org.fluidity.features.Updates;
import org.fluidity.foundation.Command;
import org.fluidity.foundation.Configuration;
import org.fluidity.testing.MockGroup;

import org.easymock.EasyMock;
import org.easymock.IAnswer;

/**
 * Helps unit testing components that depend on {@link org.fluidity.foundation.Configuration} or {@link org.fluidity.features.DynamicConfiguration} and use
 * {@link MockGroup} to implement the tests.
 * <h3>Usage</h3>
 * <h4>Direct Configuration</h4>
 * See {@link org.fluidity.foundation.testing.MockConfiguration.Direct}
 * <h4>Dynamic Configuration</h4>
 * See {@link org.fluidity.foundation.testing.MockConfiguration.Cached}
 *
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
public final class MockConfiguration {

    /**
     * Creates a new static configuration mock object.
     *
     * @param factory the {@link MockGroup} to use to create mock objects.
     * @param type    the configuration settings class.
     * @param <P>     the generic configuration settings type.
     *
     * @return a new instance of this class.
     */
    public static <P> Direct<P> direct(final MockGroup factory, final Class<P> type) {
        return new Direct<P>(factory, type);
    }

    /**
     * Creates a new dynamic configuration mock object.
     *
     * @param factory the {@link MockGroup} to use to create mock objects.
     * @param type    the configuration settings class.
     * @param <P>     the generic configuration settings type.
     *
     * @return a new instance of this class.
     */
    public static <P> Cached<P> cached(final MockGroup factory, final Class<P> type) {
        return new Cached<P>(factory, type);
    }

    /**
     * Helps unit testing components that depend on {@link Configuration}.
     * <h3>Usage</h3>
     * <h4>Static Configuration</h4>
     * <pre>
     * public class <span class="hl2">Foo</span>Test extends {@linkplain MockGroup} {
     *
     *   private final <span class="hl1">MockConfiguration.Direct</span>&lt;<span class="hl2">Foo.Settings</span>> configuration
     *       = <span class="hl1">MockConfiguration</span>.{@linkplain MockConfiguration#direct(MockGroup, Class) direct}(this, <span class="hl2">Foo.Settings</span>.class);
     *
     *   &hellip;
     *
     *   {@linkplain org.testng.annotations.Test @Test}
     *   public void testConfigurationQuery() throws Exception {
     *
     *     // expect and set up a call to {@link Configuration#query(Configuration.Query)}
     *     configuration.<span class="hl1">expectQuery</span>(new {@linkplain org.fluidity.foundation.Command.Operation}&lt;<span class="hl2">Foo.Settings</span>, Exception>() {
     *       public void run(final <span class="hl2">Foo.Settings</span> settings) throws Exception {
     *         {@linkplain EasyMock}.expect(settings.&hellip;(&hellip;)).andReturn(&hellip;);
     *       }
     *     });
     *
     *     &hellip;
     *   }
     *
     *   {@linkplain org.testng.annotations.Test @Test}
     *   public void testConfigurationSettings() throws Exception {
     *
     *     // expect and set up a call to {@link Configuration#settings()}
     *     configuration.<span class="hl1">expectSettings</span>(new {@linkplain org.fluidity.foundation.Command.Operation}&lt;<span class="hl2">Foo.Settings</span>, Exception>() {
     *       public void run(final <span class="hl2">Foo.Settings</span> settings) throws Exception {
     *         {@linkplain EasyMock}.expect(settings.&hellip;(&hellip;)).andReturn(&hellip;);
     *       }
     *     });
     *
     *     &hellip;
     *   }
     * }
     * </pre>
     *
     * @param <P> The generic settings type.
     *
     * @author Tibor Varga
     * @see MockConfiguration
     */
    public static final class Direct<P> {

        private final Configuration<P> configuration;
        private final P settings;

        @SuppressWarnings("unchecked")
        Direct(final MockGroup factory, final Class<P> type) {
            configuration = (Configuration<P>) factory.mock(Configuration.class);
            settings = factory.niceMock(type);
        }

        /**
         * Returns the {@link Configuration} mock object.
         *
         * @return the {@link Configuration} mock object.
         */
        public Configuration<P> get() {
            return configuration;
        }

        /**
         * Sets up an expectation to call {@link Configuration#settings()} and invokes the given command to setup expectations on the encapsulated mock object
         * for the settings <code>type</code> provided in {@link MockConfiguration#direct}.
         *
         * @param setup the command to invoke with the settings mock object.
         *
         * @throws Exception thrown by the given <code>setup</code> command.
         */
        public void expectSettings(final Command.Operation<P, Exception> setup) throws Exception {
            EasyMock.expect(configuration.settings()).andReturn(settings);

            setup.run(settings);
        }

        /**
         * Sets up an expectation to call {@link Configuration#query(Configuration.Query)} and invokes the given command to setup expectations on the
         * encapsulated mock object for the settings <code>type</code> provided in {@link MockConfiguration#direct}.
         *
         * @param setup the command to invoke with the settings mock object.
         *
         * @throws Exception thrown by the given <code>setup</code> command.
         */
        public <R> void expectQuery(final Command.Operation<P, Exception> setup) throws Exception {
            EasyMock.expect(configuration.query(EasyMock.<Configuration.Query<R, P>>notNull())).andAnswer(new IAnswer<R>() {
                @SuppressWarnings("unchecked")
                public R answer() throws Throwable {
                    return ((Configuration.Query<R, P>) EasyMock.getCurrentArguments()[0]).run(settings);
                }
            });

            setup.run(settings);
        }
    }

    /**
     * Helps unit testing components that depend on a {@link DynamicConfiguration} component and use {@link MockGroup} to implement the tests.
     * <h3>Usage</h3>
     * <pre>
     * public class <span class="hl2">Foo</span>Test extends {@linkplain MockGroup} {
     *
     *   private final <span class="hl1">MockConfiguration.Cached</span>&lt;<span class="hl2">Foo.Settings</span>> configuration
     *       = <span class="hl1">MockConfiguration</span>.{@linkplain MockConfiguration#cached(MockGroup, Class) cached}(this, <span class="hl2">Foo.Settings</span>.class);
     *
     *   &hellip;
     *
     *   {@linkplain org.testng.annotations.Test @Test}
     *   public void testConfigurationSettings() throws Exception {
     *
     *     // expect and set up a call to {@link org.fluidity.foundation.Configuration#settings()}
     *     configuration.<span class="hl1">expectSettings</span>(new {@linkplain org.fluidity.foundation.Command.Operation}&lt;<span class="hl2">Foo.Settings</span>, Exception>() {
     *       public void run(final <span class="hl2">Foo.Settings</span> settings) throws Exception {
     *         {@linkplain org.easymock.EasyMock}.expect(settings.&hellip;(&hellip;)).andReturn(&hellip;);
     *       }
     *     });
     *
     *     &hellip;
     *   }
     * }
     * </pre>
     *
     * @param <P> The generic settings type.
     *
     * @author Tibor Varga
     * @see MockConfiguration
     */
    public static final class Cached<P> {

        private final DynamicConfiguration<P> configuration;
        private final Updates.Snapshot<P> snapshot;
        private final P settings;

        @SuppressWarnings("unchecked")
        Cached(final MockGroup factory, final Class<P> type) {
            configuration = (DynamicConfiguration<P>) factory.mock(DynamicConfiguration.class);
            snapshot = (Updates.Snapshot<P>) factory.mock(Updates.Snapshot.class);
            settings = factory.niceMock(type);
        }

        /**
         * Returns the {@link org.fluidity.foundation.Configuration} mock object.
         *
         * @return the {@link org.fluidity.foundation.Configuration} mock object.
         */
        public DynamicConfiguration<P> get() {
            return configuration;
        }

        /**
         * Sets up an expectation to call {@link org.fluidity.foundation.Configuration#settings()} and invokes the given command to setup expectations on the
         * encapsulated mock object for the settings <code>type</code> provided in {@link MockConfiguration#cached}.
         *
         * @param setup the command to invoke with the settings mock object.
         *
         * @throws Exception thrown by the given <code>setup</code> command.
         */
        public void expectSettings(final Command.Operation<P, Exception> setup) throws Exception {
            EasyMock.expect(configuration.snapshot()).andReturn(snapshot);
            EasyMock.expect(snapshot.get()).andReturn(settings);

            setup.run(settings);
        }
    }
}
