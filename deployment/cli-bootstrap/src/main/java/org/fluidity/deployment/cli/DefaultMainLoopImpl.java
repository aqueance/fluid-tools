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
package org.fluidity.deployment.cli;

import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.Component;
import org.fluidity.deployment.DeploymentBootstrap;

/**
 * This default implementation expects the application code to be in a {@link org.fluidity.deployment.DeployedComponent}, which when done invokes {@link
 * org.fluidity.deployment.RuntimeControl#stop()}.
 */
@Component(fallback = true)
final class DefaultMainLoopImpl implements MainLoop {

    private final Object lock = new Object();

    private final DeploymentBootstrap bootstrap;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public DefaultMainLoopImpl(final DeploymentBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public void run() {
        if (bootstrap.deploymentCount() > 0 && !stopped.get()) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (final InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    public void stop() {
        if (!stopped.compareAndSet(false, true)) {
            synchronized (lock) {
                lock.notify();
            }
        }
    }
}
