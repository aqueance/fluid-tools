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

/**
 * Enables tasks to be executed when the application shuts down. A suitable implementation is used by dependency injection containers to provide means to shut
 * down components when the application shuts down.
 * <p/>
 * Application commands executed at container termination must not rely on any Fluid Tools dependency injection container functionality as containers must be
 * assumed to have shut down already when these commands are executed.
 * <p/>
 * Fluid Tools provides implementation for command line applications, web applications and OSGi bundles. If you use Fluid Tools in an application wrapper not
 * listed here then you might need to provide an implementation of this interface for Fluid Tools to function.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain org.fluidity.composition.Component @Component}
 * final class MyContainerTerminationImpl implements <span class="hl1">ContainerTermination</span> {
 *     ...
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
public interface ContainerTermination {

    /**
     * Adds a command to be execute when the application is shut down. Concrete implementations are provided for the various application types, i.e. command
     * line, web, OSGi bundle, etc.
     *
     * @param command is the command to run prior application shutdown.
     */
    void run(Runnable command);
}
