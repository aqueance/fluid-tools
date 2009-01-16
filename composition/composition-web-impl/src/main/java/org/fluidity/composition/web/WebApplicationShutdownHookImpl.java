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
package org.fluidity.composition.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainerAccess;
import org.fluidity.composition.ServiceProvider;
import org.fluidity.composition.ShutdownHook;
import org.fluidity.foundation.Logging;

/**
 * Implements the component shutdown mechanism for web applications. The implementation requires a mechanism that auto-discovers
 * <code>ServletContextListeners</code> and dispatches the respective servlet events to all without each having to be registered in the web application's
 * web.xml file. Such mechanism is the {@link org.fluidity.composition.web.WebApplicationLifecycleListener} that should then be added as a listener in the
 * web.xml.
 *
 * @author Tibor Varga
 */
@Component(api = ShutdownHook.class)
@ServiceProvider(api = ServletContextListener.class)
final class WebApplicationShutdownHookImpl implements ShutdownHook, ServletContextListener {

    private static final Map<String, Runnable> tasks = new HashMap<String, Runnable>();

    public void addTask(final String threadName, final Runnable command) {
        tasks.put(threadName, command);
    }

    public void contextInitialized(final ServletContextEvent event) {
        // empty
    }

    public void contextDestroyed(final ServletContextEvent event) {
        final Logging log = new ComponentContainerAccess().getComponent(Logging.class);

        for (final Map.Entry<String, Runnable> entry : tasks.entrySet()) {
            try {
                entry.getValue().run();
            } catch (final Exception e) {
                log.fatal(getClass(), "Running shutdown task " + entry.getKey(), e);
            }
        }
    }
}
