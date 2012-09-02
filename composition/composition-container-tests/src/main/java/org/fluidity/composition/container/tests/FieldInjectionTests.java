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

package org.fluidity.composition.container.tests;

import org.fluidity.composition.Component;
import org.fluidity.composition.Inject;
import org.fluidity.composition.Optional;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class FieldInjectionTests extends AbstractContainerTests {

    public FieldInjectionTests(final ArtifactFactory factory) {
        super(factory);
    }

    @Test
    public void testFieldInjection() throws Exception {
        registry.bindComponent(AbstractContainerTests.Value.class);
        registry.bindComponent(DependentValue.class);
        registry.bindComponent(FieldInjected.class);
        registry.bindComponent(SuperFieldInjected.class);

        final FieldInjected injected = container.getComponent(FieldInjected.class);
        assert injected != null;
        injected.verify();

        final FieldInjected superInjected = container.getComponent(SuperFieldInjected.class);
        assert superInjected != null;
        superInjected.verify();
    }

    @Test
    public void testFieldInjectionOfInstance() throws Exception {
        registry.bindComponent(AbstractContainerTests.Value.class);
        registry.bindComponent(DependentValue.class);

        final FieldInjected injected = new FieldInjected(container.getComponent(DependentKey.class));

        container.initialize(injected);
        injected.verify();
    }

    @Test
    public void testSelfDependencyViaField() throws Exception {
        registry.bindComponent(SelfDependentImpl.class);

        final SelfDependentImpl injected = (SelfDependentImpl) container.getComponent(SelfDependent.class);
        assert injected != null;
        injected.verify();
    }

    /**
     * A class that has both constructor injected and field injected dependencies.
     */
    public static class FieldInjected {

        @Optional
        @Inject
        private Key dependency1;

        private final DependentKey dependency3;

        public FieldInjected(final DependentKey dependency3) {
            this.dependency3 = dependency3;
        }

        public void verify() {
            assert dependency1 != null : "Field injection did not work on interface";
            assert dependency3 != null : "Construction injection did not work";
        }
    }

    private static class SuperFieldInjected extends FieldInjected {

        public SuperFieldInjected(final DependentKey dependency) {
            super(dependency);
        }
    }

    public interface SelfDependent { }

    /**
     * A class that has a field dependency to itself.
     */
    @Component(automatic = false)
    public static class SelfDependentImpl implements SelfDependent {

        @Inject
        private SelfDependent self;

        @Optional
        @Inject
        private DependentKey key;

        public void verify() {
            assert self != null : "Self injection did not work";
            assert key == null : "Optional dependency set";
        }
    }
}
