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

import org.fluidity.foundation.Command;
import org.fluidity.foundation.Configuration;
import org.fluidity.testing.MockGroup;

import org.easymock.EasyMock;
import org.easymock.IAnswer;

/**
 * Helps unit testing components that depend on a {@link org.fluidity.foundation.Configuration} component and use {@link MockGroup} to implement the tests.
 * <h3>Usage</h3>
 * <pre>
 * public class <span class="hl2">Foo</span>Test extends {@linkplain MockGroup} {
 *
 *   private final <span class="hl1">ConfigurationControl</span>&lt;<span class="hl2">Foo.Settings</span>> configuration
 *       = <span class="hl1">ConfigurationControl</span>.{@linkplain MockConfiguration#create(MockGroup, Class) create}(this, <span class="hl2">Foo.Settings</span>.class);
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
 */
public final class MockConfiguration<P> {

    private final Configuration<P> configuration;
    private final P settings;

    @SuppressWarnings("unchecked")
    private MockConfiguration(final MockGroup factory, final Class<P> type) {
        configuration = (Configuration<P>) factory.mock(Configuration.class);
        settings = factory.niceMock(type);
    }

    /**
     * Creates a new instance of this class.
     *
     * @param factory the {@link MockGroup} to use to create mock objects.
     * @param type    the configuration settings class.
     * @param <P>     the generic configuration settings type.
     *
     * @return a new instance of this class.
     */
    public static <P> MockConfiguration<P> create(final MockGroup factory, final Class<P> type) {
        return new MockConfiguration<P>(factory, type);
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
     * Sets up an expectation to call {@link Configuration#settings()} and invokes the given command to setup expectations on the encapsulated mock object for
     * the settings <code>type</code> provided in {@link MockConfiguration#create(MockGroup, Class)}.
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
     * Sets up an expectation to call {@link Configuration#query(Configuration.Query)} and invokes the given command to setup expectations on the encapsulated
     * mock object for the settings <code>type</code> provided in {@link MockConfiguration#create(MockGroup, Class)}.
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
