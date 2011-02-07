/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.fluidity.foundation;

import org.testng.annotations.Test;

/**
 * @author Tibor Varga
 */
public class ExceptionsTests {

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
}
