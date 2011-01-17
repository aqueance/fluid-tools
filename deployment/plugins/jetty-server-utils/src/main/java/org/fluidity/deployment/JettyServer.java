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

import java.util.List;

import org.fluidity.composition.ComponentContainerAccess;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Starts an embedded Jetty server.
 *
 * @author Tibor Varga
 */
public final class JettyServer {

    private JettyServer() {
        throw new UnsupportedOperationException("No instance allowed");
    }

    public static void start(final int httpPort, final String[] args, final WebAppContext defaultContext, final List<WebAppContext> contextList) {
        final ContextHandlerCollection contexts = new ContextHandlerCollection();

        final HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(contexts);

        contexts.addHandler(defaultContext);
        for (final WebAppContext context : contextList) {
            contexts.addHandler(context);
        }

        final Server server = new Server();

        final ComponentContainerAccess container = new ComponentContainerAccess();

        if (args != null) {
            container.setBindingProperty(LaunchArguments.ARGUMENTS_KEY, args);
        }

        container.setBindingProperty(DeploymentControl.SERVER_KEY, server);
        container.setBindingProperty(DeploymentControl.STANDALONE_KEY, false);

        if (httpPort > 0) {
            handlers.addHandler(new DefaultHandler());
            handlers.addHandler(new RequestLogHandler());

            final SelectChannelConnector connector = new SelectChannelConnector();
            connector.setPort(httpPort);
            server.addConnector(connector);
        }

        server.setHandler(handlers);

        try {
            System.out.println("Starting server - press Ctrl-C to kill.");
            server.setStopAtShutdown(true);
            server.start();
            server.join();
        } catch (final Exception e) {
            throw new RuntimeException("Starting server", e);
        }
    }
}
