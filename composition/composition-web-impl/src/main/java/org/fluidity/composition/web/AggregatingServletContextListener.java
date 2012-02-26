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

package org.fluidity.composition.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fluidity.composition.ContainerBoundary;

/**
 * Forwards servlet context listener callbacks to all <code>ServletContextListener</code>s annotated with
 * {@link org.fluidity.composition.ComponentGroup @ComponentGroup}.
 * <h3>Usage</h3>
 * This listener is registered in a <code>web.xml</code> descriptor of a web application.
 * <pre>
 * &lt;web-app ...>
 * ...
 *   &lt;listener>
 *     &lt;description>Helps to manage a Fluid Tools container life cycle&lt;/description>
 *     &lt;display-name>Fluid Tools Servlet Context Listener&lt;/display-name>
 *     &lt;listener-class><span class="hl1">org.fluidity.composition.web.AggregatingServletContextListener</span>&lt;/listener-class>
 *   &lt;/listener>
 * ...
 * &lt;/web-app>
 * </pre>
 */
public final class AggregatingServletContextListener implements ServletContextListener {

    private final ServletContextListener listeners[] = new ContainerBoundary().getComponentGroup(ServletContextListener.class);

    public void contextInitialized(final ServletContextEvent event) {
        for (final ServletContextListener listener : listeners) {
            listener.contextInitialized(event);
        }
    }

    public void contextDestroyed(final ServletContextEvent event) {
        for (final ServletContextListener listener : listeners) {
            listener.contextDestroyed(event);
        }
    }
}
