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

package org.fluidity.composition.spi;

/**
 * Enables tasks to be executed when the application shuts down. A suitable implementation is used by dependency injection containers to provide means to shut
 * down components when the application shuts down.
 *
 * @author Tibor Varga
 */
public interface ShutdownTasks {

    /**
     * Adds a task to be run when the application is shut down. Concrete implementations are needed for the various application types, i.e. command line, web,
     * etc.
     *
     * @param name    is the name of the thread to add to the shutdown hook, in case a thread is required.
     * @param command is the command to run prior application shutdown.
     */
    void add(String name, Runnable command);
}
