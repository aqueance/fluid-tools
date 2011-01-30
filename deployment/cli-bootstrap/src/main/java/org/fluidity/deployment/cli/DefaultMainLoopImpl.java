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

package org.fluidity.deployment.cli;

import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.Component;
import org.fluidity.composition.Optional;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Marker;

/**
 * This run loop implementation expects the application loop to be in an {@link Application} implementation, which when done invokes {@link
 * org.fluidity.deployment.RuntimeControl#stop()}. The application loop is optional and when not present, the application can be stopped using Ctrl-C.
 *
 * @author Tibor Varga
 */
@Component(primary = false)
final class DefaultMainLoopImpl implements MainLoop {

    private final Object lock = new Object();
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private final Application application;
    private final Log log;

    public DefaultMainLoopImpl(final @Marker(DefaultMainLoopImpl.class) Log log, final @Optional Application application) {
        this.log = log;
        this.application = application;
    }

    public void run() {
        if (application != null) {
            application.run(this);
        } else {
            log.info("Application started. Press Ctrl-C to stop it.");
        }

        synchronized (lock) {
            if (!stopped.get()) {
                try {
                    lock.wait();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();     // set the thread's interrupt status
                }
            }
        }
    }

    public void completed() {
        // ok
    }

    public boolean isStandalone() {
        return application != null;
    }

    public void stop() {
        if (!stopped.compareAndSet(false, true)) {
            synchronized (lock) {
                lock.notify();
            }
        }
    }
}
