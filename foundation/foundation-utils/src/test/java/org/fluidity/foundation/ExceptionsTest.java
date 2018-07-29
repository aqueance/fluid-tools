/*
 * Copyright (c) 2006-2018 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.foundation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.PrivilegedActionException;
import java.util.Objects;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ExceptionsTest {

    @Test
    public void testReturning() throws Exception {
        final int original = 1234;

        final Integer result = Exceptions.wrap(() -> original);

        assert result == original : result;
    }

    @Test
    public void testWrapping() throws Exception {
        final Exception original = new Exception();

        try {
            try {
                Exceptions.wrap(() -> {
                    throw original;
                });
            } catch (final Exceptions.Wrapper e) {

                // should wrap what we threw from inside
                assert e.getCause() == original : e.getCause();

                try {

                    // should return itself and we throw that
                    throw e.rethrow(RuntimeException.class);
                } catch (final Exceptions.Wrapper w) {

                    // should have caught the same exception
                    assert e == w : e;

                    // throws the wrapped exception
                    e.rethrow(Exception.class);

                    assert false : e;
                } catch (final Throwable w) {

                    // should not have come here
                    assert false : w;
                }
            } catch (final Throwable e) {

                // should not have come here
                assert false : e;
            }
        } catch (final Exception e) {

            // should receive the original exception
            assert e == original : e;
        }
    }

    @DataProvider(name = "exceptions")
    public Object[][] exceptions() throws Exception {
        return new Object[][] {
                new Object[] { new Exception() },
                new Object[] { new RuntimeException() },
        };
    }

    @Test(expectedExceptions = Exception.class, expectedExceptionsMessageRegExp = "Gotcha")
    public void testCommonWrapper() throws Exception {
        try {
            Exceptions.wrap(() -> {
                throw new PrivilegedActionException(new Exception("Gotcha"));
            });
        } catch (final Exceptions.Wrapper wrapper) {
            throw wrapper.rethrow(Exception.class);
        }
    }

    @Test(dataProvider = "exceptions")
    public void testUnwrapping(final Exception original) {
        final UndeclaredThrowableException wrapped =
                new UndeclaredThrowableException(
                        new InvocationTargetException(
                                new Exceptions.Wrapper(
                                        new InvocationTargetException(
                                                new RuntimeException(
                                                        new UndeclaredThrowableException(
                                                                new InvocationTargetException(original)))))));

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        final Throwable unwrapped = Exceptions.unwrap(wrapped);
        assert unwrapped == original : unwrapped;

        try {
            Exceptions.wrap(() -> Exceptions.wrap(() -> {
                throw wrapped;
            }));
        } catch (final Exceptions.Wrapper e) {
            assert e.getCause() == original : e.getCause();
        } catch (final RuntimeException e) {
            assert e == original : e;
        }
    }

    @Test(dataProvider = "exceptions")
    public void testUnknownWrapper(final Exception original) throws Exception {

        class MyWrapper extends Exception {

            private MyWrapper(final Throwable cause) {
                super(cause);
            }
        }

        final UndeclaredThrowableException wrapped =
                new UndeclaredThrowableException(
                        new InvocationTargetException(
                                new Exceptions.Wrapper(
                                        new InvocationTargetException(
                                                new MyWrapper(
                                                        new UndeclaredThrowableException(
                                                                new InvocationTargetException(original)))))));

        @SuppressWarnings("unchecked")
        final Exceptions.Tunnel tunnel = Exceptions.checked(MyWrapper.class);

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        final Throwable unwrapped = tunnel.unwrap(wrapped);
        assert unwrapped == original : unwrapped;

        try {
            tunnel.wrap(() -> {
                throw wrapped;
            });
        } catch (final Exceptions.Wrapper e) {
            assert e.getCause() == original : e.getCause();
        } catch (final RuntimeException e) {
            assert e == original : e;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class CustomWrapper extends RuntimeException {

        CustomWrapper(final Throwable cause) {
            super(cause);
        }

        CustomWrapper(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    @Test(dataProvider = "exceptions")
    public void testLabels(final Exception original) throws Exception {
        final UndeclaredThrowableException wrapped =
                new UndeclaredThrowableException(
                        new InvocationTargetException(
                                new Exceptions.Wrapper(
                                        new InvocationTargetException(
                                                new RuntimeException(
                                                        new UndeclaredThrowableException(
                                                                new InvocationTargetException(original)))))));

        final Deferred.Label message = Deferred.label("testing");

        try {
            Exceptions.wrap(null, CustomWrapper.class, () -> {
                throw wrapped;
            });
        } catch (final CustomWrapper e) {
            assert e.getCause() == original : e.getCause();
        } catch (final RuntimeException e) {
            assert e == original : e;
        }

        try {
            Exceptions.wrap(message, CustomWrapper.class, () -> {
                throw wrapped;
            });
        } catch (final CustomWrapper e) {
            assert e.getCause() == original : e.getCause();
            assert Objects.equals(message.toString(), e.getMessage()) : e.getMessage();
        } catch (final RuntimeException e) {
            assert e == original : e;
        }

        try {
            Exceptions.wrap(message, null, () -> {
                throw wrapped;
            });
        } catch (final Exceptions.Wrapper e) {
            assert e.getCause() == original : e.getCause();
            assert Objects.equals(message.toString(), e.getMessage()) : e.getMessage();
        } catch (final RuntimeException e) {
            assert e == original : e;
        }
    }

    @Test(dataProvider = "exceptions")
    public void testCheckedWrapper(final Exception original) throws Exception {
        try {
            Exceptions.wrap(CheckedWrapper.class, () -> {
                throw original;
            });
        } catch (final CheckedWrapper e) {
            assert e.getCause() == original : e.getCause().getClass();
        }

        final CheckedWrapper wrapped1 = new CheckedWrapper(original);

        try {
            Exceptions.wrap(CheckedWrapper.class, () -> {
                throw wrapped1;
            });
        } catch (final CheckedWrapper e) {
            assert e == wrapped1 : e.getClass();
            assert e.getCause() == original : e.getCause().getClass();
        }

        final String message = "message";

        try {
            Exceptions.wrap(message, CheckedWrapper.class, () -> {
                throw wrapped1;
            });
        } catch (final CheckedWrapper e) {
            assert e != wrapped1 : e.getClass();
            assert message.equals(e.getMessage()) : e.getMessage();
            assert e.getCause() == wrapped1 : e.getCause().getClass();
        }

        final CheckedWrapper wrapped2 = new CheckedWrapper(message, original);

        try {
            Exceptions.wrap(message, CheckedWrapper.class, () -> {
                throw wrapped2;
            });
        } catch (final CheckedWrapper e) {
            assert e == wrapped2;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    private static class CheckedWrapper extends Exception {

        private CheckedWrapper(final Throwable cause) {
            super(cause);
        }

        private CheckedWrapper(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
