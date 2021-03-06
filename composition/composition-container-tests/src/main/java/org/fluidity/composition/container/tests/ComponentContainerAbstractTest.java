/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.container.tests;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ContainerServicesFactory;
import org.fluidity.composition.MutableContainer;
import org.fluidity.composition.container.ContainerServices;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.foundation.ServiceProviders;
import org.fluidity.testing.Simulator;

import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 * Abstract test for container implementations. The custom implementation must implement the {@link #newContainer(ContainerServices)} method and create a new
 * container when invoked.
 *
 * @author Tibor Varga
 */
public abstract class ComponentContainerAbstractTest extends Simulator {

    /**
     * Creates a new container. This method is called for each test case.
     *
     * @param services service components for the container.
     *
     * @return a new container to be tested.
     */
    protected abstract MutableContainer newContainer(final ContainerServices services);

    @Factory
    public Object[] tests() {
        final ContainerServicesFactory factory = ServiceProviders.findInstance(ContainerServicesFactory.class, getClass().getClassLoader());
        assert factory != null;

        final ContainerServices services = factory.containerServices(new NoLogFactory());

        final ArtifactFactory containers = new ArtifactFactory() {
            public MutableContainer createContainer() {
                return newContainer(services);
            }

            public ComponentContext createContext(final Map<Class<? extends Annotation>, Annotation[]> map) {
                return services.emptyContext().create(map);
            }
        };

        return new Object[] {
                new ConstructorDiscoveryTests(containers),
                new BasicResolutionTests(containers),
                new OptionalDependencyTests(containers),
                new ComponentGroupTests(containers),
                new FieldInjectionTests(containers),
                new MethodInjectionTests(containers),
                new CircularReferencesTests(containers),
                new ContainerHierarchyTests(containers),
                new CustomFactoryTests(containers),
                new ComponentVariantTests(containers),
                new ComponentContextTests(containers),
                new StatefulComponentTests(containers),
                new DomainComponentTests(containers),
                new TypeParameterTests(containers),
                new ComponentInterceptorTests(containers),
        };
    }

    // IntelliJ IDEA fails to recognize subclasses as test classes without this
    @Test(enabled = false)
    public void ignored() { }
}
