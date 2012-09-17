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

package org.fluidity.features.impl;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.Component;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.features.Scheduler;
import org.fluidity.foundation.Command;
import org.fluidity.foundation.Deferred;

/**
 * @author Tibor Varga
 */
@Component(primary = false)
final class SchedulerImpl implements Scheduler {

    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Deferred.Reference.State<Timer> timer = Deferred.state(new Deferred.Factory<Timer>() {
        public Timer create() {
            return new Timer(Scheduler.class.getName(), true);
        }
    });

    SchedulerImpl(final ContainerTermination termination) {
        termination.add(new Command.Job<Exception>() {
            public void run() {
                if (stopped.compareAndSet(false, true) && timer.resolved()) {
                    timer.invalidate().cancel();
                }
            }
        });
    }

    public Control invoke(final long delay, final long period, final Runnable task) {
        return schedule(delay, period, task);
    }

    public Control invoke(final long delay, final Runnable task) {
        return schedule(delay, 0, task);
    }

    private Control schedule(final long delay, final long period, final Runnable command) {
        if (stopped.get()) {
            throw new IllegalStateException("Scheduler has been stopped");
        }

        final TimerTask task = new TimerTask() {
            public void run() {
                command.run();
            }
        };

        if (period > 0) {
            timer.get().schedule(task, delay, period);
        } else {
            timer.get().schedule(task, delay);
        }

        return new Control() {
            public boolean cancel() {
                return task.cancel();
            }
        };
    }
}
