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

package org.fluidity.composition.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fluidity.composition.BoundaryComponent;
import org.fluidity.composition.Component;
import org.fluidity.composition.Inject;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.composition.web.impl.TerminationControl;

/**
 * Adapts the Fluid Tools container life-cycle to the web application life-cycle.
 * <h3>Usage</h3>
 * This listener is registered in a <code>web.xml</code> descriptor of a web application prior to Servlet 3.0. For Servlet 3.0 applications, the archive
 * containing this class ships with a web fragment that does the same.
 * <pre>
 * &lt;web-app &hellip;&gt;
 * &hellip;
 *   &lt;listener&gt;
 *     &lt;description&gt;Manages the Fluid Tools container life cycle&lt;/description&gt;
 *     &lt;display-name&gt;Fluid Tools Web Application Life-Cycle Listener&lt;/display-name&gt;
 *     &lt;listener-class&gt;<span class="hl1">org.fluidity.composition.web.ContextLifeCycleListener</span>&lt;/listener-class&gt;
 *   &lt;/listener&gt;
 * &hellip;
 * &lt;/web-app&gt;
 * </pre>
 */
public final class ContextLifeCycleListener extends BoundaryComponent implements ServletContextListener {

    static {
        Instance.registered = true;
    }

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

    /**
     * @author Tibor Varga
     */
    @Component
    private static class Instance implements TerminationControl {

        // cannot use instance field: getting the instance injected would trigger dependency injection, which would cause WebApplicationTermination to call
        // #present() before this flag could be set
        static volatile boolean registered;

        public boolean present() {
            return Instance.registered;
        }
    }
}
