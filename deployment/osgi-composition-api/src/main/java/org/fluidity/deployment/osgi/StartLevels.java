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

package org.fluidity.deployment.osgi;

import java.util.List;

import org.osgi.framework.Bundle;

/**
 * Optional component for a standalone OSGi application to control the initial start level of the OSGi framework and the start level of individual bundles.
 * <h3>Usage</h3>
 * <pre>
 *   {@linkplain org.fluidity.composition.Component @Component}
 *   final class MyStartLevels implements <span class="hl1">StartLevels</span> {
 *      &hellip;
 *   }
 * </pre>
 *
 * @author Tibor Varga
 */
public interface StartLevels {

    /**
     * Returns a list of bundle lists to start at subsequent start levels. The start level begins at <code>2</code> for the first list of bundles in the
     * returned list and increments for each subsequent list of bundles.
     * <p/>
     * Bundles included more than once will be considered only once. The list of bundles not included in the returned lists will be added
     * to the end of the returned list by the caller.
     *
     * @param bundles the list of all bundles; the implementation may modify this parameter without affecting the caller.
     *
     * @return the list of bundles to start at various start levels.
     */
    List<List<Bundle>> bundles(List<Bundle> bundles);

    /**
     * Returns the initial start level of the OSGi framework. Bundles with start level up to the returned value will be started in the order of their start
     * level. Return <code>1</code> to leave all bundles stopped; return <code>maximum</code> to have all bundles started.
     *
     * @param maximum the last start level configured for the framework.
     *
     * @return the initial start level of the OSGi framework; will be constrained into the <code>1..maximum</code> range.
     */
    int initial(int maximum);
}
