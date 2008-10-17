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

import org.fluidity.tests.MockGroupAbstractTest;
import org.picocontainer.defaults.ComponentParameter;
import org.picocontainer.defaults.ConstantParameter;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ConstructorParameterImplTest extends MockGroupAbstractTest {

    @Test
    public void testAdaptation() throws Exception {
        assert ConstructorParameterImpl.componentParameter(Object.class).representation() instanceof ComponentParameter;
        assert ConstructorParameterImpl.constantParameter(new Object()).representation() instanceof ConstantParameter;
    }

/*
    @Test
    public void equality() throws Exception {
        Object objectValue = new Object();
        Class classValue = Object.class;

        ConstructorParameter parameter = ConstructorParameterImpl.constantParameter(objectValue);
        assert parameter.equals(parameter);

        assert parameter.equals(ConstructorParameterImpl.constantParameter(objectValue));
        assert ConstructorParameterImpl.componentParameter(classValue)
            .equals(ConstructorParameterImpl.componentParameter(classValue));

        assert !ConstructorParameterImpl.componentParameter(objectValue).equals(objectValue);
        assert !ConstructorParameterImpl.componentParameter(objectValue).equals(
            ConstructorParameterImpl.componentParameter(classValue));
    }

    @Test
    public void hash() throws Exception {
        Object objectValue = new Object();
        Class classValue = Object.class;

        assert ConstructorParameterImpl.componentParameter(objectValue).hashCode() ==
            ConstructorParameterImpl.componentParameter(objectValue).hashCode();
        assert ConstructorParameterImpl.componentParameter(classValue).hashCode() ==
            ConstructorParameterImpl.componentParameter(classValue).hashCode();
    }
*/
}
