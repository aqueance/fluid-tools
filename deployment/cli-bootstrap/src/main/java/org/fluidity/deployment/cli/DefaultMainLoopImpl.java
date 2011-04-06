/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.deployment.cli;

import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.Component;
import org.fluidity.composition.Optional;
import org.fluidity.deployment.DeploymentControl;
import org.fluidity.deployment.RuntimeControl;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Marker;

/**
 * This run loop implementation expects the application loop to be in an {@link Application} implementation, which when done invokes {@link
 * org.fluidity.deployment.RuntimeControl#stop()}. The application loop is optional and when not present, the application can be stopped using Ctrl-C.
 *
 * @author Tibor Varga
 */
@Component(api = { MainLoop.class, DeploymentControl.class, RuntimeControl.class }, primary = false)
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
