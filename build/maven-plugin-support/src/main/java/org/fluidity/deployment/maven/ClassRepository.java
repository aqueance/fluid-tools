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

package org.fluidity.deployment.maven;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;

/**
 * A repository of class data.
 *
 * @author Tibor Varga
 */
public final class ClassRepository {

    private final Map<String, ClassReader> readers = new HashMap<>();
    private final ClassLoader loader;

    /**
     * Creates a new repository based on the given class loader.
     *
     * @param loader the class loader to use to find classes.
     */
    public ClassRepository(final ClassLoader loader) {
        this.loader = loader;
    }

    /**
     * Creates and caches a class reader.
     *
     * @param name the name of the class to load a reader for.
     *
     * @return a class reader or <code>null</code> if no file found.
     *
     * @throws IOException when class loading fails.
     */
    public ClassReader reader(final String name) throws IOException {
        if (name == null) {
            return null;
        }

        if (!readers.containsKey(name)) {
            final InputStream stream = loader.getResourceAsStream(ClassReaders.fileName(name));
            readers.put(name, stream == null ? null : new ClassReader(stream));
        }

        return readers.get(name);
    }
}
