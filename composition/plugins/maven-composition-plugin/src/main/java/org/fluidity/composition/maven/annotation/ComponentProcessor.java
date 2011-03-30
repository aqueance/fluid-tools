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

package org.fluidity.composition.maven.annotation;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Processes a {@link org.fluidity.composition.Component} annotation.
 *
 * @author Tibor Varga
 */
public final class ComponentProcessor extends EmptyVisitor {

    private static final String ATR_AUTOMATIC = "automatic";

    private final ProcessorCallback<ComponentProcessor> callback;

    private boolean automatic = true;

    public ComponentProcessor(final ProcessorCallback<ComponentProcessor> callback) {
        this.callback = callback;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        return null;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, final boolean visible) {
        return null;
    }

    @Override
    public AnnotationVisitor visitArray(final String desc) {
        return null;
    }

    @Override
    public void visit(final String name, final Object value) {
        if (ATR_AUTOMATIC.equals(name) && !((Boolean) value)) {
            automatic = false;
        }
    }

    public boolean isAutomatic() {
        return automatic;
    }

    @Override
    public void visitEnd() {
        callback.complete(this);
    }
}
