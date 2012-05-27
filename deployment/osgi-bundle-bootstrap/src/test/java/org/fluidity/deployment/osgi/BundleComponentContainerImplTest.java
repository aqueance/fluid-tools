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

package org.fluidity.deployment.osgi;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.foundation.ClassDiscovery;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public class BundleComponentContainerImplTest extends MockGroupAbstractTest {

    private final ComponentContainer root = new ContainerBoundary();

    private final Bundle bundle = mock(Bundle.class);
    private final BundleContext context = mock(BundleContext.class);
    private final ClassDiscovery discovery = mock(ClassDiscovery.class);

    private final ServiceInterface1 service1 = mock(ServiceInterface1.class);
    private final ServiceInterface2 service2 = mock(ServiceInterface2.class);
    private final BundleComponentContainer.Managed item = mock(BundleComponentContainer.Managed.class);

    private final ServiceRegistration registration = mock(ServiceRegistration.class);

    private final ServiceReference reference1 = mock(ServiceReference.class);
    private final ServiceReference reference2 = mock(ServiceReference.class);

    private final Consumer consumer1 = mock(Consumer.class);
    private final Consumer consumer2 = mock(Consumer.class);

    private final BundleComponentContainer.Managed component1 = mock(BundleComponentContainer.Managed.class);
    private final BundleComponentContainer.Managed component2 = mock(BundleComponentContainer.Managed.class);
    private final BundleComponentContainer.Managed component3 = mock(BundleComponentContainer.Managed.class);
    private final BundleComponentContainer.Managed component4 = mock(BundleComponentContainer.Managed.class);
    private final BundleComponentContainer.Managed component5 = mock(BundleComponentContainer.Managed.class);
    private final BundleComponentContainer.Managed component6 = mock(BundleComponentContainer.Managed.class);

    private final ServiceInterface1 service3 = mock(ServiceInterface1.class);
    private final ServiceReference reference3 = mock(ServiceReference.class);

    private ComponentContainer container;

    private final BundleComponentContainer.Registration.Listener<Consumer> source = mock(BundleComponentContainer.Registration.Listener.class);
    private Log<BundleComponentContainerImpl> log = NoLogFactory.consume(BundleComponentContainerImpl.class);

    @BeforeMethod
    public void setup() throws Exception {
        container = root.makeChildContainer();

        Service1.delegate = service1;
        Service2.delegate = service1;
        ServiceDependent1.delegate = item;
        ServiceDependent2.delegate = item;
        Source.delegate = source;
        Component1Service12.delegate = component1;
        Component2Service2.delegate = component2;
        Component3Service1.delegate = component3;
        Component4Service1.delegate = component4;
        Component5Service2.delegate = component5;
    }

    @Test
    @SuppressWarnings({ "unchecked", "RedundantCast" })
    public void testServiceRegistration() throws Exception {
        final Class<Service1> componentClass = Service1.class;
        final Class<ServiceInterface1> serviceInterface = ServiceInterface1.class;

        final BundleComponentContainer services = discover(StatusCheck.class, componentClass);

        final Properties properties = new Properties();

        properties.setProperty("property-1", "value-1");
        properties.setProperty("property-2", "value-2");

        EasyMock.expect(Service1.delegate.properties()).andReturn(properties);
        EasyMock.expect(Service1.delegate.types()).andReturn(new Class[] { serviceInterface });

        Service1.delegate.start();

        // registering the service
        EasyMock.expect(context.registerService(EasyMock.aryEq(new String[] { serviceInterface.getName() }),
                                                EasyMock.<Service1>notNull(),
                                                (Dictionary) EasyMock.same(properties)))
                .andReturn(registration);

        replay();
        services.start();
        verify();

        assertFailed();
        assertActive(StatusCheck.class, componentClass);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        // un-registering the service
        registration.unregister();
        Service1.delegate.stop();

        replay();
        services.stop();
        verify();
    }

    @Test
    public void testServiceListener1() throws Exception {
        final Class<ServiceDependent1> componentClass = ServiceDependent1.class;
        final BundleComponentContainer services = discover(StatusCheck.class, componentClass);

        final Service annotation1 = new ServiceImpl(ServiceInterface1.class);
        final Service annotation2 = new ServiceImpl(ServiceInterface2.class);
        final Map.Entry<Class<?>, Set<Service>> dependencies = dependencies(ServiceDependent1.class, annotation1, annotation2);

        // no services yet
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null);
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null);

        final List<ListenerSpec> listeners = Arrays.asList(expectListenerRegistration(), expectListenerRegistration());

        replay();
        services.start();
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies));

        final ListenerSpec listener1 = find(listeners, ServiceInterface1.class, null);
        final ListenerSpec listener2 = find(listeners, ServiceInterface2.class, null);

        checkFilter(listener1, ServiceInterface1.class, null);
        checkFilter(listener2, ServiceInterface2.class, null);

        // responding to appearance of the first service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 });

        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies), annotation1);

        // responding to appearance of the second service
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 });

        EasyMock.expect(context.getService(reference2)).andReturn(service2);

        ServiceDependent1.delegate.start();

        replay();
        listener2.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        verify();

        assertFailed();
        assertActive(StatusCheck.class, componentClass);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        // responding to disappearance of the first service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null);

        EasyMock.expect(context.ungetService(reference1)).andReturn(false);

        ServiceDependent1.delegate.stop();

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies), annotation2);

        // responding to reappearance of the first service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 });

        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        ServiceDependent1.delegate.start();

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class, componentClass);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        // responding to disappearance of the second service
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null);

        EasyMock.expect(context.ungetService(reference2)).andReturn(false);

        ServiceDependent1.delegate.stop();

        replay();
        listener2.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference2));
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies), annotation1);

        // responding to disappearance of the first service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null);

        EasyMock.expect(context.ungetService(reference1)).andReturn(false);

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies));

        // removing the listeners
        context.removeServiceListener(listener1.listener());
        context.removeServiceListener(listener2.listener());

        replay();
        services.stop();
        verify();
    }

    @Test
    public void testServiceListener2() throws Exception {
        final Class<ServiceDependent1> componentClass = ServiceDependent1.class;
        final BundleComponentContainer services = discover(StatusCheck.class, componentClass);

        final Service annotation1 = new ServiceImpl(ServiceInterface1.class);
        final Service annotation2 = new ServiceImpl(ServiceInterface2.class);
        final Map.Entry<Class<?>, Set<Service>> dependencies = dependencies(ServiceDependent1.class, annotation1, annotation2);

        final List<ListenerSpec> listeners = Arrays.asList(expectListenerRegistration(), expectListenerRegistration());

        // first service already registered
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 });
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null);

        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        replay();
        services.start();
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies), annotation1);

        final ListenerSpec listener1 = find(listeners, ServiceInterface1.class, null);
        final ListenerSpec listener2 = find(listeners, ServiceInterface2.class, null);

        checkFilter(listener1, ServiceInterface1.class, null);
        checkFilter(listener2, ServiceInterface2.class, null);

        // responding to appearance of the second service
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 });

        EasyMock.expect(context.getService(reference2)).andReturn(service2);

        ServiceDependent1.delegate.start();

        replay();
        listener2.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        verify();

        assertFailed();
        assertActive(StatusCheck.class, componentClass);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        // responding to disappearance of the first service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null);

        EasyMock.expect(context.ungetService(reference1)).andReturn(false);

        ServiceDependent1.delegate.stop();

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies), annotation2);

        // responding to reappearance of the first service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 });

        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        ServiceDependent1.delegate.start();

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class, componentClass);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        // removing the listener
        context.removeServiceListener(listener1.listener());
        context.removeServiceListener(listener2.listener());

        ServiceDependent1.delegate.stop();

        replay();
        services.stop();
        verify();
    }

    @Test
    public void testServiceListener3() throws Exception {
        final Class<ServiceDependent2> componentClass = ServiceDependent2.class;

        final String selector1 = "filter-1";
        final String selector2 = "filter-2";

        final BundleComponentContainer services = discover(Arrays.asList(selector1, selector2), StatusCheck.class, componentClass);

        final List<ListenerSpec> listeners = Arrays.asList(expectListenerRegistration(), expectListenerRegistration());

        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), selector1)).andReturn(new ServiceReference[] { reference1 });
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), selector2)).andReturn(new ServiceReference[] { reference2 });

        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        EasyMock.expect(context.getService(reference2)).andReturn(service2);

        ServiceDependent2.delegate.start();

        replay();
        services.start();
        verify();

        assertFailed();
        assertActive(StatusCheck.class, componentClass);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        final ListenerSpec listener1 = find(listeners, ServiceInterface1.class, null);
        final ListenerSpec listener2 = find(listeners, ServiceInterface2.class, null);

        checkFilter(listener1, ServiceInterface1.class, selector1);
        checkFilter(listener2, ServiceInterface2.class, selector2);

        // removing the listeners
        context.removeServiceListener(listener1.listener());
        context.removeServiceListener(listener2.listener());

        ServiceDependent2.delegate.stop();

        replay();
        services.stop();
        verify();
    }

    @Test
    public void testEventSourcesAndConsumers1() throws Exception {
        final Class<Source> componentClass = Source.class;
        final BundleComponentContainer services = discover(StatusCheck.class, componentClass);

        final Source source = new Source();

        // service listener registration
        final ListenerSpec spec = expectListenerRegistration();

        EasyMock.expect(source.clientType()).andReturn(Consumer.class);
        EasyMock.expect(context.getServiceReferences(Consumer.class.getName(), null)).andReturn(new ServiceReference[0]);

        Source.delegate.start();

        replay();
        services.start();
        verify();

        assertFailed();
        assertActive(StatusCheck.class, componentClass);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        assert spec.filter().equals(String.format("(%s=%s)", Constants.OBJECTCLASS, Consumer.class.getName()));

        // response to appearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);
        EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);

        Source.delegate.serviceAdded(EasyMock.same(consumer1), EasyMock.<Properties>eq(new Properties()));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        // response to appearance of second consumer
        EasyMock.expect(context.getService(reference2)).andReturn(consumer2);

        final Properties properties = new Properties();
        properties.setProperty("xxx", "yyy");
        EasyMock.expect(reference2.getPropertyKeys()).andReturn(new String[] { "xxx" });
        EasyMock.expect(reference2.getProperty("xxx")).andReturn(properties.getProperty("xxx"));

        Source.delegate.serviceAdded(EasyMock.same(consumer2), EasyMock.<Properties>eq(properties));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        verify();

        // response to disappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);

        Source.delegate.serviceRemoved(EasyMock.same(consumer1));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        // response to reappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);
        EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);

        Source.delegate.serviceAdded(EasyMock.same(consumer1), EasyMock.eq(new Properties()));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        // response to disappearance of second consumer
        EasyMock.expect(context.getService(reference2)).andReturn(consumer2);

        Source.delegate.serviceRemoved(EasyMock.same(consumer2));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference2));
        verify();

        // response to disappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);

        Source.delegate.serviceRemoved(EasyMock.same(consumer1));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        // removing the event source
        context.removeServiceListener(spec.listener());
        Source.delegate.stop();

        replay();
        services.stop();
        verify();
    }

    @Test
    public void testEventSourcesAndConsumers2() throws Exception {
        final Class<Source> componentClass = Source.class;
        final BundleComponentContainer services = discover(StatusCheck.class, componentClass);

        final Source source = new Source();

        // service listener registration
        final ListenerSpec spec = expectListenerRegistration();

        EasyMock.expect(context.getServiceReferences(Consumer.class.getName(), null)).andReturn(new ServiceReference[] { reference1 });
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);
        EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);

        EasyMock.expect(source.clientType()).andReturn(Consumer.class);
        source.serviceAdded(EasyMock.same(consumer1), EasyMock.<Properties>notNull());

        Source.delegate.start();

        replay();
        services.start();
        verify();

        assertFailed();
        assertActive(StatusCheck.class, componentClass);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        assert spec.filter().equals(String.format("(%s=%s)", Constants.OBJECTCLASS, Consumer.class.getName()));

        // response to appearance of second consumer
        EasyMock.expect(context.getService(reference2)).andReturn(consumer2);
        EasyMock.expect(reference2.getPropertyKeys()).andReturn(new String[0]);

        Source.delegate.serviceAdded(EasyMock.same(consumer2), EasyMock.eq(new Properties()));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        verify();

        // response to disappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);

        Source.delegate.serviceRemoved(EasyMock.same(consumer1));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        // response to reappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);
        EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);

        Source.delegate.serviceAdded(EasyMock.same(consumer1), EasyMock.eq(new Properties()));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        // removing the event source
        context.removeServiceListener(spec.listener());
        Source.delegate.stop();

        replay();
        services.stop();
        verify();
    }

    @Test
    public void testManagedComponents() throws Exception {
        final BundleComponentContainer services = discover(StatusCheck.class,
                                                           Component1Service12.class,
                                                           Component2Service2.class,
                                                           Component3Service1.class,
                                                           Component4Service1.class,
                                                           Component5Service2.class);

        final ServiceImpl annotation1 = new ServiceImpl(ServiceInterface1.class);
        final ServiceImpl annotation2 = new ServiceImpl(ServiceInterface2.class);
        final Map.Entry<Class<?>, Set<Service>> dependencies1 = dependencies(Component1Service12.class, annotation1, annotation2);
        final Map.Entry<Class<?>, Set<Service>> dependencies2 = dependencies(Component2Service2.class, annotation2);
        final Map.Entry<Class<?>, Set<Service>> dependencies3 = dependencies(Component3Service1.class, annotation1);
        final Map.Entry<Class<?>, Set<Service>> dependencies4 = dependencies(Component4Service1.class, annotation1);
        final Map.Entry<Class<?>, Set<Service>> dependencies5 = dependencies(Component5Service2.class, annotation2);

        // one listener per service specification
        final List<ListenerSpec> listeners = Arrays.asList(expectListenerRegistration(), expectListenerRegistration());

        // no services yet
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null);
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null);

        replay();
        services.start();
        verify();

        final ListenerSpec listener1 = find(listeners, ServiceInterface1.class, null);
        final ListenerSpec listener2 = find(listeners, ServiceInterface2.class, null);

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies1, dependencies2, dependencies3, dependencies4, dependencies5));

        // add ServiceInterface1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 });
        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        // start components that require service 1 only

        Component3Service1.delegate.start();
        Component4Service1.delegate.start();

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class, Component3Service1.class, Component4Service1.class);
        assertInactive(collect(dependencies1, dependencies2, dependencies5), annotation1);

        // add ServiceInterface2
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 });
        EasyMock.expect(context.getService(reference2)).andReturn(service2);

        // start components that require service 2 only

        Component5Service2.delegate.start();
        Component2Service2.delegate.start();

        // start components that require both services

        Component1Service12.delegate.start();

        replay();
        listener2.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        verify();

        assertFailed();
        assertActive(StatusCheck.class,
                     Component1Service12.class,
                     Component2Service2.class,
                     Component3Service1.class,
                     Component4Service1.class,
                     Component5Service2.class);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        // remove ServiceInterface1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null);
        EasyMock.expect(context.ungetService(reference1)).andReturn(false);

        Component1Service12.delegate.stop();
        Component3Service1.delegate.stop();
        Component4Service1.delegate.stop();

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class, Component2Service2.class, Component5Service2.class);
        assertInactive(collect(dependencies1, dependencies3, dependencies4), annotation2);

        // add ServiceInterface1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 });
        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        // start components that require service 1 only

        Component3Service1.delegate.start();
        Component4Service1.delegate.start();

        // start components that require both services

        Component1Service12.delegate.start();

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class,
                     Component1Service12.class,
                     Component2Service2.class,
                     Component3Service1.class,
                     Component4Service1.class,
                     Component5Service2.class);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        // remove ServiceInterface2
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null);
        EasyMock.expect(context.ungetService(reference2)).andReturn(false);

        Component1Service12.delegate.stop();
        Component2Service2.delegate.stop();
        Component5Service2.delegate.stop();

        replay();
        listener2.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference2));
        verify();

        assertFailed();
        assertActive(StatusCheck.class, Component3Service1.class, Component4Service1.class);
        assertInactive(collect(dependencies1, dependencies2, dependencies5), annotation1);

        // remove ServiceInterface1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null);
        EasyMock.expect(context.ungetService(reference1)).andReturn(false);

        Component3Service1.delegate.stop();
        Component4Service1.delegate.stop();

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies1, dependencies2, dependencies3, dependencies4, dependencies5));

        // add ServiceInterface2
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 });
        EasyMock.expect(context.getService(reference2)).andReturn(service2);

        // start components that require service 2 only

        Component2Service2.delegate.start();
        Component5Service2.delegate.start();

        replay();
        listener2.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        verify();

        assertFailed();
        assertActive(StatusCheck.class, Component2Service2.class, Component5Service2.class);
        assertInactive(collect(dependencies1, dependencies3, dependencies4), annotation2);

        // stop
        context.removeServiceListener(listener1.listener());
        context.removeServiceListener(listener2.listener());

        Component2Service2.delegate.stop();
        Component5Service2.delegate.stop();

        replay();
        services.stop();
        verify();
    }

    @Test
    public void testFailingComponents() throws Exception {
        FailingComponent.delegate = component6;

        final BundleComponentContainer services = discover(StatusCheck.class, FailingComponent.class, Component4Service1.class);

        final ServiceImpl annotation = new ServiceImpl(ServiceInterface1.class);
        final Map.Entry<Class<?>, Set<Service>> dependencies1 = dependencies(FailingComponent.class, annotation);
        final Map.Entry<Class<?>, Set<Service>> dependencies2 = dependencies(Component4Service1.class, annotation);

        // one listener per service specification
        final ListenerSpec listener = expectListenerRegistration();

        // no services yet
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null);

        replay();
        services.start();
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies1, dependencies2));

        // add ServiceInterface1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 });
        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        // start components that require service 1 only

        Component4Service1.delegate.start();

        replay();
        listener.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        assertFailed(FailingComponent.class);
        assertActive(StatusCheck.class, Component4Service1.class);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        // remove ServiceInterface1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null);
        EasyMock.expect(context.ungetService(reference1)).andReturn(false);

        Component4Service1.delegate.stop();

        replay();
        listener.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies1, dependencies2));

        // add ServiceInterface1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 });
        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        // start components that require service 1 only

        Component4Service1.delegate.start();

        replay();
        listener.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        assertFailed(FailingComponent.class);
        assertActive(StatusCheck.class, Component4Service1.class);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        // stop
        context.removeServiceListener(listener.listener());

        Component4Service1.delegate.stop();

        replay();
        services.stop();
        verify();
    }

    @Test
    public void testFilterDependency() throws Exception {
        MultipleServiceFiltersComponent.delegate = component6;

        final String selector1 = "filter-1";
        final String selector2 = "filter-2";

        final BundleComponentContainer services = discover(Arrays.asList(selector1, selector2), StatusCheck.class, MultipleServiceFiltersComponent.class);

        final ServiceImpl annotation1 = new ServiceImpl(ServiceInterface1.class, selector1);
        final ServiceImpl annotation2 = new ServiceImpl(ServiceInterface1.class, selector2);
        final Map.Entry<Class<?>, Set<Service>> dependencies = dependencies(MultipleServiceFiltersComponent.class, annotation1, annotation2);

        // one listener per service specification
        final List<ListenerSpec> listeners = Arrays.asList(expectListenerRegistration(), expectListenerRegistration());

        // no services yet
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), selector1)).andReturn(null);
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), selector2)).andReturn(null);

        replay();
        services.start();
        verify();

        final ListenerSpec listener1 = find(listeners, ServiceInterface1.class, selector1);
        final ListenerSpec listener2 = find(listeners, ServiceInterface1.class, selector2);

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies));

        // add service with filter 1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), selector1)).andReturn(new ServiceReference[] { reference1 });
        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies), annotation1);

        // add service with filter 2
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), selector2)).andReturn(new ServiceReference[] { reference3 });
        EasyMock.expect(context.getService(reference3)).andReturn(service3);

        // start the component
        MultipleServiceFiltersComponent.delegate.start();

        replay();
        listener2.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference3));
        verify();

        assertFailed();
        assertActive(StatusCheck.class, MultipleServiceFiltersComponent.class);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        // remove service with filter 1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), selector1)).andReturn(null);
        EasyMock.expect(context.ungetService(reference1)).andReturn(false);

        MultipleServiceFiltersComponent.delegate.stop();

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies), annotation2);

        // add service with filter 1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), selector1)).andReturn(new ServiceReference[] { reference1 });
        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        // start the component
        MultipleServiceFiltersComponent.delegate.start();

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class, MultipleServiceFiltersComponent.class);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        // remove service with filter 2
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), selector2)).andReturn(null);
        EasyMock.expect(context.ungetService(reference3)).andReturn(false);

        MultipleServiceFiltersComponent.delegate.stop();

        replay();
        listener2.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference3));
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies), annotation1);

        // remove service with filter 1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), selector1)).andReturn(null);
        EasyMock.expect(context.ungetService(reference1)).andReturn(false);

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies));

        // add service with filter 2
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), selector2)).andReturn(new ServiceReference[] { reference3 });
        EasyMock.expect(context.getService(reference3)).andReturn(service3);

        replay();
        listener2.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference3));
        verify();

        assertFailed();
        assertActive(StatusCheck.class);
        assertInactive(collect(dependencies), annotation2);

        // add service with filter 1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), selector1)).andReturn(new ServiceReference[] { reference1 });
        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        // start the component
        MultipleServiceFiltersComponent.delegate.start();

        replay();
        listener1.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        assertFailed();
        assertActive(StatusCheck.class, MultipleServiceFiltersComponent.class);
        assertInactive(Collections.<Class<?>, Set<Service>>emptyMap());

        // stop
        context.removeServiceListener(listener1.listener());
        context.removeServiceListener(listener2.listener());

        MultipleServiceFiltersComponent.delegate.stop();

        replay();
        services.stop();
        verify();
    }

    private ListenerSpec find(final List<ListenerSpec> listeners, final Class<?> service, final String filter) {
        for (final ListenerSpec listener : listeners) {
            final String query = listener.filter();

            if (query.contains(service.getName()) && (filter == null || query.contains(filter))) {
                return listener;
            }
        }

        assert false : String.format("%s (%s)", service, filter);
        return null;
    }

    private void assertActive(final Class<?>... components) {
        assert StatusCheck.component != null : BundleComponentContainer.Status.class;
        final List<Class<?>> expected = Arrays.asList(components);
        final Collection<Class<?>> actual = StatusCheck.component.active();
        assert new HashSet<Class<?>>(actual).equals(new HashSet<Class<?>>(expected)) : String.format("Expected %s, actual %s", expected, actual);
    }

    private void assertFailed(final Class<?>... components) {
        assert StatusCheck.component != null : BundleComponentContainer.Status.class;
        final List<Class<?>> expected = Arrays.asList(components);
        final Collection<Class<?>> actual = StatusCheck.component.failed();
        assert new HashSet<Class<?>>(actual).equals(new HashSet<Class<?>>(expected)) : String.format("Expected %s, actual %s", expected, actual);
    }

    private Map.Entry<Class<?>, Set<Service>> dependencies(final Class<?> component, final Service... services) {
        return new AbstractMap.SimpleEntry<Class<?>, Set<Service>>(component, new HashSet<Service>(Arrays.asList(services)));
    }

    private Map<Class<?>, Set<Service>> collect(final Map.Entry<Class<?>, Set<Service>>... entries) {
        final Map<Class<?>, Set<Service>> map = new HashMap<Class<?>, Set<Service>>();

        for (final Map.Entry<Class<?>, Set<Service>> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }

    private void assertInactive(final Map<Class<?>, Set<Service>> original, final Service... active) {
        assert StatusCheck.component != null : BundleComponentContainer.Status.class;

        final Map<Class<?>, Set<Service>> expected = new HashMap<Class<?>, Set<Service>>();
        for (final Map.Entry<Class<?>, Set<Service>> entry : original.entrySet()) {
            expected.put(entry.getKey(), new HashSet<Service>(entry.getValue()));
        }

        final List<Service> resolved = Arrays.asList(active);

        for (final Set<Service> dependencies : expected.values()) {
            dependencies.removeAll(resolved);
        }

        final Map<Class<?>, Collection<Service>> actual = StatusCheck.component.inactive();
        assert actual.keySet().equals(expected.keySet()) : String.format("Expected %s, actual %s", expected, actual);
        for (final Map.Entry<Class<?>, Collection<Service>> entry : actual.entrySet()) {
            final Class<?> type = entry.getKey();
            assert new HashSet<Service>(entry.getValue()).equals(expected.get(type)) : String.format("Expected %s, actual %s", expected, actual);
        }
    }

    private BundleComponentContainer discover(final Class... types) throws Exception {
        EasyMock.expect(context.getBundle()).andReturn(bundle);
        EasyMock.expect(bundle.getSymbolicName()).andReturn("test-bundle");
        EasyMock.expect(discovery.findComponentClasses(BundleComponentContainer.Managed.class, BundleComponentContainerImpl.class.getClassLoader(), false)).andReturn(types);
        return new BundleComponentContainerImpl(context, container, log, discovery);
    }

    private BundleComponentContainer discover(final List<String> filters, final Class... types) throws Exception {
        EasyMock.expect(context.getBundle()).andReturn(bundle);
        EasyMock.expect(bundle.getSymbolicName()).andReturn("test-bundle");
        EasyMock.expect(discovery.findComponentClasses(BundleComponentContainer.Managed.class, BundleComponentContainerImpl.class.getClassLoader(), false)).andReturn(types);

        for (final String filter : filters) {
            EasyMock.expect(context.createFilter(filter)).andReturn(null);
        }

        return new BundleComponentContainerImpl(context, container, log, discovery);
    }

    private void checkFilter(final ListenerSpec listener, final Class<?> api, final String filter) {
        final String actual = listener.filter();

        final String expected = filter == null
                                 ? String.format("(%s=%s)", Constants.OBJECTCLASS, api.getName())
                                 : String.format("(&(%s=%s)%s)", Constants.OBJECTCLASS, api.getName(), filter);

        assert expected.equals(actual) : String.format("Expected '%s', actual '%s'", expected, actual);
    }

    private ListenerSpec expectListenerRegistration() throws Exception {
        final ListenerSpecImpl spec = new ListenerSpecImpl();

        context.addServiceListener(EasyMock.<ServiceListener>notNull(), EasyMock.<String>notNull());
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                spec.listener = (ServiceListener) EasyMock.getCurrentArguments()[0];
                spec.filter = (String) EasyMock.getCurrentArguments()[1];
                return null;
            }
        });

        return spec;
    }

    private interface ListenerSpec {

        ServiceListener listener();

        String filter();
    }

    private static class ListenerSpecImpl implements ListenerSpec {
        public ServiceListener listener;
        public String filter;

        public ServiceListener listener() {
            return listener;
        }

        public String filter() {
            return filter;
        }
    }

    public interface ServiceInterface1 extends BundleComponentContainer.Registration { }

    public interface ServiceInterface2 extends BundleComponentContainer.Registration { }

    public static final class Service1 implements ServiceInterface1 {

        private static ServiceInterface1 delegate;

        public Class[] types() {
            return delegate.types();
        }

        public Properties properties() {
            return delegate.properties();
        }

        public void start() throws Exception {
            delegate.start();
        }

        public void stop() throws Exception {
            delegate.stop();
        }
    }

    public static final class Service2 implements ServiceInterface1 {

        private static ServiceInterface1 delegate;

        public Class[] types() {
            return delegate.types();
        }

        public Properties properties() {
            return delegate.properties();
        }

        public void start() throws Exception {
            delegate.start();
        }

        public void stop() throws Exception {
            delegate.stop();
        }
    }

    public static final class ServiceDependent1 implements BundleComponentContainer.Managed {

        private static BundleComponentContainer.Managed delegate;

        public ServiceDependent1(final @Service ServiceInterface1 service1, final @Service ServiceInterface2 service2) {
            assert service1 != null;
            assert service2 != null;
        }

        public void start() throws Exception {
            delegate.start();
        }

        public void stop() throws Exception {
            delegate.stop();
        }
    }

    public static final class ServiceDependent2 implements BundleComponentContainer.Managed {

        private static BundleComponentContainer.Managed delegate;

        public ServiceDependent2(final @Service(filter = "filter-1") ServiceInterface1 service1, final @Service(filter = "filter-2") ServiceInterface2 service2) {
            assert service1 != null;
            assert service2 != null;
        }

        public void start() throws Exception {
            delegate.start();
        }

        public void stop() throws Exception {
            delegate.stop();
        }
    }

    public interface Consumer extends BundleComponentContainer.Managed { }

    public static class Source implements BundleComponentContainer.Registration.Listener<Consumer> {

        private static BundleComponentContainer.Registration.Listener<Consumer> delegate;

        public Class<Consumer> clientType() {
            return delegate.clientType();
        }

        public void serviceAdded(final Consumer component, final Properties properties) {
            delegate.serviceAdded(component, properties);
        }

        public void serviceRemoved(final Consumer component) {
            delegate.serviceRemoved(component);
        }

        public void start() throws Exception {
            delegate.start();
        }

        public void stop() throws Exception {
            delegate.stop();
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class Component1Service12 implements BundleComponentContainer.Managed {

        private static BundleComponentContainer.Managed delegate;

        public Component1Service12(final @Service ServiceInterface1 service1, final Component2Service2 dependency) {
            // empty
        }

        public void start() throws Exception {
            delegate.start();
        }

        public void stop() throws Exception {
            delegate.stop();
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class Component2Service2 implements BundleComponentContainer.Managed {

        private static BundleComponentContainer.Managed delegate;

        public Component2Service2(final @Service ServiceInterface2 service2) {
            // empty
        }

        public void start() throws Exception {
            delegate.start();
        }

        public void stop() throws Exception {
            delegate.stop();
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class Component3Service1 implements BundleComponentContainer.Managed {

        private static BundleComponentContainer.Managed delegate;

        public Component3Service1(final Component4Service1 dependency) {
            // empty
        }

        public void start() throws Exception {
            delegate.start();
        }

        public void stop() throws Exception {
            delegate.stop();
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class Component4Service1 implements BundleComponentContainer.Managed {

        private static BundleComponentContainer.Managed delegate;

        public Component4Service1(final @Service ServiceInterface1 service1) {
            // empty
        }

        public void start() throws Exception {
            delegate.start();
        }

        public void stop() throws Exception {
            delegate.stop();
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class Component5Service2 implements BundleComponentContainer.Managed {

        private static BundleComponentContainer.Managed delegate;

        public Component5Service2(final @Service ServiceInterface2 service2) {
            // empty
        }

        public void start() throws Exception {
            delegate.start();
        }

        public void stop() throws Exception {
            delegate.stop();
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class FailingComponent implements BundleComponentContainer.Managed {

        @SuppressWarnings("UnusedDeclaration")
        private static BundleComponentContainer.Managed delegate;

        public FailingComponent(final @Service ServiceInterface1 service1) { }

        public void start() throws Exception {
            throw new UnsupportedOperationException("Failed");
        }

        public void stop() throws Exception {
            throw new UnsupportedOperationException("Failed");
        }
    }

    public static class MultipleServiceFiltersComponent implements BundleComponentContainer.Managed {

        private static BundleComponentContainer.Managed delegate;

        public MultipleServiceFiltersComponent(final @Service(filter = "filter-1") ServiceInterface1 service1,
                                               final @Service(filter = "filter-2") ServiceInterface1 service2) {
            assert service1 != service2;
        }

        public void start() throws Exception {
            delegate.start();
        }

        public void stop() throws Exception {
            delegate.stop();
        }
    }

    public static class StatusCheck implements BundleComponentContainer.Managed {

        public static ComponentStatus component;

        public StatusCheck(final ComponentStatus status) {
            StatusCheck.component = status;
        }

        public void start() throws Exception {
            // empty
        }

        public void stop() throws Exception {
            StatusCheck.component = null;
        }
    }
}
