/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ExceptionsTest {

    @Test
    public void testReturning() throws Exception {
        final int original = 1234;

        final Integer result = Exceptions.wrap(new Exceptions.Command<Integer>() {
            public Integer run() throws Exception {
                return original;
            }
        });

        assert result == original : result;
    }

    @Test
    public void testWrapping() throws Exception {
        final Exception original = new Exception();

        try {
            try {
                Exceptions.wrap(new Exceptions.Command<Void>() {
                    public Void run() throws Exception {
                        throw original;
                    }
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

    @Test
    public void testUnwrapping() throws Exception {
        final Exception original = new Exception();

        try {
            Exceptions.wrap(new Exceptions.Command<Object>() {
                public Object run() throws Exception {
                    throw new UndeclaredThrowableException(new InvocationTargetException(new RuntimeException(new InvocationTargetException(new UndeclaredThrowableException(original)))));
                }
            });
        } catch (final Exceptions.Wrapper e) {
            try {
                e.rethrow(Exception.class);
            } catch (final Exception thrown) {
                assert thrown == original : thrown;
            }
        }
    }
}
