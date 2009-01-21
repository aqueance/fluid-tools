/*
 * Copyright (c) 2006-2009 Tibor Adam Varga (tibor.adam.varga on gmail)
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
 */
package org.fluidity.composition.pico;

import org.picocontainer.ComponentAdapter;
import org.picocontainer.Parameter;
import org.picocontainer.PicoIntrospectionException;
import org.picocontainer.defaults.AssignabilityRegistrationException;
import org.picocontainer.defaults.ConstructorInjectionComponentAdapterFactory;
import org.picocontainer.defaults.NotConcreteRegistrationException;

/**
 * Creates @{link AnnotatedConstructorInjectionComponentAdapter}s.
 */
public class AnnotatedConstructorInjectionComponentAdapterFactory extends ConstructorInjectionComponentAdapterFactory {

    private final AnnotatedFieldInjector fieldInjector;

    public AnnotatedConstructorInjectionComponentAdapterFactory(final AnnotatedFieldInjector fieldInjector) {
        super(true);
        this.fieldInjector = fieldInjector;
    }

    public ComponentAdapter createComponentAdapter(final Object componentKey, final Class componentImplementation, final Parameter[] parameters)
        throws PicoIntrospectionException, AssignabilityRegistrationException, NotConcreteRegistrationException {
        return new AnnotatedConstructorInjectionComponentAdapter(fieldInjector, componentKey, componentImplementation);
    }
}