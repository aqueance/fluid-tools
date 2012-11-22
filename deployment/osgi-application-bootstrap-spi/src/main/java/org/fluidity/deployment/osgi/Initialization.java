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

import org.fluidity.composition.ComponentGroup;

import org.osgi.framework.Bundle;
import org.osgi.framework.launch.Framework;

/**
 * Optional components for a standalone OSGi application to initialize the OSGi framework after it has been started but before any of the bundles have. The
 * receiver can set start levels, initialize conditional permissions, install or remove bundles, etc.
 * <p/>
 * If the receiver stops the <code>framework</code>, the application will terminate.
 * <h3>Usage</h3>
 * <pre>
 * {@linkplain org.fluidity.composition.Component @Component}
 * final class BundleStartLevels implements <span class="hl1">Initialization</span> {
 *
 *   public void <span class="hl1">initialize</span>({@linkplain Framework} framework, final {@linkplain Bundle}... bundles) throws {@linkplain Exception} {
 *     &hellip;
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
@ComponentGroup
public interface Initialization {

    /**
     * Initializes the OSGi framework before any of the installed bundles are started.
     *
     * @param framework the OSGi framework to initialize.
     * @param bundles   the installed OSGi bundles; this list will <code>not</code> include the system bundle.
     *
     * @throws Exception thrown to terminate the initialization and the application and print an exception stack trace to the console.
     */
    void initialize(Framework framework, Bundle... bundles) throws Exception;
}
