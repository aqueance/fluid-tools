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

package org.fluidity.tests.osgi.bundle2;

import java.net.URL;
import java.util.Collection;
import java.util.Properties;
import java.util.jar.JarFile;

import org.fluidity.composition.Component;
import org.fluidity.foundation.Archives;
import org.fluidity.tests.osgi.BundleTest;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@Component(automatic = false)
public final class ExtraDependenciesTest implements BundleTest {

    private static final String EXTRAS = "extras";
    private static final String NESTED = "nested";

    private final Bundle bundle;

    ExtraDependenciesTest(final BundleContext context) {
        bundle = context.getBundle();
    }

    @Test
    public void test() throws Exception {
        assert !Archives.Nested.dependencies(true, bundle.getEntry("/"), EXTRAS).isEmpty() : EXTRAS;
        assert !Archives.Nested.dependencies(true, new URL(bundle.getEntry("/"), JarFile.MANIFEST_NAME), EXTRAS).isEmpty() : EXTRAS;

        // the bundle location is not guaranteed to be a URL so this is not portable
        assert !Archives.Nested.dependencies(true, new URL(bundle.getLocation()), EXTRAS).isEmpty() : EXTRAS;

        final Collection<URL> nested = Archives.Nested.dependencies(true, bundle.getEntry("/"), NESTED);
        assert !nested.isEmpty() : NESTED;

        for (final URL url : nested) {
            assert Archives.manifest(url, true) != null : url;
            assert !Archives.manifest(url, true).getMainAttributes().isEmpty() : url;
        }
    }

    public Properties properties() {
        return null;
    }

    public void start() throws Exception {
        // empty
    }

    public void stop() throws Exception {
        // empty
    }
}
