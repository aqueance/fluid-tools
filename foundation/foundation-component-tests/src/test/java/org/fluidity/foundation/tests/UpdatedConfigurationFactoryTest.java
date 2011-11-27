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

package org.fluidity.foundation.tests;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.foundation.Configuration;
import org.fluidity.foundation.Updates;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class UpdatedConfigurationFactoryTest {

    private final ComponentContainer container = new ContainerBoundary();

    @Test
    public void testWithoutContext() throws Exception {
        assert container.getComponent(NoContextConfigured.class) != null;
    }

    @Test
    public void testWithContext() throws Exception {
        assert container.getComponent(ContextConfigured.class) != null;
    }

    @Component
    private static class NoContextConfigured {

        private NoContextConfigured(final Configuration.Updated<Settings> configuration) {
            final Updates.Snapshot<Settings> snapshot = configuration.snapshot(20);

            assert snapshot != null;
            final Settings settings = snapshot.get();

            assert settings != null;
            assert Settings.SOME_PROPERTY.equals(settings.property()) : settings.property();
        }
    }

    @Component
    @Configuration.Context(ContextConfigured.ROOT)
    private static class ContextConfigured {

        private static final String ROOT = "root";
        private static final String CONTEXT1 = "context1";
        private static final String CONTEXT2 = "context2";

        private ContextConfigured(final @Configuration.Context(CONTEXT1) Configuration.Updated<Settings> configuration1,
                                  final @Configuration.Context(CONTEXT2) Configuration.Updated<Settings> configuration2,
                                  final @Configuration.Context(CONTEXT1) Configuration.Updated<Settings> configuration3) {
            final Settings settings1 = configuration1.snapshot(20).get();
            final Settings settings2 = configuration2.snapshot(20).get();

            assert settings1 != null;
            assert String.format("%s.%s.%s", ROOT, CONTEXT1, Settings.SOME_PROPERTY).equals(settings1.property()) : settings1.property();

            assert settings2 != null;
            assert String.format("%s.%s.%s", ROOT, CONTEXT2, Settings.SOME_PROPERTY).equals(settings2.property()) : settings2.property();

            assert configuration1 == configuration3;
        }
    }
}