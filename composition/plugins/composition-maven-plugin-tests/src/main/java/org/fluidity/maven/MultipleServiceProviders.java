/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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
