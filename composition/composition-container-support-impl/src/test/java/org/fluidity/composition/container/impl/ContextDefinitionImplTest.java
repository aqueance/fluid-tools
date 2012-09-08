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

package org.fluidity.composition.container.impl;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.fluidity.composition.Component;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.testing.MockGroup;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("UnusedDeclaration")
public class ContextDefinitionImplTest extends MockGroup {

    @Test
    public void testExpansion() throws Exception {
        final ContextDefinition root = new ContextDefinitionImpl();

        assert root.active().isEmpty() : root;

        final ContextDefinition expanded1 = root.expand(Definition1.class.getAnnotations());

        assert expanded1 == root;
        assert expanded1.active().isEmpty();

        assert expanded1.reference() == null;

        final ContextDefinition accepted1 = expanded1.accept(ConsumerAll.class);

        assert accepted1 == expanded1;
        assert !accepted1.isEmpty();

        final Map<Class<? extends Annotation>, Annotation[]> map1 = accepted1.active();

        assert !map1.isEmpty() : AnnotationMaps.toString(map1);
        check(Accumulated.class, map1, "accumulated-1");
        check(Inherited.class, map1, "inherited-1");
        check(Immediate.class, map1, "immediate-1");
        check(None.class, map1);

        final ContextDefinition expanded2 = expanded1.expand(Definition2.class.getAnnotations());
        final ContextDefinition accepted2 = expanded2.accept(ConsumerAll.class);

        assert expanded2 == expanded1;
        assert accepted2 == expanded2;
        assert !accepted2.isEmpty();

        final Map<Class<? extends Annotation>, Annotation[]> map2 = accepted2.active();

        assert !map2.isEmpty() : AnnotationMaps.toString(map2);
        check(Accumulated.class, map2, "accumulated-1", "accumulated-2");
        check(Inherited.class, map2, "inherited-2");
        check(Immediate.class, map2, "immediate-2");
        check(None.class, map2);

        final ContextDefinition expanded3 = expanded2.expand(Definition2.class.getAnnotations());
        final ContextDefinition accepted3 = expanded3.accept(ConsumerAll.class);

        assert expanded3 == expanded2;
        assert accepted3 == expanded3;
        assert !accepted3.isEmpty();

        final Map<Class<? extends Annotation>, Annotation[]> map3 = accepted3.active();

        assert !map3.isEmpty() : AnnotationMaps.toString(map3);
        check(Accumulated.class, map3, "accumulated-1", "accumulated-2");
        check(Inherited.class, map3, "inherited-2");
        check(Immediate.class, map3, "immediate-2");
        check(None.class, map3);

        final ContextDefinition expanded4 = expanded3.advance(ConsumerAll.class).expand(Definition3.class.getAnnotations());
        final ContextDefinition accepted4 = expanded4.accept(ConsumerAll.class);

        assert expanded4 != expanded2;
        assert accepted4 == expanded4;
        assert !accepted4.isEmpty();

        final Map<Class<? extends Annotation>, Annotation[]> map4 = accepted4.active();

        assert !map4.isEmpty() : AnnotationMaps.toString(map4);
        check(Accumulated.class, map4, "accumulated-1", "accumulated-2");
        check(Inherited.class, map4, "inherited-2");
        check(Immediate.class, map4);
        check(None.class, map4);
    }

    @SuppressWarnings("ConstantConditions")
    private void check(final Class<? extends Annotation> type, final Map<Class<? extends Annotation>, Annotation[]> map, final String... values)
            throws Exception {
        assert values.length > 0 ? map.containsKey(type) : !map.containsKey(type) : AnnotationMaps.toString(map);
        final Annotation[] list = map.get(type);
        assert values.length > 0 ? list.length == values.length : list == null;

        for (int i = 0, length = values.length; i < length; i++) {
            assert values[i].equals(type.getMethod("value").invoke(list[i]));
        }
    }

    @Test
    public void testAnnotationOrder() throws Exception {
        final Map<Class<? extends Annotation>, Annotation[]> map1 = new ContextDefinitionImpl()
                .expand(Definition4.class.getAnnotations())
                .expand(Definition4.class.getField("field").getAnnotations())
                .expand(Definition5.class.getField("field").getAnnotations())
                .expand(Definition6.class.getField("field").getAnnotations())
                .expand(Definition7.class.getAnnotations())
                .expand(Definition7.class.getField("field").getAnnotations())
                .expand(Definition8.class.getField("field").getAnnotations())
                .expand(Definition9.class.getField("field").getAnnotations())
                .defined();

        assert new ArrayList<Class>(map1.keySet()).equals(Arrays.asList((Class) Annotation5.class,
                                                                        (Class) Annotation4.class,
                                                                        (Class) Annotation3.class,
                                                                        (Class) Annotation2.class,
                                                                        (Class) Annotation1.class)) : map1.keySet();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Component.Context(collect = Component.Context.Collection.ALL)
    public @interface Accumulated {

        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Component.Context(collect = Component.Context.Collection.LAST)
    public @interface Inherited {

        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Component.Context(collect = Component.Context.Collection.IMMEDIATE)
    public @interface Immediate {

        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Component.Context(collect = Component.Context.Collection.NONE)
    public @interface None {

        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Component.Context(collect = Component.Context.Collection.ALL)
    public @interface Annotation1 { }

    @Retention(RetentionPolicy.RUNTIME)
    @Component.Context(collect = Component.Context.Collection.ALL)
    public @interface Annotation2 { }

    @Retention(RetentionPolicy.RUNTIME)
    @Component.Context(collect = Component.Context.Collection.ALL)
    public @interface Annotation3 { }

    @Retention(RetentionPolicy.RUNTIME)
    @Component.Context(collect = Component.Context.Collection.ALL)
    public @interface Annotation4 { }

    @Retention(RetentionPolicy.RUNTIME)
    @Component.Context(collect = Component.Context.Collection.ALL)
    public @interface Annotation5 { }

    @Component.Context(value = Accumulated.class)
    private static class Consumer1 { }

    @Component.Context(value = Inherited.class)
    private static class Consumer2 { }

    @Component.Context(value = Immediate.class)
    private static class Consumer3 { }

    @Component.Context(value = None.class)
    private static class Consumer4 { }

    @Component.Context(value = { Accumulated.class, Inherited.class, Immediate.class, None.class })
    private static class ConsumerAll { }

    private static class ConsumerNone { }

    @Accumulated("accumulated-1")
    @Inherited("inherited-1")
    @Immediate("immediate-1")
    @None("none-1")
    private static class Definition1 { }

    @Accumulated("accumulated-2")
    @Inherited("inherited-2")
    @Immediate("immediate-2")
    @None("none-2")
    private static class Definition2 { }

    private static class Definition3 { }

    @Annotation1
    private static class Definition4 {
        @Annotation5
        public Void field;
    }

    private static class Definition5 {
        @Annotation2
        public Void field;
    }

    private static class Definition6 {
        @Annotation1
        public Void field;
    }

    @Annotation4
    private static class Definition7 {
        @Annotation3
        public Void field;
    }

    private static class Definition8 {
        @Annotation2
        public Void field;
    }

    private static class Definition9 {
        @Annotation1
        public Void field;
    }
}
