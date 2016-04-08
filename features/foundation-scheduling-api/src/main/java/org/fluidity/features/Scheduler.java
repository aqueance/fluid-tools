/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.features;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.fluidity.foundation.Command;

/**
 * Provides scheduled and periodic command invocation. This component keeps a hard reference to the supplied commands until their scheduled invocation is
 * canceled - either automatically or manually by calling {@link Scheduler.Task.Control#cancel()}.
 * <p>
 * <b>NOTE</b>: This component keeps a hard reference to the tasks supplied in its {@link #invoke(long, Scheduler.Task)} and {@link #invoke(long, long,
 * Scheduler.Task)} methods. If you need periodic updates <i>without</i> the consequent hard reference, use {@link Updates} instead of directly using this
 * component.
 * <p>
 * Scheduled tasks may throw {@linkplain Exception exceptions} and {@linkplain Error errors}. A task that throws an error is immediately {@linkplain
 * Scheduler.Task.Control#cancel() canceled}. A task that throws an exception for {@link Scheduler.Task.Control#exceptionLimit} consecutive invocations is
 * suspended for {@link Scheduler.Task.Control#penaltyTime()} milliseconds. If the task throws an exception right after the penalty, it is again suspended for
 * the penalty time.
 * <p>
 * The default penalty time can be configured by implementing a {@link org.fluidity.foundation.spi.PropertyProvider PropertyProvider} component that returns a
 * valid number for the {@link #TASK_EXCEPTION_PENALTY} key. The default penalty time is 1 minute.
 * <p>
 * The default exception limit can be configured by implementing a {@link org.fluidity.foundation.spi.PropertyProvider PropertyProvider} component that returns
 * a valid number for the {@link #TASK_EXCEPTION_LIMIT} key. The default exception limit is 3.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain org.fluidity.composition.Component @Component}
 * public final class <span class="hl2">MyComponent</span> {
 *
 *   private static final long period = {@linkplain java.util.concurrent.TimeUnit#MILLISECONDS TimeUnit.MILLISECONDS}.{@linkplain java.util.concurrent.TimeUnit#convert(long, java.util.concurrent.TimeUnit) convert}(1, {@linkplain java.util.concurrent.TimeUnit#SECONDS TimeUnit.SECONDS});
 *
 *   private volatile long <span class="hl3">timestamp</span>;
 *
 *   <span class="hl2">MyComponent</span>(final <span class="hl1">Scheduler</span> scheduler) {
 *     scheduler.<span class="hl1">invoke</span>(period, period, new {@linkplain Scheduler.Task}() {
 *       public void run() {
 *         <span class="hl3">timestamp</span> = {@linkplain System#currentTimeMillis()};
 *       }
 *     });
 *   }
 *
 *   &hellip;
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@SuppressWarnings("JavadocReference")
public interface Scheduler {

    /**
     * The configuration property that specifies how many consecutive errors are tolerated in task invocations by default. Can be overridden for individual
     * tasks via {@link Scheduler.Task.Control#exceptionLimit}.
     */
    String TASK_EXCEPTION_LIMIT = "org.fluidity.features.scheduler-default-task-exception-limit";

    /**
     * The configuration property that specifies how long a task is suspended, in milliseconds, after too many consecutive errors. Can be overridden for
     * individual tasks via {@link Scheduler.Task.Control#penaltyTime()}.
     */
    String TASK_EXCEPTION_PENALTY = "org.fluidity.features.scheduler-default-task-exception-penalty-ms";

    /**
     * Adds the given task to be invoked at a fixed <code>period</code> milliseconds rate after <code>delay</code> milliseconds.
     *
     * @param delay  the delay in milliseconds before the first invocation.
     * @param period the time in milliseconds between the invocations.
     * @param task   the task to invoke.
     *
     * @return an object to control the invocations.
     */
    Task.Control invoke(long delay, long period, Task task);

    /**
     * Adds the given task to be invoked after <code>delay</code> milliseconds.
     *
     * @param delay the delay in milliseconds before the invocation.
     * @param task  the task to invoke.
     *
     * @return an object to control the invocation.
     */
    Task.Control invoke(long delay, Task task);

    /**
     * A scheduled task.
     *
     * @author Tibor Varga
     */
    interface Task extends Command.Job<Exception> {

        /**
         * Controls an invocation scheduled by the <code>invoke()</code> methods of a {@link Scheduler}.
         * <h3>Usage</h3>
         * See {@link Scheduler}.
         */
        interface Control {

            /**
             * Suspends the task. A task suspended by calling this method is not invoked until {@linkplain #resume() resumed} again. If the task was
             * {@linkplain Scheduler#invoke(long, Scheduler.Task) scheduled for a single invocation} and that single invocation happened while the task was
             * suspended, then the task will be invoked when it is resumed.
             */
            void suspend();

            /**
             * Resumes a suspended task. The task may be suspended by calling {@link #suspend()} or by it throwing some exception in {@link #exceptionLimit()}
             * consecutive invocations.
             */
            void resume();

            /**
             * Tells if the task is suspended or not. The task may be suspended by calling {@link #suspend()} or by it throwing some exception in {@link
             * #exceptionLimit} consecutive invocations.
             *
             * @return <code>true</code> if the task is suspended, <code>false</code> otherwise.
             */
            boolean suspended();

            /**
             * Returns the exception limit, which is the number of consecutive invocations throwing an exception that will cause the task to be suspended. A
             * suspended task is not invoked until it is {@linkplain #resume() resumed} or until a {@linkplain #penaltyTime() penalty time} is elapsed.
             * <p>
             * The default exception limit is specified using the {@link Scheduler#TASK_EXCEPTION_LIMIT} property; see {@link Scheduler} for details.
             *
             * @return an atomically mutable integer that can be used to set or query the exception limit.
             */
            AtomicInteger exceptionLimit();

            /**
             * The time in milliseconds a task suspended due to too many consecutive invocations throwing an exception will be suspended for. If the task
             * throws an exception again after the penalty time, it is again suspended for the penalty time.
             * <p>
             * The default penalty time is specified using the {@link Scheduler#TASK_EXCEPTION_PENALTY} property; see {@link Scheduler} for details.
             *
             * @return an atomically mutable long that can be used to set or query the penalty time.
             */
            AtomicLong penaltyTime();

            /**
             * Tells if the task has been canceled, either manually or due to the task having thrown an {@link Error}.
             *
             * @return <code>true</code> if the task has been canceled, <code>false</code> otherwise.
             */
            boolean canceled();

            /**
             * Cancels any subsequent scheduled invocation of the task this control corresponds to.
             *
             * @return <code>true</code> if the call prevented one or more scheduled executions from taking place;
             *         <code>false</code> otherwise.
             */
            boolean cancel();
        }
    }
}
