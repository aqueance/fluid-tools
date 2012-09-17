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

package org.fluidity.foundation;

/**
 * Namespace for the various extensions of the {@link Runnable} concept.
 *
 * @author Tibor Varga
 */
public final class Command extends Utility {

    /**
     * Extends the {@link Runnable} concept to commands that can throw some exception.
     *
     * @author Tibor Varga
     */
    public interface Job<E extends Throwable> {

        /**
         * Executes the command.
         */
        void run() throws E;
    }

    /**
     * Extends the {@link Runnable} concept to commands that can return some value and throw some exception.
     *
     * @author Tibor Varga
     */
    public interface Process<R, E extends Throwable> {

        /**
         * Executes the command.
         */
        R run() throws E;
    }

    /**
     * Extends the {@link Runnable} concept to commands that can take some parameter, return some value, and throw some exception.
     *
     * @author Tibor Varga
     */
    public interface Function<R, P, E extends Throwable> {

        /**
         * Executes the command.
         */
        R run(P parameter) throws E;
    }
}