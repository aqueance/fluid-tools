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

package org.fluidity.deployment.osgi.impl;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentDiscovery;
import org.fluidity.composition.Containers;
import org.fluidity.deployment.osgi.BundleComponents;
import org.fluidity.deployment.osgi.Service;
import org.fluidity.foundation.Deferred;
import org.fluidity.foundation.Log;
import org.fluidity.foundation.NoLogFactory;
import org.fluidity.testing.Simulator;

import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
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
public class BundleComponentContainerLogicTest extends Simulator {

    private final ClassLoader loader = BundleComponentContainerImpl.class.getClassLoader();
    private final ComponentContainer root = Containers.global();
    private final MockObjects dependencies = dependencies();

    private final Bundle bundle = dependencies.normal(Bundle.class);
    private final BundleContext context = dependencies.normal(BundleContext.class);
    private final ComponentDiscovery discovery = dependencies.normal(ComponentDiscovery.class);

    private final ServiceInterface1 service1 = dependencies.normal(ServiceInterface1.class);
    private final ServiceInterface2 service2 = dependencies.normal(ServiceInterface2.class);
    private final BundleComponents.Managed item = dependencies.normal(BundleComponents.Managed.class);

    private final ServiceRegistration registration = dependencies.normal(ServiceRegistration.class);

    private final ServiceReference reference1 = dependencies.normal(ServiceReference.class);
    private final ServiceReference reference2 = dependencies.normal(ServiceReference.class);

    private final Consumer consumer1 = dependencies.normal(Consumer.class);
    private final Consumer consumer2 = dependencies.normal(Consumer.class);

    private final BundleComponents.Managed component1 = dependencies.normal(BundleComponents.Managed.class);
    private final BundleComponents.Managed component2 = dependencies.normal(BundleComponents.Managed.class);
    private final BundleComponents.Managed component3 = dependencies.normal(BundleComponents.Managed.class);
    private final BundleComponents.Managed component4 = dependencies.normal(BundleComponents.Managed.class);
    private final BundleComponents.Managed component5 = dependencies.normal(BundleComponents.Managed.class);
    private final BundleComponents.Managed component6 = dependencies.normal(BundleComponents.Managed.class);

    private final ServiceInterface1 service3 = dependencies.normal(ServiceInterface1.class);
    private final ServiceReference reference3 = dependencies.normal(ServiceReference.class);

    private ComponentContainer container;

    private final BundleComponents.Registration.Listener<Consumer> source = dependencies.normal(BundleComponents.Registration.Listener.class);
    private final Log log = NoLogFactory.consume(BundleComponentContainerImpl.class);

    @BeforeMethod
    public void setup() throws Exception {
        container = root.makeChildContainer();

        Service1.delegate = service1;
        Service2.delegate = service1;
        ServiceDependent1.delegate = item;
        ServiceDependent2.delegate = item;
        EventSource.delegate = source;
        Component1Service12.delegate = component1;
        Component2Service2.delegate = component2;
        Component3Service1.delegate = component3;
        Component4Service1.delegate = component4;
        Component5Service2.delegate = component5;
    }

    @Test
    public void testServiceRegistration() throws Exception {
        final Class<Service1> componentClass = Service1.class;

        final Deferred.Reference<BundleComponentContainerImpl.Logic> services = discover(StatusCheck.class, componentClass);

        // registering the service
        test(() -> {
            final Properties properties = new Properties();

            properties.setProperty("property-1", "value-1");
            properties.setProperty("property-2", "value-2");

            final Class<ServiceInterface1> serviceInterface = ServiceInterface1.class;

            EasyMock.expect(Service1.delegate.properties()).andReturn(properties);

            Service1.delegate.start();
            EasyMock.expect(context.registerService(EasyMock.aryEq(new String[] { serviceInterface.getName() }),
                                                    EasyMock.<Service1>notNull(),
                                                    (Dictionary) EasyMock.same(properties))).andReturn(registration);

            verify(() -> services.get().start());

            assertFailed();
            assertActive(StatusCheck.class, componentClass);
            assertInactive(Collections.emptyMap());
        });

        // un-registering the service
        test(() -> {
            registration.unregister();
            Service1.delegate.stop();

            verify(() -> services.get().stop());
        });
    }

    @Test
    public void testServiceListener1() throws Exception {
        final Class<ServiceDependent1> componentClass = ServiceDependent1.class;
        final Deferred.Reference<BundleComponentContainerImpl.Logic> services = discover(StatusCheck.class, componentClass);

        final Service annotation1 = new ServiceImpl(ServiceInterface1.class);
        final Service annotation2 = new ServiceImpl(ServiceInterface2.class);
        final Map.Entry<Class<?>, Set<Service>> dependencies = dependencies(ServiceDependent1.class, annotation1, annotation2);

        // no services yet
        final ServiceListener[] listeners = test(() -> {
            noServices(ServiceInterface1.class, null);
            noServices(ServiceInterface2.class, null);

            final List<ListenerSpec> _listeners = Arrays.asList(expectListenerRegistration(), expectListenerRegistration());

            verify(() -> services.get().start());

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies));

            final ListenerSpec spec1 = find(_listeners, ServiceInterface1.class, null);
            final ListenerSpec spec2 = find(_listeners, ServiceInterface2.class, null);

            checkFilter(spec1, ServiceInterface1.class, null);
            checkFilter(spec2, ServiceInterface2.class, null);

            assert spec1 != null;
            assert spec2 != null;
            return new ServiceListener[] { spec1.listener(), spec2.listener() };
        });

        // responding to appearance of the first service
        test(() -> {
            serviceAdded(ServiceInterface1.class, null, reference1, service1);

            verify(event(listeners[0], ServiceEvent.REGISTERED, reference1));

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies), annotation1);
        });

        // responding to appearance of the second service
        test(() -> {
            serviceAdded(ServiceInterface2.class, null, reference2, service2);
            ServiceDependent1.delegate.start();

            verify(event(listeners[1], ServiceEvent.REGISTERED, reference2));

            assertFailed();
            assertActive(StatusCheck.class, componentClass);
            assertInactive(Collections.emptyMap());
        });

        // responding to disappearance of the first service
        test(() -> {
            serviceRemoved(ServiceInterface1.class, null, reference1);
            ServiceDependent1.delegate.stop();

            verify(event(listeners[0], ServiceEvent.UNREGISTERING, reference1));

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies), annotation2);
        });

        // responding to reappearance of the first service
        test(() -> {
            serviceAdded(ServiceInterface1.class, null, reference1, service1);
            ServiceDependent1.delegate.start();

            verify(event(listeners[0], ServiceEvent.REGISTERED, reference1));

            assertFailed();
            assertActive(StatusCheck.class, componentClass);
            assertInactive(Collections.emptyMap());
        });

        // responding to disappearance of the second service
        test(() -> {
            serviceRemoved(ServiceInterface2.class, null, reference2);
            ServiceDependent1.delegate.stop();

            verify(event(listeners[1], ServiceEvent.UNREGISTERING, reference2));

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies), annotation1);
        });

        // responding to disappearance of the first service
        test(() -> {
            serviceRemoved(ServiceInterface1.class, null, reference1);

            verify(event(listeners[0], ServiceEvent.UNREGISTERING, reference1));

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies));
        });

        // removing the listeners
        test(() -> {
            context.removeServiceListener(listeners[0]);
            context.removeServiceListener(listeners[1]);

            verify(() -> services.get().stop());
        });
    }

    @Test
    public void testServiceListener2() throws Exception {
        final Class<ServiceDependent1> componentClass = ServiceDependent1.class;
        final Deferred.Reference<BundleComponentContainerImpl.Logic> services = discover(StatusCheck.class, componentClass);

        final Service annotation1 = new ServiceImpl(ServiceInterface1.class);
        final Service annotation2 = new ServiceImpl(ServiceInterface2.class);
        final Map.Entry<Class<?>, Set<Service>> dependencies = dependencies(ServiceDependent1.class, annotation1, annotation2);

        // first service already registered
        final ServiceListener[] listeners = test(() -> {
            final List<ListenerSpec> _listeners = Arrays.asList(expectListenerRegistration(), expectListenerRegistration());

            serviceAdded(ServiceInterface1.class, null, reference1, service1);
            noServices(ServiceInterface2.class, null);

            verify(() -> services.get().start());

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies), annotation1);

            final ListenerSpec spec1 = find(_listeners, ServiceInterface1.class, null);
            final ListenerSpec spec2 = find(_listeners, ServiceInterface2.class, null);

            checkFilter(spec1, ServiceInterface1.class, null);
            checkFilter(spec2, ServiceInterface2.class, null);

            assert spec1 != null;
            assert spec2 != null;
            return new ServiceListener[] { spec1.listener(), spec2.listener() };
        });

        // responding to appearance of the second service
        test(() -> {
            serviceAdded(ServiceInterface2.class, null, reference2, service2);
            ServiceDependent1.delegate.start();

            verify(event(listeners[1], ServiceEvent.REGISTERED, reference2));

            assertFailed();
            assertActive(StatusCheck.class, componentClass);
            assertInactive(Collections.emptyMap());
        });

        // responding to disappearance of the first service
        test(() -> {
            serviceRemoved(ServiceInterface1.class, null, reference1);
            ServiceDependent1.delegate.stop();

            verify(event(listeners[0], ServiceEvent.UNREGISTERING, reference1));

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies), annotation2);
        });

        // responding to reappearance of the first service
        test(() -> {
            serviceAdded(ServiceInterface1.class, null, reference1, service1);
            ServiceDependent1.delegate.start();

            verify(event(listeners[0], ServiceEvent.REGISTERED, reference1));

            assertFailed();
            assertActive(StatusCheck.class, componentClass);
            assertInactive(Collections.emptyMap());
        });

        // removing the listener
        test(() -> {
            context.removeServiceListener(listeners[0]);
            context.removeServiceListener(listeners[1]);

            ServiceDependent1.delegate.stop();

            verify(() -> services.get().stop());
        });
    }

    @Test
    public void testServiceListener3() throws Exception {
        final Class<ServiceDependent2> componentClass = ServiceDependent2.class;

        final String selector1 = "filter-1";
        final String selector2 = "filter-2";

        final Deferred.Reference<BundleComponentContainerImpl.Logic> services = discover(Arrays.asList(selector1, selector2), StatusCheck.class, componentClass);

        final ServiceListener[] listeners = test(() -> {
            final List<ListenerSpec> _listeners = Arrays.asList(expectListenerRegistration(), expectListenerRegistration());

            serviceAdded(ServiceInterface1.class, selector1, reference1, service1);
            serviceAdded(ServiceInterface2.class, selector2, reference2, service2);
            ServiceDependent2.delegate.start();

            verify(() -> services.get().start());

            assertFailed();
            assertActive(StatusCheck.class, componentClass);
            assertInactive(Collections.emptyMap());

            final ListenerSpec spec1 = find(_listeners, ServiceInterface1.class, null);
            final ListenerSpec spec2 = find(_listeners, ServiceInterface2.class, null);

            checkFilter(spec1, ServiceInterface1.class, selector1);
            checkFilter(spec2, ServiceInterface2.class, selector2);

            assert spec1 != null;
            assert spec2 != null;
            return new ServiceListener[] { spec1.listener(), spec2.listener() };
        });

        // removing the listeners
        test(() -> {
            context.removeServiceListener(listeners[0]);
            context.removeServiceListener(listeners[1]);

            ServiceDependent2.delegate.stop();

            verify(() -> services.get().stop());
        });
    }

    @Test
    public void testEventSourcesAndConsumers1() throws Exception {
        final Class<EventSource> componentClass = EventSource.class;
        final Deferred.Reference<BundleComponentContainerImpl.Logic> services = discover(StatusCheck.class, componentClass);

        final EventSource source = new EventSource();

        // service listener registration
        final ListenerSpec spec = test(() -> {
            final ListenerSpec _spec = expectListenerRegistration();

            EasyMock.expect(source.type()).andReturn(Consumer.class);
            noServices(Consumer.class, null);
            EventSource.delegate.start();

            verify(() -> services.get().start());

            assertFailed();
            assertActive(StatusCheck.class, componentClass);
            assertInactive(Collections.emptyMap());

            assert Objects.equals(_spec.filter(), String.format("(%s=%s)", Constants.OBJECTCLASS, Consumer.class.getName()));

            return _spec;
        });

        // response to appearance of first consumer
        test(() -> {
            resolveService(reference1, consumer1);
            EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);
            EventSource.delegate.serviceAdded(EasyMock.same(consumer1), EasyMock.eq(new Properties()));

            verify(event(spec.listener(), ServiceEvent.REGISTERED, reference1));
        });

        // response to appearance of second consumer
        test(() -> {
            final Properties properties = new Properties();

            final String key = "xxx";
            properties.setProperty(key, "yyy");

            resolveService(reference2, consumer2);
            EasyMock.expect(reference2.getPropertyKeys()).andReturn(new String[] { key });
            EasyMock.expect(reference2.getProperty(key)).andReturn(properties.getProperty(key));
            EventSource.delegate.serviceAdded(EasyMock.same(consumer2), EasyMock.eq(properties));

            verify(event(spec.listener(), ServiceEvent.REGISTERED, reference2));
        });

        // response to disappearance of first consumer
        test(() -> {
            resolveService(reference1, consumer1);
            EventSource.delegate.serviceRemoved(EasyMock.same(consumer1));

            verify(event(spec.listener(), ServiceEvent.UNREGISTERING, reference1));
        });

        // response to reappearance of first consumer
        test(() -> {
            resolveService(reference1, consumer1);
            EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);
            EventSource.delegate.serviceAdded(EasyMock.same(consumer1), EasyMock.eq(new Properties()));

            verify(event(spec.listener(), ServiceEvent.REGISTERED, reference1));
        });

        // response to disappearance of second consumer
        test(() -> {
            resolveService(reference2, consumer2);

            EventSource.delegate.serviceRemoved(EasyMock.same(consumer2));

            verify(event(spec.listener(), ServiceEvent.UNREGISTERING, reference2));
        });

        // response to disappearance of first consumer
        test(() -> {
            resolveService(reference1, consumer1);

            EventSource.delegate.serviceRemoved(EasyMock.same(consumer1));

            verify(event(spec.listener(), ServiceEvent.UNREGISTERING, reference1));
        });

        // removing the event source
        test(() -> {
            context.removeServiceListener(spec.listener());
            EventSource.delegate.stop();

            verify(() -> services.get().stop());
        });
    }

    @Test
    public void testEventSourcesAndConsumers2() throws Exception {
        final Class<EventSource> componentClass = EventSource.class;
        final Deferred.Reference<BundleComponentContainerImpl.Logic> services = discover(StatusCheck.class, componentClass);

        final EventSource source = new EventSource();

        // service listener registration
        final ListenerSpec spec = test(() -> {
            final ListenerSpec _spec = expectListenerRegistration();

            serviceAdded(Consumer.class, null, reference1, consumer1);
            EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);
            EasyMock.expect(source.type()).andReturn(Consumer.class);
            source.serviceAdded(EasyMock.same(consumer1), EasyMock.notNull());
            EventSource.delegate.start();

            verify(() -> services.get().start());

            assertFailed();
            assertActive(StatusCheck.class, componentClass);
            assertInactive(Collections.emptyMap());

            assert Objects.equals(_spec.filter(), String.format("(%s=%s)", Constants.OBJECTCLASS, Consumer.class.getName()));

            return _spec;
        });

        // response to appearance of second consumer
        test(() -> {
            resolveService(reference2, consumer2);
            EasyMock.expect(reference2.getPropertyKeys()).andReturn(new String[0]);
            EventSource.delegate.serviceAdded(EasyMock.same(consumer2), EasyMock.eq(new Properties()));

            verify(event(spec.listener(), ServiceEvent.REGISTERED, reference2));
        });

        // response to disappearance of first consumer
        test(() -> {
            resolveService(reference1, consumer1);
            EventSource.delegate.serviceRemoved(EasyMock.same(consumer1));

            verify(event(spec.listener(), ServiceEvent.UNREGISTERING, reference1));
        });

        // response to reappearance of first consumer
        test(() -> {
            resolveService(reference1, consumer1);
            EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);
            EventSource.delegate.serviceAdded(EasyMock.same(consumer1), EasyMock.eq(new Properties()));

            verify(event(spec.listener(), ServiceEvent.REGISTERED, reference1));
        });

        // removing the event source
        test(() -> {
            context.removeServiceListener(spec.listener());
            EventSource.delegate.stop();

            verify(() -> services.get().stop());
        });
    }

    @Test
    public void testManagedComponents() throws Exception {
        final Deferred.Reference<BundleComponentContainerImpl.Logic> services = discover(StatusCheck.class,
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
        final ListenerSpec[] listeners = test(() -> {

            // no services yet
            final List<ListenerSpec> _listeners = Arrays.asList(expectListenerRegistration(), expectListenerRegistration());
            noServices(ServiceInterface1.class, null);
            noServices(ServiceInterface2.class, null);

            verify(() -> services.get().start());

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies1, dependencies2, dependencies3, dependencies4, dependencies5));

            return new ListenerSpec[] { find(_listeners, ServiceInterface1.class, null), find(_listeners, ServiceInterface2.class, null) };
        });

        // add ServiceInterface1
        test(() -> {
            serviceAdded(ServiceInterface1.class, null, reference1, service1);

            // start components that require service 1 only
            Component3Service1.delegate.start();
            Component4Service1.delegate.start();

            verify(event(listeners[0].listener(), ServiceEvent.REGISTERED, reference1));

            assertFailed();
            assertActive(StatusCheck.class, Component3Service1.class, Component4Service1.class);
            assertInactive(collect(dependencies1, dependencies2, dependencies5), annotation1);
        });

        // add ServiceInterface2
        test(() -> {
            serviceAdded(ServiceInterface2.class, null, reference2, service2);

            // start components that require service 2 only
            Component5Service2.delegate.start();
            Component2Service2.delegate.start();

            // start components that require both services
            Component1Service12.delegate.start();

            verify(event(listeners[1].listener(), ServiceEvent.REGISTERED, reference2));

            assertFailed();
            assertActive(StatusCheck.class,
                         Component1Service12.class,
                         Component2Service2.class,
                         Component3Service1.class,
                         Component4Service1.class,
                         Component5Service2.class);
            assertInactive(Collections.emptyMap());
        });

        // remove ServiceInterface1
        test(() -> {
            serviceRemoved(ServiceInterface1.class, null, reference1);

            Component1Service12.delegate.stop();
            Component3Service1.delegate.stop();
            Component4Service1.delegate.stop();

            verify(event(listeners[0].listener(), ServiceEvent.UNREGISTERING, reference1));

            assertFailed();
            assertActive(StatusCheck.class, Component2Service2.class, Component5Service2.class);
            assertInactive(collect(dependencies1, dependencies3, dependencies4), annotation2);
        });

        // add ServiceInterface1
        test(() -> {
            serviceAdded(ServiceInterface1.class, null, reference1, service1);

            // start components that require service 1 only
            Component3Service1.delegate.start();
            Component4Service1.delegate.start();

            // start components that require both services
            Component1Service12.delegate.start();

            verify(event(listeners[0].listener(), ServiceEvent.REGISTERED, reference1));

            assertFailed();
            assertActive(StatusCheck.class,
                         Component1Service12.class,
                         Component2Service2.class,
                         Component3Service1.class,
                         Component4Service1.class,
                         Component5Service2.class);
            assertInactive(Collections.emptyMap());
        });

        // remove ServiceInterface2
        test(() -> {
            serviceRemoved(ServiceInterface2.class, null, reference2);

            Component1Service12.delegate.stop();
            Component2Service2.delegate.stop();
            Component5Service2.delegate.stop();

            verify(event(listeners[1].listener(), ServiceEvent.UNREGISTERING, reference2));

            assertFailed();
            assertActive(StatusCheck.class, Component3Service1.class, Component4Service1.class);
            assertInactive(collect(dependencies1, dependencies2, dependencies5), annotation1);
        });

        // remove ServiceInterface1
        test(() -> {
            serviceRemoved(ServiceInterface1.class, null, reference1);

            Component3Service1.delegate.stop();
            Component4Service1.delegate.stop();

            verify(event(listeners[0].listener(), ServiceEvent.UNREGISTERING, reference1));

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies1, dependencies2, dependencies3, dependencies4, dependencies5));
        });

        // add ServiceInterface2
        test(() -> {
            serviceAdded(ServiceInterface2.class, null, reference2, service2);

            // start components that require service 2 only
            Component2Service2.delegate.start();
            Component5Service2.delegate.start();

            verify(event(listeners[1].listener(), ServiceEvent.REGISTERED, reference2));

            assertFailed();
            assertActive(StatusCheck.class, Component2Service2.class, Component5Service2.class);
            assertInactive(collect(dependencies1, dependencies3, dependencies4), annotation2);
        });

        // stop
        test(() -> {
            context.removeServiceListener(listeners[0].listener());
            context.removeServiceListener(listeners[1].listener());

            Component2Service2.delegate.stop();
            Component5Service2.delegate.stop();

            verify(() -> services.get().stop());
        });
    }

    @Test
    public void testFailingComponents() throws Exception {
        FailingComponent.delegate = component6;

        final Deferred.Reference<BundleComponentContainerImpl.Logic> services = discover(StatusCheck.class, FailingComponent.class, Component4Service1.class);

        final ServiceImpl annotation = new ServiceImpl(ServiceInterface1.class);
        final Map.Entry<Class<?>, Set<Service>> dependencies1 = dependencies(FailingComponent.class, annotation);
        final Map.Entry<Class<?>, Set<Service>> dependencies2 = dependencies(Component4Service1.class, annotation);

        // one listener per service specification
        final ListenerSpec listener = expectListenerRegistration();

        // no services yet
        test(() -> {
            noServices(ServiceInterface1.class, null);

            verify(() -> services.get().start());

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies1, dependencies2));
        });

        // add ServiceInterface1
        test(() -> {
            serviceAdded(ServiceInterface1.class, null, reference1, service1);

            // start components that require service 1 only
            Component4Service1.delegate.start();

            verify(event(listener.listener(), ServiceEvent.REGISTERED, reference1));

            assertFailed(FailingComponent.class);
            assertActive(StatusCheck.class, Component4Service1.class);
            assertInactive(Collections.emptyMap());
        });

        // remove ServiceInterface1
        test(() -> {
            serviceRemoved(ServiceInterface1.class, null, reference1);

            Component4Service1.delegate.stop();

            verify(event(listener.listener(), ServiceEvent.UNREGISTERING, reference1));

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies1, dependencies2));
        });

        // add ServiceInterface1
        test(() -> {
            serviceAdded(ServiceInterface1.class, null, reference1, service1);

            // start components that require service 1 only
            Component4Service1.delegate.start();

            verify(event(listener.listener(), ServiceEvent.REGISTERED, reference1));

            assertFailed(FailingComponent.class);
            assertActive(StatusCheck.class, Component4Service1.class);
            assertInactive(Collections.emptyMap());
        });

        // stop
        test(() -> {
            context.removeServiceListener(listener.listener());

            Component4Service1.delegate.stop();

            verify(() -> services.get().stop());
        });
    }

    @Test
    public void testFilterDependency() throws Exception {
        MultipleServiceFiltersComponent.delegate = component6;

        final String selector1 = "filter-1";
        final String selector2 = "filter-2";

        final Deferred.Reference<BundleComponentContainerImpl.Logic> services = discover(Arrays.asList(selector1, selector2), StatusCheck.class, MultipleServiceFiltersComponent.class);

        final ServiceImpl annotation1 = new ServiceImpl(ServiceInterface1.class, selector1);
        final ServiceImpl annotation2 = new ServiceImpl(ServiceInterface1.class, selector2);
        final Map.Entry<Class<?>, Set<Service>> dependencies = dependencies(MultipleServiceFiltersComponent.class, annotation1, annotation2);

        // one listener per service specification
        final ServiceListener[] listeners = test(() -> {
            final List<ListenerSpec> _listeners = Arrays.asList(expectListenerRegistration(), expectListenerRegistration());

            // no services yet
            noServices(ServiceInterface1.class, selector1);
            noServices(ServiceInterface1.class, selector2);

            verify(() -> services.get().start());

            final ListenerSpec listener1 = find(_listeners, ServiceInterface1.class, selector1);
            final ListenerSpec listener2 = find(_listeners, ServiceInterface1.class, selector2);

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies));

            assert listener1 != null;
            assert listener2 != null;
            return new ServiceListener[] { listener1.listener(), listener2.listener() };
        });

        // add service with filter 1
        test(() -> {
            serviceAdded(ServiceInterface1.class, selector1, reference1, service1);

            verify(event(listeners[0], ServiceEvent.REGISTERED, reference1));

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies), annotation1);
        });

        // add service with filter 2
        test(() -> {
            serviceAdded(ServiceInterface1.class, selector2, reference3, service3);

            // start the component
            MultipleServiceFiltersComponent.delegate.start();

            verify(event(listeners[1], ServiceEvent.REGISTERED, reference3));

            assertFailed();
            assertActive(StatusCheck.class, MultipleServiceFiltersComponent.class);
            assertInactive(Collections.emptyMap());
        });

        // remove service with filter 1
        test(() -> {
            serviceRemoved(ServiceInterface1.class, selector1, reference1);

            MultipleServiceFiltersComponent.delegate.stop();

            verify(event(listeners[0], ServiceEvent.UNREGISTERING, reference1));

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies), annotation2);
        });

        // add service with filter 1
        test(() -> {
            serviceAdded(ServiceInterface1.class, selector1, reference1, service1);

            // start the component
            MultipleServiceFiltersComponent.delegate.start();

            verify(event(listeners[0], ServiceEvent.REGISTERED, reference1));

            assertFailed();
            assertActive(StatusCheck.class, MultipleServiceFiltersComponent.class);
            assertInactive(Collections.emptyMap());
        });

        // remove service with filter 2
        test(() -> {
            serviceRemoved(ServiceInterface1.class, selector2, reference3);

            MultipleServiceFiltersComponent.delegate.stop();

            verify(event(listeners[1], ServiceEvent.UNREGISTERING, reference3));

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies), annotation1);
        });

        // remove service with filter 1
        test(() -> {
            serviceRemoved(ServiceInterface1.class, selector1, reference1);

            verify(event(listeners[0], ServiceEvent.UNREGISTERING, reference1));

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies));
        });

        // add service with filter 2
        test(() -> {
            serviceAdded(ServiceInterface1.class, selector2, reference3, service3);

            verify(event(listeners[1], ServiceEvent.REGISTERED, reference3));

            assertFailed();
            assertActive(StatusCheck.class);
            assertInactive(collect(dependencies), annotation2);
        });

        // add service with filter 1
        test(() -> {
            serviceAdded(ServiceInterface1.class, selector1, reference1, service1);

            // start the component
            MultipleServiceFiltersComponent.delegate.start();

            verify(event(listeners[0], ServiceEvent.REGISTERED, reference1));

            assertFailed();
            assertActive(StatusCheck.class, MultipleServiceFiltersComponent.class);
            assertInactive(Collections.emptyMap());
        });

        // stop
        test(() -> {
            context.removeServiceListener(listeners[0]);
            context.removeServiceListener(listeners[1]);

            MultipleServiceFiltersComponent.delegate.stop();

            verify(() -> services.get().stop());
        });
    }

    private Task event(final ServiceListener listener, final int event, final ServiceReference reference) {
        return () -> listener.serviceChanged(new ServiceEvent(event, reference));
    }

    private void noServices(final Class<?> api, final String selector) throws InvalidSyntaxException {
        EasyMock.expect(context.getServiceReferences(api, selector)).andReturn(Collections.emptyList());
    }

    private void resolveService(final ServiceReference reference, final Object service) {
        EasyMock.expect(context.getService(reference)).andReturn(service);
    }

    private <T> void serviceAdded(final Class<T> api, final String selector, final ServiceReference<T> reference, final T service) throws InvalidSyntaxException {
        EasyMock.expect(context.getServiceReferences(api, selector)).andReturn(Collections.singleton(reference));
        resolveService(reference, service);
    }

    private <T> void serviceRemoved(final Class<T> api, final String selector, final ServiceReference<T> reference) throws InvalidSyntaxException {
        noServices(api, selector);
        EasyMock.expect(context.ungetService(reference)).andReturn(false);
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
        assert StatusCheck.component != null : BundleComponents.Status.class;
        final List<Class<?>> expected = Arrays.asList(components);
        final Collection<Class<?>> actual = StatusCheck.component.active();
        assert Objects.equals(new HashSet<>(actual), new HashSet<>(expected)) : String.format("Expected %s, actual %s", expected, actual);
    }

    private void assertFailed(final Class<?>... components) {
        assert StatusCheck.component != null : BundleComponents.Status.class;
        final List<Class<?>> expected = Arrays.asList(components);
        final Collection<Class<?>> actual = StatusCheck.component.failed();
        assert Objects.equals(new HashSet<>(actual), new HashSet<>(expected)) : String.format("Expected %s, actual %s", expected, actual);
    }

    private Map.Entry<Class<?>, Set<Service>> dependencies(final Class<?> component, final Service... services) {
        return new AbstractMap.SimpleEntry<>(component, new HashSet<>(Arrays.asList(services)));
    }

    private Map<Class<?>, Set<Service>> collect(final Map.Entry<Class<?>, Set<Service>>... entries) {
        final Map<Class<?>, Set<Service>> map = new HashMap<>();

        for (final Map.Entry<Class<?>, Set<Service>> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }

    private void assertInactive(final Map<Class<?>, Set<Service>> original, final Service... active) {
        assert StatusCheck.component != null : BundleComponents.Status.class;

        final Map<Class<?>, Set<Service>> expected = new HashMap<>();
        for (final Map.Entry<Class<?>, Set<Service>> entry : original.entrySet()) {
            expected.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        final List<Service> resolved = Arrays.asList(active);

        for (final Set<Service> dependencies : expected.values()) {
            dependencies.removeAll(resolved);
        }

        final Map<Class<?>, Collection<Service>> actual = StatusCheck.component.inactive();
        assert Objects.equals(actual.keySet(), expected.keySet()) : String.format("Expected %s, actual %s", expected, actual);
        for (final Map.Entry<Class<?>, Collection<Service>> entry : actual.entrySet()) {
            final Class<?> type = entry.getKey();
            assert Objects.equals(new HashSet<>(entry.getValue()), expected.get(type)) : String.format("Expected %s, actual %s", expected, actual);
        }
    }

    private Deferred.Reference<BundleComponentContainerImpl.Logic> discover(final Class... types) throws Exception {
        EasyMock.expect(context.getBundle()).andReturn(bundle);
        EasyMock.expect(bundle.getSymbolicName()).andReturn("test-bundle");
        EasyMock.expect(discovery.findComponentClasses(BundleComponents.Managed.class, BundleComponentContainerImpl.class.getClassLoader(), false)).andReturn(types);

        return Deferred.local(() -> new BundleComponentContainerImpl.Logic(context, discovery, log, container, loader));
    }

    private Deferred.Reference<BundleComponentContainerImpl.Logic> discover(final List<String> filters, final Class... types) throws Exception {
        EasyMock.expect(context.getBundle()).andReturn(bundle);
        EasyMock.expect(bundle.getSymbolicName()).andReturn("test-bundle");
        EasyMock.expect(discovery.findComponentClasses(BundleComponents.Managed.class, BundleComponentContainerImpl.class.getClassLoader(), false)).andReturn(types);

        for (final String filter : filters) {
            EasyMock.expect(context.createFilter(filter)).andReturn(null);
        }

        return Deferred.local(() -> new BundleComponentContainerImpl.Logic(context, discovery, log, container, loader));
    }

    private void checkFilter(final ListenerSpec listener, final Class<?> api, final String filter) {
        final String actual = listener.filter();

        final String expected = filter == null
                                 ? String.format("(%s=%s)", Constants.OBJECTCLASS, api.getName())
                                 : String.format("(&(%s=%s)%s)", Constants.OBJECTCLASS, api.getName(), filter);

        assert Objects.equals(expected, actual) : String.format("Expected '%s', actual '%s'", expected, actual);
    }

    private ListenerSpec expectListenerRegistration() throws Exception {
        final ListenerSpecImpl spec = new ListenerSpecImpl();

        context.addServiceListener(EasyMock.notNull(), EasyMock.notNull());
        EasyMock.expectLastCall().andAnswer(() -> {
            spec.listener = (ServiceListener) EasyMock.getCurrentArguments()[0];
            spec.filter = (String) EasyMock.getCurrentArguments()[1];
            return null;
        });

        return spec;
    }

    private interface ListenerSpec {

        ServiceListener listener();

        String filter();
    }

    private static class ListenerSpecImpl implements ListenerSpec {

        ServiceListener listener;
        String filter;

        public ServiceListener listener() {
            return listener;
        }

        public String filter() {
            return filter;
        }
    }

    private interface ServiceInterface1 extends BundleComponents.Registration { }

    private interface ServiceInterface2 extends BundleComponents.Registration { }

    @Component(automatic = false)
    private static final class Service1 implements ServiceInterface1 {

        private static ServiceInterface1 delegate;

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

    @Component(automatic = false)
    private static final class Service2 implements ServiceInterface1 {

        private static ServiceInterface1 delegate;

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

    @Component(automatic = false)
    private static final class ServiceDependent1 implements BundleComponents.Managed {

        private static BundleComponents.Managed delegate;

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

    @Component(automatic = false)
    private static final class ServiceDependent2 implements BundleComponents.Managed {

        private static BundleComponents.Managed delegate;

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

    private interface Consumer extends BundleComponents.Managed { }

    @Component(automatic = false)
    private static class EventSource implements BundleComponents.Registration.Listener<Consumer> {

        private static BundleComponents.Registration.Listener<Consumer> delegate;

        public Class<Consumer> type() {
            return delegate.type();
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

    @Component(automatic = false)
    private static class Component1Service12 implements BundleComponents.Managed {

        private static BundleComponents.Managed delegate;

        @SuppressWarnings("UnusedParameters")
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

    @Component(automatic = false)
    private static class Component2Service2 implements BundleComponents.Managed {

        private static BundleComponents.Managed delegate;

        @SuppressWarnings("UnusedParameters")
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

    @Component(automatic = false)
    private static class Component3Service1 implements BundleComponents.Managed {

        private static BundleComponents.Managed delegate;

        @SuppressWarnings("UnusedParameters")
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

    @Component(automatic = false)
    private static class Component4Service1 implements BundleComponents.Managed {

        private static BundleComponents.Managed delegate;

        @SuppressWarnings("UnusedParameters")
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

    @Component(automatic = false)
    private static class Component5Service2 implements BundleComponents.Managed {

        private static BundleComponents.Managed delegate;

        @SuppressWarnings("UnusedParameters")
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

    @Component(automatic = false)
    private static class FailingComponent implements BundleComponents.Managed {

        @SuppressWarnings("UnusedDeclaration")
        private static BundleComponents.Managed delegate;

        @SuppressWarnings("UnusedParameters")
        public FailingComponent(final @Service ServiceInterface1 service1) { }

        public void start() throws Exception {
            throw new UnsupportedOperationException("Failed");
        }

        public void stop() throws Exception {
            throw new UnsupportedOperationException("Failed");
        }
    }

    @Component(automatic = false)
    private static class MultipleServiceFiltersComponent implements BundleComponents.Managed {

        private static BundleComponents.Managed delegate;

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

    @Component(automatic = false)
    private static class StatusCheck implements BundleComponents.Managed {

        static ComponentStatus component;

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
