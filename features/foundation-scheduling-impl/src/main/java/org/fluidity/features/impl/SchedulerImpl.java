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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.fluidity.composition.Component;
import org.fluidity.composition.spi.ContainerTermination;
import org.fluidity.features.Scheduler;
import org.fluidity.foundation.Command;
import org.fluidity.foundation.Configuration;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Log;

/**
 * @author Tibor Varga
 */
@Component(primary = false)
final class SchedulerImpl implements Scheduler {

    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Deferred.Reference<Timer> timer = Deferred.reference(new Deferred.Factory<Timer>() {
        public Timer create() {
            return new Timer(Scheduler.class.getName(), true);
        }
    });

    private final Configuration<Settings> configuration;
    private final Log log;

    SchedulerImpl(final ContainerTermination termination,
                  final @Component.Context(ignore = Configuration.Prefix.class) Configuration<Settings> configuration,
                  final Log<SchedulerImpl> log) {
        this.configuration = configuration;
        this.log = log;

        termination.add(new Command.Job<Exception>() {
            public void run() {
                if (stopped.compareAndSet(false, true) && timer.resolved()) {
                    timer.invalidate().cancel();
                }
            }
        });
    }

    public Task.Control invoke(final long delay, final long period, final Task task) {
        return schedule(delay, period, task);
    }

    public Task.Control invoke(final long delay, final Task task) {
        return schedule(delay, 0, task);
    }

    private Task.Control schedule(final long delay, final long period, final Task command) {
        if (stopped.get()) {
            throw new IllegalStateException("Scheduler has been stopped");
        }

        final AtomicBoolean suspended = new AtomicBoolean();
        final AtomicInteger errorCount = new AtomicInteger();
        final AtomicLong timestamp = new AtomicLong();

        final AtomicInteger errorLimit = new AtomicInteger(-1);
        final AtomicLong penaltyTime = new AtomicLong(-1);

        final AtomicBoolean canceled = new AtomicBoolean();
        final AtomicBoolean delayed = new AtomicBoolean();

        final TimerTask task = new TimerTask() {
            @Override
            public boolean cancel() {
                canceled.set(true);
                return super.cancel();
            }

            @Override
            public void run() {
                final AtomicLong penalty = new AtomicLong(penaltyTime.get());
                final AtomicInteger limit = new AtomicInteger(errorLimit.get());

                // make sure we don't have invalid data in our user exposed settings
                if (penalty.get() <= 0 || limit.get() < 0) {
                    configuration.query(new Configuration.Query<Void, Settings>() {
                        public Void run(final Settings settings) throws Exception {
                            long penaltyValue = penaltyTime.get();
                            while (penaltyValue <= 0 && !penaltyTime.compareAndSet(penaltyValue, settings.defaultErrorPenalty())) {
                                penaltyValue = penaltyTime.get();
                            }

                            int limitValue = errorLimit.get();
                            while (limitValue < 0 && !errorLimit.compareAndSet(limitValue, settings.defaultErrorLimit())) {
                                limitValue = errorLimit.get();
                            }

                            limit.set(errorLimit.get());
                            penalty.set(penaltyTime.get());

                            return null;
                        }
                    });
                }

                final long time = timestamp.get();

                if (suspended.get()) {
                    if (time > 0 && System.currentTimeMillis() - time >= penalty.get()) {
                        errorCount.set(0);
                        suspended.set(false);
                    } else {
                        if (period == 0) {
                            delayed.set(true);
                        }

                        return;
                    }
                }

                try {
                    command.run();
                    timestamp.set(0);
                } catch (final Exception e) {
                    if (limit.get() > 0) {
                        final int errors = errorCount.incrementAndGet();

                        if (time > 0 || errors >= limit.get()) {
                            timestamp.set(System.currentTimeMillis());
                            suspended.set(true);
                            log.error(e, "Scheduled task (%s) suspended for %d ms due to too many exceptions", command, penalty.get());
                        } else {
                            log.error(e, "Scheduled task (%s) exception %d/%d", command, errors, limit.get());
                        }
                    } else {
                        log.error(e, "Scheduled task (%s) exception", command);
                    }
                } catch (final Error e) {
                    if (cancel()) {
                        log.error(e, "Scheduled task (%s) cancelled due to error", command);
                    } else {
                        log.error(e, "Scheduled task (%s) error", command);
                    }
                }
            }
        };

        if (period > 0) {
            timer.get().scheduleAtFixedRate(task, delay, period);
        } else {
            timer.get().schedule(task, delay);
        }

        return new Task.Control() {
            public void suspend() {
                suspended.set(true);
                errorCount.set(0);
                timestamp.set(0);
            }

            public void resume() {
                timestamp.set(0);
                errorCount.set(0);
                suspended.set(false);

                if (delayed.compareAndSet(true, false)) {
                    task.run();
                }
            }

            public boolean suspended() {
                return suspended.get();
            }

            public AtomicInteger exceptionLimit() {
                return errorLimit;
            }

            public AtomicLong penaltyTime() {
                return penaltyTime;
            }

            public boolean canceled() {
                return canceled.get();
            }

            public boolean cancel() {
                return task.cancel();
            }
        };
    }

    /**
     * Provides settings to the {@link Scheduler} component.
     *
     * @author Tibor Varga
     */
    interface Settings {

        /**
         * Tells how many consecutive errors are tolerated in task invocations by default. Can be overridden for individual tasks via {@link
         * Scheduler.Task.Control#exceptionLimit()}.
         *
         * @return a number greater than 0.
         */
        @Configuration.Property(key = Scheduler.TASK_EXCEPTION_LIMIT, undefined = "3")
        int defaultErrorLimit();

        /**
         * Tells how long a task is suspended, in milliseconds, after too many consecutive errors. Can be overridden for individual tasks via {@link
         * Scheduler.Task.Control#penaltyTime()}.
         *
         * @return a number greater than 0.
         */
        @Configuration.Property(key = Scheduler.TASK_EXCEPTION_PENALTY, undefined = "60000")
        long defaultErrorPenalty();
    }
}
