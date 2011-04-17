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

package org.fluidity.composition;

import org.fluidity.foundation.logging.NoLogFactory;
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
                return newContainer(new ProductionServices(new NoLogFactory(), null));
            }
        };

        return new Object[] {
                new ConstructorDiscoveryTests(containers),
                new BasicResolutionTests(containers),
                new OptionalDependencyTests(containers),
                new ComponentGroupTests(containers),
                new FieldInjectionTests(containers),
                new CircularReferencesTests(containers),
                new ContainerHierarchyTests(containers),
                new ComponentFactoryTests(containers),
                new ComponentVariantTests(containers),
                new ComponentContextTests(containers),
                new StatefulComponentTests(containers),
        };
    }

    // IDEA fails to recognize subclasses as test classes without this
    @Test(enabled = false)
    public void ignored() {}
}
