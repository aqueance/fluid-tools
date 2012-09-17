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

package org.fluidity.composition.cli.impl;

import org.fluidity.composition.Component;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.foundation.Command;

/**
 * Uses the {@link Runtime} object to add shutdown tasks to. The caller must make sure it has enough privileges to add a shutdown hook.
 *
 * @author Tibor Varga
 */
@Component
final class RuntimeTermination implements ContainerTermination {

    private final Jobs jobs;

    RuntimeTermination(final Jobs jobs) {
        this.jobs = jobs;

        Runtime.getRuntime().addShutdownHook(new Thread("Container shutdown") {
            public void run() {
                jobs.flush();
            }
        });
    }

    public void add(final Command.Job<Exception> job) {
        jobs.add(job);
    }

    public void remove(final Command.Job<Exception> job) {
        jobs.remove(job);
    }
}
