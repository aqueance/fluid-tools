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

package org.fluidity.foundation;

import java.net.URL;
import java.net.URLClassLoader;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ClassLoadersTest {

    @Test
    public void testSetsContextClassLoader() throws Exception {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();

        final ClassLoader context = new URLClassLoader(new URL[] { Archives.containing(getClass()) }, null);
        final Class<?> type = context.loadClass(getClass().getName());

        assert type.getClassLoader() == context : type.getClassLoader();
        assert ClassLoaders.set(type) == original;
        assert thread.getContextClassLoader() == context : thread.getContextClassLoader();
    }

    @Test
    public void testRestoredContextClassLoader() throws Exception {
        final Thread thread = Thread.currentThread();

        final ClassLoader original = thread.getContextClassLoader();
        final ClassLoader context = new URLClassLoader(new URL[0], null);

        assert ClassLoaders.context(context, loader -> context) == context;
        assert thread.getContextClassLoader() == original;
    }
}
