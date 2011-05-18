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

import java.util.Properties;

import org.fluidity.composition.ComponentContainer;
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
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public class WhiteboardImplTest extends MockGroupAbstractTest {

    private final BundleContext context = addControl(BundleContext.class);
    private final ComponentContainer container = addControl(ComponentContainer.class);
    private final OpenComponentContainer child = addControl(OpenComponentContainer.class);
    private final ComponentContainer.Registry registry = addControl(ComponentContainer.Registry.class);
    private final LogFactory logs = new NoLogFactory();

    private final ServiceInterface1 service1 = addControl(ServiceInterface1.class);
    private final ServiceInterface2 service2 = addControl(ServiceInterface2.class);

    private final ServiceRegistration registration = addControl(ServiceRegistration.class);

    private final ServiceReference reference1 = addControl(ServiceReference.class);
    private final ServiceReference reference2 = addControl(ServiceReference.class);

    private final Startable startable = addControl(Startable.class);
    private final Whiteboard.Stoppable stoppable = addControl(Whiteboard.Stoppable.class);

    private final Consumer consumer1 = addControl(Consumer.class);
    private final Consumer consumer2 = addControl(Consumer.class);

    @SuppressWarnings("unchecked")
    private final Whiteboard.EventSource<Consumer> source = addControl(Whiteboard.EventSource.class);

    @Test
    public void testServiceRegistration() throws Exception {

        final Properties properties = new Properties();

        properties.setProperty("property-1", "value-1");
        properties.setProperty("property-2", "value-2");

        EasyMock.expect(service1.properties()).andReturn(properties);
        EasyMock.expect(service1.types()).andReturn(new Class[] { ServiceInterface1.class });

        final Service1 service = new Service1(service1);

        // registering the service
        EasyMock.expect(context.registerService(EasyMock.aryEq(new String[] { ServiceInterface1.class.getName() }),
                                                EasyMock.same(service),
                                                EasyMock.same(properties))).andReturn(registration);

        // no dependencies, domain container is created and asked to invoke the start() method
        EasyMock.expect(container.invoke(service, Service1.class.getMethod("start"))).andReturn(stoppable);

        replay();
        final Whiteboard whiteboard = new WhiteboardImpl(context, container, logs, null, null, new Whiteboard.Registration[] { service });
        verify();

        // un-registering the service
        registration.unregister();
        stoppable.stop();

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
        final ServiceDependent1 dependent = new ServiceDependent1(startable);

        // no services yet
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null).anyTimes();

        final ListenerSpec spec = expectListenerRegistration();

        replay();
        final Whiteboard whiteboard = new WhiteboardImpl(context, container, logs, null, new Whiteboard.Component[] { dependent }, null);

        verify();

        final String filter1 = String.format("(|(%1$s=%2$s)(%1$s=%3$s))", Constants.OBJECTCLASS, ServiceInterface1.class.getName(), ServiceInterface2.class.getName());
        final String filter2 = String.format("(|(%1$s=%3$s)(%1$s=%2$s))", Constants.OBJECTCLASS, ServiceInterface1.class.getName(), ServiceInterface2.class.getName());

        final String filter = spec.filter();
        assert filter.equals(filter1) || filter.equals(filter2): filter;

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
        EasyMock.expect(child.invoke(dependent, ServiceDependent1.class.getMethod("start", ServiceInterface1.class, ServiceInterface2.class))).andReturn(stoppable);

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        verify();

        // responding to disappearance of the first service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 }).anyTimes();

        EasyMock.expect(context.ungetService(reference1)).andReturn(false);

        stoppable.stop();

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
        EasyMock.expect(child.invoke(dependent, ServiceDependent1.class.getMethod("start", ServiceInterface1.class, ServiceInterface2.class))).andReturn(stoppable);

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        // responding to disappearance of the second service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null).anyTimes();

        EasyMock.expect(context.ungetService(reference2)).andReturn(false);

        stoppable.stop();

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
        final ServiceDependent1 dependent = new ServiceDependent1(startable);

        final ListenerSpec spec = expectListenerRegistration();

        // first service already registered
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(null).anyTimes();

        EasyMock.expect(context.getService(reference1)).andReturn(service1);

        replay();
        final Whiteboard whiteboard = new WhiteboardImpl(context, container, logs, null, new Whiteboard.Component[] { dependent }, null);
        verify();

        final String filter1 = String.format("(|(%1$s=%2$s)(%1$s=%3$s))", Constants.OBJECTCLASS, ServiceInterface1.class.getName(), ServiceInterface2.class.getName());
        final String filter2 = String.format("(|(%1$s=%3$s)(%1$s=%2$s))", Constants.OBJECTCLASS, ServiceInterface1.class.getName(), ServiceInterface2.class.getName());

        final String filter = spec.filter();
        assert filter.equals(filter1) || filter.equals(filter2) : filter;

        // responding to appearance of the second service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(new ServiceReference[] { reference1 }).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 }).anyTimes();

        EasyMock.expect(context.getService(reference2)).andReturn(service2);

        EasyMock.expect(container.makeChildContainer()).andReturn(child);
        EasyMock.expect(child.getRegistry()).andReturn(registry);
        registry.bindInstance(service1, ServiceInterface1.class);
        registry.bindInstance(service2, ServiceInterface2.class);
        EasyMock.expect(child.invoke(dependent, ServiceDependent1.class.getMethod("start", ServiceInterface1.class, ServiceInterface2.class))).andReturn(stoppable);

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        verify();

        // responding to disappearance of the first service
        EasyMock.expect(context.getServiceReferences(ServiceInterface1.class.getName(), null)).andReturn(null).anyTimes();
        EasyMock.expect(context.getServiceReferences(ServiceInterface2.class.getName(), null)).andReturn(new ServiceReference[] { reference2 }).anyTimes();

        EasyMock.expect(context.ungetService(reference1)).andReturn(false);

        stoppable.stop();

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
        EasyMock.expect(child.invoke(dependent, ServiceDependent1.class.getMethod("start", ServiceInterface1.class, ServiceInterface2.class))).andReturn(stoppable);

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        // removing the listener
        context.removeServiceListener(spec.listener());

        stoppable.stop();

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
        final ServiceDependent2 dependent = new ServiceDependent2(startable);

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
        EasyMock.expect(child.invoke(dependent, ServiceDependent2.class.getMethod("start", ServiceInterface1.class, ServiceInterface2.class))).andReturn(stoppable);

        replay();
        final Whiteboard whiteboard = new WhiteboardImpl(context, container, logs, null, new Whiteboard.Component[] { dependent }, null);
        verify();

        final String filter1 = String.format("(|(&(%1$s=%2$s)%4$s)(&(%1$s=%3$s)%5$s))", Constants.OBJECTCLASS, ServiceInterface1.class.getName(), ServiceInterface2.class.getName(), selector1, selector2);
        final String filter2 = String.format("(|(&(%1$s=%3$s)%5$s)(&(%1$s=%2$s)%4$s))", Constants.OBJECTCLASS, ServiceInterface1.class.getName(), ServiceInterface2.class.getName(), selector1, selector2);

        final String filter = spec.filter();
        assert filter.equals(filter1) || filter.equals(filter2) : filter;

        // removing the listener
        context.removeServiceListener(spec.listener());

        stoppable.stop();

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
        final Source source = new Source(this.source);

        // service listener registration
        final ListenerSpec spec = expectListenerRegistration();

        EasyMock.expect(source.clientType()).andReturn(Consumer.class);
        EasyMock.expect(context.getServiceReferences(Consumer.class.getName(), null)).andReturn(new ServiceReference[0]);

        EasyMock.expect(container.invoke(source, Source.class.getMethod("start"))).andReturn(stoppable);

        replay();
        final Whiteboard whiteboard = new WhiteboardImpl(context, container, logs, new Whiteboard.EventSource<?>[] { source }, null, null);
        verify();

        assert spec.filter().equals(String.format("(%s=%s)", Constants.OBJECTCLASS, Consumer.class.getName()));

        // response to appearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);
        EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);

        this.source.clientAdded(EasyMock.same(consumer1), EasyMock.<Properties>eq(new Properties()));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        // response to appearance of second consumer
        EasyMock.expect(context.getService(reference2)).andReturn(consumer2);

        final Properties properties = new Properties();
        properties.setProperty("xxx", "yyy");
        EasyMock.expect(reference2.getPropertyKeys()).andReturn(new String[] { "xxx" });
        EasyMock.expect(reference2.getProperty("xxx")).andReturn(properties.getProperty("xxx"));

        this.source.clientAdded(EasyMock.same(consumer2), EasyMock.<Properties>eq(properties));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        verify();

        // response to disappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);

        this.source.clientRemoved(EasyMock.same(consumer1));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        // response to reappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);
        EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);

        this.source.clientAdded(EasyMock.same(consumer1), EasyMock.eq(new Properties()));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        // response to disappearance of second consumer
        EasyMock.expect(context.getService(reference2)).andReturn(consumer2);

        this.source.clientRemoved(EasyMock.same(consumer2));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference2));
        verify();

        // response to disappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);

        this.source.clientRemoved(EasyMock.same(consumer1));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        // removing the event source
        context.removeServiceListener(spec.listener());
        stoppable.stop();

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
        final Source source = new Source(this.source);

        // service listener registration
        final ListenerSpec spec = expectListenerRegistration();

        EasyMock.expect(context.getServiceReferences(Consumer.class.getName(), null)).andReturn(new ServiceReference[] { reference1 });
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);
        EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);

        EasyMock.expect(source.clientType()).andReturn(Consumer.class);
        source.clientAdded(EasyMock.same(consumer1), EasyMock.<Properties>notNull());

        EasyMock.expect(container.invoke(source, Source.class.getMethod("start"))).andReturn(stoppable);

        replay();
        final Whiteboard whiteboard = new WhiteboardImpl(context, container, logs, new Whiteboard.EventSource<?>[] { source }, null, null);
        verify();

        assert spec.filter().equals(String.format("(%s=%s)", Constants.OBJECTCLASS, Consumer.class.getName()));

        // response to appearance of second consumer
        EasyMock.expect(context.getService(reference2)).andReturn(consumer2);
        EasyMock.expect(reference2.getPropertyKeys()).andReturn(new String[0]);

        this.source.clientAdded(EasyMock.same(consumer2), EasyMock.eq(new Properties()));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference2));
        verify();

        // response to disappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);

        this.source.clientRemoved(EasyMock.same(consumer1));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reference1));
        verify();

        // response to reappearance of first consumer
        EasyMock.expect(context.getService(reference1)).andReturn(consumer1);
        EasyMock.expect(reference1.getPropertyKeys()).andReturn(new String[0]);

        this.source.clientAdded(EasyMock.same(consumer1), EasyMock.eq(new Properties()));

        replay();
        spec.listener().serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reference1));
        verify();

        // removing the event source
        context.removeServiceListener(spec.listener());
        stoppable.stop();

        replay();
        whiteboard.stop();
        verify();

        // no more action at second invocation

        replay();
        whiteboard.stop();
        verify();
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

    public static interface Startable {

        Whiteboard.Stoppable start(ServiceInterface1 service1, ServiceInterface2 service2);
    }

    public static interface ServiceInterface1 extends Whiteboard.Registration { }

    public static interface ServiceInterface2 extends Whiteboard.Registration { }

    public final class Service1 implements ServiceInterface1 {

        private final ServiceInterface1 service;

        public Service1(final ServiceInterface1 service) {
            this.service = service;
        }

        public Class[] types() {
            return service.types();
        }

        public Properties properties() {
            return service.properties();
        }

        @Whiteboard.Start
        public Whiteboard.Stoppable start() {
            return stoppable;
        }
    }

    public static final class Service2 implements ServiceInterface1 {

        private final ServiceInterface1 service;

        public Service2(final ServiceInterface1 service) {
            this.service = service;
        }

        public Class[] types() {
            return service.types();
        }

        public Properties properties() {
            return service.properties();
        }

        @Whiteboard.Start
        public Whiteboard.Stoppable start() {
            throw new UnsupportedOperationException();
        }
    }

    public static final class ServiceDependent1 implements Whiteboard.Component, Startable {

        private final Startable startable;

        public ServiceDependent1(final Startable startable) {
            this.startable = startable;
        }

        @Whiteboard.Start
        public Whiteboard.Stoppable start(final @Service ServiceInterface1 service1, final @Service ServiceInterface2 service2) {
            return startable.start(service1, service2);
        }
    }

    public static final class ServiceDependent2 implements Whiteboard.Component, Startable {

        private final Startable startable;

        public ServiceDependent2(final Startable startable) {
            this.startable = startable;
        }

        @Whiteboard.Start
        public Whiteboard.Stoppable start(final @Service(filter = "filter-1") ServiceInterface1 service1, final @Service(filter = "filter-2") ServiceInterface2 service2) {
            return startable.start(service1, service2);
        }
    }

    public static interface Consumer extends Whiteboard.Component { }

    public class Source implements Whiteboard.EventSource<Consumer> {

        private final Whiteboard.EventSource<Consumer> delegate;

        public Source(final Whiteboard.EventSource<Consumer> delegate) {
            this.delegate = delegate;
        }

        public Class<Consumer> clientType() {
            return delegate.clientType();
        }

        public void clientAdded(final Consumer consumer, final Properties properties) {
            delegate.clientAdded(consumer, properties);
        }

        public void clientRemoved(final Consumer consumer) {
            delegate.clientRemoved(consumer);
        }

        @Whiteboard.Start
        public Whiteboard.Stoppable start() {
            throw new UnsupportedOperationException();
        }
    }

}
