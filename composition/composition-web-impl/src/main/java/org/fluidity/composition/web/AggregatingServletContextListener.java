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

package org.fluidity.composition.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fluidity.composition.BoundaryComponent;

/**
 * Forwards servlet context listener callbacks to all {@link ServletContextListener ServletContextListeners} annotated with
 * {@link org.fluidity.composition.ComponentGroup @ComponentGroup}. This makes it possible to register those listeners without explicitly listing them in
 * <code>web.xml</code>.
 * <p/>
 * <b>Note</b>: servlet specification version 3.0 and later offers an annotation based solution to the same problem, thus obsoleting the solution presented by
 * this class.
 * <h3>Usage</h3>
 * This listener is registered in a <code>web.xml</code> descriptor of a web application.
 * <pre>
 * &lt;web-app &hellip;>
 * &hellip;
 *   &lt;listener>
 *     &lt;description>Helps to manage a Fluid Tools container life cycle&lt;/description>
 *     &lt;display-name>Fluid Tools Servlet Context Listener&lt;/display-name>
 *     &lt;listener-class><span class="hl1">org.fluidity.composition.web.AggregatingServletContextListener</span>&lt;/listener-class>
 *   &lt;/listener>
 * &hellip;
 * &lt;/web-app>
 * </pre>
 */
public final class AggregatingServletContextListener extends BoundaryComponent implements ServletContextListener {

    private final ServletContextListener listeners[];

    /**
     * Invoked by the servlet container to create a new instance.
     */
    public AggregatingServletContextListener() {
        listeners = container().getComponentGroup(ServletContextListener.class);
    }

    /**
     * Invoked by the servlet container.
     *
     * @param event the event that trigger the invocation.
     */
    public void contextInitialized(final ServletContextEvent event) {
        for (final ServletContextListener listener : listeners) {
            listener.contextInitialized(event);
        }
    }

    /**
     * Invoked by the servlet container.
     *
     * @param event the event that trigger the invocation.
     */
    public void contextDestroyed(final ServletContextEvent event) {
        for (final ServletContextListener listener : listeners) {
            listener.contextDestroyed(event);
        }
    }
}
