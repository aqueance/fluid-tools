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

package org.fluidity.foundation.jarjar;

import java.net.URL;
import java.util.Collections;

import org.fluidity.foundation.ClassLoaders;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class URLClassLoaderTest {

    private final String container = "classpath/samples.jar";
    private final URL root = getClass().getClassLoader().getResource(container);

    @Test
    public void testEmbeddedClassPath() throws Exception {

        // this is the reference
        assert new java.net.URLClassLoader(new URL[] { root }).loadClass("org.fluidity.samples.Root").newInstance() != null;

        // this is what we are verifying
        assert ClassLoaders.create(Collections.singleton(root), null).loadClass("org.fluidity.samples.Root").newInstance() != null;
    }
}
