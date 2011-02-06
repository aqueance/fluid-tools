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

package org.fluidity.composition;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public final class FieldInjectionTests extends AbstractContainerTests {

    public FieldInjectionTests(final ContainerFactory factory) {
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
        @Component
        @SuppressWarnings("UnusedDeclaration")
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

    public static interface SelfDependent {

    }

    /**
     * A class that has a field dependency to itself.
     */
    public static class SelfDependentImpl implements SelfDependent {

        @Component
        @SuppressWarnings("UnusedDeclaration")
        private SelfDependent self;

        @Optional
        @Component
        @SuppressWarnings("UnusedDeclaration")
        private DependentKey key;

        public void verify() {
            assert self == this : "Self injection did not work";
            assert key == null : "Optional dependency set";
        }
    }
}
