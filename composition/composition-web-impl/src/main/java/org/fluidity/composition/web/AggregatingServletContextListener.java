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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fluidity.composition.ContainerBoundary;

/**
 * Finds all implementations of the {@link ServletContextListener} that have been marked with the
 * @{@link org.fluidity.composition.ServiceProvider} annotation and dispatches the listener events to all.
 *
 * @author Tibor Varga
 */
public final class AggregatingServletContextListener implements ServletContextListener {

    private final ServletContextListener delegate;

    public AggregatingServletContextListener() {
        this.delegate = new ContainerBoundary().getComponent(AggregatingListenerDelegate.class);
    }

    public void contextInitialized(final ServletContextEvent event) {
        delegate.contextInitialized(event);
    }

    public void contextDestroyed(final ServletContextEvent event) {
        delegate.contextDestroyed(event);
    }
}