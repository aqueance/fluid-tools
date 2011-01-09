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

package org.fluidity.composition.spi;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.fluidity.composition.ContainerServices;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.composition.ServiceProvider;

/**
 * Provides actual dependency injection container instances and related functionality. The implementing class must be public so as to be able to find using the
 * Service Provider discovery mechanism described in the JAR file specification.
 *
 * @author Tibor Varga
 */
@ServiceProvider(jdk = true)
public interface ContainerProvider {

    /**
     * Creates and returns and empty standalone dependency injection container.
     *
     * @param services provides service components for the container.
     *
     * @return and empty standalone dependency injection container.
     */
    OpenComponentContainer newContainer(ContainerServices services);

    /**
     * Instantiates all {@link org.fluidity.composition.spi.PackageBindings} classes in the given assemblySet and returns the instances in instantiation order.
     *
     * @param services    provides service components for containers, in case needed.
     * @param properties  is to be made available to any {@link org.fluidity.composition.spi.PackageBindings} object that may depend on it
     * @param assemblySet the collection of classes to instantiate. Some may depend on others in the set.
     *
     * @return the list of {@link org.fluidity.composition.spi.PackageBindings} instances in instantiation order.
     */
    List<PackageBindings> instantiateBindings(ContainerServices services, Map properties, Collection<Class<PackageBindings>> assemblySet);
}
