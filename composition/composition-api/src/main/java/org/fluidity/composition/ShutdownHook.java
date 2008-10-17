/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package org.fluidity.composition;

/**
 * Enables tasks to be hooked to the application shutdown event. This is used by component bootstrap implementations to
 * provide means to shut down components when the application shuts down. For instance, a command line application may
 * want to use the runtime system to do this while a web application would use a context listener for the same purpose.
 *
 * @author Tibor Varga
 * @version $Revision$
 */
public interface ShutdownHook {

    /**
     * Adds a task to be run when the application is shut down. Concrete implementations are needed for the various
     * application types, i.e. command line, web, etc.
     *
     * @param threadName is the name of the thread to add to the shutdown hook when a thread is required.
     * @param command    is the command to run prior application shutdown.
     */
    void addTask(String threadName, Runnable command);
}
