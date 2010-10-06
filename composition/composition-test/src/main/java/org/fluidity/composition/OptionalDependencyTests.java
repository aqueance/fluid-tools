/*
 * Copyright (c) 2006-2010 Tibor Adam Varga (tibor.adam.varga on gmail)
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
public final class OptionalDependencyTests extends AbstractContainerTests {

    public OptionalDependencyTests(final ContainerFactory factory) {
        super(factory);
    }

    @Test
    public void optionalDependencyNotResolved() throws Exception {
        registry.bindDefault(OptionalDependentValue.class);
        assert container.getComponent(OptionalDependentValue.class) != null : "Component with optional and missing dependency not instantiated";
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class)
    public void missingMandatoryDependencyRaised() throws Exception {
        registry.bindDefault(MandatoryDependentValue.class);
        container.getComponent(MandatoryDependentValue.class);      // must raise exception
    }

    private static class OptionalDependentValue {

        public OptionalDependentValue(@Optional final DependentKey dependent) {
            assert dependent == null : "Missing dependency could not possibly be instantiated";
        }
    }

    private static class MandatoryDependentValue {

        @SuppressWarnings({ "UnusedDeclaration" })
        public MandatoryDependentValue(final DependentKey dependent) {
            assert false : "Should not have been instantiated";
        }
    }
}
