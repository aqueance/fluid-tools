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

package org.fluidity.composition.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContext;
import org.fluidity.foundation.Command;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.Strings;

/**
 * @author Tibor Varga
 */
@Component
@Component.Context(Component.Reference.class)
final class ContainerTerminationJobs<T> implements ContainerTermination.Jobs<T> {

    private final List<Command.Job<Exception>> jobs = new ArrayList<Command.Job<Exception>>();
    private final List<Command.Job<Exception>> added = new ArrayList<Command.Job<Exception>>();
    private final List<Command.Job<Exception>> removed = new ArrayList<Command.Job<Exception>>();

    private final Log log;
    private final Class<?> caller;

    private boolean flushing;

    ContainerTerminationJobs(final Log<ContainerTerminationJobs> log, final ComponentContext context) {
        this.log = log;
        this.caller = context.annotation(Component.Reference.class, ContainerTermination.Jobs.class).parameter(0);
    }

    public void flush() {
        final boolean go;

        synchronized (this) {
            if (go = !flushing) {
                flushing = true;
            }
        }

        if (go) {
            flush(jobs);

            synchronized (this) {
                flushing = false;
            }

            synchronized (this) {
                flush(added);
                removed.clear();

                // all must be flushed
                assert jobs.isEmpty();
                assert added.isEmpty();
                assert removed.isEmpty();
            }
        }
    }

    private void flush(final List<Command.Job<Exception>> list) {
        for (final ListIterator<Command.Job<Exception>> iterator = list.listIterator(list.size()); iterator.hasPrevious(); ) {
            final Command.Job<Exception> job = iterator.previous();

            try {
                synchronized (this) {
                    if (removed.remove(job)) {
                        continue;
                    }
                }

                job.run();
            } catch (final Exception e) {
                log.error(e, job.getClass().getName());
            } finally {
                iterator.remove();
            }
        }
    }

    private boolean check(final Command.Job<Exception> job, final Class<?> caller) {
        if (job == null) {
            throw new IllegalArgumentException(String.format("Attempted to add null job to %s", Strings.formatClass(false, true, caller)));
        }

        final ClassLoader check = job.getClass().getClassLoader();
        for (ClassLoader loader = caller.getClassLoader(); loader != null; loader = loader.getParent()) {
            if (loader == check) {
                return true;
            }
        }

        return caller.getClassLoader() == null && check == null;
    }

    public void add(final Command.Job<Exception> job) {
        if (!check(job, caller)) {
            throw new IllegalArgumentException(String.format("The class loader of job %s is neither in the ancestry of, nor the same as, that of %s",
                                                             Strings.formatObject(false, true, job),
                                                             Strings.formatClass(false, true, caller)));
        }

        synchronized (this) {
            if (flushing) {
                added.add(job);
            } else {
                jobs.add(job);
            }
        }
    }

    public synchronized void remove(final Command.Job<Exception> job) {
        if (flushing) {
            removed.add(job);
            added.remove(job);
        } else {
            jobs.remove(job);
        }
    }
}
