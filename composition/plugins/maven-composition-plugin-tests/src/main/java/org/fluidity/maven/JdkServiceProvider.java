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

import org.fluidity.composition.ServiceProvider;

@ServiceProvider
public interface JdkServiceProvider {

}

class JdkServiceProviderImpl implements JdkServiceProvider {

}

class DefaultProvider {

    private final JdkServiceProvider provider1 = new NonStaticProvider();

    private final JdkServiceProvider provider2 = new JdkServiceProvider() {
        // anonymous inner class: should be ignored
    };

    public class NonStaticProvider implements JdkServiceProvider {
        // non-static inner class: should also be ignored
    }
}