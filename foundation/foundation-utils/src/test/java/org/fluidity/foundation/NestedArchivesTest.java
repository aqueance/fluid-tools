/*
 * Copyright (c) 2006-2018 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class NestedArchivesTest {

    private final String container = "samples.jar";
    private final URL root = getClass().getClassLoader().getResource(container);

    private ClassLoader loader;
    private boolean standard;

    private ClassLoader createLoader(final URL root, final String... paths) throws IOException {
        final List<URL> urls = new ArrayList<>();

        urls.add(root);
        for (final String path : paths) {
            urls.add(Archives.Nested.formatURL(root, path));
        }

        return ClassLoaders.create(urls, getClass().getClassLoader(), null);
    }

    @BeforeMethod
    public void setup() throws Exception {
        loader = createLoader(root,
                              "META-INF/dependencies/dependency-1.jar",
                              "META-INF/dependencies/dependency-2.jar",
                              "META-INF/dependencies/dependency-3.jar");
        standard = loader instanceof URLClassLoader;
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
    public void testFindingOneResource() throws Exception {
        final URL url1 = loader.getResource("resource-1.txt");

        assert url1 != null;
        assert Objects.equals(url1.getProtocol(), standard ? Archives.PROTOCOL : Archives.Nested.PROTOCOL) : url1;
        assert Objects.equals(new URL(url1.getFile()).getProtocol(), Archives.FILE) : url1;

        final URL url2 = loader.getResource("resource-2.txt");
        assert url2 != null;
        assert Objects.equals(url2.getProtocol(), standard ? Archives.PROTOCOL : Archives.Nested.PROTOCOL) : url2;
        assert Objects.equals(new URL(url2.getFile()).getProtocol(), standard ? Archives.Nested.PROTOCOL : Archives.FILE) : url2;
    }

    @Test
    public void testFindingAllResources() throws Exception {
        final List<URL> resources = Collections.list(loader.getResources("resource-1.txt"));
        assert resources.size() == 4 : resources;

        final Set<String> protocols = new HashSet<>();
        for (final URL url : resources) {
            protocols.add(new URL(url.getFile()).getProtocol());
        }

        if (standard) {
            assert protocols.size() == 2 : protocols;
            assert protocols.contains(Archives.FILE) : protocols;
            assert protocols.contains(Archives.Nested.PROTOCOL) : protocols;
        } else {
            assert protocols.size() == 1 : protocols;
            assert protocols.contains(Archives.FILE) : protocols;
        }
    }

    @Test
    public void testReferenceFromRootClass() throws Exception {
        final Class<?> rootClass = loader.loadClass("org.fluidity.samples.Root");
        final Object rootObject = rootClass.newInstance();

        assert rootObject != null;

        assert rootClass.getMethod("dependency1").invoke(rootObject) != null;
        assert rootClass.getMethod("dependency2").invoke(rootObject) != null;
        assert rootClass.getMethod("dependency3").invoke(rootObject) != null;
    }

    @Test
    public void testExplodedArchive() throws Exception {
        final Class<? extends NestedArchivesTest> me = getClass();
        final Class<?> type = ClassLoaders.create(Collections.singleton(Archives.containing(me)), null, null).loadClass(me.getName());
        assert type != null;
    }
}
