/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package org.fluidity.composition.pico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ConstructorParameter;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.foundation.Logging;
import org.fluidity.foundation.logging.StandardOutLogging;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.Parameter;
import org.picocontainer.PicoContainer;
import org.picocontainer.PicoInitializationException;
import org.picocontainer.PicoIntrospectionException;
import org.picocontainer.PicoVisitor;
import org.picocontainer.defaults.CachingComponentAdapter;
import org.picocontainer.defaults.CachingComponentAdapterFactory;
import org.picocontainer.defaults.ComponentAdapterFactory;
import org.picocontainer.defaults.ConstructorInjectionComponentAdapterFactory;
import org.picocontainer.defaults.DecoratingComponentAdapter;
import org.picocontainer.defaults.DefaultPicoContainer;
import org.picocontainer.defaults.ImplementationHidingComponentAdapter;

/**
 * Uses PicoContainer.
 *
 * @author Tibor Varga
 */
final class PicoComponentContainer implements OpenComponentContainer {

    private final Logging log = new StandardOutLogging(null);

    private static ComponentAdapterFactory defaultAdapterFactory
        = new ConstructorInjectionComponentAdapterFactory(true);

    private static final ComponentAdapterFactory singletonAdapterFactory
        = new CachingComponentAdapterFactory(defaultAdapterFactory);

    private final ComponentContainer.Registry registry = new PicoComponentRegistry();

    private final MutablePicoContainer pico;

    private final Set<Class> resolvedDependencies = new HashSet<Class>();

    private final Map<Class, List<Class>> unresolvedDependencies = new HashMap<Class, List<Class>>();

    public PicoComponentContainer() {
        this(new DefaultPicoContainer(singletonAdapterFactory));
    }

    private PicoComponentContainer(MutablePicoContainer pico) {
        this.pico = pico;

        // add ourselves to the container and quarantee that we are read only when accessed through the container
        registry.bind(ComponentContainer.class, new ComponentContainer() {
            public <T> T getComponent(Class<T> componentClass) {
                return PicoComponentContainer.this.getComponent(componentClass);
            }

            public <T> T getComponent(Class<T> componentClass, Bindings bindings) {
                return PicoComponentContainer.this.getComponent(componentClass, bindings);
            }

            public OpenComponentContainer makeNestedContainer() {
                return PicoComponentContainer.this.makeNestedContainer();
            }

            @Override
            public String toString() {
                return PicoComponentContainer.this.toString();
            }
        });
    }

    protected void finalize() throws Throwable {
        super.finalize();

        if (pico.getParent() != null) {
            ((MutablePicoContainer) pico.getParent()).removeChildContainer(pico);
        }
    }

    @SuppressWarnings({ "unchecked" })
    public <T> T getComponent(Class<T> componentClass) {
        T component = (T) pico.getComponentInstance(componentClass);
        Package componentPackage = componentClass.getPackage();
        return component == null && (componentPackage == null || !componentPackage.getName().startsWith("java."))
            ? (T) pico.getComponentInstanceOfType(componentClass)
            : component;
    }

    public <T> T getComponent(Class<T> componentClass, Bindings bindings) {
        OpenComponentContainer nestedContainer = makeNestedContainer();
        bindings.registerComponents(nestedContainer.getRegistry());
        return nestedContainer.getComponent(componentClass);
    }

    public OpenComponentContainer makeNestedContainer() {
        return new PicoComponentContainer(new DefaultPicoContainer(singletonAdapterFactory, pico));
    }

    public Registry getRegistry() {
        return registry;
    }

    public Map<Class, List<Class>> getUnresolvedDependencies() {
        HashMap<Class, List<Class>> answer = new HashMap<Class, List<Class>>(unresolvedDependencies);
        answer.keySet().removeAll(resolvedDependencies);
        return answer;
    }

    public String toString() {
        List<String> chain = new LinkedList<String>();

        PicoContainer pc = pico;
        do {
            String pcs = pc.toString();
            chain.add(pcs.substring(pcs.lastIndexOf("@") + 1));
            pc = pc.getParent();
        } while (pc != null);

        StringBuilder id = new StringBuilder();

        final String delimiter = " > ";

        for (String link : chain) {
            id.append(delimiter).append(link);
        }

        id.replace(0, delimiter.length(), "container ");
        return id.toString();
    }

    private class PicoComponentRegistry implements Registry {

        public void requireDependency(Class dependencyInterface, Class dependentClass) {
            assert dependencyInterface != null;
            assert dependentClass != null;

            if (pico.getComponentAdapterOfType(dependencyInterface) == null) {
                List<Class> dependents = unresolvedDependencies.get(dependencyInterface);

                if (dependents == null) {
                    unresolvedDependencies.put(dependencyInterface, dependents = new ArrayList<Class>());
                }

                dependents.add(dependentClass);
            }
        }

        @SuppressWarnings({ "unchecked" })
        public <T> void bind(Class<? extends T> implementation) {
            bind(implementation, (Class) implementation);
        }

        public <T> void bind(Class<T> key, Class<? extends T> implementation, ConstructorParameter... parameters) {
            bind(key, implementation, true, false, false, parameters);
        }

        public <T> void bind(Class<T> key,
                             Class<? extends T> implementation,
                             boolean singleton,
                             boolean thread,
                             boolean deferred,
                             ConstructorParameter... parameters) {
            Parameter[] params = parameters != null && parameters.length > 0 ? new Parameter[parameters.length] : null;
            StringBuffer report = new StringBuffer();

            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    assert params != null;
                    params[i] = (Parameter) parameters[i].representation();

                    if (i > 0) {
                        report.append(", ");
                    }

                    report.append(parameters[i]);
                }
            }

            log.info(getClass(), this + ": registering " + implementation + " for " + key
                + (params != null ? " with parameter(s) " + report : ""));
            registerAdapter(key, implementation, deferred, singleton, thread, params);
        }

        public <T> void bind(final Class<T> key,
                             final Class<? extends T> implementation,
                             boolean singleton,
                             boolean thread,
                             boolean deferred,
                             final ComponentFactory<T> factory) {
            ComponentAdapter adapter = new ComponentFactoryInstanceAdapter<T>(key, implementation, factory);
            registerAdapter(key, adapter, singleton, thread, deferred);
        }

        public <T> void bind(Class<T> key,
                             Class<? extends T> implementation,
                             boolean singleton,
                             boolean thread,
                             boolean deferred,
                             Class<? extends ComponentFactory<T>> factory) {
            ComponentAdapter adapter = new ComponentFactoryClassAdapter<T>(key, implementation, factory);
            requireDependency(key, factory);
            registerAdapter(key, adapter, singleton, thread, deferred);
        }

        public <T> void bind(Class<? super T> key, T instance) {
            String value = instance instanceof String || instance instanceof Number
                ? ('\'' + String.valueOf(instance) + '\'')
                : (instance == null ? null : "instance of " + instance.getClass());
            log.info(getClass(), this + ": registering " + value + " for '" + key + "'");
            pico.registerComponentInstance(key, instance);
        }

        public OpenComponentContainer makeNestedContainer() {
            return new PicoComponentContainer(pico.makeChildContainer());
        }

        public <T> OpenComponentContainer makeNestedContainer(Class<T> key,
                                                              Class<? extends T> implementation,
                                                              ConstructorParameter... parameters) {
            return makeNestedContainer(key, implementation, true, false, false, parameters);
        }

        public <T> OpenComponentContainer makeNestedContainer(Class<T> key,
                                                              Class<? extends T> implementation,
                                                              boolean singleton,
                                                              boolean thread,
                                                              boolean deferred,
                                                              ConstructorParameter... parameters) {
            OpenComponentContainer container = makeNestedContainer(key);
            container.getRegistry().bind(key, implementation, singleton, thread, deferred, parameters);
            return container;
        }

        public <T> OpenComponentContainer makeNestedContainer(Class<T> key,
                                                              Class<? extends T> implementation,
                                                              boolean singleton,
                                                              boolean thread,
                                                              boolean deferred,
                                                              ComponentFactory<T> factory) {
            OpenComponentContainer container = makeNestedContainer(key);
            container.getRegistry().bind(key, implementation, singleton, thread, deferred, factory);
            return container;
        }

        public <T> OpenComponentContainer makeNestedContainer(Class<T> key,
                                                              Class<? extends T> implementation,
                                                              boolean singleton,
                                                              boolean thread,
                                                              boolean deferred,
                                                              Class<? extends ComponentFactory<T>> factory) {
            OpenComponentContainer container = makeNestedContainer(key);
            container.getRegistry().bind(key, implementation, singleton, thread, deferred, factory);
            return container;
        }

        private <T> void registerAdapter(Class<T> key,
                                         Class<? extends T> implementation,
                                         boolean deferred,
                                         boolean singleton,
                                         boolean thread,
                                         Parameter[] parameters) {
            registerAdapter(key, defaultAdapterFactory.createComponentAdapter(key, implementation, parameters),
                singleton, thread, deferred);
        }

        private <T> void registerAdapter(Class<T> key,
                                         ComponentAdapter adapter,
                                         boolean singleton,
                                         boolean thread,
                                         boolean deferred) {
            if (deferred) {
                adapter = new ImplementationHidingComponentAdapter(adapter, true);
            }

            if (singleton) {
                adapter = thread
                    ? new ThreadLocalComponentAdapter(adapter)
                    : new CachingComponentAdapter(adapter);
            }

            pico.registerComponent(adapter);
            resolvedDependencies.add(key);
        }

        private OpenComponentContainer makeNestedContainer(Class key) {
            MutablePicoContainer nested = pico.makeChildContainer();
            pico.registerComponent(new LinkingComponentAdapter(nested, key, key));
            resolvedDependencies.add(key);
            return new PicoComponentContainer(nested);
        }

        public ConstructorParameter component(Class key) {
            return ConstructorParameterImpl.componentParameter(key);
        }

        public ConstructorParameter constant(Object value) {
            return ConstructorParameterImpl.constantParameter(value);
        }

        public ConstructorParameter constant(char value) {
            return constant(new Character(value));
        }

        public ConstructorParameter constant(byte value) {
            return constant(new Byte(value));
        }

        public ConstructorParameter constant(short value) {
            return constant(new Short(value));
        }

        public ConstructorParameter constant(int value) {
            return constant(new Integer(value));
        }

        public ConstructorParameter constant(long value) {
            return constant(new Long(value));
        }

        public ConstructorParameter constant(boolean value) {
            return constant(Boolean.valueOf(value));
        }

        public ConstructorParameter array(Class componentClass) {
            return ConstructorParameterImpl.arrayParameter(componentClass);
        }

        public String toString() {
            return PicoComponentContainer.this.toString();
        }

        private class ComponentFactoryClassAdapter<T> implements ComponentAdapter {

            private final Class<T> key;

            private final Class<? extends T> implementation;

            private final Class<? extends ComponentFactory<T>> factoryClass;

            public ComponentFactoryClassAdapter(Class<T> key,
                                                Class<? extends T> implementation,
                                                Class<? extends ComponentFactory<T>> factoryClass) {
                this.key = key;
                this.implementation = implementation;
                this.factoryClass = factoryClass;
            }

            public Object getComponentKey() {
                return key;
            }

            public Class getComponentImplementation() {
                return implementation;
            }

            @SuppressWarnings({ "unchecked" })
            public Object getComponentInstance(PicoContainer picoContainer)
                throws PicoInitializationException, PicoIntrospectionException {
                ComponentFactory<T> factory = (ComponentFactory<T>) picoContainer.getComponentInstance(factoryClass);
                assert factory != null : factoryClass;
                return factory.makeComponent(PicoComponentContainer.this);
            }

            public void verify(PicoContainer picoContainer) throws PicoIntrospectionException {
                // empty
            }

            public void accept(PicoVisitor picoVisitor) {
                // empty
            }
        }

        private class ComponentFactoryInstanceAdapter<T> implements ComponentAdapter {

            private final Class<T> key;

            private final Class<? extends T> implementation;

            private final ComponentFactory<T> factory;

            public ComponentFactoryInstanceAdapter(Class<T> key,
                                                   Class<? extends T> implementation,
                                                   ComponentFactory<T> factory) {
                this.key = key;
                this.implementation = implementation;
                this.factory = factory;
            }

            public Object getComponentKey() {
                return key;
            }

            public Class getComponentImplementation() {
                return implementation;
            }

            public Object getComponentInstance(PicoContainer picoContainer)
                throws PicoInitializationException, PicoIntrospectionException {
                return factory.makeComponent(PicoComponentContainer.this);
            }

            public void verify(PicoContainer picoContainer) throws PicoIntrospectionException {
                // empty
            }

            public void accept(PicoVisitor picoVisitor) {
                // empty
            }
        }
    }

    private static class ThreadLocalComponentAdapter extends DecoratingComponentAdapter {

        private final ThreadLocal<Object> cache = new ThreadLocal<Object>();

        public ThreadLocalComponentAdapter(ComponentAdapter componentAdapter) {
            super(componentAdapter);
        }

        public synchronized Object getComponentInstance(PicoContainer picoContainer)
            throws PicoInitializationException,
            PicoIntrospectionException {
            Object answer = cache.get();

            if (answer == null) {
                answer = super.getComponentInstance(picoContainer);
                cache.set(answer);
            }

            return answer;
        }
    }
}
