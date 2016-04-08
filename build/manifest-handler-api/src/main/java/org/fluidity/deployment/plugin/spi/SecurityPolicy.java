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

package org.fluidity.deployment.plugin.spi;

import java.io.File;
import java.io.IOException;

/**
 * Manage a security policy file.
 *
 * @author Tibor Varga
 */
public interface SecurityPolicy {

    /**
     * The system property that refers to the class path. Used to refer, in URLs within security policy files, to the containing archive.
     */
    String JAVA_CLASS_PATH = "java.class.path";

    /**
     * Something where a named entry can be saved.
     */
    interface Output {

        /**
         * Saves the given <code>content</code> with the given <code>name</code>.
         *
         * @param name    the name of the new entry; never <code>null</code>.
         * @param content the value of the named entry; may be <code>null</code>, in which case the entry will be deleted if exists.
         *
         * @throws IOException on I/O failure.
         */
        void save(String name, String content) throws IOException;
    }

    /**
     * Returns the name of the security policy entry in the given archive.
     *
     * @param file the archive to return the embedded security policy name from.
     *
     * @return the name of the security policy entry in the given archive; may be <code>null</code>.
     *
     * @throws IOException on I/O failure.
     */
    String name(File file) throws IOException;

    /**
     * Returns the archive for which the policy is managed.
     *
     * @return the archive for which the policy is managed.
     */
    File archive();

    /**
     * Returns the I/O buffer used by the receiver.
     *
     * @return the I/O buffer used by the receiver.
     */
    byte[] buffer();

    /**
     * Adds a new archive to include in the management of security policy.
     *
     * @param archive  the archive to add.
     * @param level    the arbitrary number that can be used to order the archives within themselves.
     * @param location the location of the archive within the host archive.
     *
     * @throws IOException on I/O failure.
     */
    void add(File archive, int level, String location) throws IOException;

    /**
     * Updates the the metadata based on the policy files that have been loaded. Passing <code>null</code> for the value of some entry will delete that entry.
     *
     * @param metadata the metadata to update.
     *
     * @throws IOException on I/O failure.
     */
    void update(Output metadata) throws IOException;

    /**
     * Processes all archives and saves the resulting security policy in the given <code>output</code>.
     *
     * @param output  the output to save the security policy.
     *
     * @throws IOException on I/O failure.
     */
    void save(Output output) throws IOException;
}
