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

package org.fluidity.foundation.testing;

import java.util.function.Supplier;

import org.fluidity.features.DynamicConfiguration;
import org.fluidity.foundation.Command;
import org.fluidity.foundation.Configuration;
import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;

/**
 * Helps unit testing components that depend on {@link Configuration} or {@link DynamicConfiguration} and use {@link Simulator} to implement the tests.
 * <h3>Usage</h3>
 * <h4>Direct Configuration</h4>
 * See {@link org.fluidity.foundation.testing.MockConfiguration.Direct}
 * <h4>Dynamic Configuration</h4>
 * See {@link org.fluidity.foundation.testing.MockConfiguration.Cached}
 *
 * @author Tibor Varga
 */
@SuppressWarnings({ "UnusedDeclaration", "WeakerAccess" })
public final class MockConfiguration {

    /**
     * Creates a new static configuration mock object.
     *
     * @param type  the configuration settings class.
     * @param mocks the {@link org.fluidity.testing.Simulator.MockObjects} to create mock objects with.
     * @param <P>   the settings interface type to implement by the returned configuration.
     *
     * @return a new instance of this class.
     */
    public static <P> Direct<P> direct(final Class<P> type, final Simulator.MockObjects mocks) {
        return new Direct<>(type, mocks);
    }

    /**
     * Creates a new dynamic configuration mock object.
     *
     * @param type  the configuration settings class.
     * @param mocks the {@link org.fluidity.testing.Simulator.MockObjects} to create mock objects with.
     * @param <P>   the settings interface type to implement by the returned configuration.
     *
     * @return a new instance of this class.
     */
    public static <P> Cached<P> cached(final Class<P> type, final Simulator.MockObjects mocks) {
        return new Cached<>(type, mocks);
    }

    /**
     * Helps unit testing components that depend on {@link Configuration}.
     * <h3>Usage</h3>
     * <pre>
     * public class <span class="hl2">Foo</span>Test extends {@linkplain Simulator} {
     *
     *   private final <span class="hl1">MockConfiguration.Direct</span>&lt;<span class="hl2">Foo.Settings</span>&gt; configuration
     *       = <span class="hl1">MockConfiguration</span>.{@linkplain MockConfiguration#direct(Class, Simulator.MockObjects) direct}(<span class="hl2">Foo.Settings</span>.class, {@linkplain org.fluidity.testing.Simulator#dependencies() dependencies}());
     *
     *   &hellip;
     *
     *   {@linkplain org.testng.annotations.Test @Test}
     *   public void testConfigurationQuery() throws Exception {
     *
     *     // expect and set up a call to {@link Configuration#query(Configuration.Query)}
     *     configuration.<span class="hl1">expectQuery</span>(new {@linkplain org.fluidity.foundation.Command.Operation}&lt;<span class="hl2">Foo.Settings</span>, Exception&gt;() {
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
     *     configuration.<span class="hl1">expectSettings</span>(new {@linkplain org.fluidity.foundation.Command.Operation}&lt;<span class="hl2">Foo.Settings</span>, Exception&gt;() {
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
     * @param <P> the settings interface type to implement by the mocked configuration.
     *
     * @author Tibor Varga
     * @see MockConfiguration
     */
    public static final class Direct<P> {

        private final Configuration<P> configuration;
        private final P settings;

        @SuppressWarnings("unchecked")
        Direct(final Class<P> type, final Simulator.MockObjects mocks) {
            configuration = (Configuration<P>) mocks.normal(Configuration.class);
            settings = mocks.lenient(type);
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
         * @param <R>   the type of the returned value.
         * @param setup the command to invoke with the settings mock object.
         *
         * @throws Exception thrown by the given <code>setup</code> command.
         */
        @SuppressWarnings("unchecked")
        public <R> void expectQuery(final Command.Operation<P, Exception> setup) throws Exception {
            EasyMock.expect(configuration.query(EasyMock.<Configuration.Query<R, P>>notNull())).andAnswer(() -> ((Configuration.Query<R, P>) EasyMock.getCurrentArguments()[0]).run(settings));

            setup.run(settings);
        }
    }

    /**
     * Helps unit testing components that depend on a {@link DynamicConfiguration} component and use {@link Simulator} to implement the tests.
     * <h3>Usage</h3>
     * <pre>
     * public class <span class="hl2">Foo</span>Test extends {@linkplain Simulator} {
     *
     *   private final <span class="hl1">MockConfiguration.Cached</span>&lt;<span class="hl2">Foo.Settings</span>&gt; configuration
     *       = <span class="hl1">MockConfiguration</span>.{@linkplain MockConfiguration#cached(Class, Simulator.MockObjects) cached}(<span class="hl2">Foo.Settings</span>.class, {@linkplain Simulator#dependencies() dependencies}());
     *
     *   &hellip;
     *
     *   {@linkplain org.testng.annotations.Test @Test}
     *   public void testConfigurationSettings() throws Exception {
     *
     *     // expect and set up a call to {@link org.fluidity.foundation.Configuration#settings()}
     *     configuration.<span class="hl1">expectSettings</span>(new {@linkplain org.fluidity.foundation.Command.Operation}&lt;<span class="hl2">Foo.Settings</span>, Exception&gt;() {
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
     * @param <P> the settings interface type to implement by the mocked dynamic configuration.
     *
     * @author Tibor Varga
     * @see MockConfiguration
     */
    public static final class Cached<P> {

        private final DynamicConfiguration<P> configuration;
        private final Supplier<P> snapshot;
        private final P settings;

        @SuppressWarnings("unchecked")
        Cached(final Class<P> type, final Simulator.MockObjects mocks) {
            configuration = (DynamicConfiguration<P>) mocks.normal(DynamicConfiguration.class);
            snapshot = (Supplier<P>) mocks.normal(Supplier.class);
            settings = mocks.lenient(type);
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
