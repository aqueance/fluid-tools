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

package org.fluidity.maven;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentGroup;

@ComponentGroup(api = { Service1.class, Service2.class })
public interface MultipleServiceProvider extends Service1, Service2 { }

class MultipleServiceProviderImpl implements MultipleServiceProvider { }

@Component
class MultipleServicesConsumer {

    MultipleServicesConsumer(final @ComponentGroup Service1[] providers1, final @ComponentGroup Service2[] providers2) {
        assert providers1.length == 1 : providers1.length;
        assert providers2.length == 1 : providers2.length;

        assert providers1[0] == providers2[0];
    }
}

interface Service1 { }

interface Service2 { }
