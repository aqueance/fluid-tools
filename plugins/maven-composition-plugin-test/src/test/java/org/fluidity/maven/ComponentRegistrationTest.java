/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.maven;

import org.fluidity.composition.ComponentContainerAccess;

import org.testng.annotations.Test;

public class ComponentRegistrationTest {

    private final ComponentContainerAccess container = new ComponentContainerAccess();

    @Test
    public void testComponents() throws Exception {
        component(SimpleComponent.class, SimpleComponentImpl.class);
        component(SuperComponent.class, InheritedComponentImpl.class);
        component(ComponentInterface1.class, MultipleInterfacesComponent.class);
        component(ComponentInterface2.class, MultipleInterfacesComponent.class);
        component(DefaultComponent.class, PriorityComponentImpl.class);
        component(DefaultComponentImpl.class, DefaultComponentImpl.class);

        assert container.getComponent(ManualComponent.class) == null;
    }

    @Test
    public void testServiceProviders() throws Exception {
        serviceConsumer(SimpleServiceConsumer.class);
        serviceConsumer(MultipleServiceConsumer.class);
        serviceConsumer(MultipleServicesConsumer.class);
    }

    private <T> void component(final Class<T> componentInterface, final Class<? extends T> componentClass) {
        final T simpleComponent = container.getComponent(componentInterface);
        assert simpleComponent != null : componentInterface;
        assert componentClass.isAssignableFrom(simpleComponent.getClass()) : componentClass;
    }

    private <T> void serviceConsumer(final Class<T> consumerClass) {
        final T consumer = container.getComponent(consumerClass);
        assert consumer != null : consumerClass;
    }
}
