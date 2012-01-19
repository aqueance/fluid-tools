package org.fluidity.foundation.tests;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.ContainerBoundary;
import org.fluidity.composition.Inject;
import org.fluidity.foundation.Deferred;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class DeferredDependencyTest {

    private final ComponentContainer container = new ContainerBoundary();

    @Test
    public void testPlainDependency() throws Exception {
        assert container.getComponent(PlainDependent.class) != null;
    }

    @Test
    public void testContextDependency() throws Exception {
        assert container.getComponent(ContextDependent.class) != null;
    }

    @Test(enabled = false)
    public void testReferenceDependency() throws Exception {
        assert container.getComponent(ReferenceDependent.class) != null;
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

    @Component
    private static class PlainDependent {

        public PlainDependent(final Deferred.Reference<PlainDependency> deferred, final PlainDependency instance) {
            assert deferred != null;
            assert !deferred.resolved();
            assert deferred.get() == instance;
        }
    }

    @Component
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

    @Component
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
            assert (ReferenceDependency) deferred1.get() != (ReferenceDependency) deferred2.get();

            assert String.class.equals(deferred1.get().context.annotation(Component.Reference.class, null).parameter(0));
            assert String.class.equals(instance1.context.annotation(Component.Reference.class, null).parameter(0));

            assert Long.class.equals(deferred2.get().context.annotation(Component.Reference.class, null).parameter(0));
            assert Long.class.equals(instance2.context.annotation(Component.Reference.class, null).parameter(0));
        }
    }
}
