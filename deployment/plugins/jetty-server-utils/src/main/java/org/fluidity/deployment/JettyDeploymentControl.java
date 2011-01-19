/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.deployment;

import org.fluidity.composition.Component;
import org.fluidity.foundation.Exceptions;

import org.eclipse.jetty.server.Server;

/**
* @author Tibor Varga
*/
@Component(automatic = false)
final class JettyDeploymentControl implements DeploymentControl {

    private final Server server;
    private final boolean standalone;

    public JettyDeploymentControl(final Server server, final boolean standalone) {
        this.server = server;
        this.standalone = standalone;
    }

    public void completed() {
        // empty
    }

    public boolean isStandalone() {
        return standalone;
    }

    public void stop() {
        Exceptions.wrap("stopping Jetty server", new Exceptions.Command<Void>() {
            public Void run() throws Exception {
                server.stop();
                return null;
            }
        });
    }
}
