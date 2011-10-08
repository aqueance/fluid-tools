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

package org.fluidity.deployment.osgi;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

/**
 * @author Tibor Varga
 */
public class IsolatedClassLoader extends ClassLoader {

    private final Map<String, Class> loaded = new ConcurrentHashMap<String, Class>();
    private final Set<String> shared = new HashSet<String>();

    private final String name;

    public IsolatedClassLoader(final String name, final Class... shared) {
        super(WhiteboardImplTest.class.getClassLoader());

        this.name = name;

        for (final Class type : shared) {
            this.shared.add(type.getName());
        }

        this.shared.add(getClass().getName());
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", getClass().getSimpleName(), name);
    }

    public String name() {
        return name;
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        final Class type = loaded.containsKey(name) ? loaded.get(name) : findClass(name);

        if (resolve) {
            resolveClass(type);
        }

        return type;
    }

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
        if (shared.contains(name)) {
            return super.loadClass(name, false);
        } else {
            if (loaded.containsKey(name)) {
                return loaded.get(name);
            } else {
                synchronized (loaded) {
                    if (loaded.containsKey(name)) {
                        return loaded.get(name);
                    } else {
                        final ByteArrayOutputStream output = new ByteArrayOutputStream();
                        final byte[] buffer = new byte[1024];

                        final InputStream input = new BufferedInputStream(getResourceAsStream(name.replaceAll("\\.", "/").concat(".class")));

                        try {
                            int read;
                            while ((read = input.read(buffer)) > 0) {
                                output.write(buffer, 0, read);
                            }

                            final byte[] bytes = output.toByteArray();

                            final ClassReader reader = new ClassReader(new ByteArrayInputStream(bytes));

                            if ((reader.getAccess() & Opcodes.ACC_INTERFACE) != 0 || reader.getClassName().startsWith("java/")) {
                                loaded.put(name, super.loadClass(name, false));
                            } else {
                                loaded.put(name, defineClass(name, bytes, 0, bytes.length));
                            }
                        } catch (final IOException e) {
                            throw new ClassNotFoundException(name, e);
                        } finally {
                            try {
                                input.close();
                            } catch (final IOException e) {
                                // ignore
                            }
                        }

                        return loaded.get(name);
                    }
                }
            }
        }
    }
}
