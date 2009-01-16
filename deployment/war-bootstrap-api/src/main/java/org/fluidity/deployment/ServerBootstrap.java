/*
 * Copyright (c) 2006-2009 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.deployment;

import java.io.File;
import java.util.List;

import org.fluidity.composition.ServiceProvider;

/**
 * Bootstraps an HTTP server and deploys one or more .war files.
 */
@ServiceProvider
public interface ServerBootstrap {

    /**
     * Bootstraps a web server and deploys a list of .war files, either automatically or by providing a component through which another entity can manage the
     * deployment.
     *
     * @param httpPort      The HTTP port to listen on. If == 0 then no HTTP listener is needed.
     * @param bootApp       The web application .war file used to establish the boot class path. The list of .jar files to be added to the boot class path will
     *                      be found under <code>WEB-INF/boot</code> of this .war file. This .war file shall be deployed under the root (<code>/</code>)
     *                      context.
     * @param managedApps   The web application .war files to deploy. These .war files shall be deployed under a context that is derived from the .war name
     *                      exlucing the version number and the extension.
     * @param workDirectory The working directory for use by the web server.
     * @param args          The list of command line arguments left unprocessed by the invoker.
     */
    void bootstrap(final int httpPort, final File bootApp, final List<File> managedApps, final File workDirectory, final String args[]);
}
