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

import org.fluidity.composition.Component;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.composition.web.ContextLifeCycleListener;
import org.fluidity.foundation.Command;
import org.fluidity.foundation.Strings;

/**
 * Implements the component shutdown mechanism for web applications. The implementation requires, to be able to serve its function, {@link
 * ContextLifeCycleListener} to be registered as a <code>ServletContextListeners</code>.
 *
 * @author Tibor Varga
 */
@Component
@SuppressWarnings("unchecked")
final class WebApplicationTermination implements ContainerTermination {

    private final Jobs jobs;
    private final TerminationControl control;

    // we must use the jobs instance flushed by ContextLifeCycleListener
    WebApplicationTermination(final Jobs<ContextLifeCycleListener> jobs, final TerminationControl control) {
        this.jobs = jobs;
        this.control = control;
    }

    public void add(final Command.Job<Exception> job) {
        checkControl();
        jobs.add(job);
    }

    public void remove(final Command.Job<Exception> job) {
        checkControl();
        jobs.remove(job);
    }

    private void checkControl() {
        if (!control.present()) {
            throw new IllegalStateException(String.format("%s has not been registered", Strings.formatClass(false, true, ContextLifeCycleListener.class)));
        }
    }
}
