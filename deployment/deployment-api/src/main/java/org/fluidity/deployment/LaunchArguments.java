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
     * Assist in passing the launch arguments to the implementation.
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
