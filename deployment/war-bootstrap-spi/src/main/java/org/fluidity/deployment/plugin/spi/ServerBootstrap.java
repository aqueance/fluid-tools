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

package org.fluidity.deployment.plugin.spi;

import java.io.File;
import java.util.List;

import org.fluidity.composition.ServiceProvider;

/**
 * Bootstraps an HTTP server and deploys one or more WAR files.
 *
 * @author Tibor Varga
 */
@ServiceProvider
public interface ServerBootstrap {

    /**
     * Bootstraps a web server and deploys a list of WAR files.
     *
     * @param httpPort    The HTTP port to listen on. If == 0 then no HTTP listener is needed.
     * @param extract     specifies whether the WAR content should be extracted (<code>true</code>) or not (<code>false</code>).
     * @param bootApp     The web application WAR file used to establish the boot class path. The list of JAR files to be added to the boot class path will
     *                    be found under <code>WEB-INF/boot</code> of this WAR file. This WAR file shall be deployed under the root (<code>/</code>)
     *                    context.
     * @param managedApps The web application WAR files to deploy. These WAR files shall be deployed under a context that is derived from the WAR name
     *                    excluding the version number and the extension.
     * @param args        The list of command line arguments left unprocessed by the invoker.
     *
     * @throws Exception thrown when something goes wrong.
     */
    void bootstrap(int httpPort, boolean extract, File bootApp, List<File> managedApps, String args[]) throws Exception;
}
