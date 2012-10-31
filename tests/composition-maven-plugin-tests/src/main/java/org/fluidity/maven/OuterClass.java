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
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.Inject;
import org.fluidity.composition.Optional;

@Component
public class OuterClass {

    @Component
    public class InnerClass {
        private final ComponentContainer container;

        public InnerClass(final ComponentContainer container) {
            this.container = container;
        }

        public Object getLocal() {

            @Component
            class LocalClass { }

            class Reference {

                @Inject
                @Optional
                LocalClass local;
            }

            return container.instantiate(Reference.class).local;
        }
    }
}
