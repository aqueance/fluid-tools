/*
 * Copyright (c) 2006-2018 Tibor Adam Varga (tibor.adam.varga on gmail)
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
import org.fluidity.foundation.security.Security;

/**
 * Uses the {@link Runtime} object to add shutdown tasks to. The caller must make sure it has enough privileges to add a shutdown hook.
 *
 * @author Tibor Varga
 */
@Component
@SuppressWarnings("unchecked")
final class RuntimeTermination implements ContainerTermination {

    private final Jobs jobs;

    RuntimeTermination(final Jobs<RuntimeTermination> jobs) {
        this.jobs = jobs;

        Security.invoke(() -> {
            Runtime.getRuntime().addShutdownHook(new Thread(jobs::flush, "Container shutdown"));
            return null;
        });
    }

    public void add(final Command.Job<Exception> job) {
        jobs.add(job);
    }

    public void remove(final Command.Job<Exception> job) {
        jobs.remove(job);
    }
}
