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

package org.fluidity.deployment;

/**
 * Provides access to the raw command line arguments. There may not always be an implementation so you may want to make your dependency on this interface {@link
 * org.fluidity.composition.Optional}.
 *
 * The reasons for this interface to exist is to give a type to the launch argument so that components requiring access won't get just any stale string array
 * that happens to be in the container but the actual arguments that was given when the application was launched.
 *
 * @author Tibor Varga
 */
public interface LaunchArguments {

    /**
     * Used to assist passing the launch arguments to the implementation.
     */
    Object ARGUMENTS_KEY = new Object();

    /**
     * Returns the list of arguments received from the command line. You can modify the array returned by this method as it is not linked to the array returned
     * by the same method some time later.
     *
     * @return the list of arguments received from the command line.
     */
    String[] arguments();
}
