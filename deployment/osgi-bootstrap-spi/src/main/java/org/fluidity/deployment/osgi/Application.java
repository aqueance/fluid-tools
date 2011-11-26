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

package org.fluidity.deployment.osgi;

import org.osgi.framework.BundleActivator;

/**
 * OSGi application root object. This is one of the two ways to implement an OSGi application using Fluid Tools. One way, this, is to use the Fluid Tools
 * implementation of the {@link org.osgi.framework.BundleActivator} interface, which in turn uses the implementation of this interface, if exists, to notify
 * your application of the bundle's start and stop events. The other way is for the application to provide an implementation of the {@link
 * org.osgi.framework.BundleActivator} interface. In either case, bootstrapping the container is then the responsibility of the {@link
 * org.osgi.framework.BundleActivator} implementation.
 */
public interface Application extends BundleActivator { }
