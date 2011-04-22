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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.xbean.classloader.JarFileClassLoader;
import org.testng.annotations.AfterClass;

/**
 * @author Tibor Varga
 */
public class ParentJarClassLoaderTest extends NestedJarClassLoaderTest {

    private final List<JarFileClassLoader> classLoaders = new ArrayList<JarFileClassLoader>();

    @Override
    protected ClassLoader createLoader(final URL root, final ClassLoader parent, final String... paths) throws IOException {
        final JarFileClassLoader loader = ClassLoaders.jarFileClassLoaders().create(parent, root);
        classLoaders.add(loader);
        return new ParentJarClassLoader(loader, paths);
    }

    @Override
    protected boolean supportsRootClasses() {
        return false;
    }

    @AfterClass
    public void unload() throws Exception {
        for (final JarFileClassLoader loader : classLoaders) {
            loader.destroy();
        }
    }
}
