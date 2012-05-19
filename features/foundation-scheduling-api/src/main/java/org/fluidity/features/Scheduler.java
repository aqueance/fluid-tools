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

package org.fluidity.features;

/**
 * Provides scheduled and periodic command invocation. This component keeps a hard reference to the supplied commands until their scheduled invocation is
 * canceled - either automatically or manually by calling {@link Scheduler.Control#cancel()}.
 * <p/>
 * <b>NOTE</b>: This component keeps a hard reference to the tasks supplied in its {@link #invoke(long, Runnable)} and {@link #invoke(long, long, Runnable)}
 * methods. If you need periodic updates without the consequent hard reference, use {@link Updates} instead of directly using this component.
 *
 * @author Tibor Varga
 */
public interface Scheduler {

    /**
     * Adds the given task to be invoked at a fixed <code>period</code> milliseconds rate after <code>delay</code> milliseconds.
     *
     * @param delay  the delay in milliseconds before the first invocation.
     * @param period the time in milliseconds between the invocations.
     * @param task   the object to invoke.
     *
     * @return an object to stop the invocationss.
     */
    Control invoke(long delay, long period, Runnable task);

    /**
     * Adds the given task to be invoked after <code>delay</code> milliseconds.
     *
     * @param delay the delay in milliseconds before the first invocation.
     * @param task  the object to invoke.
     *
     * @return an object to cancel the invocation.
     */
    Control invoke(long delay, Runnable task);

    /**
     * Controls an invocation scheduled by {@link Scheduler#invoke(long, Runnable)} or {@link Scheduler#invoke(long, long, Runnable)}.
     */
    interface Control {

        /**
         * Cancels any subsequent scheduled invocation of the task this control corresponds to.
         *
         * @return <code>true</code> if the call prevented one or more scheduled executions from taking place;
         *         <code>false</code> otherwise.
         */
        boolean cancel();
    }
}
