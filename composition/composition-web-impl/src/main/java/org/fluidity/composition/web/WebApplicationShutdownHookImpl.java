/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga@gmail.com)
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
 *
 */
package org.fluidity.composition.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fluidity.composition.Component;
import org.fluidity.composition.ShutdownHook;

/**
 * Implements the component shutdown mechanism for web applications. The implementation requires a mechanism that
 * auto-discovers <code>ServletContextListeners</code> and dispatches the respective servlet events to all without each
 * having to be registered in the web application's web.xml file.
 *
 * @author Tibor Varga
 * @version $Revision$
 */
@Component(api = ShutdownHook.class)
final class WebApplicationShutdownHookImpl implements ShutdownHook, ServletContextListener {

    private static final List<Runnable> tasks = new ArrayList<Runnable>();

    public void addTask(String threadName, Runnable command) {
        tasks.add(command);
    }

    public void contextInitialized(ServletContextEvent event) {
        // empty
    }

    public void contextDestroyed(ServletContextEvent event) {
        for (Runnable task : tasks) {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
