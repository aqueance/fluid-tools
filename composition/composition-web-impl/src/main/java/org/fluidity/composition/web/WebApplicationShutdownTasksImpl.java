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

package org.fluidity.composition.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fluidity.composition.Component;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.composition.spi.ShutdownTasks;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Marker;

/**
 * Implements the component shutdown mechanism for web applications. The implementation requires either to be explicitly registered as a
 * <code>ServletContextListeners</code> or a mechanism that finds this class and forwards listener method invocations to it. Fluid Tools has such mechanism in
 * the form of a <code>ServletContextListeners</code>, <code>AggregatingServletContextListener</code>, which uses service provider discovery to find any
 * implementation of the <code>ServletContextListeners</code> interface, including this class, as long the implementation is marked by the {@link
 * ServiceProvider} annotation.
 *
 * @author Tibor Varga
 */
@Component(api = ShutdownTasks.class)
@ServiceProvider(api = ServletContextListener.class)
final class WebApplicationShutdownTasksImpl implements ShutdownTasks, ServletContextListener {

    private static final Map<String, Runnable> tasks = new HashMap<String, Runnable>();

    private final Log log;

    public WebApplicationShutdownTasksImpl(final @Marker(WebApplicationShutdownTasksImpl.class) Log log) {
        this.log = log;
    }

    public void add(final String name, final Runnable command) {
        tasks.put(name, command);
    }

    public void contextInitialized(final ServletContextEvent event) {
        // empty
    }

    public void contextDestroyed(final ServletContextEvent event) {
        for (final Map.Entry<String, Runnable> entry : tasks.entrySet()) {
            try {
                entry.getValue().run();
            } catch (final Exception e) {
                log.error(e, "Running shutdown task %s", entry.getKey());
            }
        }
    }
}
