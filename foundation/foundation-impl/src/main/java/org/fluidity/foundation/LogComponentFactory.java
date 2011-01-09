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

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentContainer;
import org.fluidity.composition.ComponentContext;
import org.fluidity.composition.Context;
import org.fluidity.composition.OpenComponentContainer;
import org.fluidity.composition.spi.ComponentFactory;
import org.fluidity.foundation.logging.Log;
import org.fluidity.foundation.logging.Source;
import org.fluidity.foundation.spi.LogFactory;

/**
 * Creates {@link org.fluidity.foundation.logging.Log} objects for the source specified by a {@link org.fluidity.foundation.logging.Source} annotation, using
 * the user provided implementation of the {@link org.fluidity.foundation.spi.LogFactory} interface.
 */
@Component(api = Log.class, type = Log.class)
@Context(Source.class)
final class LogComponentFactory implements ComponentFactory<Log> {

    private final LogFactory factory;

    public LogComponentFactory(final LogFactory factory) {
        this.factory = factory;
    }

    public Log newComponent(final OpenComponentContainer container, final ComponentContext context) throws ComponentContainer.ResolutionException {
        final Source source = context.annotation(Source.class);

        if (source == null) {
            throw new ComponentContainer.ResolutionException("Annotation %s is missing from %s dependency", Source.class, Log.class);
        }

        return factory.createLog(source.value());
    }
}
