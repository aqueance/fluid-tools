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

package org.fluidity.foundation.logging;

/**
 * Log interface to use by Fluid Tools components. An instance of this component is provided to your class via dependency injection. You need to specify the
 * source of the log messages as an annotation of the dependency on this interface. For instance:
 * <pre>
 * &#64;Component
 * public class MyComponent {
 *
 *   private final Log log;
 *
 *   public MyComponent(&#64;Marker(MyComponent.class) Log log) {
 *     this.log = log;
 *   }
 * }
 * </pre>
 *
 * @author Tibor Varga
 */
public interface Log {

    boolean isTraceEnabled();

    boolean isDebugEnabled();

    boolean isInfoEnabled();

    void trace(final String message, final Object... args);

    void debug(final String message, final Object... args);

    void info(final String message, final Object... args);

    void warning(final String message, final Object... args);

    void error(final String message, final Object... args);

    void trace(final Throwable exception, final String message, final Object... args);

    void debug(final Throwable exception, final String message, final Object... args);

    void info(final Throwable exception, final String message, final Object... args);

    void warning(final Throwable exception, final String message, final Object... args);

    void error(final Throwable exception, final String message, final Object... args);

    void timer(String message, long beginMillis);
}
