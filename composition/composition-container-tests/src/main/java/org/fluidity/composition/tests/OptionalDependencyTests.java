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

package org.fluidity.composition.tests;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.Optional;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class OptionalDependencyTests extends AbstractContainerTests {

    public OptionalDependencyTests(final ArtifactFactory factory) {
        super(factory);
    }

    @Test
    public void optionalDependencyNotResolved() throws Exception {
        registry.bindComponent(OptionalDependentValue.class);
        assert container.getComponent(OptionalDependentValue.class) != null : "Component with optional and missing dependency not instantiated";
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class)
    public void missingMandatoryDependencyRaised() throws Exception {
        registry.bindComponent(MandatoryDependentValue.class);
        container.getComponent(MandatoryDependentValue.class);      // must raise exception
    }

    private static class OptionalDependentValue {

        public OptionalDependentValue(@Optional final DependentKey dependent) {
            assert dependent == null : "Missing dependency could not possibly be instantiated";
        }
    }

    private static class MandatoryDependentValue {

        @SuppressWarnings("UnusedDeclaration")
        public MandatoryDependentValue(final DependentKey dependent) {
            assert false : "Should not have been instantiated";
        }
    }
}
