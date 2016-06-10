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

package org.fluidity.composition.spi;

import org.fluidity.foundation.Command;

/**
 * Enables jobs to be executed when the application shuts down. The implementation is used by dependency injection containers to provide means to
 * notifies components about application termination. Concrete implementations are provided for the various application types, i.e. command line,
 * web, OSGi bundle, etc.
 * <p>
 * Application jobs executed at container termination must not rely on any Fluid Tools dependency injection container functionality as containers must be
 * assumed to have shut down already when these jobs are executed.
 * <p>
 * Fluid Tools provides implementation for command line applications, web applications and OSGi bundles. If you use Fluid Tools in an application wrapper not
 * listed here then you might need to provide an implementation of this interface for Fluid Tools to function.
 * <p>
 * Fluid Tools provides a {@link ContainerTermination.Jobs} component, which the custom {@link ContainerTermination} implementation must simply delegate its
 * method calls to. The only business logic in the latter is the call to {@link ContainerTermination.Jobs#flush()}. In a command line application, for
 * instance, that call is triggered by the JVM shutdown.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain org.fluidity.composition.Component @Component}
 * final class <span class="hl2">MyContainerTerminationImpl</span> implements <span class="hl1">ContainerTermination</span> {
 *
 *   private final ContainerTermination.Jobs jobs;
 *
 *   <span class="hl2">MyContainerTerminationImpl</span>(final <span class="hl1">{@linkplain ContainerTermination.Jobs}</span>&lt;<span class="hl2">MyContainerTerminationImpl</span>&gt; jobs) {
 *     this.jobs = jobs;
 *     &hellip;
 *   }
 *
 *
 *   public void add(final {@linkplain org.fluidity.foundation.Command.Job}&lt;{@linkplain Exception}&gt; job) {
 *     jobs.{@linkplain ContainerTermination.Jobs#add(org.fluidity.foundation.Command.Job) add}(job);
 *   }
 *
 *   public void remove(final {@linkplain org.fluidity.foundation.Command.Job}&lt;{@linkplain Exception}&gt; job) {
 *     jobs.{@linkplain ContainerTermination.Jobs#remove(org.fluidity.foundation.Command.Job) remove}(job);
 *   }
 *
 *   &hellip;
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
public interface ContainerTermination {

    /**
     * Adds a job to be execute when the application is shut down.
     *
     * @param job the job to run prior application shutdown.
     */
    void add(Command.Job<Exception> job);

    /**
     * Undoes a previous call to {@link #add(Command.Job)} with the same job.
     *
     * @param job the job to run prior application shutdown.
     */
    void remove(Command.Job<Exception> job);

    /**
     * Maintains the list of jobs on behalf of a {@link ContainerTermination} component. This component is implemented by Fluid Tools and the {@code
     * ContainerTermination} implementation is expected to use it.
     * <p>
     * The type parameter is used to gain access to the {@code ContainerTermination} component using this component in order to verify that no {@linkplain
     * org.fluidity.foundation.Command.Job job} {@linkplain #add(org.fluidity.foundation.Command.Job) added} to this component is loaded by a class loader less
     * stable than that of the {@code ContainerTermination} component; i.e., the class loader of all jobs added must be the same as, or in the ancestry of, the
     * class loader of the {@code ContainerTermination} component.
     * <p>
     * <b>NOTE</b>: Do not use this component, only <em>implement</em> it when integrating a new termination mechanism to Fluid Tools; use {@link
     * ContainerTermination} instead.
     *
     * @param <T> identifies the dependent component's class a particular instance belongs to; used to match class loaders.
     *
     * @author Tibor Varga
     */
    @SuppressWarnings("UnusedDeclaration")
    interface Jobs<T> {

        /**
         * Executes the jobs in reverse addition order. The executed jobs are then removed from the list.
         */
        void flush();

        /**
         * Adds a job to the list.
         *
         * @param job the job to add.
         */
        void add(Command.Job<Exception> job);

        /**
         * Removes a job from the list.
         *
         * @param job the job to remove.
         */
        void remove(Command.Job<Exception> job);
    }
}
