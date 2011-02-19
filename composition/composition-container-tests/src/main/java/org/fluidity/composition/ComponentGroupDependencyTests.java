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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class ComponentGroupDependencyTests extends AbstractContainerTests {

    public ComponentGroupDependencyTests(final ContainerFactory factory) {
        super(factory);
    }

    @Test
    public void dependencyResolved() throws Exception {
        registry.bindComponent(Processor.class);
        registry.bindGroup(Filter.class, Filter1.class, Filter2.class);
        assert container.getComponent(Processor.class) != null;
    }

    @ComponentGroup
    public static interface Filter {

    }

    @Component
    public static class Processor {

        public Processor(final @ComponentGroup Filter[] filters) {
            assert filters != null : "Component group dependency was not resolved";
            assert filters.length == 2 : String.format("Component group dependency did not find all implementations: %s", Arrays.toString(filters));

            final Set<Class<? extends Filter>> provided = new HashSet<Class<? extends Filter>>();
            for (final Filter filter : filters) {
                provided.add(filter.getClass());
            }

            assert provided.equals(new HashSet<Class<? extends Filter>>(Arrays.asList(Filter1.class, Filter2.class))) : provided;
        }
    }

    public static class Filter1 implements Filter {

    }

    public static class Filter2 implements Filter {

    }
}
