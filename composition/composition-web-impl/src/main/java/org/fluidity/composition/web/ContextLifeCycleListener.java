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
import org.fluidity.composition.Inject;
import org.fluidity.composition.spi.ContainerTermination;

/**
 * Adapts the Fluid Tools container life-cycle to the web application life-cycle.
 * <h3>Usage</h3>
 * This listener is registered in a <code>web.xml</code> descriptor of a web application.
 * <pre>
 * &lt;web-app &hellip;>
 * &hellip;
 *   &lt;listener>
 *     &lt;description>Helps manage the Fluid Tools container life cycle&lt;/description>
 *     &lt;display-name>Fluid Tools Web Application Life-Cycle Listener&lt;/display-name>
 *     &lt;listener-class><span class="hl1">org.fluidity.composition.web.ContextLifeCycleListener</span>&lt;/listener-class>
 *   &lt;/listener>
 * &hellip;
 * &lt;/web-app>
 * </pre>
 */
public final class ContextLifeCycleListener extends BoundaryComponent implements ServletContextListener {

    @Inject
    private ContainerTermination.Jobs<ContextLifeCycleListener> jobs;

    /**
     * Default constructor to be used by the servlet container.
     */
    public ContextLifeCycleListener() { }

    /**
     * Invoked by the servlet container.
     *
     * @param event the event that trigger the invocation.
     */
    public void contextInitialized(final ServletContextEvent event) {
        // empty
    }

    /**
     * Invoked by the servlet container.
     *
     * @param event the event that trigger the invocation.
     */
    public void contextDestroyed(final ServletContextEvent event) {
        jobs.flush();
    }
}
