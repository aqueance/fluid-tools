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

import org.fluidity.deployment.DeploymentControl;

/**
 * The main class of a command line application that parses command line arguments and keeps the application's main thread running until stopped. The
 * implementation can access the command line arguments by having a constructor with, among other dependencies, a {@link
 * org.fluidity.deployment.LaunchArguments} parameter.
 * <p/>
 * An application loop can be provided in an {@link Application} implementation, which when done invokes {@link org.fluidity.deployment.RuntimeControl#stop()}.
 * The application loop is optional and when not present, the application can be stopped using Ctrl-C.
 * <p/>
 * The application exits when the call to the {@link Runnable#run()} method returns, unless the developer has started and failed to stop non-daemon threads.
 *
 * @author Tibor Varga
 */
public interface MainLoop extends Runnable, DeploymentControl {

}
