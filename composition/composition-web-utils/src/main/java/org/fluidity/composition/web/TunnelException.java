/*
 * Copyright (c) 2006-2008 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Softweare"), to deal
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.fluidity.composition.web;

import java.io.IOException;

import javax.servlet.ServletException;

/**
 * Tunnels some checked exceptions through a <code>RuntimeException</code> and allows rethrowing them later.
 */
final class TunnelException extends RuntimeException {

    private final IOException ioException;

    private final ServletException servletException;

    /**
     * Creates a new wrapper for an <code>IOException</code>.
     *
     * @param cause is the exception to wrap.
     */
    public TunnelException(final IOException cause) {
        super(cause);
        ioException = cause;
        servletException = null;
    }

    /**
     * Creates a new wrapper for an <code>ServletException</code>.
     *
     * @param cause is the exception to wrap.
     */
    public TunnelException(final ServletException cause) {
        super(cause);
        ioException = null;
        servletException = cause;
    }

    /**
     * Throws the wrapped exception again.
     *
     * @throws IOException      the wrapped <code>IOException</code> if any.
     * @throws ServletException the wrapped <code>ServletException</code> if any.
     */
    public void rethrow() throws IOException, ServletException {
        if (ioException != null) {
            throw ioException;
        } else {
            assert servletException != null;
            throw servletException;
        }
    }
}
