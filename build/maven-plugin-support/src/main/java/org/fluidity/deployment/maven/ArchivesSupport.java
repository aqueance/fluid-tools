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
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.fluidity.foundation.Archives;

/**
 * Common JAR processing utilities for standalone archive processing plugins.
 *
 * @author Tibor Varga
 */
public interface ArchivesSupport {

    String META_INF = Archives.META_INF.concat("/");

    /**
     * Iterates through the {@link File}s returned by the given {@link Feed feed} and loads the manifest attributes and service provider files into the given
     * maps, respectively.
     *
     * @param attributes the map to store JAR manifest attributes of the input JARs.
     * @param providers  the map to store contents of service provider files encountered in the input JARs.
     * @param buffer     the buffer to use when copying data.
     * @param log        the Maven log to use.
     * @param feed       the provider of the list of input JARs to load.
     *
     * @throws IOException when JAR processing fails.
     */
    void load(Map<String, Attributes> attributes, Map<String, String[]> providers, byte[] buffer, Logger log, Feed feed) throws IOException;

    /**
     * Iterates through the {@link File}s returned by the given {@link Feed feed} and stores all entries found therein in the output JAR stream, except the JAR
     * manifest, JAR indexes, signatures, and whatever else is not {@linkplain ArchivesSupport.Feed#include(JarEntry) included} by the <code>feed</code>.
     * The service providers in the given map loaded by {@link #load(Map, Map, byte[], Logger, ArchivesSupport.Feed)} are saved as well.
     *
     * @param output   the JAR output stream to add entries to.
     * @param buffer   the buffer to use when {@linkplain org.fluidity.foundation.Streams#copy(java.io.InputStream, java.io.OutputStream, byte[], boolean,
     *                 boolean) copying} data.
     * @param services the service provider map computed by {@link #load(Map, Map, byte[], Logger, ArchivesSupport.Feed)}.
     * @param feed     the provider of the list of JAR inputs to expand.
     *
     * @throws IOException when JAR processing fails.
     */
    void expand(JarOutputStream output, byte[] buffer, Map<String, String[]> services, Feed feed) throws IOException;

    /**
     * Adds the given JAR manifest entries to the given JAR manifest.
     *
     * @param entries  the entries.
     * @param manifest the manifest.
     */
    void include(Map<String, Attributes> entries, Manifest manifest);

    /**
     * Provides input files to {@link ArchivesSupport#load(Map, Map, byte[], Logger, ArchivesSupport.Feed)} and {@link ArchivesSupport#expand(JarOutputStream,
     * byte[], Map, ArchivesSupport.Feed)}.
     *
     * @author Tibor Varga
     */
    interface Feed {

        /**
         * Returns the next JAR {@link File} to {@link ArchivesSupport#load(Map, Map, byte[], Logger, ArchivesSupport.Feed)} or {@link
         * ArchivesSupport#expand(JarOutputStream, byte[], Map, ArchivesSupport.Feed)}.
         *
         * @return a {@link File} object or <code>null</code> if there is no more.
         *
         * @throws IOException when constructing the return object fails.
         */
        File next() throws IOException;

        /**
         * Tells if the given entry should be included when {@linkplain ArchivesSupport#load(Map, Map, byte[], Logger, ArchivesSupport.Feed) loading} or
         * {@linkplain ArchivesSupport#expand(JarOutputStream, byte[], Map, ArchivesSupport.Feed) expanding} JAR entries.
         *
         * @param entry the entry to consider for inclusion.
         *
         * @return <code>true</code> if the given entry should be included, <code>false</code> otherwise.
         */
        boolean include(JarEntry entry);
    }
}
