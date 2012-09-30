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

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.Containers;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ConfigurationTest {

    private final ComponentContainer container = Containers.global();

    @Test
    public void testWithoutContext() throws Exception {
        assert container.getComponent(NoContextConfigured.class) != null;
    }

    @Test
    public void testWithContext() throws Exception {
        assert container.getComponent(ContextConfigured.class) != null;
    }

    @Test
    public void testDefaults() throws Exception {
        assert container.getComponent(DefaultsTest.class) != null;
    }

    @Component
    @SuppressWarnings("UnusedDeclaration")
    private static class NoContextConfigured {

        private NoContextConfigured(final Configuration<Settings> configuration) {
            final Settings settings = configuration.settings();

            assert settings != null;
            assert Settings.SOME_PROPERTY.equals(settings.property()) : settings.property();
        }
    }

    @Component
    @SuppressWarnings("UnusedDeclaration")
    private static class DefaultsTest {

        private DefaultsTest(final Configuration<Settings2> configuration) {
            final Settings2 settings = configuration.settings();

            assert settings != null;
            assert DefaultSettings.SPECIFIED.equals(settings.specified()) : settings.specified();
            assert Settings.DEFAULT_VALUE.equals(settings.unspecified()) : settings.unspecified();
        }
    }

    @Component
    @Configuration.Prefix(ContextConfigured.ROOT)
    @SuppressWarnings("UnusedDeclaration")
    private static class ContextConfigured {

        public static final String ROOT = "root";

        private static final String PREFIX1 = "prefix1";
        private static final String PREFIX2 = "prefix2";

        private ContextConfigured(final @Configuration.Prefix(PREFIX1) Configuration<Settings> configuration1,
                                  final @Configuration.Prefix(PREFIX2) Configuration<Settings> configuration2,
                                  final @Configuration.Prefix(PREFIX1) Configuration<Settings> configuration3) {
            final Settings settings1 = configuration1.settings();
            final Settings settings2 = configuration2.settings();

            assert settings1 != null;
            assert String.format("%s.%s.%s", ROOT, PREFIX1, Settings.SOME_PROPERTY).equals(settings1.property()) : settings1.property();

            assert settings2 != null;
            assert String.format("%s.%s.%s", ROOT, PREFIX2, Settings.SOME_PROPERTY).equals(settings2.property()) : settings2.property();

            assert configuration1 == configuration3;

            final Settings settings3 = configuration3.settings();

            assert settings3 != null;
            assert String.format("%s.%s.%s", ROOT, PREFIX1, Settings.SOME_PROPERTY).equals(settings3.property()) : settings3.property();
        }
    }

    // Property names prefixed with {@link EchoPropertyProviderImpl#UNKNOWN}.
    interface Settings2 {

        @Configuration.Property(key = "unknown-specified", undefined = Settings.DEFAULT_VALUE)
        String specified();

        @Configuration.Property(key = "unknown-unspecified", undefined = Settings.DEFAULT_VALUE)
        String unspecified();
    }

    @Component
    private static class DefaultSettings implements Settings2 {

        static final String SPECIFIED = String.format("run-time %s", Settings.DEFAULT_VALUE);

        public String specified() {
            return SPECIFIED;
        }

        public String unspecified() {
            return null;
        }
    }
}
