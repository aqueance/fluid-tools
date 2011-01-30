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
public final class StatefulComponentTests extends AbstractContainerTests {

    public StatefulComponentTests(final ContainerFactory factory) {
        super(factory);
    }

    @Test
    public void standaloneComponent() throws Exception {
        registry.bindComponent(StatefulComponent.class);

        final StatefulComponent instance1 = container.getComponent(StatefulComponent.class);
        final StatefulComponent instance2 = container.getComponent(StatefulComponent.class);

        assert instance1 != null;
        assert instance2 != null;
        assert instance1 != instance2;
    }

    @Test
    public void dependentComponent() throws Exception {
        registry.bindComponent(StatefulComponent.class);
        registry.bindComponent(StatelessComponent1.class);
        registry.bindComponent(StatelessComponent2.class);

        final StatefulComponent instance = container.getComponent(StatefulComponent.class);
        final StatelessComponent1 stateless1 = container.getComponent(StatelessComponent1.class);
        final StatelessComponent2 stateless2 = container.getComponent(StatelessComponent2.class);

        assert instance != null;
        assert stateless1 != null;
        assert stateless2 != null;
        assert stateless1.dependency != null;
        assert stateless2.dependency != null;
        assert stateless1.dependency != instance;
        assert stateless2.dependency != instance;
        assert stateless1.dependency != stateless2.dependency;
    }

    @Component(stateful = true)
    private static class StatefulComponent { }

    @Component
    private static class StatelessComponent1 {

        public final StatefulComponent dependency;

        private StatelessComponent1(final StatefulComponent dependency) {
            this.dependency = dependency;
        }
    }

    @Component
    private static class StatelessComponent2 {

        public final StatefulComponent dependency;

        private StatelessComponent2(final StatefulComponent dependency) {
            this.dependency = dependency;
        }
    }
}
