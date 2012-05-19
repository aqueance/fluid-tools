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

package org.fluidity.composition.web.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentGroup;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.foundation.Log;

/**
 * Implements the component shutdown mechanism for web applications. The implementation requires either to be explicitly registered as a
 * <code>ServletContextListeners</code> or a mechanism that finds this class and forwards listener method invocations to it. Fluid Tools has such mechanism in
 * the form of a <code>ServletContextListeners</code>, <code>AggregatingServletContextListener</code>, which uses service provider discovery to find any
 * implementation of the <code>ServletContextListeners</code> interface, including this class, as long the implementation is marked by the {@link
 * ComponentGroup @ComponentGroup} annotation.
 *
 * @author Tibor Varga
 */
@Component
@ComponentGroup(api = ServletContextListener.class)
final class WebApplicationTermination implements ContainerTermination, ServletContextListener {

    private final List<Runnable> tasks = new ArrayList<Runnable>();
    private final Log log;

    public WebApplicationTermination(final Log<WebApplicationTermination> log) {
        this.log = log;
    }

    public void run(final Runnable command) {
        tasks.add(command);
    }

    public void contextInitialized(final ServletContextEvent event) {
        // empty
    }

    public void contextDestroyed(final ServletContextEvent event) {
        for (final ListIterator<Runnable> iterator = tasks.listIterator(tasks.size()); iterator.hasPrevious();) {
            final Runnable task = iterator.previous();
            try {
                task.run();
            } catch (final Exception e) {
                log.error(e, task.getClass().getName());
            }
        }
    }
}
