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

package org.fluidity.maven;

import java.util.List;

import org.fluidity.composition.ContainerBoundary;
import org.fluidity.foundation.ServiceProviders;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ComponentRegistrationTest {

    private final ContainerBoundary container = new ContainerBoundary();

    @Test
    public void testComponents() throws Exception {
        component(SimpleComponent.class, SimpleComponentImpl.class);
        component(SuperComponent.class, InheritedComponentImpl.class);
        component(ComponentInterface1.class, MultipleInterfacesComponent.class);
        component(ComponentInterface2.class, SingleInterfaceComponent.class);
        component(DefaultComponent.class, PrimaryComponentImpl.class);
        component(FallbackComponent.class, FallbackComponentImpl.class);

        assert container.getComponent(DefaultComponentImpl.class) == null;
        assert container.getComponent(ManualComponent.class) == null;
    }

    @Test
    public void testServiceProviders() throws Exception {
        serviceConsumer(SimpleServiceConsumer.class);
        serviceConsumer(MultipleServiceConsumer.class);
        serviceConsumer(MultipleServicesConsumer.class);
    }

    @Test
    public void testJdkServiceProviders() throws Exception {
        jdkProvider(JdkServiceProvider.class, 3);
        jdkProvider(UnwittingJdkServiceProvider.class, 1);
    }

    @Test
    public void testInnerAndLocalClasses() throws Exception {
        final OuterClass.InnerClass inner = container.getComponent(OuterClass.InnerClass.class);
        assert inner != null : String.format("Inner class %s not instantiated", OuterClass.InnerClass.class);
        assert inner.getLocal() != null : String.format("Local class not instantiated");
    }

    private void jdkProvider(final Class<?> providerInterface, final int count) {
        final List<?> instances = ServiceProviders.findInstances(providerInterface, getClass().getClassLoader());
        assert instances.size() == count : instances.size();
        for (final Object instance : instances) {
            assert instance != null : providerInterface;
            assert providerInterface.isAssignableFrom(instance.getClass()) : providerInterface;
        }
    }

    private <T> void component(final Class<T> componentInterface, final Class<? extends T> componentClass) {
        final T component = container.getComponent(componentInterface);
        assert component != null : componentInterface;
        assert componentClass.isAssignableFrom(component.getClass()) : componentClass;
    }

    private <T> void serviceConsumer(final Class<T> consumerClass) {
        final T consumer = container.getComponent(consumerClass);
        assert consumer != null : consumerClass;
    }
}
