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
import org.fluidity.composition.Containers;
import org.fluidity.composition.Inject;
import org.fluidity.composition.Optional;
import org.fluidity.foundation.ServiceProviders;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ComponentRegistrationTest {

    private final ContainerBoundary container = Containers.prepare();

    @Test
    public void testComponents() throws Exception {
        class References {
            @Inject public @Optional SimpleComponent simpleComponent;
            @Inject public @Optional SuperComponent superComponent;
            @Inject public @Optional ComponentInterface1 componentInterface1;
            @Inject public @Optional ComponentInterface2 componentInterface2;
            @Inject public @Optional DefaultComponent defaultComponent;
            @Inject public @Optional FallbackComponent fallbackComponent;
            @Inject public @Optional ManualComponent manualComponent;
        }

        final References references = container.initialize(new References());

        component(references.simpleComponent, SimpleComponent.class, SimpleComponentImpl.class);
        component(references.superComponent, SuperComponent.class, InheritedComponentImpl.class);
        component(references.componentInterface1, ComponentInterface1.class, MultipleInterfacesComponent.class);
        component(references.componentInterface2, ComponentInterface2.class, SingleInterfaceComponent.class);
        component(references.defaultComponent, DefaultComponent.class, PrimaryComponentImpl.class);
        component(references.fallbackComponent, FallbackComponent.class, FallbackComponentImpl.class);
        component(references.manualComponent, ManualComponent.class, null);
    }

    @Test
    public void testServiceProviders() throws Exception {
        class References {
            @Inject public @Optional SimpleServiceConsumer simpleServiceConsumer;
            @Inject public @Optional MultipleServiceConsumer multipleServiceConsumer;
            @Inject public @Optional MultipleServicesConsumer multipleServicesConsumer;
        }

        final References references = container.initialize(new References());

        serviceConsumer(references.simpleServiceConsumer, SimpleServiceConsumer.class);
        serviceConsumer(references.multipleServiceConsumer, MultipleServiceConsumer.class);
        serviceConsumer(references.multipleServicesConsumer, MultipleServicesConsumer.class);
    }

    @Test
    public void testJdkServiceProviders() throws Exception {
        jdkProvider(JdkServiceProvider.class, 3);
        jdkProvider(UnwittingJdkServiceProvider.class, 1);
    }

    @Test
    public void testInnerAndLocalClasses() throws Exception {
        class Reference {
            public @Inject @Optional OuterClass.InnerClass component;
        }

        final OuterClass.InnerClass inner = container.initialize(new Reference()).component;
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

    private <T> void component(final T component, final Class<T> componentInterface, final Class<? extends T> componentClass) {
        assert componentClass == null || component != null : componentInterface;
        assert componentClass == null ? component == null : componentClass == component.getClass() : componentClass;
    }

    private <T> void serviceConsumer(final T consumer, final Class<T> consumerClass) {
        assert consumer != null : consumerClass;
    }
}
