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

package org.fluidity.foundation;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class JarJarClassLoaderTest {
    private final String container = "samples.jar";
    private final URL root = getClass().getClassLoader().getResource(container);

    private ClassLoader loader;

    @BeforeMethod
    public void setup() throws Exception {
        loader = new JarJarClassLoader(root, null, "META-INF/dependencies");
    }

    @Test
    public void testClassInRootJar() throws Exception {
        assert loader.loadClass("org.fluidity.samples.Level0").newInstance() != null;
    }

    @Test
    public void testClassInNestedJar() throws Exception {
        assert loader.loadClass("org.fluidity.samples.Dependency1Level1").newInstance() != null;
        assert loader.loadClass("org.fluidity.samples.Dependency2Level1").newInstance() != null;
        assert loader.loadClass("org.fluidity.samples.Dependency3Level1").newInstance() != null;
    }

    @Test
    public void testClassCrossingJarBoundary() throws Exception {
        final Class<?> rootClass = loader.loadClass("org.fluidity.samples.Root");
        final Object rootObject = rootClass.newInstance();

        assert rootObject != null;

        assert rootClass.getMethod("dependency1").invoke(rootObject) != null;
        assert rootClass.getMethod("dependency2").invoke(rootObject) != null;
        assert rootClass.getMethod("dependency3").invoke(rootObject) != null;
    }

    @Test
    public void testFindingOneResource() throws Exception {
        final URL url1 = loader.getResource("resource-1.txt");
        assert url1 != null;
        assert url1.getProtocol().equals("jar");

        final URL url2 = loader.getResource("resource-2.txt");
        assert url2 != null;
        assert url2.getProtocol().equals("jarjar");
    }

    @Test
    public void testFindingAllResources() throws Exception {
        final List<URL> resources = Collections.list(loader.getResources("resource-1.txt"));
        assert resources.size() == 4 : resources;

        final Set<String> protocols = new HashSet<String>();
        for (final URL url : resources) {
            protocols.add(url.getProtocol());
        }

        assert protocols.size() == 2 : protocols;
        assert protocols.contains("jar");
        assert protocols.contains("jarjar");
    }
}
