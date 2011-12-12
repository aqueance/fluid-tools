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

package org.fluidity.features.impl;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fluidity.composition.Component;
import org.fluidity.features.Scheduler;
import org.fluidity.foundation.Deferred;

/**
 * @author Tibor Varga
 */
@Component(automatic = false)
final class SchedulerImpl implements Scheduler {

    private final AtomicBoolean stopped = new AtomicBoolean();
    private final Deferred.Reference<Timer> timer;

    public SchedulerImpl() {
        this.timer = Deferred.reference(new Deferred.Factory<Timer>() {
            public Timer create() {
                return new Timer(Scheduler.class.getName(), true);
            }
        });
    }

    public synchronized void stop() {
        if (stopped.compareAndSet(false, true) && timer.resolved()) {
            final Timer timer = this.timer.get();

            if (timer != null) {
                timer.cancel();
            }
        }
    }

    public Control invoke(final long delay, final long period, final Runnable task) {
        return schedule(delay, period, new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        });
    }

    public Control invoke(final long delay, final Runnable task) {
        return schedule(delay, 0, new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        });
    }

    private Control schedule(final long delay, final long period, final TimerTask task) {
        if (stopped.get()) {
            throw new IllegalStateException("Scheduler has been stopped");
        }

        timer.get().schedule(task, delay, period);

        return new Control() {
            public boolean cancel() {
                return task.cancel();
            }
        };
    }
}
