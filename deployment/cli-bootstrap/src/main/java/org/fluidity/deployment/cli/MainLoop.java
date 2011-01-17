/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.deployment.cli;

import org.fluidity.deployment.DeploymentControl;

/**
 * The main class of the command line application that parses parameters and keeps the application's main thread running until stopped. The implementation can
 * access the command line parameters by having a constructor with, among other dependencies, a {@link org.fluidity.deployment.LaunchArguments} parameter.
 * <p/>
 * The application loop is expected in a {@link Application}, which when done invokes {@link org.fluidity.deployment.RuntimeControl#stop()}. The application
 * loop is optional and when not present, the application can be stopped using Ctrl-C.
 * <p/>
 * The application exits when the call to the {@link Runnable#run()} method returns, unless the developer has started but failed to stop non-daemon threads.
 *
 * @author Tibor Varga
 */
public interface MainLoop extends Runnable, DeploymentControl {

}
