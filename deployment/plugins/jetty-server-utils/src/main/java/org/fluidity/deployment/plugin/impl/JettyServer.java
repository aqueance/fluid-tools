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

package org.fluidity.deployment.plugin.impl;

import java.util.List;

import org.fluidity.foundation.Utility;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * An embedded Jetty server.
 *
 * @author Tibor Varga
 */
public final class JettyServer extends Utility {

    private JettyServer() { }

    /**
     * Starts an embedded Jetty server and waits until it stops.
     *
     * @param httpPort       the port for the server to listen on.
     * @param defaultContext the default web application.
     * @param contextList    optional list of other web applications.
     */
    public static void start(final int httpPort, final WebAppContext defaultContext, final List<WebAppContext> contextList) {
        final ContextHandlerCollection contexts = new ContextHandlerCollection();

        final HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(contexts);

        contexts.addHandler(defaultContext);
        for (final WebAppContext context : contextList) {
            contexts.addHandler(context);
        }

        final Server server = new Server();

        if (httpPort > 0) {
            handlers.addHandler(new DefaultHandler());
            handlers.addHandler(new RequestLogHandler());

            final SelectChannelConnector connector = new SelectChannelConnector();
            connector.setPort(httpPort);
            server.addConnector(connector);
        }

        server.setHandler(handlers);

        try {
            System.out.println("Starting server - press Ctrl-C to stop.");
            server.setStopAtShutdown(true);
            server.start();
            server.join();
        } catch (final Exception e) {
            throw new RuntimeException("Starting server", e);
        }
    }
}
