/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition;

import java.lang.reflect.Constructor;

import org.fluidity.composition.spi.DependencyResolver;

/**
 * Performs dependency injection.
 *
 * @author Tibor Varga
 */
public interface DependencyInjector {

    /**
     * Returns the argument array for the given constructor. The constructor must be satisfiable.
     *
     * @param resolver     the resolver to satisfy dependencies.
     * @param componentApi the interface implemented by the component.
     * @param context      the instantiation context of the object being constructed.
     * @param constructor  the constructor to find the arguments for.
     *
     * @return the argument array for the given constructor.
     */
    Object[] injectConstructor(DependencyResolver resolver, Class<?> componentApi, ComponentContext context, Constructor<?> constructor);

    /**
     * Sets all {@link Component} annotated fields of the receiver.
     *
     * @param resolver     the resolver to satisfy dependencies.
     * @param componentApi the interface implemented by the component.
     * @param context      the instantiation context of the object being constructed.
     * @param instance     the object to set the fields of.
     *
     * @return the received instance.
     */
    <T> T injectFields(DependencyResolver resolver, Class<?> componentApi, ComponentContext context, T instance);
}
