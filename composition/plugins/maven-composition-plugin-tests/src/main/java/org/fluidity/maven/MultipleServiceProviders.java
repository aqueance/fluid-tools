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

package org.fluidity.maven;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentGroup;

public class MultipleServiceProviders implements ServiceProvider1, ServiceProvider2 {

    private final ServiceProvider1 ignored = new ServiceProvider1() { };

}

@Component
class MultipleServiceConsumer {

    MultipleServiceConsumer(final @ComponentGroup ServiceProvider1[] providers1, final @ComponentGroup ServiceProvider2[] providers2) {
        assert providers1.length == 1 : providers1.length;
        assert providers2.length == 1 : providers2.length;

        assert providers1[0] == providers2[0];
    }
}

@ComponentGroup
interface ServiceProvider1 { }

@ComponentGroup
interface ServiceProvider2 { }
