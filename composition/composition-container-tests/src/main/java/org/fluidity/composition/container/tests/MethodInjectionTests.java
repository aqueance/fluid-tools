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

import java.lang.reflect.Method;

import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.Inject;
import org.fluidity.composition.Optional;
import org.fluidity.foundation.Exceptions;
import org.fluidity.foundation.Methods;

import org.easymock.EasyMock;
import org.testng.annotations.Test;

import static org.fluidity.foundation.Command.Job;

/**
 * @author Tibor Varga
 */
@SuppressWarnings("unchecked")
public class MethodInjectionTests extends AbstractContainerTests {

    private final InjectedMethods component = mock(InjectedMethods.class);
    private final Dependency1 dependency1 = mock(Dependency1.class);
    private final Dependency2 dependency2 = mock(Dependency2.class);

    private final Method[] methods = Methods.get(InjectedMethods.class, new Methods.Invoker<InjectedMethods>() {
        public void invoke(final InjectedMethods capture) throws Throwable {
            capture.explicit(null, null);
            capture.explicit(0, null, null, null);
        }
    });

    private final Method injectable = methods[0];
    private final Method explicit = methods[1];

    public MethodInjectionTests(final ArtifactFactory artifacts) {
        super(artifacts);
    }

    @Test
    public void testExplicitInjectionWithNoParameters() throws Exception {
        registry.bindInstance(dependency1, Dependency1.class);
        registry.bindInstance(dependency2, Dependency2.class);

        component.explicit(dependency1, dependency2);

        verify(new Task() {
            public void run() throws Exception {
                container.invoke(component, injectable);
            }
        });
    }

    @Test
    public void testExplicitInjectionWithUninitializedParameters() throws Exception {
        registry.bindInstance(dependency1, Dependency1.class);
        registry.bindInstance(dependency2, Dependency2.class);

        component.explicit(1234, "abcd", dependency1, dependency2);

        verify(new Task() {
            public void run() throws Exception {
                container.invoke(component, explicit, 1234, "abcd");
            }
        });
    }

    @Test
    public void testExplicitInjectionWithInitializedParameters() throws Exception {
        registry.bindInstance(dependency1, Dependency1.class);
        registry.bindInstance(dependency2, Dependency2.class);

        final Dependency1 local = localMock(Dependency1.class);
        component.explicit(1234, "abcd", local, dependency2);

        verify(new Task() {
            public void run() throws Exception {
                container.invoke(component, explicit, 1234, "abcd", local);
            }
        });
    }

    @Test(expectedExceptions = ComponentContainer.ResolutionException.class, expectedExceptionsMessageRegExp = ".*Dependency1.*")
    public void testComplainsAboutMissingMandatoryParameter() throws Exception {
        registry.bindInstance(dependency2, Dependency2.class);

        verify(new Task() {
            public void run() throws Exception {
                container.invoke(component, explicit, 1234, "abcd");
            }
        });
    }

    @Test
    public void testHandlesMissingOptionalParameter() throws Exception {
        registry.bindInstance(dependency1, Dependency1.class);

        component.explicit(1234, "abcd", dependency1, null);

        verify(new Task() {
            public void run() throws Exception {
                container.invoke(component, explicit, 1234, "abcd");
            }
        });
    }

    @Test(expectedExceptions = CheckedException.class)
    public void testExplicitInvocationException() throws Exception {
        registry.bindInstance(dependency1, Dependency1.class);
        registry.bindInstance(dependency2, Dependency2.class);

        component.explicit(dependency1, dependency2);
        EasyMock.expectLastCall().andThrow(new CheckedException());

        guarantee(new Task() {
            public void run() throws Exception {
                try {
                    Exceptions.wrap(new Job<Exception>() {
                        public void run() throws Exception {
                            container.invoke(component, injectable);
                        }
                    });
                } catch (Exceptions.Wrapper e) {
                    throw e.rethrow(CheckedException.class);
                }
            }
        });
    }

    @Test
    public void testComponentCompletion() throws Exception {
        registry.bindInstance(dependency1, Dependency1.class);
        registry.bindInstance(dependency2, Dependency2.class);

        final InjectedMethods completed = container.complete(component, InjectedMethods.class);

        // no parameter is @Inject-ed
        test(new Task() {
            public void run() throws Exception {
                component.explicit(1234, "abcd", null, null);
                EasyMock.expectLastCall().times(2);

                verify(new Task() {
                    public void run() throws Exception {
                        completed.explicit(1234, "abcd", null, null);
                        completed.explicit(1234, "abcd", null, null);
                    }
                });
            }
        });

        // last two parameters are @Inject-ed
        test(new Task() {
            public void run() throws Exception {
                component.implicit(1234, "abcd", dependency1, dependency2);
                EasyMock.expectLastCall().times(2);

                verify(new Task() {
                    public void run() throws Exception {
                        completed.implicit(1234, "abcd", null, null);
                        completed.implicit(1234, "abcd", null, null);
                    }
                });
            }
        });
    }

    @Test(expectedExceptions = CheckedException.class)
    public void testCompletedComponentException() throws Exception {
        registry.bindInstance(dependency1, Dependency1.class);
        registry.bindInstance(dependency2, Dependency2.class);

        final InjectedMethods completed = container.complete(component, InjectedMethods.class);

        component.explicit(1234, "abcd", null, null);
        EasyMock.expectLastCall().andThrow(new CheckedException());

        verify(new Task() {
            public void run() throws Exception {
                completed.explicit(1234, "abcd", null, null);
            }
        });
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
