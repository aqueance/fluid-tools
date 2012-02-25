package org.fluidity.composition.container.impl;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import org.fluidity.composition.Component;
import org.fluidity.composition.container.api.ContextDefinition;
import org.fluidity.tests.MockGroupAbstractTest;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ContextDefinitionImplTest extends MockGroupAbstractTest {

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
}
