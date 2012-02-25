package org.fluidity.composition.container.tests;

import java.lang.reflect.Method;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.Inject;
import org.fluidity.composition.Optional;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Methods;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public class MethodInjectionTests extends AbstractContainerTests {

    private final InjectedMethods component = mock(InjectedMethods.class);
    private final Dependency1 dependency1 = mock(Dependency1.class);
    private final Dependency2 dependency2 = mock(Dependency2.class);

    private final Method injectable = Methods.get(InjectedMethods.class, new Methods.Invoker<InjectedMethods>() {
        public void invoke(final InjectedMethods capture) throws Throwable {
            capture.explicit(null, null);
        }
    });

    private final Method explicit = Methods.get(InjectedMethods.class, new Methods.Invoker<InjectedMethods>() {
        public void invoke(final InjectedMethods capture) throws Throwable {
            capture.explicit(0, null, null, null);
        }
    });

    public MethodInjectionTests(final ArtifactFactory artifacts) {
        super(artifacts);
    }

    @Test
    public void testExplicitInjectionWithNoParameters() throws Exception {
        registry.bindInstance(dependency1, Dependency1.class);
        registry.bindInstance(dependency2, Dependency2.class);

        component.explicit(dependency1, dependency2);

        replay();
        container.invoke(component, injectable);
        verify();
    }

    @Test
    public void testExplicitInjectionWithUninitializedParameters() throws Exception {
        registry.bindInstance(dependency1, Dependency1.class);
        registry.bindInstance(dependency2, Dependency2.class);

        component.explicit(1234, "abcd", dependency1, dependency2);

        replay();
        container.invoke(component, explicit, 1234, "abcd");
        verify();
    }

    @Test
    public void testExplicitInjectionWithInitializedParameters() throws Exception {
        registry.bindInstance(dependency1, Dependency1.class);
        registry.bindInstance(dependency2, Dependency2.class);

        final Dependency1 local = localMock(Dependency1.class);
        component.explicit(1234, "abcd", local, dependency2);

        replay();
        container.invoke(component, explicit, 1234, "abcd", local);
        verify();
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*Dependency1.*")
    public void testComplainsAboutMissingMandatoryParameter() throws Exception {
        registry.bindInstance(dependency2, Dependency2.class);

        replay();
        container.invoke(component, explicit, 1234, "abcd");
        verify();
    }

    @Test
    public void testHandlesMissingOptionalParameter() throws Exception {
        registry.bindInstance(dependency1, Dependency1.class);

        component.explicit(1234, "abcd", dependency1, null);

        replay();
        container.invoke(component, explicit, 1234, "abcd");
        verify();
    }

    @Test(expectedExceptions = CheckedException.class)
    public void testExplicitInvocationException() throws Exception {
        registry.bindInstance(dependency1, Dependency1.class);
        registry.bindInstance(dependency2, Dependency2.class);

        component.explicit(dependency1, dependency2);
        EasyMock.expectLastCall().andThrow(new CheckedException());

        replay();
        try {
            Exceptions.wrap(new Exceptions.Command<Void>() {
                public Void run() throws Throwable {
                    container.invoke(component, injectable);
                    return null;
                }
            });
        } catch (Exceptions.Wrapper e) {
            throw e.rethrow(CheckedException.class);
        }
        verify();
    }

    @Test
    public void testComponentCompletion() throws Exception {
        registry.bindInstance(dependency1, Dependency1.class);
        registry.bindInstance(dependency2, Dependency2.class);

        final InjectedMethods completed = container.complete(component, InjectedMethods.class);

        // no parameter is @Inject-ed
        component.explicit(1234, "abcd", null, null);
        EasyMock.expectLastCall().times(2);

        replay();
        completed.explicit(1234, "abcd", null, null);
        completed.explicit(1234, "abcd", null, null);
        verify();

        // last two parameters are @Inject-ed
        component.implicit(1234, "abcd", dependency1, dependency2);
        EasyMock.expectLastCall().times(2);

        replay();
        completed.implicit(1234, "abcd", null, null);
        completed.implicit(1234, "abcd", null, null);
        verify();
    }

    @Test(expectedExceptions = CheckedException.class)
    public void testCompletedComponentException() throws Exception {
        registry.bindInstance(dependency1, Dependency1.class);
        registry.bindInstance(dependency2, Dependency2.class);

        final InjectedMethods completed = container.complete(component, InjectedMethods.class);

        component.explicit(1234, "abcd", null, null);
        EasyMock.expectLastCall().andThrow(new CheckedException());

        replay();
        completed.explicit(1234, "abcd", null, null);
        verify();
    }

    private interface Dependency1 { }

    private interface Dependency2 { }

    private interface InjectedMethods {

        void explicit(Dependency1 injected1, @Optional Dependency2 injected2) throws CheckedException;

        void explicit(int number, String text, Dependency1 injected1, @Optional Dependency2 injected2) throws CheckedException;

        void implicit(int number, String text, @Inject Dependency1 injected1, @Inject @Optional Dependency2 injected2) throws CheckedException;
    }

    private static class CheckedException extends Exception { }
}
