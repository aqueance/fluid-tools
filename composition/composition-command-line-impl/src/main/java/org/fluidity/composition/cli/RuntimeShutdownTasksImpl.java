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

package org.fluidity.composition.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.fluidity.composition.Component;
import org.fluidity.composition.spi.ShutdownTasks;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Source;

/**
 * Uses the {@link Runtime} object to add shutdown tasks to. The caller must make sure it has enough privileges to add a runtime shutdown hook.
 *
 * @author Tibor Varga
 */
@Component
final class RuntimeShutdownTasksImpl implements ShutdownTasks {

    private final List<Runnable> tasks = new ArrayList<Runnable>();

    public RuntimeShutdownTasksImpl(final @Source(RuntimeShutdownTasksImpl.class) Log log) {
        Runtime.getRuntime().addShutdownHook(new Thread("Container shutdown") {
            @Override
            public void run() {
                for (final ListIterator<Runnable> iterator = tasks.listIterator(tasks.size()); iterator.hasPrevious(); ) {
                    final Runnable task = iterator.previous();

                    try {
                        task.run();
                    } catch (final Exception e) {
                        log.error(e, task.getClass().getName());
                    } finally {
                        iterator.remove();
                    }
                }
            }
        });
    }

    public void add(final Runnable command) {
        tasks.add(command);
    }
}
