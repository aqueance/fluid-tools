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
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.container.ContextDefinition;
import org.fluidity.composition.spi.ComponentInterceptor;
import org.fluidity.tests.MockGroupAbstractTest;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class InterceptorFilterImplTest extends MockGroupAbstractTest {

    private final ContextDefinition context = mock(ContextDefinition.class);
    private final ContextDefinition copy = mock(ContextDefinition.class);
    private final ContextDefinition accepted = mock(ContextDefinition.class);

    private final ComponentInterceptor[] interceptors = {
            new ComponentInterceptor1(),
            new ComponentInterceptor2(),
            new ComponentInterceptor3(),
            new ComponentInterceptor4(),
            new ComponentInterceptor5(),
    };

    private final Annotation1 annotation1 = mock(Annotation1.class);
    private final Annotation2 annotation2 = mock(Annotation2.class);
    private final Annotation3 annotation3 = mock(Annotation3.class);
    private final Annotation4 annotation4 = mock(Annotation4.class);
    private final Annotation5 annotation5 = mock(Annotation5.class);

    private final InterceptorFilter filter = new InterceptorFilterImpl();

    @Test
    public void testNoDescriptors() throws Exception {
        replay();
        assert filter.filter(context, null) == null;
        verify();
    }

    @Test
    public void testNoAnnotation() throws Exception {
        final HashMap<Class<? extends Annotation>, Annotation[]> annotations = new HashMap<Class<? extends Annotation>, Annotation[]>();

        EasyMock.expect(context.defined()).andReturn(annotations);

        filter(annotations);

        replay();
        final ComponentInterceptor[] list = filter.filter(context, interceptors);
        verify();

        assert list != null;
        assert list.length == 1 : list.length;
        assert list[0] == interceptors[4] : list[0].getClass().getSimpleName();
    }

    @Test
    public void test1() throws Exception {
        final LinkedHashMap<Class<? extends Annotation>, Annotation[]> annotations = new LinkedHashMap<Class<? extends Annotation>, Annotation[]>();

        annotations.put(Annotation1.class, new Annotation[] { annotation1 });
        annotations.put(Annotation2.class, new Annotation[] { annotation2 });

        EasyMock.expect(context.defined()).andReturn(annotations);

        filter(annotations);

        replay();
        final ComponentInterceptor[] list = filter.filter(context, interceptors);
        verify();

        assert list != null;
        assert list.length == 2 : list.length;
        assert list[0] == interceptors[1] : list[0].getClass().getSimpleName();
        assert list[1] == interceptors[4] : list[1].getClass().getSimpleName();
    }

    @Test
    public void test2() throws Exception {
        final LinkedHashMap<Class<? extends Annotation>, Annotation[]> annotations = new LinkedHashMap<Class<? extends Annotation>, Annotation[]>();

        annotations.put(Annotation1.class, new Annotation[] { annotation1 });
        annotations.put(Annotation5.class, new Annotation[] { annotation5 });
        annotations.put(Annotation2.class, new Annotation[] { annotation2 });

        EasyMock.expect(context.defined()).andReturn(annotations);

        filter(annotations);

        replay();
        ComponentInterceptor[] list = filter.filter(context, interceptors);
        verify();

        assert list != null;
        assert list.length == 3 : list.length;
        assert list[0] == interceptors[1] : list[0].getClass().getSimpleName(); // @Annotation2 is "closer" than the rest
        assert list[1] == interceptors[0] : list[1].getClass().getSimpleName();
        assert list[2] == interceptors[4] : list[2].getClass().getSimpleName(); // interceptor with missing context is always last

        annotations.clear();
        annotations.put(Annotation2.class, new Annotation[] { annotation2 });
        annotations.put(Annotation1.class, new Annotation[] { annotation1 });
        annotations.put(Annotation5.class, new Annotation[] { annotation5 });

        EasyMock.expect(context.defined()).andReturn(annotations);

        filter(annotations);

        replay();
        list = filter.filter(context, interceptors);
        verify();

        assert list != null;
        assert list.length == 3 : list.length;
        assert list[0] == interceptors[0] : list[0].getClass().getSimpleName(); // @Annotation2 is "farther" than the rest
        assert list[1] == interceptors[1] : list[1].getClass().getSimpleName();
        assert list[2] == interceptors[4] : list[2].getClass().getSimpleName(); // interceptor with missing context is always last
    }

    @Test
    public void testAll() throws Exception {
        final LinkedHashMap<Class<? extends Annotation>, Annotation[]> annotations = new LinkedHashMap<Class<? extends Annotation>, Annotation[]>();

        annotations.put(Annotation1.class, new Annotation[] { annotation1 });
        annotations.put(Annotation2.class, new Annotation[] { annotation2 });
        annotations.put(Annotation3.class, new Annotation[] { annotation3 });
        annotations.put(Annotation4.class, new Annotation[] { annotation4 });
        annotations.put(Annotation5.class, new Annotation[] { annotation5 });

        EasyMock.expect(context.defined()).andReturn(annotations);

        filter(annotations);

        replay();
        ComponentInterceptor[] list = filter.filter(context, interceptors);
        verify();

        assert list != null;
        assert list.length == 5 : list.length;
        assert list[0] == interceptors[0] : list[0].getClass().getSimpleName(); // @Annotation5 is "closest"
        assert list[1] == interceptors[3] : list[1].getClass().getSimpleName();
        assert list[2] == interceptors[2] : list[2].getClass().getSimpleName();
        assert list[3] == interceptors[1] : list[3].getClass().getSimpleName();
        assert list[4] == interceptors[4] : list[4].getClass().getSimpleName();

        annotations.clear();
        annotations.put(Annotation1.class, new Annotation[] { annotation1 });
        annotations.put(Annotation5.class, new Annotation[] { annotation5 });
        annotations.put(Annotation2.class, new Annotation[] { annotation2 });
        annotations.put(Annotation3.class, new Annotation[] { annotation3 });
        annotations.put(Annotation4.class, new Annotation[] { annotation4 });

        EasyMock.expect(context.defined()).andReturn(annotations);

        filter(annotations);

        replay();
        list = filter.filter(context, interceptors);
        verify();

        assert list != null;
        assert list.length == 5 : list.length;
        assert list[0] == interceptors[3] : list[0].getClass().getSimpleName();
        assert list[1] == interceptors[2] : list[1].getClass().getSimpleName();
        assert list[2] == interceptors[1] : list[2].getClass().getSimpleName();
        assert list[3] == interceptors[0] : list[3].getClass().getSimpleName();
        assert list[4] == interceptors[4] : list[4].getClass().getSimpleName();
    }

    private void filter(final Map<Class<? extends Annotation>, Annotation[]> annotations) {
        for (final ComponentInterceptor interceptor : interceptors) {
            final Class<? extends ComponentInterceptor> type = interceptor.getClass();

            EasyMock.expect(context.copy()).andReturn(copy);
            EasyMock.expect(copy.accept(type)).andReturn(accepted);

            final Map<Class<? extends Annotation>, Annotation[]> active = new HashMap<Class<? extends Annotation>, Annotation[]>(annotations);

            final Component.Context specified = type.getAnnotation(Component.Context.class);
            if (specified != null) {
                active.keySet().retainAll(Arrays.asList(specified.value()));
            } else {
                active.clear();
            }

            EasyMock.expect(accepted.active()).andReturn(active);
        }
    }

    private static abstract class EmptyInterceptor implements ComponentInterceptor {

        public Dependency intercept(final Type reference, final ComponentContext context, final Dependency dependency) {
            throw new UnsupportedOperationException("should not have been invoked");
        }
    }

    @Component.Context({ Annotation1.class, Annotation5.class })
    private static class ComponentInterceptor1 extends EmptyInterceptor {}

    @Component.Context(Annotation2.class)
    private static class ComponentInterceptor2 extends EmptyInterceptor {}

    @Component.Context(Annotation3.class)
    private static class ComponentInterceptor3 extends EmptyInterceptor {}

    @Component.Context(Annotation4.class)
    private static class ComponentInterceptor4 extends EmptyInterceptor {}

    private static class ComponentInterceptor5 extends EmptyInterceptor {}

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Annotation1 {}

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Annotation2 {}

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Annotation3 {}

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Annotation4 {}

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Annotation5 {}
}
