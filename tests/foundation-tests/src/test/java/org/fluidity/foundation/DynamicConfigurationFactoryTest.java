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

package org.fluidity.foundation;

import java.util.function.Supplier;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.Containers;
import org.fluidity.features.DynamicConfiguration;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class DynamicConfigurationFactoryTest {

    private final ComponentContainer container = Containers.global();

    @Test
    public void testWithoutContext() throws Exception {
        container.instantiate(NoContextConfigured.class);
    }

    @Test
    public void testWithContext() throws Exception {
        container.instantiate(ContextConfigured.class);
    }

    @Component(automatic = false)
    private static class NoContextConfigured {

        @SuppressWarnings("UnusedDeclaration")
        private NoContextConfigured(final DynamicConfiguration<Settings> configuration) {
            final Supplier<Settings> snapshot = configuration.snapshot();

            assert snapshot != null;
            final Settings settings = snapshot.get();

            assert settings != null;
            assert Settings.SOME_PROPERTY.equals(settings.property()) : settings.property();
        }
    }

    @Component(automatic = false)
    @Configuration.Prefix(ContextConfigured.ROOT)
    private static class ContextConfigured {

        public static final String ROOT = "root";

        private static final String PREFIX1 = "prefix1";
        private static final String PREFIX2 = "prefix2";

        @SuppressWarnings("UnusedDeclaration")
        private ContextConfigured(final @Configuration.Prefix(PREFIX1) DynamicConfiguration<Settings> configuration1,
                                  final @Configuration.Prefix(PREFIX2) DynamicConfiguration<Settings> configuration2,
                                  final @Configuration.Prefix(PREFIX1) DynamicConfiguration<Settings> configuration3) {
            final Settings settings1 = configuration1.snapshot().get();
            final Settings settings2 = configuration2.snapshot().get();

            assert settings1 != null;
            assert String.format("%s.%s.%s", ROOT, PREFIX1, Settings.SOME_PROPERTY).equals(settings1.property()) : settings1.property();

            assert settings2 != null;
            assert String.format("%s.%s.%s", ROOT, PREFIX2, Settings.SOME_PROPERTY).equals(settings2.property()) : settings2.property();

            assert configuration1 == configuration3;
        }
    }
}
