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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;

/**
 * Aggregates all servlet contaxt listeners and dispatches servlet context events to each. <p/> This class is public and
 * must be registered as a listener in the host application's web.xml file.
 *
 * @author Tibor Varga
 * @version $Revision$
 */
public final class ServletContextListener implements javax.servlet.ServletContextListener {

    private static DependencyResolver resolver = new DependencyResolverImpl();

    private javax.servlet.ServletContextListener[] list;

    public void contextInitialized(ServletContextEvent event) {
        try {
            ServletContextListenerList listeners = (ServletContextListenerList) resolver.findComponent(
                event.getServletContext().getInitParameter(DependencyResolver.CONTAINER_CLASS),
                ServletContextListenerList.class.getName());

            list = listeners.list();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }

        for (javax.servlet.ServletContextListener listener : list) {
            listener.contextInitialized(event);
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        for (javax.servlet.ServletContextListener listener : list) {
            listener.contextDestroyed(event);
        }
    }
}
