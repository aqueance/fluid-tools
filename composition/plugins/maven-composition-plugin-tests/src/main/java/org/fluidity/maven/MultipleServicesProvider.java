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
import org.fluidity.composition.ServiceProvider;

@ServiceProvider(api = { Service1.class, Service2.class })
public interface MultipleServicesProvider extends Service1, Service2 {

}

class MultipleServicesProviderImpl implements MultipleServicesProvider {

}

@Component
class MultipleServicesConsumer {

    MultipleServicesConsumer(@ServiceProvider final Service1[] providers1, @ServiceProvider final Service2[] providers2) {
        assert providers1.length == 1 : providers1.length;
        assert providers2.length == 1 : providers2.length;

//        TODO: turn this back on
//        assert providers1[0] == providers2[0];
    }
}

interface Service1 {

}

interface Service2 {

}
