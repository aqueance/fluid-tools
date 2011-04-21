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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ContextDefinition;
import org.fluidity.composition.spi.PlatformContainer;
import org.fluidity.foundation.logging.NoLogFactory;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ServiceContainerTest extends MockGroupAbstractTest {

    private final BundleContext bundle = addControl(BundleContext.class);
    private final ContextDefinition definition = addControl(ContextDefinition.class);
    private final ComponentContext context = addControl(ComponentContext.class);
    private final PlatformContainer container = new ServiceContainer(bundle);

    private final ServiceReference service1 = addControl(ServiceReference.class);
    private final ServiceReference service2 = addControl(ServiceReference.class);
    private final ServiceReference service3 = addControl(ServiceReference.class);
    private final ComponentApi component1 = addControl(ComponentApi.class);
    private final ComponentApi component2 = addControl(ComponentApi.class);
    private final ComponentApi component3 = addControl(ComponentApi.class);

    private final Service selector1 = ContextSites.class.getDeclaredField("field1").getAnnotation(Service.class);
    private final Service selector2 = ContextSites.class.getDeclaredField("field2").getAnnotation(Service.class);

    public ServiceContainerTest() throws Exception {
        final Field field = container.getClass().getDeclaredField("log");
        field.setAccessible(true);
        field.set(container, NoLogFactory.sink);
    }

    @Test
    public void testFailedComponentLookup() throws Exception {
        expectContext();

        replay();
        assert !container.containsComponent(ComponentApi.class, definition);
        verify();

        EasyMock.expect(bundle.getServiceReference(ComponentApi.class.getName())).andReturn(null);
        expectContext(selector1);

        replay();
        assert container.getComponent(ComponentApi.class, definition) == null;
        verify();
    }

    @Test
    public void testSuccessfulComponentLookup() throws Exception {
        expectContext(selector1);

        replay();
        assert container.containsComponent(ComponentApi.class, definition);
        verify();

        EasyMock.expect(bundle.getServiceReference(ComponentApi.class.getName())).andReturn(service1);
        EasyMock.expect(bundle.getService(service1)).andReturn(component1);
        expectContext(selector1);

        replay();
        assert container.getComponent(ComponentApi.class, definition) == component1;
        verify();
    }

    @Test
    public void testFailedGroupLookup() throws Exception {
        expectContext();

        replay();
        assert !container.containsComponentGroup(ComponentApi.class, definition);
        verify();

        EasyMock.expect(bundle.getServiceReferences(ComponentApi.class.getName(), selector1.filter())).andReturn(null);
        expectContext(selector1);

        replay();
        assert container.getComponentGroup(ComponentApi.class, definition) == null;
        verify();
    }

    @Test
    public void testSuccessfulGroupLookup() throws Exception {
        final ServiceReference[] references = { service1, service2, service3 };
        expectContext(selector1);

        replay();
        assert container.containsComponentGroup(ComponentApi.class, definition);
        verify();

        EasyMock.expect(bundle.getServiceReferences(ComponentApi.class.getName(), selector2.filter())).andReturn(references);
        expectContext(selector1, selector2);
        EasyMock.expect(bundle.getService(service1)).andReturn(component1);
        EasyMock.expect(bundle.getService(service2)).andReturn(component2);
        EasyMock.expect(bundle.getService(service3)).andReturn(component3);

        replay();
        assert Arrays.equals(container.getComponentGroup(ComponentApi.class, definition), new Object[] { component1, component2, component3 });
        verify();
    }

    private void expectContext(final Service... selectors) {
        EasyMock.expect(definition.reduce(EasyMock.<Set<Class<? extends Annotation>>>notNull())).andReturn(definition);
        EasyMock.expect(definition.create()).andReturn(context);
        EasyMock.expect(context.annotations(Service.class)).andReturn(selectors);
    }

    @SuppressWarnings("unchecked")
    private Map<Class<? extends Annotation>, Annotation[]> selectors(final Annotation... selectors) {
        final Map<Class<? extends Annotation>, Annotation[]> map = new HashMap<Class<? extends Annotation>, Annotation[]>();
        map.put(Service.class, selectors);
        return map;
    }

    private static interface ComponentApi { }

    @SuppressWarnings("UnusedDeclaration")
    private static class ContextSites {

        @Service(api = Object.class, filter = "selector1")
        private static final String field1 = null;
        @Service(api = Object.class, filter = "selector2")
        private static final String field2 = null;
    }
}
