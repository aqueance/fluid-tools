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

package org.fluidity.composition;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.atomic.AtomicInteger;

import org.fluidity.foundation.Deferred;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class DeferredDependencyTest {

    private final ComponentContainer container = Containers.global();

    @Test
    public void testPlainDependency() throws Exception {
        container.instantiate(PlainDependent.class);
    }

    @Test
    public void testContextDependency() throws Exception {
        container.instantiate(ContextDependent.class);
    }

    @Test
    public void testReferenceDependency() throws Exception {
        container.instantiate(ReferenceDependent.class);
    }

    @Test
    public void testAnnotatedDependency() throws Exception {
        AnnotatedDependencyImpl.instances.set(0);
        container.instantiate(AnnotatedDependent1.class);

        AnnotatedDependencyImpl.instances.set(0);
        container.instantiate(AnnotatedDependent2.class);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Data {

        String value();
    }

    @Component
    private static class PlainDependency {}

    @Component
    @Component.Context(Data.class)
    private static class ContextDependency {

        public final ComponentContext context;

        @SuppressWarnings("UnusedDeclaration")
        private ContextDependency(final ComponentContext context) {
            this.context = context;
        }
    }

    @Component
    @Component.Context(Component.Reference.class)
    @SuppressWarnings("UnusedDeclaration")
    private static class ReferenceDependency<T> {

        @Inject
        public ComponentContext context;
    }

    @Component(automatic = false)
    private static class PlainDependent {

        public PlainDependent(final Deferred.Reference<PlainDependency> deferred, final PlainDependency instance) {
            assert deferred != null;
            assert !deferred.resolved();
            assert deferred.get() == instance;
        }
    }

    @Component(automatic = false)
    private static class ContextDependent {

        public ContextDependent(final @Data("1") Deferred.Reference<ContextDependency> deferred1,
                                final @Data("2") Deferred.Reference<ContextDependency> deferred2,
                                final @Data("1") ContextDependency instance1,
                                final @Data("2") ContextDependency instance2) {
            assert deferred1 != null;
            assert deferred2 != null;

            assert !deferred1.resolved();
            assert !deferred2.resolved();

            assert deferred1.get() == instance1;
            assert deferred2.get() == instance2;
            assert deferred1.get() != deferred2.get();

            assert "1".equals(deferred1.get().context.annotation(Data.class, null).value());
            assert "1".equals(instance1.context.annotation(Data.class, null).value());

            assert "2".equals(deferred2.get().context.annotation(Data.class, null).value());
            assert "2".equals(instance2.context.annotation(Data.class, null).value());
        }
    }

    @Component(automatic = false)
    private static class ReferenceDependent {

        public ReferenceDependent(final Deferred.Reference<ReferenceDependency<String>> deferred1,
                                  final Deferred.Reference<ReferenceDependency<Long>> deferred2,
                                  final ReferenceDependency<String> instance1,
                                  final ReferenceDependency<Long> instance2) {
            assert deferred1 != null;
            assert deferred2 != null;

            assert !deferred1.resolved();
            assert !deferred2.resolved();

            assert deferred1.get() == instance1;
            assert deferred2.get() == instance2;
            assert (ReferenceDependency) deferred1.get() != deferred2.get();

            assert String.class.equals(deferred1.get().context.annotation(Component.Reference.class, null).parameter(0));
            assert String.class.equals(instance1.context.annotation(Component.Reference.class, null).parameter(0));

            assert Long.class.equals(deferred2.get().context.annotation(Component.Reference.class, null).parameter(0));
            assert Long.class.equals(instance2.context.annotation(Component.Reference.class, null).parameter(0));
        }
    }

    private interface AnnotatedDependency {

        ComponentContext context();

        AnnotatedDependency instance();
    }

    @Component
    @Component.Context(Data.class)
    @SuppressWarnings("UnusedDeclaration")
    private static class AnnotatedDependencyImpl implements AnnotatedDependency {

        public static AtomicInteger instances = new AtomicInteger(0);

        public final ComponentContext context;

        private AnnotatedDependencyImpl(final ComponentContext context) {
            this.context = context;
            instances.incrementAndGet();
        }

        public ComponentContext context() {
            return context;
        }

        public AnnotatedDependency instance() {
            return this;
        }
    }

    @Component(automatic = false)
    private static class AnnotatedDependent1 {

        public AnnotatedDependent1(final @Data("1") @Defer AnnotatedDependency deferred1,
                                   final @Data("2") @Defer AnnotatedDependency deferred2,
                                   final @Data("1") AnnotatedDependency instance1,
                                   final @Data("2") AnnotatedDependency instance2) {
            assert deferred1 != null;
            assert deferred2 != null;

            assert AnnotatedDependencyImpl.instances.get() == 2 : AnnotatedDependencyImpl.instances.get();

            assert deferred1.instance() == instance1;

            assert AnnotatedDependencyImpl.instances.get() == 2 : AnnotatedDependencyImpl.instances.get();

            assert deferred2.instance() == instance2;

            assert AnnotatedDependencyImpl.instances.get() == 2 : AnnotatedDependencyImpl.instances.get();

            assert deferred1.instance() != deferred2.instance();

            assert AnnotatedDependencyImpl.instances.get() == 2 : AnnotatedDependencyImpl.instances.get();

            assert "1".equals(deferred1.context().annotation(Data.class, null).value());
            assert "1".equals(instance1.context().annotation(Data.class, null).value());

            assert "2".equals(deferred2.context().annotation(Data.class, null).value());
            assert "2".equals(instance2.context().annotation(Data.class, null).value());

            assert AnnotatedDependencyImpl.instances.get() == 2 : AnnotatedDependencyImpl.instances.get();
        }
    }

    @Component(automatic = false)
    private static class AnnotatedDependent2 {

        public AnnotatedDependent2(final @Data("3") @Defer AnnotatedDependency deferred1,
                                   final @Data("4") @Defer AnnotatedDependency deferred2) {
            assert deferred1 != null;
            assert deferred2 != null;

            assert AnnotatedDependencyImpl.instances.get() == 0 : AnnotatedDependencyImpl.instances.get();

            assert deferred1.instance() != deferred2.instance();

            assert AnnotatedDependencyImpl.instances.get() == 2 : AnnotatedDependencyImpl.instances.get();

            assert "3".equals(deferred1.context().annotation(Data.class, null).value());
            assert "4".equals(deferred2.context().annotation(Data.class, null).value());

            assert AnnotatedDependencyImpl.instances.get() == 2 : AnnotatedDependencyImpl.instances.get();
        }
    }
}
