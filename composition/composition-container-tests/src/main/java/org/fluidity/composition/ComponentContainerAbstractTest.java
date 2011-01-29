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

package org.fluidity.composition;

import org.fluidity.foundation.NoLogFactory;
import org.fluidity.tests.MockGroupAbstractTest;

import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 * Abstract test for container implementations.
 *
 * @author Tibor Varga
 */
public abstract class ComponentContainerAbstractTest extends MockGroupAbstractTest {

    /**
     * Creates a new container. This method is called for each test case.
     *
     * @param services service components for the container.
     *
     * @return a new container to be tested.
     */
    protected abstract OpenComponentContainer newContainer(final ContainerServices services);

    @Factory
    public Object[] tests() {
        final ContainerFactory containers = new ContainerFactory() {
            public OpenComponentContainer createContainer() {
                return newContainer(new ProductionServices(new NoLogFactory()));
            }
        };

        return new Object[] {
                new ConstructorDiscoveryTests(containers),
                new BasicResolutionTests(containers),
                new OptionalDependencyTests(containers),
                new ServiceProvidersDependencyTests(containers),
                new FieldInjectionTests(containers),
                new CircularReferencesTests(containers),
                new ContainerHierarchyTests(containers),
                new ComponentFactoryTests(containers),
                new ComponentVariantTests(containers),
                new ComponentContextTests(containers),
        };
    }

    // IDEA fails to recognize subclasses as test classes without this
    @Test(enabled = false)
    public void ignored() {}
}
