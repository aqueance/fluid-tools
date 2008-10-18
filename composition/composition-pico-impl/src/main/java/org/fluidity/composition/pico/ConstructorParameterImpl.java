/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import org.fluidity.composition.ConstructorParameter;
import org.picocontainer.Parameter;
import org.picocontainer.defaults.CollectionComponentParameter;
import org.picocontainer.defaults.ComponentParameter;
import org.picocontainer.defaults.ConstantParameter;

/**
 * @author Tibor Varga
 */
final class ConstructorParameterImpl implements ConstructorParameter {

    private final org.picocontainer.Parameter representation;

    static ConstructorParameter constantParameter(final Object value) {
        return new ConstructorParameterImpl(new ConstantParameter(value));
    }

    static ConstructorParameter componentParameter(final Class key) {
        return new ConstructorParameterImpl(new ComponentParameter(key));
    }

    static ConstructorParameter arrayParameter(final Class componentClass) {
        return new ConstructorParameterImpl(new CollectionComponentParameter(componentClass, null, true));
    }

    private ConstructorParameterImpl(final Parameter representation) {
        this.representation = representation;
    }

    public Object representation() {
        return representation;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConstructorParameter)) {
            return false;
        }

        final ConstructorParameter other = (ConstructorParameter) o;

        return (representation != null
            ? representation.equals(other.representation()) : other.representation() == null);
    }

    public int hashCode() {
        return representation != null ? representation.hashCode() : 0;
    }

    public String toString() {
        return representation == null ? null : representation.toString();
    }
}
