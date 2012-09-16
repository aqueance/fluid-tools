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

package org.fluidity.deployment.osgi;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Tibor Varga
 */
public class IsolatedClassLoader extends ClassLoader {

    private final Map<String, Class> loaded = new ConcurrentHashMap<String, Class>();
    private final Set<String> invisible = new HashSet<String>();

    private final String name;

    IsolatedClassLoader(final String name, final Class... invisible) {
        super(BundleComponentContainerImplTest.class.getClassLoader());
        this.name = name;

        for (final Class type : invisible) {
            this.invisible.add(type.getName());
        }
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
        for (final String type : invisible) {
            if (name.startsWith(type)) {
                throw new ClassNotFoundException(name);
            }
        }

        if (name.startsWith(BundleBoundaryImpl.class.getName()) || name.contains(".isolated.")) {
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

                        final Class<?> type = defineClass(name, bytes, 0, bytes.length);

                        loaded.put(name, type);

                        if (resolve) {
                            resolveClass(type);
                        }

                        return type;
                    } catch (final IOException e) {
                        throw new ClassNotFoundException(name, e);
                    } finally {
                        try {
                            input.close();
                        } catch (final IOException e) {
                            // ignore
                        }
                    }
                }
            }
        } else {
            return super.loadClass(name, resolve);
        }
    }
}
