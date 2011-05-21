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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.fluidity.composition.ClassDiscovery;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.DependencyInjector;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.foundation.logging.NoLogFactory;
import org.fluidity.foundation.spi.LogFactory;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
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
public class WhiteboardImplTest extends MockGroupAbstractTest {

    private final BundleContext context = mock(BundleContext.class);
    private final ComponentContainer container = mock(ComponentContainer.class);
    private final OpenComponentContainer child = mock(OpenComponentContainer.class);
    private final ComponentContainer.Registry registry = mock(ComponentContainer.Registry.class);
    private final ClassDiscovery discovery = mock(ClassDiscovery.class);
    private final DependencyInjector injector = mock(DependencyInjector.class);
    private final LogFactory logs = new NoLogFactory();

    private final ServiceInterface1 service1 = mock(ServiceInterface1.class);
    private final ServiceInterface2 service2 = mock(ServiceInterface2.class);
    private final Whiteboard.Item item = mock(Whiteboard.Item.class);

    private final ServiceRegistration registration = mock(ServiceRegistration.class);

    private final ServiceReference reference1 = mock(ServiceReference.class);
    private final ServiceReference reference2 = mock(ServiceReference.class);

    private final Consumer consumer1 = mock(Consumer.class);
    private final Consumer consumer2 = mock(Consumer.class);

    private final Whiteboard.Item component1 = mock(Whiteboard.Item.class);
    private final Whiteboard.Item component2 = mock(Whiteboard.Item.class);
    private final Whiteboard.Item component3 = mock(Whiteboard.Item.class);
    private final Whiteboard.Item component4 = mock(Whiteboard.Item.class);
    private final Whiteboard.Item component5 = mock(Whiteboard.Item.class);

    @SuppressWarnings("unchecked")
    private final Whiteboard.EventSource<Consumer> source = mock(Whiteboard.EventSource.class);

    @BeforeMethod
    public void setup() throws Exception {
        Service1.delegate = service1;
        Service2.delegate = service1;
        ServiceDependent1.delegate = item;
        ServiceDependent2.delegate = item;
        Source.delegate = source;
        Cluster1Component1.delegate = component1;
        Cluster1Component2.delegate = component2;
        Cluster2Component1.delegate = component3;
        Cluster2Component2.delegate = component4;
        Cluster3Component1.delegate = component5;
    }

    @Test
    public void testServiceRegistration() throws Exception {
        final Class<Service1> componentClass = Service1.class;
        final Class<ServiceInterface1> serviceInterface = ServiceInterface1.class;

        final Whiteboard whiteboard = discover(componentClass);

        EasyMock.expect(injector.findConstructor(componentClass)).andReturn((Constructor) componentClass.getConstructor());

        final Properties properties = new Properties();

        properties.setProperty("property-1", "value-1");
        properties.setProperty("property-2", "value-2");

        final Service1 service = new Service1();

        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindComponent(componentClass);
        EasyMock.expect(child.getComponent(componentClass)).andReturn(service);

        EasyMock.expect(Service1.delegate.properties()).andReturn(properties);
        EasyMock.expect(Service1.delegate.types()).andReturn(new Class[] { serviceInterface });

        Service1.delegate.start();

        // registering the service
        EasyMock.expect(context.registerService(EasyMock.aryEq(new String[] { serviceInterface.getName() }),
                                                EasyMock.same(service),
                                                EasyMock.same(properties)))
                .andReturn(registration);

        replay();
        whiteboard.start();
        verify();

        // un-registering the service
        registration.unregister();
        Service1.delegate.stop();

        replay();
        whiteboard.stop();
        verify();

        // no more action at second invocation

        replay();
        whiteboard.stop();
        verify();
    }

    @Test
    public void testServiceListener1() throws Exception {
        final Class<ServiceDependent1> componentClass = ServiceDependent1.class;
        final Whiteboard whiteboard = discover(componentClass);

        EasyMock.expect(injector.findConstructor(componentClass)).andReturn((Constructor) componentClass.getConstructor(ServiceInterface1.class, ServiceInterface2.class));

        // no services yet
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null).anyTimes();

        final ListenerSpec spec = expectListenerRegistration();

        final ServiceDependent1 dependent = new ServiceDependent1(service1, service2);

        replay();
        whiteboard.start();
        verify();

        checkFilter(spec, new ServiceSpecification(ServiceInterface1.class), new ServiceSpecification(ServiceInterface2.class));

        // responding to appearance of the first service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null).anyTimes();

        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        // responding to appearance of the second service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 }).anyTimes();

        EasyMock.expect(context.getService(reference2)).andReturn(service2);

        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindInstance(service1, ServiceInterface1.class);
        registry.bindInstance(service2, ServiceInterface2.class);
        registry.bindComponent(componentClass);
        EasyMock.expect(child.getComponent(componentClass)).andReturn(dependent);
        ServiceDependent1.delegate.start();

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        verify();

        // responding to disappearance of the first service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 }).anyTimes();

        EasyMock.expect(context.ungetService(reference1)).andReturn(false);

        ServiceDependent1.delegate.stop();

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference2));
        verify();

        // responding to reappearance of the first service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 }).anyTimes();

        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindInstance(service1, ServiceInterface1.class);
        registry.bindInstance(service2, ServiceInterface2.class);
        registry.bindComponent(componentClass);
        EasyMock.expect(child.getComponent(componentClass)).andReturn(dependent);
        ServiceDependent1.delegate.start();

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        // responding to disappearance of the second service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null).anyTimes();

        EasyMock.expect(context.ungetService(reference2)).andReturn(false);

        ServiceDependent1.delegate.stop();

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference2));
        verify();

        // responding to disappearance of the first service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null).anyTimes();

        EasyMock.expect(context.ungetService(reference1)).andReturn(false);

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        // removing the listener
        context.removeServiceListener(spec.listener());

        replay();
        whiteboard.stop();
        verify();

        // no more action at second invocation

        replay();
        whiteboard.stop();
        verify();
    }

    @Test
    public void testServiceListener2() throws Exception {
        final Class<ServiceDependent1> componentClass = ServiceDependent1.class;
        final Whiteboard whiteboard = discover(componentClass);

        EasyMock.expect(injector.findConstructor(componentClass)).andReturn((Constructor) componentClass.getConstructor(ServiceInterface1.class, ServiceInterface2.class));

        final ListenerSpec spec = expectListenerRegistration();

        // first service already registered
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null).anyTimes();

        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        final ServiceDependent1 dependent = new ServiceDependent1(service1, service2);

        replay();
        whiteboard.start();
        verify();

        checkFilter(spec, new ServiceSpecification(ServiceInterface1.class), new ServiceSpecification(ServiceInterface2.class));

        // responding to appearance of the second service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 }).anyTimes();

        EasyMock.expect(context.getService(reference2)).andReturn(service2);

        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindInstance(service1, ServiceInterface1.class);
        registry.bindInstance(service2, ServiceInterface2.class);
        registry.bindComponent(componentClass);
        EasyMock.expect(child.getComponent(componentClass)).andReturn(dependent);
        ServiceDependent1.delegate.start();

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        verify();

        // responding to disappearance of the first service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 }).anyTimes();

        EasyMock.expect(context.ungetService(reference1)).andReturn(false);

        ServiceDependent1.delegate.stop();

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference2));
        verify();

        // responding to reappearance of the first service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 }).anyTimes();

        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindInstance(service1, ServiceInterface1.class);
        registry.bindInstance(service2, ServiceInterface2.class);
        registry.bindComponent(componentClass);
        EasyMock.expect(child.getComponent(componentClass)).andReturn(dependent);
        ServiceDependent1.delegate.start();

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        // removing the listener
        context.removeServiceListener(spec.listener());

        ServiceDependent1.delegate.stop();

        replay();
        whiteboard.stop();
        verify();

        // no more action at second invocation

        replay();
        whiteboard.stop();
        verify();
    }

    @Test
    public void testServiceListener3() throws Exception {
        final Class<ServiceDependent2> componentClass = ServiceDependent2.class;
        final Whiteboard whiteboard = discover(componentClass);

        EasyMock.expect(injector.findConstructor(componentClass)).andReturn((Constructor) componentClass.getConstructor(ServiceInterface1.class, ServiceInterface2.class));

        final ServiceDependent2 dependent = new ServiceDependent2(service1, service2);

        final ListenerSpec spec = expectListenerRegistration();

        // first service already registered
        final String selector1 = "filter-1";
        final String selector2 = "filter-2";

        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), selector1)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), selector2)).andReturn(new ServiceReference[] { reference2 }).anyTimes();

        EasyMock.expect(context.getService(reference1)).andReturn(service1);
        EasyMock.expect(context.getService(reference2)).andReturn(service2);

        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindInstance(service1, ServiceInterface1.class);
        registry.bindInstance(service2, ServiceInterface2.class);
        registry.bindComponent(componentClass);
        EasyMock.expect(child.getComponent(componentClass)).andReturn(dependent);
        ServiceDependent2.delegate.start();

        replay();
        whiteboard.start();
        verify();

        checkFilter(spec, new ServiceSpecification(ServiceInterface1.class, selector1), new ServiceSpecification(ServiceInterface2.class, selector2));

        // removing the listener
        context.removeServiceListener(spec.listener());

        ServiceDependent2.delegate.stop();

        replay();
        whiteboard.stop();
        verify();

        // no more action at second invocation

        replay();
        whiteboard.stop();
        verify();
    }

    @Test
    public void testEventSourcesAndConsumers1() throws Exception {
        final Class<Source> componentClass = Source.class;
        final Whiteboard whiteboard = discover(componentClass);

        EasyMock.expect(injector.findConstructor(componentClass)).andReturn((Constructor) componentClass.getConstructor());
        final Source source = new Source();

        // service listener registration
        final ListenerSpec spec = expectListenerRegistration();

        EasyMock.expect(source.clientType()).andReturn(Consumer.class);
        EasyMock.expect(context.getServiceReferences(Consumer.class.getName(), null)).andReturn(new ServiceReference[0]);

        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindComponent(componentClass);
        EasyMock.expect(child.getComponent(componentClass)).andReturn(source);
        Source.delegate.start();

        replay();
        whiteboard.start();
        verify();

        assert spec.filter().equals(String.format("(%s=%s)", Constants.OBJECTCLASS, Consumer.class.getName()));

        // response to appearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);
        EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);

        Source.delegate.clientAdded(EasyMock.same(consumer1), EasyMock.<Properties>eq(new Properties()));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        // response to appearance of second consumer
        EasyMock.expect(context.getService(reference2)).andReturn(consumer2);

        final Properties properties = new Properties();
        properties.setProperty("xxx", "yyy");
        EasyMock.expect(reference2.getPropertyKeys()).andReturn(new String[] { "xxx" });
        EasyMock.expect(reference2.getProperty("xxx")).andReturn(properties.getProperty("xxx"));

        Source.delegate.clientAdded(EasyMock.same(consumer2), EasyMock.<Properties>eq(properties));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        verify();

        // response to disappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);

        Source.delegate.clientRemoved(EasyMock.same(consumer1));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        // response to reappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);
        EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);

        Source.delegate.clientAdded(EasyMock.same(consumer1), EasyMock.eq(new Properties()));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        // response to disappearance of second consumer
        EasyMock.expect(context.getService(reference2)).andReturn(consumer2);

        Source.delegate.clientRemoved(EasyMock.same(consumer2));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference2));
        verify();

        // response to disappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);

        Source.delegate.clientRemoved(EasyMock.same(consumer1));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        // removing the event source
        context.removeServiceListener(spec.listener());
        Source.delegate.stop();

        replay();
        whiteboard.stop();
        verify();

        // no more action at second invocation

        replay();
        whiteboard.stop();
        verify();
    }

    @Test
    public void testEventSourcesAndConsumers2() throws Exception {
        final Class<Source> componentClass = Source.class;
        final Whiteboard whiteboard = discover(componentClass);

        EasyMock.expect(injector.findConstructor(componentClass)).andReturn((Constructor) componentClass.getConstructor());
        final Source source = new Source();

        // service listener registration
        final ListenerSpec spec = expectListenerRegistration();

        EasyMock.expect(context.getServiceReferences(Consumer.class.getName(), null)).andReturn(new ServiceReference[] { reference1 });
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);
        EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);

        EasyMock.expect(source.clientType()).andReturn(Consumer.class);
        source.clientAdded(EasyMock.same(consumer1), EasyMock.<Properties>notNull());

        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindComponent(componentClass);
        EasyMock.expect(child.getComponent(componentClass)).andReturn(source);
        Source.delegate.start();

        replay();
        whiteboard.start();
        verify();

        assert spec.filter().equals(String.format("(%s=%s)", Constants.OBJECTCLASS, Consumer.class.getName()));

        // response to appearance of second consumer
        EasyMock.expect(context.getService(reference2)).andReturn(consumer2);
        EasyMock.expect(reference2.getPropertyKeys()).andReturn(new String[0]);

        Source.delegate.clientAdded(EasyMock.same(consumer2), EasyMock.eq(new Properties()));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        verify();

        // response to disappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);

        Source.delegate.clientRemoved(EasyMock.same(consumer1));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        // response to reappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);
        EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);

        Source.delegate.clientAdded(EasyMock.same(consumer1), EasyMock.eq(new Properties()));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        // removing the event source
        context.removeServiceListener(spec.listener());
        Source.delegate.stop();

        replay();
        whiteboard.stop();
        verify();

        // no more action at second invocation

        replay();
        whiteboard.stop();
        verify();
    }

    @Test
    public void testComponentClusters() throws Exception {
        final Whiteboard whiteboard = discover(Cluster1Component1.class,
                                               Cluster1Component2.class,
                                               Cluster2Component1.class,
                                               Cluster2Component2.class,
                                               Cluster3Component1.class);

        EasyMock.expect(injector.findConstructor(Cluster1Component1.class))
                .andReturn((Constructor) Cluster1Component1.class.getConstructor(ServiceInterface1.class, Cluster1Component2.class));

        EasyMock.expect(injector.findConstructor(Cluster1Component2.class))
                .andReturn((Constructor) Cluster1Component2.class.getConstructor(ServiceInterface2.class));

        EasyMock.expect(injector.findConstructor(Cluster2Component1.class))
                .andReturn((Constructor) Cluster2Component1.class.getConstructor(Cluster2Component2.class));

        EasyMock.expect(injector.findConstructor(Cluster2Component2.class))
                .andReturn((Constructor) Cluster2Component2.class.getConstructor(ServiceInterface1.class));

        EasyMock.expect(injector.findConstructor(Cluster3Component1.class))
                .andReturn((Constructor) Cluster3Component1.class.getConstructor(ServiceInterface2.class));

        final Cluster1Component2 cluster1Component2 = new Cluster1Component2(service2);
        final Cluster1Component1 cluster1Component1 = new Cluster1Component1(service1, cluster1Component2);
        final Cluster2Component2 cluster2Component2 = new Cluster2Component2(service1);
        final Cluster2Component1 cluster2Component1 = new Cluster2Component1(cluster2Component2);
        final Cluster3Component1 cluster3Component1 = new Cluster3Component1(service2);

        // we expect three clusters only
        final ListenerSpec listener1 = expectListenerRegistration();
        final ListenerSpec listener2 = expectListenerRegistration();
        final ListenerSpec listener3 = expectListenerRegistration();

        // no services yet
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null).anyTimes();

        replay();
        whiteboard.start();
        verify();

        final Map<Class<?>, Set<ListenerSpec>> listenerMap = new HashMap<Class<?>, Set<ListenerSpec>>();
        listenerMap.put(ServiceInterface1.class, filterListeners(ServiceInterface1.class, listener1, listener2, listener3));
        listenerMap.put(ServiceInterface2.class, filterListeners(ServiceInterface2.class, listener1, listener2, listener3));

        final Set<ListenerSpec> service1Listeners = listenerMap.get(ServiceInterface1.class);
        final Set<ListenerSpec> service2Listeners = listenerMap.get(ServiceInterface2.class);

        // add ServiceInterface1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null).anyTimes();

        EasyMock.expect(context.getService(reference1)).andReturn(service1).anyTimes();

        // instantiate clusters that require service 1 only
        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindInstance(service1, ServiceInterface1.class);
        registry.bindComponent(Cluster2Component1.class);
        registry.bindComponent(Cluster2Component2.class);
        EasyMock.expect(child.getComponent(Cluster2Component2.class)).andReturn(cluster2Component2);
        EasyMock.expect(child.getComponent(Cluster2Component1.class)).andReturn(cluster2Component1);

        Cluster2Component1.delegate.start();
        Cluster2Component2.delegate.start();

        replay();
        for (final ListenerSpec listener : service1Listeners) {
            listener.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        }
        verify();

        // add ServiceInterface2
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 }).anyTimes();

        EasyMock.expect(context.getService(reference1)).andReturn(service1).anyTimes();
        EasyMock.expect(context.getService(reference2)).andReturn(service2).anyTimes();

        // instantiate clusters that require service 2 only
        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindInstance(service2, ServiceInterface2.class);
        registry.bindComponent(Cluster3Component1.class);
        EasyMock.expect(child.getComponent(Cluster3Component1.class)).andReturn(cluster3Component1);

        Cluster3Component1.delegate.start();

        // instantiate clusters that require both services
        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindInstance(service1, ServiceInterface1.class);
        registry.bindInstance(service2, ServiceInterface2.class);
        registry.bindComponent(Cluster1Component1.class);
        registry.bindComponent(Cluster1Component2.class);
        EasyMock.expect(child.getComponent(Cluster1Component2.class)).andReturn(cluster1Component2);
        EasyMock.expect(child.getComponent(Cluster1Component1.class)).andReturn(cluster1Component1);

        Cluster1Component1.delegate.start();
        Cluster1Component2.delegate.start();

        replay();
        for (final ListenerSpec listener : service2Listeners) {
            listener.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        }
        verify();

        // remove ServiceInterface1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 }).anyTimes();

        EasyMock.expect(context.ungetService(reference1)).andReturn(false).anyTimes();

        Cluster1Component1.delegate.stop();
        Cluster1Component2.delegate.stop();
        Cluster2Component1.delegate.stop();
        Cluster2Component2.delegate.stop();

        replay();
        for (final ListenerSpec listener : service1Listeners) {
            listener.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        }
        verify();

        // add ServiceInterface1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 }).anyTimes();

        EasyMock.expect(context.getService(reference1)).andReturn(service1).anyTimes();
        EasyMock.expect(context.getService(reference2)).andReturn(service2).anyTimes();

        // instantiate clusters that require service 1 only
        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindInstance(service1, ServiceInterface1.class);
        registry.bindComponent(Cluster2Component1.class);
        registry.bindComponent(Cluster2Component2.class);
        EasyMock.expect(child.getComponent(Cluster2Component2.class)).andReturn(cluster2Component2);
        EasyMock.expect(child.getComponent(Cluster2Component1.class)).andReturn(cluster2Component1);

        Cluster2Component1.delegate.start();
        Cluster2Component2.delegate.start();

        // instantiate clusters that require both services
        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindInstance(service1, ServiceInterface1.class);
        registry.bindInstance(service2, ServiceInterface2.class);
        registry.bindComponent(Cluster1Component1.class);
        registry.bindComponent(Cluster1Component2.class);
        EasyMock.expect(child.getComponent(Cluster1Component2.class)).andReturn(cluster1Component2);
        EasyMock.expect(child.getComponent(Cluster1Component1.class)).andReturn(cluster1Component1);

        Cluster1Component1.delegate.start();
        Cluster1Component2.delegate.start();

        replay();
        for (final ListenerSpec listener : service1Listeners) {
            listener.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        }
        verify();

        // remove ServiceInterface2
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null).anyTimes();

        EasyMock.expect(context.ungetService(reference2)).andReturn(false).anyTimes();

        Cluster1Component1.delegate.stop();
        Cluster1Component2.delegate.stop();
        Cluster3Component1.delegate.stop();

        replay();
        for (final ListenerSpec listener : service2Listeners) {
            listener.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference2));
        }
        verify();

        // remove ServiceInterface1
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null).anyTimes();

        EasyMock.expect(context.ungetService(reference1)).andReturn(false).anyTimes();

        Cluster2Component1.delegate.stop();
        Cluster2Component2.delegate.stop();

        replay();
        for (final ListenerSpec listener : service1Listeners) {
            listener.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        }
        verify();

        // add ServiceInterface2
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 }).anyTimes();

        EasyMock.expect(context.getService(reference2)).andReturn(service2).anyTimes();

        // instantiate clusters that require service 2 only
        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindInstance(service2, ServiceInterface2.class);
        registry.bindComponent(Cluster3Component1.class);
        EasyMock.expect(child.getComponent(Cluster3Component1.class)).andReturn(cluster3Component1);

        Cluster3Component1.delegate.start();

        replay();
        for (final ListenerSpec listener : service2Listeners) {
            listener.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        }
        verify();

        // stop
        context.removeServiceListener(listener1.listener());
        context.removeServiceListener(listener2.listener());
        context.removeServiceListener(listener3.listener());
        Cluster3Component1.delegate.stop();

        replay();
        whiteboard.stop();
        verify();

        // no more action at second invocation

        replay();
        whiteboard.stop();
        verify();
    }

    private Set<ListenerSpec> filterListeners(final Class<?> type, final ListenerSpec... listeners) {
        final String name = type.getName();
        final Set<ListenerSpec> list = new HashSet<ListenerSpec>();

        for (final ListenerSpec listener : listeners) {
            if (listener.filter().contains(name)) {
                list.add(listener);
            }
        }

        return list;
    }

    private Whiteboard discover(final Class... types) {
        EasyMock.expect(discovery.findComponentClasses(Whiteboard.Item.class, WhiteboardImpl.class.getClassLoader(), false)).andReturn(types);

        replay();
        final Whiteboard whiteboard = new WhiteboardImpl(context, container, logs, injector, discovery);
        verify();

        return whiteboard;
    }

    private void checkFilter(final ListenerSpec listener, final ServiceSpecification... specifications) {
        String filter = listener.filter();

        for (final ServiceSpecification specification : specifications) {
            final String reference = specification.filter == null
                                     ? String.format("(%s=%s)", Constants.OBJECTCLASS, specification.api.getName())
                                     : String.format("&(%s=%s)%s)", Constants.OBJECTCLASS, specification.api.getName(), specification.filter);

            final int index = filter.indexOf(reference);
            assert index >= 0 : String.format("Expected '%s' in '%s'", reference, listener.filter());
            filter = filter.substring(0, index).concat(filter.substring(index + reference.length()));
        }

        assert !filter.contains(Constants.OBJECTCLASS) : String.format("Filter '%s' refers to more services than expected (%d)", listener.filter(), specifications.length);
    }

    private ListenerSpec expectListenerRegistration() throws Exception {
        context.addServiceListener(EasyMock.<ServiceListener>notNull(), EasyMock.<String>notNull());

        final ListenerSpecImpl spec = new ListenerSpecImpl();
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            public Object answer() throws Throwable {
                spec.listener = (ServiceListener) EasyMock.getCurrentArguments()[0];
                spec.filter = (String) EasyMock.getCurrentArguments()[1];
                return null;
            }
        });

        return spec;
    }

    private static interface ListenerSpec {

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

    public static interface ServiceInterface1 extends Whiteboard.Registration { }

    public static interface ServiceInterface2 extends Whiteboard.Registration { }

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

    public static final class ServiceDependent1 implements Whiteboard.Item {

        private static Whiteboard.Item delegate;

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

    public static final class ServiceDependent2 implements Whiteboard.Item {

        private static Whiteboard.Item delegate;

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

    public static interface Consumer extends Whiteboard.Item { }

    public static class Source implements Whiteboard.EventSource<Consumer> {

        private static Whiteboard.EventSource<Consumer> delegate;

        public Class<Consumer> clientType() {
            return delegate.clientType();
        }

        public void clientAdded(final Consumer consumer, final Properties properties) {
            delegate.clientAdded(consumer, properties);
        }

        public void clientRemoved(final Consumer consumer) {
            delegate.clientRemoved(consumer);
        }

        public void start() throws Exception {
            delegate.start();
        }

        public void stop() throws Exception {
            delegate.stop();
        }
    }

    @SuppressWarnings("UnusedParameters")
    public static class Cluster1Component1 implements Whiteboard.Item {

        private static Whiteboard.Item delegate;

        public Cluster1Component1(final @Service ServiceInterface1 service1, final Cluster1Component2 dependency) {
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
    public static class Cluster1Component2 implements Whiteboard.Item {

        private static Whiteboard.Item delegate;

        public Cluster1Component2(final @Service ServiceInterface2 service2) {
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
    public static class Cluster2Component1 implements Whiteboard.Item {

        private static Whiteboard.Item delegate;

        public Cluster2Component1(final Cluster2Component2 dependency) {
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
    public static class Cluster2Component2 implements Whiteboard.Item {

        private static Whiteboard.Item delegate;

        public Cluster2Component2(final @Service ServiceInterface1 service1) {
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
    public static class Cluster3Component1 implements Whiteboard.Item {

        private static Whiteboard.Item delegate;

        public Cluster3Component1(final @Service ServiceInterface2 service2) {
        }

        public void start() throws Exception {
            delegate.start();
        }

        public void stop() throws Exception {
            delegate.stop();
        }
    }
}
