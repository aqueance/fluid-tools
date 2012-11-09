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

package org.fluidity.tests.osgi;

import org.fluidity.deployment.osgi.BundleComponentContainer;

/**
 * Individual integration tests executed within an OSGi bundle. The implementation is an ordinary TestNG unit test with appropriate annotations, etc.
 *
 * @author Tibor Varga
 */
public interface BundleTest extends BundleComponentContainer.Registration { }