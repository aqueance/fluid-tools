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

import org.fluidity.foundation.logging.Log;

/**
 * This is an service provider interface for Fluid Tools. For use in client code, see to the documentation of the {@link Log} class.
 * <p/>
 * Creates source-bound {@link org.fluidity.foundation.logging.Log} objects. Your implementation should hook the {@link org.fluidity.foundation.logging.Log} interface to the
 * respective log sink of the logging framework of your choice. The prevalent implementation of this interface will be looked for as a JAR Service Provider. See
 * the JAR file specification for details.
 * <p/>
 * If you use Fluid Tools composition then all you need is annotate your implementation class with org.fluidity.composition.ServiceProvider and put it in the
 * class path for it to be found and used, provided that there is no other suitable implementation in the class path. If you use more of Fluid Tools than just
 * the composition framework then you should also annotate your implementation with org.fluidity.composition.Component for components of Fluid Tools to find
 * it.
 * <p/>
 * {@link org.fluidity.foundation.logging.Log} objects returned by this factory are thread safe as long as the underlying log implementation is.
 */
public interface LogFactory {

    /**
     * Creates a log whose output is annotated by the given source class.
     *
     * @param source the class to annotate log messages with.
     *
     * @return a log sink.
     */
    Log createLog(Class<?> source);
}
