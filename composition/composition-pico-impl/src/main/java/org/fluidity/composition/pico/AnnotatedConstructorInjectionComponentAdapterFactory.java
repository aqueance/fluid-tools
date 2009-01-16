/*****************************************************************************
 * Copyright (c) PicoContainer Organization. All rights reserved.            *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Idea by Rachel Davies, Original code by Aslak Hellesoy and Paul Hammant   *
 *****************************************************************************/

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

    public ComponentAdapter createComponentAdapter(final Object componentKey,
                                                   final Class componentImplementation,
                                                   final Parameter[] parameters)
        throws PicoIntrospectionException, AssignabilityRegistrationException, NotConcreteRegistrationException {
        return new AnnotatedConstructorInjectionComponentAdapter(componentKey, componentImplementation);
    }
}