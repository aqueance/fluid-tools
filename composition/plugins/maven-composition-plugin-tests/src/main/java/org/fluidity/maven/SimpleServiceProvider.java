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

@ComponentGroup
public interface SimpleServiceProvider { }

interface ExtendedServiceProvider extends SimpleServiceProvider { }

@Component
class SimpleServiceConsumer {

    SimpleServiceConsumer(final @ComponentGroup SimpleServiceProvider[] providers) {
        assert providers.length == 7 : providers.length;
    }
}

class SimpleServiceProvider1 implements SimpleServiceProvider { }

class SimpleServiceProvider2 implements SimpleServiceProvider { }

class SimpleServiceProvider3 implements SimpleServiceProvider { }

class SimpleServiceProvider4 implements SimpleServiceProvider { }

@Component(automatic = false)
class SimpleServiceProvider5 implements SimpleServiceProvider { }

class SimpleServiceProvider6 extends SimpleServiceProvider1 { }

class SimpleServiceProvider7 extends SimpleServiceProvider6 { }

class SimpleServiceProvider8 implements ExtendedServiceProvider { }
