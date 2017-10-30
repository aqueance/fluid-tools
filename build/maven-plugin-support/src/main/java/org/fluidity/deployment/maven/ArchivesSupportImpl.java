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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.fluidity.foundation.Archives;
import org.fluidity.foundation.IOStreams;
import org.fluidity.foundation.ServiceProviders;
import org.fluidity.foundation.Strings;

import org.codehaus.plexus.component.annotations.Component;

/**
 * @author Tibor Varga
 */
@Component(role = ArchivesSupport.class)
final class ArchivesSupportImpl implements ArchivesSupport {

    @Override
    public void load(final Map<String, Attributes> attributes,
                     final Map<String, String[]> providers,
                     final byte[] buffer,
                     final Logger log,
                     final Feed feed) throws IOException {
        for (File input; (input = feed.next()) != null; ) {
            Archives.read(input.toURI().toURL(), true, (url, entry) -> {

                // read all entries except the MANIFEST
                return !feed.include(entry) || entry.getName().equals(JarFile.MANIFEST_NAME) ? null : (_url, _entry, stream) -> {
                    final String entryName = _entry.getName();

                    if (!attributes.containsKey(entryName)) {
                        if (Objects.equals(entryName, Archives.INDEX_NAME)) {
                            log.warn(String.format("JAR index ignored in %s", _url));
                        } else if (entryName.startsWith(Archives.META_INF) && entryName.toUpperCase().endsWith(".SF")) {
                            throw new IOException(String.format("JAR signatures not supported in %s", _url));
                        } else if (entryName.startsWith(ServiceProviders.LOCATION) && !_entry.isDirectory()) {
                            final String[] list = IOStreams.load(stream, Strings.UTF_8, buffer).split("[\n\r]+");
                            final String[] present = providers.get(entryName);

                            if (present == null) {
                                providers.put(entryName, list);
                            } else {
                                final String[] combined = Arrays.copyOf(present, present.length + list.length);
                                System.arraycopy(list, 0, combined, present.length, list.length);
                                providers.put(entryName, combined);
                            }
                        } else {
                            attributes.put(entryName, _entry.getAttributes());
                        }
                    } else if (!_entry.isDirectory()) {
                        log.warn(String.format("Duplicate entry: %s", entryName));
                    }

                    return true;
                };
            });
        }
    }

    @Override
    public void expand(final JarOutputStream output, final byte[] buffer, final Map<String, String[]> services, final Feed feed) throws IOException {
        final Set<String> copied = new HashSet<>();

        for (final Map.Entry<String, String[]> entry : services.entrySet()) {
            final String entryName = entry.getKey();

            if (!copied.contains(entryName)) {
                copied.add(entryName);

                final StringBuilder contents = new StringBuilder();

                for (final String line : entry.getValue()) {
                    contents.append(line).append('\n');
                }

                output.putNextEntry(new JarEntry(entryName));
                IOStreams.store(output, contents.toString(), Strings.UTF_8, buffer);
            }
        }

        for (File input; (input = feed.next()) != null; ) {
            Archives.read(input.toURI().toURL(), true, (url, entry) -> {
                final String entryName = entry.getName();

                final boolean done = copied.contains(entryName);
                final boolean manifest = entryName.equals(JarFile.MANIFEST_NAME) || entryName.equals(META_INF);
                final boolean index = Objects.equals(entryName, Archives.INDEX_NAME);
                final boolean signature = entryName.startsWith(Archives.META_INF) && entryName.toUpperCase().endsWith(".SF");

                return done || manifest || index || signature || !feed.include(entry) ? null : (_url, _entry, stream) -> {
                    copied.add(_entry.getName());
                    output.putNextEntry(_entry);
                    IOStreams.pipe(stream, output, buffer);

                    return true;
                };
            });
        }
    }

    @Override
    public void include(final Map<String, Attributes> entries, final Manifest manifest) {
        final Map<String, Attributes> attributes = manifest.getEntries();

        for (final Map.Entry<String, Attributes> entry : entries.entrySet()) {
            final Attributes value = entry.getValue();

            if (value != null) {
                final String key = entry.getKey();
                final Attributes list = attributes.get(key);

                if (list == null) {
                    attributes.put(key, value);
                } else {
                    for (final Map.Entry<Object, Object> item : value.entrySet()) {
                        list.put(item.getKey(), item.getValue());
                    }
                }
            }
        }
    }
}
