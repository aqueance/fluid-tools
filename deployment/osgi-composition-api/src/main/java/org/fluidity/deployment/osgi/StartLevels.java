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
 *
 *      ...
 *   }
 * </pre>
 *
 * @author Tibor Varga
 */
public interface StartLevels {

    /**
     * Returns the list of bundles to start at the given level. This method is called for each level starting from <code>2</code> progressively until the
     * returned list contains no more of the remaining bundles.
     *
     * @param level   the start level to return the list of bundles to start at.
     * @param bundles the list of bundles not yet assigned a start level.
     *
     * @return the list of bundles to start at the given start level.
     */
    List<Bundle> bundles(int level, List<Bundle> bundles);

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
