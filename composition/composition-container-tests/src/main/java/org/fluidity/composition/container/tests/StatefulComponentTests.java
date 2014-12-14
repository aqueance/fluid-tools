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
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.spi.ComponentFactory;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public final class StatefulComponentTests extends AbstractContainerTests {

    public StatefulComponentTests(final ArtifactFactory factory) {
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

    @Test
    public void testStatefulFactory() throws Exception {
        registry.bindComponent(StatefulComponentFactory.class);

        final StatelessComponent instance1 = container.getComponent(StatelessComponent.class);
        final StatelessComponent instance2 = container.getComponent(StatelessComponent.class);

        assert instance1 != null;
        assert instance2 != null;
        assert instance1 != instance2;
    }

    @Component(stateful = true, automatic = false)
    private static class StatefulComponent { }

    @Component(automatic = false)
    private static class StatelessComponent1 {

        public final StatefulComponent dependency;

        @SuppressWarnings("UnusedDeclaration")
        private StatelessComponent1(final StatefulComponent dependency) {
            this.dependency = dependency;
        }
    }

    @Component(automatic = false)
    private static class StatelessComponent2 {

        public final StatefulComponent dependency;

        @SuppressWarnings("UnusedDeclaration")
        private StatelessComponent2(final StatefulComponent dependency) {
            this.dependency = dependency;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class StatelessComponent { }

    @Component(api = StatelessComponent.class, stateful = true)
    private static class StatefulComponentFactory implements ComponentFactory {

        public Instance resolve(final ComponentContext context, final Resolver dependencies) throws Exception {

            return new Instance() {
                public void bind(final Registry registry) throws Exception {
                    registry.bindComponent(StatelessComponent.class);
                }
            };
        }
    }
}
