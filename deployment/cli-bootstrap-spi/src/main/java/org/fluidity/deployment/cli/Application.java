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

package org.fluidity.deployment.cli;

/**
 * Command line application root object. Command line applications may provide an implementation of this interface to have a main application loop.
 * <p/>
 * The application exits when the call to the {@link Runnable#run()} method returns, unless the developer has started and failed to stop non-daemon threads.
 */
public interface Application {

    void run(String[] args);
}
