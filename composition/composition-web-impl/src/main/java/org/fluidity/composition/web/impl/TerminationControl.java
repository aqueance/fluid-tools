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

package org.fluidity.composition.web.impl;

/**
 * Tells whether a {@link org.fluidity.composition.web.ContextLifeCycleListener ContextLifeCycleListener} has been registered or not. This allows {@link
 * WebApplicationTermination} to fail as early as possible if servlet context life-cycle notifications will trigger the dependency injection container shutdown
 * process.
 *
 * @author Tibor Varga
 */
public interface TerminationControl {

    /**
     * Returns whether a {@link org.fluidity.composition.web.ContextLifeCycleListener} has been registered or not.
     *
     * @return <code>true</code> if the listener has been registered, <code>false</code> if it has not.
     */
    boolean present();
}
