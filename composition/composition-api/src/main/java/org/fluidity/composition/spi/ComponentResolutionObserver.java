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

package org.fluidity.composition.spi;

/**
 * Observes graph node resolutions.
 *
 * @author Tibor Varga
 */
public interface ComponentResolutionObserver {

    /**
     * Invoked for each resolved graph node. The path and type are not final, they may change as circular references are handled.
     *
     * @param path the dependency path at which the given type has been resolved.
     * @param type the type that has been resolved at the given dependency path.
     */
    void resolved(DependencyPath path, Class<?> type);

    /**
     * Invoked for each instantiated graph node. The path and type are final. The receiver <em>must not</em> call any method on the just instantiated component
     * other than those of {@link Object}.
     *
     * @param path      the dependency path at which the given type has been instantiated. Does not yet include <code>type</code>.
     *
     */
    void instantiated(DependencyPath path);
}
